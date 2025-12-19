package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Crawler {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java Crawler <studentId> [port]");
            System.exit(1);
        }
        String studentId = args[0];
        int port = 8080;
        if (args.length >= 2) {
            port = Integer.parseInt(args[1]);
        }

        String baseUrl = "http://localhost:" + port;

        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // Потоковый пул виртуальных потоков
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            Queue<String> toVisit = new ConcurrentLinkedQueue<>();
            var visited = ConcurrentHashMap.newKeySet();
            Queue<String> messages = new ConcurrentLinkedQueue<>();

            // Начинаем с пути, формируем корень как "/" (сервер ожидает URL-пути).
            toVisit.add("/");

            AtomicInteger activeTasks = new AtomicInteger(0);

            // Главный цикл: пока есть что посетить или пока есть активные задачи
            while (true) {
                String path = toVisit.poll();
                if (path == null) {
                    if (activeTasks.get() == 0) {
                        break; // всё обработано
                    } else {
                        // немного подождём, чтобы не спинлить CPU
                        Thread.sleep(50);
                        continue;
                    }
                }

                // у нас уже была проверка на visited при добавлении, но на всякий случай:
                if (!visited.add(path)) {
                    continue;
                }

                activeTasks.incrementAndGet();
                String finalPath = path;
                executor.submit(() -> {
                    try {
                        fetchAndProcess(http, baseUrl, finalPath, toVisit, visited, messages);
                    } catch (Exception e) {
                        // логируем ошибку в STDERR, но не останавливаем обход
                        System.err.println("Error fetching " + finalPath + ": " + e.getMessage());
                    } finally {
                        activeTasks.decrementAndGet();
                    }
                });
            }

            // аккуратно завершаем executor (close уже вызван try-with-resources),
            // но можем подождать небольшое время, чтобы завершились все фоновые виртуальные потоки
            executor.shutdown(); // допустимо — виртуальные потоки завершаются быстро
            // (не вызываем awaitTermination длительное время — задачи уже завершены по условию цикла)

            // Сортируем и выводим результаты
            List<String> out = new ArrayList<>(messages);
            Collections.sort(out);
            for (String m : out) {
                System.out.println(m);
            }
        }
    }

    /**
     * Выполняет GET baseUrl + path, парсит JSON {"message":"...","successors":["/a","/b",...]}
     * и помещает message в messages, а новые непосещённые пути — в toVisit.
     *
     * Простая JSON-парсерка, достаточная для ожидаемого формата.
     */
    static void fetchAndProcess(HttpClient http, String baseUrl, String path,
                                Queue<String> toVisit,
                                ConcurrentHashMap.KeySetView<Object, Boolean> visited,
                                Queue<String> messages) throws Exception {

        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        String url = baseUrl + normalizedPath;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15)) // запросы могут длиться до 12 секунд — ставим запас
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Non-200 response: " + resp.statusCode());
        }
        String body = resp.body();

        // Разбираем JSON вручную (здесь предполагается корректный и предсказуемый формат)
        Parsed parsed = simpleParse(body);
        if (parsed.message != null) {
            messages.add(parsed.message);
        }
        if (parsed.successors != null) {
            for (String succ : parsed.successors) {
                String p = succ.startsWith("/") ? succ : ("/" + succ);
                // добавляем в очередь только если ещё не посещали (visited — concurrent set)
                if (!visited.contains(p)) {
                    // добавляем без атомарной проверки visited.add чтобы избежать лишнего посещения,
                    // но double-check при polling всё равно присутствует
                    toVisit.add(p);
                }
            }
        }
    }

    // Короткая структура для результата парсинга
    static class Parsed {
        String message;
        List<String> successors;
    }

    /**
     * Простой парсер JSON в ожидаемом формате:
     * {"message":"some text", "successors":["/a","/b",...]}
     *
     * Поддерживает экранированные символы внутри строк (учтены \").
     * Не является общим JSON-парсером, но достаточен для задачи.
     */
    static Parsed simpleParse(String s) {
        Parsed out = new Parsed();
        int n = s.length();
        int i = 0;

        while (i < n) {
            char c = s.charAt(i);
            if (c == '"') {
                // прочитаем имя поля
                int nameStart = i + 1;
                int nameEnd = findStringEnd(s, nameStart);
                if (nameEnd < 0) break;
                String name = s.substring(nameStart, nameEnd);
                i = nameEnd + 1;
                // пропускаем до ':'
                while (i < n && s.charAt(i) != ':') i++;
                i++; // за ':'
                // пропускаем пробелы
                while (i < n && Character.isWhitespace(s.charAt(i))) i++;
                if (i >= n) break;

                if (s.charAt(i) == '"') {
                    // строковое значение
                    int valStart = i + 1;
                    int valEnd = findStringEnd(s, valStart);
                    if (valEnd < 0) break;
                    String val = unescapeString(s.substring(valStart, valEnd));
                    if ("message".equals(name)) {
                        out.message = val;
                    } else {
                        // возможны и другие поля с строкой (игнорируем)
                    }
                    i = valEnd + 1;
                } else if (s.charAt(i) == '[') {
                    // массив — ожидаем массив строк для successors
                    int arrPos = i + 1;
                    List<String> list = new ArrayList<>();
                    while (arrPos < n) {
                        // пропускаем пробелы и запятые
                        while (arrPos < n && (Character.isWhitespace(s.charAt(arrPos)) || s.charAt(arrPos) == ',')) arrPos++;
                        if (arrPos >= n) break;
                        if (s.charAt(arrPos) == ']') {
                            arrPos++;
                            break;
                        }
                        if (s.charAt(arrPos) == '"') {
                            int valStart = arrPos + 1;
                            int valEnd = findStringEnd(s, valStart);
                            if (valEnd < 0) break;
                            String val = unescapeString(s.substring(valStart, valEnd));
                            list.add(val);
                            arrPos = valEnd + 1;
                        } else {
                            // неожиданное содержимое — пропустим
                            arrPos++;
                        }
                    }
                    if ("successors".equals(name)) {
                        out.successors = list;
                    }
                    i = arrPos;
                } else {
                    // другое значение (число, объект) — пропускаем элемент на простую реализацию
                    // пропускаем до запятой или закрывающей фигурной скобки
                    while (i < n && s.charAt(i) != ',' && s.charAt(i) != '}') i++;
                }
            } else {
                i++;
            }
        }
        return out;
    }

    // Находит конец строки (индекс символа перед закрывающей кавычкой), учитывая экранирование
    static int findStringEnd(String s, int start) {
        int n = s.length();
        boolean esc = false;
        for (int i = start; i < n; i++) {
            char c = s.charAt(i);
            if (esc) {
                esc = false;
                continue;
            }
            if (c == '\\') {
                esc = true;
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    static String unescapeString(String s) {
        StringBuilder sb = new StringBuilder();
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < n) {
                char d = s.charAt(i + 1);
                i++;
                switch (d) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 < n) {
                            String hex = s.substring(i + 1, i + 5);
                            try {
                                int code = Integer.parseInt(hex, 16);
                                sb.append((char) code);
                                i += 4;
                            } catch (NumberFormatException ex) {
                                // если не получилось — положим дословно
                                sb.append("\\u");
                            }
                        } else {
                            sb.append("\\u");
                        }
                        break;
                    default:
                        // неизвестный escape, вставляем следующий символ как есть
                        sb.append(d);
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}