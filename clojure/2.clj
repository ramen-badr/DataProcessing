(ns primes)

;; Основная реализация
(defn primes-sieve []
  "Возвращает бесконечную ленивую последовательность простых чисел."
  (letfn [(sieve [xs]
                 (let [p (first xs)]
                   (cons p
                         (lazy-seq
                          (sieve (filter #(pos? (rem % p))
                                         (rest xs)))))))]
         (sieve (iterate inc 2))))

(def primes (primes-sieve))

;; Вспомогательные функции
(defn is-prime? [n]
  "Проверяет, является ли число простым."
  (if (< n 2)
    false
    (->> primes
         (take-while #(<= (* % %) n))
         (every? #(pos? (rem n %))))))

(defn primes-upto [limit]
  "Возвращает все простые числа до указанного предела."
  (->> primes
       (take-while #(<= % limit))
       (doall)))

;; Тесты без внешних зависимостей
(defn run-tests []
  (println "Запуск тестов...")

  (let [tests-passed (atom 0)
        tests-failed (atom 0)]

    (defn assert-eq [actual expected msg]
      (if (= actual expected)
        (do
          (swap! tests-passed inc)
          (println "✓" msg))
        (do
          (swap! tests-failed inc)
          (println "✗" msg "ожидалось:" expected "получено:" actual))))

    (defn assert-true [condition msg]
      (if condition
        (do
          (swap! tests-passed inc)
          (println "✓" msg))
        (do
          (swap! tests-failed inc)
          (println "✗" msg))))

    (println "\n=== Тест 1: Первые простые числа ===")
    (assert-eq (take 10 primes) [2 3 5 7 11 13 17 19 23 29]
               "Первые 10 простых чисел")

    (println "\n=== Тест 2: Конкретные значения ===")
    (assert-eq (nth primes 0) 2 "Первое простое число")
    (assert-eq (nth primes 4) 11 "Пятое простое число")
    (assert-eq (nth primes 99) 541 "Сотое простое число")

    (println "\n=== Тест 3: Свойства простых чисел ===")
    (assert-true (every? #(> % 1) (take 100 primes))
                 "Все числа больше 1")

    (let [first-100 (take 100 primes)]
      (assert-eq (filter even? first-100) [2]
                 "Только 2 является четным простым"))

    (println "\n=== Тест 4: Проверка на составные числа ===")
    (let [first-50 (take 50 primes)
          composites [4 6 8 9 10 12 14 15 16 18 20 21 22 24 25]
          has-composite? (some (set first-50) composites)]
      (assert-true (not has-composite?)
                   "Нет составных чисел в последовательности"))

    (println "\n=== Тест 5: Вспомогательные функции ===")
    (assert-true (is-prime? 17) "17 - простое число")
    (assert-true (not (is-prime? 25)) "25 - не простое число")
    (assert-eq (primes-upto 10) [2 3 5 7] "Простые числа до 10")

    (println "\n=== Тест 6: Ленивость и бесконечность ===")
    (assert-true (seq? primes) "Последовательность ленивая")
    (assert-eq (count (take 100 primes)) 100 "Можно взять 100 элементов")
    (assert-eq (count (take 500 primes)) 500 "Можно взять 500 элементов")

    (println "\n=== Результаты тестов ===")
    (println "Пройдено:" @tests-passed)
    (println "Не пройдено:" @tests-failed)
    (println "Всего:" (+ @tests-passed @tests-failed))

    (if (zero? @tests-failed)
      (println "\n✅ Все тесты пройдены успешно!")
      (println "\n❌ Есть непройденные тесты."))

    (zero? @tests-failed)))

(println "\n1. Первые 20 простых чисел:")
(println (take 20 primes))

(println "\n2. Простые числа от 100 до 150:")
(println (->> primes
              (drop-while #(< % 100))
              (take-while #(<= % 150))
              doall))

(println "\n3. Каждое 50-е простое число (до 250):")
(doseq [i [0 49 99 149 199 249]]
  (println (format "%4d-е простое число: %4d" (inc i) (nth primes i))))

(println "\n4. Проверка чисел на простоту:")
(doseq [n [2 17 29 49 91 97]]
  (println (format "%3d %s простое" n (if (is-prime? n) "-" "не"))))

(println "\n5. Сумма первых 50 простых чисел:")
(println (apply + (take 50 primes)))

(println "\n" (apply str (repeat 50 "=")))
(println "Запуск автоматических тестов...")
(run-tests)
(System/exit 0)