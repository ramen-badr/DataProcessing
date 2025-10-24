package com.example.rsakey;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class KeyClient {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 9000;
        String name = null;
        int delaySecs = 0;
        boolean exitAfterSend = false;

        for (int i = 0; i < args.length; ++i) {
            switch (args[i]) {
                case "--host":
                    host = args[++i];
                    break;
                case "--port":
                    port = Integer.parseInt(args[++i]);
                    break;
                case "--name":
                    name = args[++i];
                    break;
                case "--delay-secs":
                    delaySecs = Integer.parseInt(args[++i]);
                    break;
                case "--exit-after-send":
                    exitAfterSend = true;
                    break;
                default:
                    System.err.println("Unknown arg: " + args[i]);
            }
        }
        if (name == null) {
            System.err.println("name is required");
            return;
        }

        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 5000);
            s.setSoTimeout(0);
            OutputStream out = s.getOutputStream();
            out.write(name.getBytes(StandardCharsets.US_ASCII));
            out.write(0);
            out.flush();
            if (exitAfterSend) {
                System.out.println("Exited after send (no read).");
                return;
            }
            if (delaySecs > 0) {
                System.out.println("Sleeping " + delaySecs + " seconds before reading...");
                Thread.sleep(delaySecs * 1000L);
            }
            InputStream in = s.getInputStream();

            byte[] int4 = in.readNBytes(4);
            if (int4.length < 4) throw new EOFException("Unexpected EOF reading key length");
            int keyLen = ByteBuffer.wrap(int4).getInt();
            if (keyLen < 0 || keyLen > 50_000_000) throw new IOException("Invalid keyLen " + keyLen);
            byte[] keyPem = in.readNBytes(keyLen);
            if (keyPem.length < keyLen) throw new EOFException("Unexpected EOF reading keyPem");

            byte[] int4b = in.readNBytes(4);
            if (int4b.length < 4) throw new EOFException("Unexpected EOF reading cert length");
            int certLen = ByteBuffer.wrap(int4b).getInt();
            if (certLen < 0 || certLen > 50_000_000) throw new IOException("Invalid certLen " + certLen);
            byte[] certPem = in.readNBytes(certLen);
            if (certPem.length < certLen) throw new EOFException("Unexpected EOF reading certPem");

            String keyFile = name + ".key";
            String crtFile = name + ".crt";
            try (FileOutputStream fk = new FileOutputStream(keyFile)) {
                fk.write(keyPem);
            }
            try (FileOutputStream fc = new FileOutputStream(crtFile)) {
                fc.write(certPem);
            }
            System.out.println("Saved key -> " + keyFile + " and cert -> " + crtFile);
        }
    }
}
