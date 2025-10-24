package com.example.rsakey;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeyServer {
    private static final Logger logger = Logger.getLogger("KeyServer");

    private final ConcurrentHashMap<String, CompletableFuture<KeyEntry>> store = new ConcurrentHashMap<>();

    private final BlockingQueue<GenTask> genQueue = new LinkedBlockingQueue<>();
    private final Writer writer;
    private final PrivateKey issuerPrivateKey;
    private final X500Name issuerName;

    private final Selector selector;
    private final ServerSocketChannel serverChannel;

    private volatile boolean running = true;

    public KeyServer(int port, PrivateKey issuerPrivateKey, String issuerNameStr, int genThreadsCount) throws Exception {
        this.issuerPrivateKey = issuerPrivateKey;
        this.issuerName = new X500Name(issuerNameStr);
        this.writer = new Writer();

        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        logger.info("Server listening on port " + port);

        Thread[] genThreads = new Thread[genThreadsCount];
        for (int i = 0; i < genThreadsCount; ++i) {
            genThreads[i] = new Thread(this::genLoop, "Gen-" + i);
            genThreads[i].setDaemon(true);
            genThreads[i].start();
        }
        Thread writerThread = new Thread(writer, "Writer");
        writerThread.setDaemon(true);
        writerThread.start();
    }

    public void run() throws Exception {
        while (running) {
            selector.select();
            var it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                var key = it.next();
                it.remove();
                try {
                    if (!key.isValid()) continue;
                    if (key.isAcceptable()) accept(key);
                    else if (key.isReadable()) read(key);
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "IO error on key: " + ex.getMessage(), ex);
                    closeKeyChannel(key);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Error handling key: " + ex.getMessage(), ex);
                    closeKeyChannel(key);
                }
            }
        }
        selector.close();
        serverChannel.close();
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssc.accept();
        if (sc == null) return;
        sc.configureBlocking(false);
        sc.setOption(StandardSocketOptions.TCP_NODELAY, true);
        sc.register(selector, SelectionKey.OP_READ, new ClientContext(sc));
        logger.info("Accepted connection from " + sc.getRemoteAddress());
    }

    private void read(SelectionKey key) throws IOException {
        ClientContext ctx = (ClientContext) key.attachment();
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer buf = ctx.readBuffer;
        int r = sc.read(buf);
        if (r == -1) {
            logger.info("Client closed connection before finishing name: " + sc.getRemoteAddress());
            closeKeyChannel(key);
            return;
        }
        buf.flip();
        while (buf.hasRemaining()) {
            byte b = buf.get();
            if (b == 0) {
                int nameLen = ctx.nameBytes.size();
                byte[] nameBytes = new byte[nameLen];
                for (int i = 0; i < nameLen; ++i) nameBytes[i] = ctx.nameBytes.get(i);
                String name = new String(nameBytes, StandardCharsets.US_ASCII);
                logger.info("Received name '" + name + "' from " + sc.getRemoteAddress());
                handleNameForClient(name, sc);
                return;
            } else {
                ctx.nameBytes.add(b);
                if (ctx.nameBytes.size() > 1024 * 16) {
                    logger.warning("Name too long, closing connection");
                    closeKeyChannel(key);
                    return;
                }
            }
        }
        buf.clear();
    }

    private void closeKeyChannel(SelectionKey key) {
        try {
            key.channel().close();
        } catch (IOException ignore) {
        }
        key.cancel();
    }

    private void handleNameForClient(String name, SocketChannel sc) {
        CompletableFuture<KeyEntry> newFuture = new CompletableFuture<>();
        CompletableFuture<KeyEntry> existing = store.putIfAbsent(name, newFuture);
        final CompletableFuture<KeyEntry> fut;
        if (existing == null) {
            fut = newFuture;
            genQueue.add(new GenTask(name, newFuture));
            logger.info("Enqueued generation for '" + name + "'");
        } else {
            fut = existing;
            logger.info("Using existing future for '" + name + "'");
        }

        fut.whenComplete((ke, ex) -> {
            if (ex != null) {
                logger.log(Level.WARNING, "Generation failed for " + name + ": " + ex.getMessage(), ex);
                try {
                    sc.close();
                } catch (IOException ignore) {}
            } else {
                try {
                    byte[] keyPem = ke.privatePem.getBytes(StandardCharsets.UTF_8);
                    byte[] certPem = ke.certPem.getBytes(StandardCharsets.UTF_8);
                    ByteBuffer out = ByteBuffer.allocate(4 + keyPem.length + 4 + certPem.length);
                    out.putInt(keyPem.length);
                    out.put(keyPem);
                    out.putInt(certPem.length);
                    out.put(certPem);
                    out.flip();
                    writer.enqueue(sc, out);
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Failed to prepare send to client: " + t.getMessage(), t);
                    try {
                        sc.close();
                    } catch (IOException ignore) {}
                }
            }
        });
    }

    private void genLoop() {
        while (true) {
            try {
                GenTask task = genQueue.take();
                String name = task.name;
                CompletableFuture<KeyEntry> future = task.future;
                if (future.isDone()) continue;
                logger.info(Thread.currentThread().getName() + " generating keys for " + name);
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(8192);
                KeyPair kp = kpg.generateKeyPair();

                X500Name subject = new X500Name("CN=" + name);
                BigInteger serial = new BigInteger(64, new SecureRandom());
                Date notBefore = Date.from(Instant.now().minusSeconds(60));
                Date notAfter = Date.from(Instant.now().plusSeconds(60L * 60 * 24 * 365 * 20)); // 20 years
                JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                        issuerName, serial, notBefore, notAfter, subject, kp.getPublic()
                );
                ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(issuerPrivateKey);
                X509CertificateHolder holder = certBuilder.build(signer);
                X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);

                String pemKey = PemUtils.privateKeyToPem(kp.getPrivate());
                String pemCert = PemUtils.certToPem(cert);

                KeyEntry entry = new KeyEntry(pemKey, pemCert, kp, cert);
                future.complete(entry);
                logger.info("Generated keys for " + name);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Error in generator thread: " + t.getMessage(), t);
            }
        }
    }

    public void shutdown() throws IOException {
        running = false;
        selector.wakeup();
        serverChannel.close();
        writer.shutdown();
    }

    private static class ClientContext {
        final SocketChannel channel;
        final ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        final java.util.ArrayList<Byte> nameBytes = new java.util.ArrayList<>();

        ClientContext(SocketChannel c) {
            this.channel = c;
        }
    }

    private static class KeyEntry {
        final String privatePem;
        final String certPem;
        final KeyPair keyPair;
        final X509Certificate cert;

        KeyEntry(String privatePem, String certPem, KeyPair kp, X509Certificate cert) {
            this.privatePem = privatePem;
            this.certPem = certPem;
            this.keyPair = kp;
            this.cert = cert;
        }
    }

    private record GenTask(String name, CompletableFuture<KeyEntry> future) {
    }

    private static class Writer implements Runnable {
        private final Selector sel;
        private final ConcurrentLinkedQueue<Pending> pending = new ConcurrentLinkedQueue<>();
        private volatile boolean running = true;

        Writer() throws IOException {
            this.sel = Selector.open();
        }

        public void enqueue(SocketChannel sc, ByteBuffer data) {
            pending.offer(new Pending(sc, data));
            sel.wakeup();
        }

        public void shutdown() throws IOException {
            running = false;
            sel.wakeup();
            sel.close();
        }

        @Override
        public void run() {
            try {
                while (running) {
                    Pending p;
                    while ((p = pending.poll()) != null) {
                        try {
                            SocketChannel sc = p.channel;
                            sc.configureBlocking(false);
                            SelectionKey key = sc.keyFor(sel);
                            if (key == null) {
                                sc.register(sel, SelectionKey.OP_WRITE, p.data);
                            } else {
                                key.attach(p.data);
                                key.interestOps(SelectionKey.OP_WRITE);
                            }
                        } catch (IOException ioe) {
                            try {
                                p.channel.close();
                            } catch (IOException ignore) {
                            }
                        }
                    }

                    sel.select();
                    var it = sel.selectedKeys().iterator();
                    while (it.hasNext()) {
                        var key = it.next();
                        it.remove();
                        if (!key.isValid()) continue;
                        if (key.isWritable()) {
                            SocketChannel sc = (SocketChannel) key.channel();
                            ByteBuffer buf = (ByteBuffer) key.attachment();
                            try {
                                sc.write(buf);
                                if (!buf.hasRemaining()) {
                                    key.cancel();
                                    sc.close();
                                }
                            } catch (IOException ioe) {
                                try {
                                    sc.close();
                                } catch (IOException ignore) {
                                }
                                key.cancel();
                            }
                        }
                    }
                }
            } catch (IOException _) {
            } finally {
                try {
                    sel.close();
                } catch (IOException ignore) {
                }
            }
        }

        private static class Pending {
            final SocketChannel channel;
            final ByteBuffer data;

            Pending(SocketChannel c, ByteBuffer b) {
                this.channel = c;
                this.data = b;
            }
        }
    }

    static class PemUtils {
        static String privateKeyToPem(PrivateKey key) {
            return "-----BEGIN PRIVATE KEY-----\n" +
                    java.util.Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(key.getEncoded()) +
                    "\n-----END PRIVATE KEY-----\n";
        }

        static String certToPem(java.security.cert.Certificate cert) throws CertificateEncodingException {
            return "-----BEGIN CERTIFICATE-----\n" +
                    java.util.Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(cert.getEncoded()) +
                    "\n-----END CERTIFICATE-----\n";
        }
    }

    static PrivateKey loadPrivateKeyFromPem(File pemFile) throws Exception {
        try (Reader r = new FileReader(pemFile); PEMParser p = new PEMParser(r)) {
            Object obj = p.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            switch (obj) {
                case PEMKeyPair pemKeyPair -> {
                    KeyPair kp = converter.getKeyPair(pemKeyPair);
                    return kp.getPrivate();
                }
                case org.bouncycastle.asn1.pkcs.PrivateKeyInfo privateKeyInfo -> {
                    return converter.getPrivateKey(privateKeyInfo);
                }
                case PEMEncryptedKeyPair _ ->
                        throw new IllegalArgumentException("Encrypted key not supported in this demo");
                case null, default -> {
                    byte[] all = java.nio.file.Files.readAllBytes(pemFile.toPath());
                    String s = new String(all, StandardCharsets.UTF_8);
                    String base64 = s.replaceAll("-----.*PRIVATE KEY-----", "").replaceAll("\\s", "");
                    byte[] decoded = java.util.Base64.getDecoder().decode(base64);
                    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    return kf.generatePrivate(spec);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        Security.addProvider(new BouncyCastleProvider());

        String mode = args[0];
        if ("server".equalsIgnoreCase(mode)) {
            int port = 9000;
            String issuerKeyPath = null;
            String issuerName = "CN=DefaultIssuer";
            int genThreads = Runtime.getRuntime().availableProcessors();
            for (int i = 1; i < args.length; ++i) {
                switch (args[i]) {
                    case "--port":
                        port = Integer.parseInt(args[++i]);
                        break;
                    case "--issuer-key":
                        issuerKeyPath = args[++i];
                        break;
                    case "--issuer-name":
                        issuerName = args[++i];
                        break;
                    case "--gen-threads":
                        genThreads = Integer.parseInt(args[++i]);
                        break;
                    default:
                        System.err.println("Unknown arg: " + args[i]);
                }
            }
            if (issuerKeyPath == null) {
                System.err.println("issuer-key is required");
                printUsage();
                return;
            }
            PrivateKey issuerKey = loadPrivateKeyFromPem(new File(issuerKeyPath));
            KeyServer server = new KeyServer(port, issuerKey, issuerName, genThreads);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.shutdown();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error while handling client connection", e);
                }
            }));
            server.run();
        } else if ("client".equalsIgnoreCase(mode)) {
            KeyClient.main(slice(args));
        } else {
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar ... server --port <port> --issuer-key <path> --issuer-name \"CN=...\" --gen-threads <n>");
        System.out.println("  java -jar ... client --host <host> --port <port> --name <name> [--delay-secs N] [--exit-after-send]");
    }

    private static String[] slice(String[] arr) {
        String[] r = new String[Math.max(0, arr.length - 1)];
        System.arraycopy(arr, 1, r, 0, r.length);
        return r;
    }
}
