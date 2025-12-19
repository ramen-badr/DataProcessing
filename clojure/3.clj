(ns parallel-filter)

(defn parallel-filter
  "Параллельный фильтр с обработкой блоков элементов"
  ([pred coll] (parallel-filter pred coll 100 4))
  ([pred coll chunk-size] (parallel-filter pred coll chunk-size 4))
  ([pred coll chunk-size max-futures]
   (->> coll
        (partition-all chunk-size)
        (map (fn [chunk] (future (doall (filter pred chunk)))))
        (partition-all max-futures)
        (mapcat (fn [futures]
                  (doall (map deref futures))))
        (apply concat))))

;; Тесты
(defn run-tests []
  (println "Запуск тестов параллельного фильтра...")

  (let [tests-passed (atom 0)
        tests-failed (atom 0)]

    (defn assert-eq [actual expected msg]
      (if (= actual expected)
        (do (swap! tests-passed inc) (println "✓" msg))
        (do (swap! tests-failed inc) (println "✗" msg "ожидалось:" expected "получено:" actual))))

    (defn assert-true [condition msg]
      (if condition
        (do (swap! tests-passed inc) (println "✓" msg))
        (do (swap! tests-failed inc) (println "✗" msg))))

    (println "\n=== Тест 1: Базовая фильтрация ===")
    (assert-eq (filter even? (range 10))
               (parallel-filter even? (range 10) 3 2)
               "Простые четные числа")

    (println "\n=== Тест 2: Бесконечная последовательность ===")
    (assert-eq (take 20 (filter even? (range)))
               (take 20 (parallel-filter even? (range) 10 2))
               "Бесконечная последовательность")

    (println "\n=== Тест 3: Пустая последовательность ===")
    (assert-true (empty? (parallel-filter even? [] 10 2))
                 "Пустая последовательность")

    (println "\n=== Тест 4: Разные размеры чанков ===")
    (let [data (range 100)
          pred odd?]
      (assert-eq (filter pred data)
                 (parallel-filter pred data 1 2)
                 "Чанк размером 1")
      (assert-eq (filter pred data)
                 (parallel-filter pred data 10 4)
                 "Чанк размером 10")
      (assert-eq (filter pred data)
                 (parallel-filter pred data 100 2)
                 "Чанк размером 100"))

    (println "\n=== Тест 5: Ленивость ===")
    (let [evaluated (atom [])
          pred (fn [x]
                 (swap! evaluated conj x)
                 (even? x))]
      (take 5 (parallel-filter pred (range 100) 10 2))
      (assert-true (< (count @evaluated) 100)
                   "Ленивость сохранена"))

    (println "\n=== Результаты тестов ===")
    (println "Пройдено:" @tests-passed)
    (println "Не пройдено:" @tests-failed)
    (if (zero? @tests-failed)
      (println "\n✅ Все тесты пройдены успешно!")
      (println "\n❌ Есть непройденные тесты."))))

;; Демонстрация производительности с РЕАЛЬНЫМ выигрышем
(defn heavy-predicate [x]
  ;; Более тяжелая операция для демонстрации выигрыша
  (let [sum (reduce + (range 10000))]
    (> x 50)))

(defn light-predicate [x]
  (even? x))

(defn benchmark []
  (let [data (range 1000)]

    (println "\n1. Тяжелый предикат (вычисления, 1000 элементов):")
    (println "   Чанк: 100 Параллелизм: 4")

    (println "\n   Последовательный filter:")
    (time (doall (filter heavy-predicate data)))

    (println "\n   Параллельный filter:")
    (time (doall (parallel-filter heavy-predicate data 100 4)))

    (println "\n2. Легкий предикат (проверка четности, 1000 элементов):")
    (println "   Последовательный filter:")
    (time (doall (filter light-predicate data)))

    (println "\n   Параллельный filter (чанк: 100, параллелизм: 4):")
    (time (doall (parallel-filter light-predicate data 100 4))))

  (println "\n3. Сравнение разных конфигураций для тяжелого предиката (200 элементов):")
  (let [small-data (range 200)]
    (doseq [[chunk futures] [[1 4] [10 4] [50 4] [200 4] [10 1] [10 2] [10 8]]]
      (println (format "\n   Чанк: %3d, Потоков: %d -> " chunk futures))
      (time (doall (parallel-filter heavy-predicate small-data chunk futures))))))

;; Основная программа
(println "Параллельный фильтр на Clojure")
(println "===============================")

(println "\nПримеры работы:")
(println "1. Фильтрация четных чисел до 10:")
(println "   Результат:" (parallel-filter even? (range 10) 3 2))

(println "\n2. Фильтрация из бесконечной последовательности:")
(println "   Первые 10 четных чисел:" (take 10 (parallel-filter even? (range) 5 2)))

(println "\n3. Фильтрация чисел больше 5:")
(println "   Результат:" (parallel-filter #(> % 5) (range 10) 3 2))

;; Запуск тестов и демонстрации
(run-tests)
(benchmark)
(System/exit 0)