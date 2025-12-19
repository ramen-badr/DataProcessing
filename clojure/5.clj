(ns dining-philosophers)

;; Глобальные атомы для статистики
(def restart-counter (atom 0))
(def execution-times (atom {}))
(def philosophers-done (atom 0))

;; Функция для создания философа
(defn create-philosopher
  "Создает и запускает философа"
  [id forks think-time eat-time max-meals total-philosophers]
  (let [left-fork (forks id)
        right-fork (forks (mod (inc id) total-philosophers))]
    (future
     (try
       (let [start-time (System/currentTimeMillis)]
         (loop [meal-count 0]
           (when (< meal-count max-meals)
                 ;; Фаза размышлений
                 (Thread/sleep think-time)

                 ;; Фаза еды (с попыткой взять вилки)
                 (let [attempt (atom 0)
                       success (atom false)]
                   (while (not @success)
                          (swap! attempt inc)
                          (when (> @attempt 1)
                                (swap! restart-counter inc))

                          (try
                            (dosync
                             ;; Проверяем, доступны ли вилки
                             (when (and (< @left-fork 2) (< @right-fork 2))
                                   ;; Берем вилки
                                   (alter left-fork inc)
                                   (alter right-fork inc)
                                   (reset! success true)))
                            (catch Exception e
                              ;; Игнорируем ошибки транзакций
                              nil))

                          ;; Если не удалось взять вилки, ждем немного
                          (when-not @success
                                    (Thread/sleep 1)))

                   ;; Освобождаем вилки после еды
                   (Thread/sleep eat-time)

                   (dosync
                    (alter left-fork dec)
                    (alter right-fork dec))

                   ;; Переходим к следующей итерации
                   (recur (inc meal-count)))))

         ;; Записываем время выполнения
         (let [end-time (System/currentTimeMillis)
               execution-time (- end-time start-time)]
           (swap! execution-times assoc id execution-time)
           (swap! philosophers-done inc)))

       (catch Exception e
         (println "Философ" id "ошибся:" (.getMessage e)))))))

;; Функция для создания философа с возможностью deadlock
(defn create-philosopher-deadlock-prone
  "Создает философа с возможностью deadlock"
  [id forks think-time eat-time max-meals total-philosophers strategy]
  (future
   (try
     (let [start-time (System/currentTimeMillis)
           ;; Определяем порядок взятия вилок в зависимости от стратегии
           [first-fork second-fork] (if (= strategy :normal)
                                      [id (mod (inc id) total-philosophers)]
                                      [(mod (inc id) total-philosophers) id])
           fork1 (forks first-fork)
           fork2 (forks second-fork)]

       (loop [meal-count 0]
         (when (< meal-count max-meals)
               ;; Фаза размышлений
               (Thread/sleep (+ think-time (rand-int 10)))

               ;; Пытаемся взять вилки
               (let [success (atom false)
                     attempts (atom 0)]

                 (while (and (not @success) (< @attempts 100))
                        (swap! attempts inc)
                        (when (> @attempts 1)
                              (swap! restart-counter inc))

                        (try
                          (dosync
                           (when (< @fork1 2)
                                 (alter fork1 inc)

                                 ;; Небольшая задержка для увеличения шанса deadlock
                                 (Thread/sleep (rand-int 2))

                                 (when (< @fork2 2)
                                       (alter fork2 inc)
                                       (reset! success true))))
                          (catch Exception e
                            nil))

                        (when-not @success
                                  (Thread/sleep 1)))

                 (if @success
                   (do
                     ;; Философ ест
                     (Thread/sleep eat-time)

                     ;; Возвращаем вилки
                     (dosync
                      (alter fork1 dec)
                      (alter fork2 dec))

                     (recur (inc meal-count)))
                   ;; Не удалось поесть
                   (recur meal-count)))))

       (let [end-time (System/currentTimeMillis)]
         (swap! execution-times assoc id (- end-time start-time))
         (swap! philosophers-done inc)))

     (catch Exception e
       (println "Философ" id "ошибся:" (.getMessage e))))))

;; Функция для запуска эксперимента
(defn run-experiment
  "Запускает эксперимент с заданными параметрами"
  [n-philosophers think-time eat-time max-meals strategy]
  (println "\n" (apply str (repeat 70 "=")))
  (println "Эксперимент:" n-philosophers "философов, стратегия:" strategy)
  (println "Время размышлений:" think-time "мс, время еды:" eat-time "мс")
  (println "Количество блюд на философа:" max-meals)
  (println (apply str (repeat 70 "=")))

  ;; Сбрасываем статистику
  (reset! restart-counter 0)
  (reset! execution-times {})
  (reset! philosophers-done 0)

  ;; Создаем вилки как ref с счетчиком использований
  (let [forks (vec (repeatedly n-philosophers #(ref 0)))
        start-time (System/currentTimeMillis)

        ;; Создаем философов
        philosophers (if (= strategy :deadlock-prone)
                       (doall (map #(create-philosopher-deadlock-prone
                                     % forks think-time eat-time max-meals
                                     n-philosophers (if (even? %) :normal :reverse-order))
                                   (range n-philosophers)))
                       (doall (map #(create-philosopher % forks think-time eat-time
                                     max-meals n-philosophers)
                                   (range n-philosophers))))]

    ;; Ждем завершения всех философов или таймаута
    (loop [timeout 0]
      (when (and (< @philosophers-done n-philosophers) (< timeout 60000))
            (Thread/sleep 100)
            (recur (+ timeout 100))))

    (let [end-time (System/currentTimeMillis)
          total-time (- end-time start-time)]

      ;; Выводим статистику
      (println "\nРезультаты:")
      (println "Общее время выполнения:" total-time "мс")
      (println "Количество перезапусков транзакций:" @restart-counter)
      (println "Завершили философы:" @philosophers-done "/" n-philosophers)

      (when (pos? @philosophers-done)
            (let [times (vals @execution-times)
                  avg-time (/ (reduce + times) (count times))]
              (println "Среднее время на философа:" (int avg-time) "мс")))

      ;; Выводим использование вилок
      (println "\nИспользование вилок:")
      (doseq [i (range n-philosophers)]
        (println "  Вилка" i "использована:" @(forks i) "раз"))

      ;; Определяем, был ли deadlock/livelock
      (if (< @philosophers-done n-philosophers)
        (do
          (println "\n⚠️  ВОЗМОЖНЫЙ DEADLOCK/LIVELOCK!")
          (println "Не все философы завершили работу.")
          (doseq [i (range (count philosophers))]
            (when (not (future-done? (nth philosophers i)))
                  (println "  Философ" i "застрял"))))
        (println "\n✅ Все философы успешно завершили работу."))

      ;; Отменяем оставшиеся фьючерсы
      (doseq [p philosophers]
        (when (not (future-done? p))
              (future-cancel p)))

      {:total-time total-time
       :restarts @restart-counter
       :completed @philosophers-done
       :forks-usage (mapv deref forks)})))

;; Функция для запуска серии экспериментов
(defn run-experiment-series []
  (println "РЕШЕНИЕ ЗАДАЧИ 'ОБЕДАЮЩИХ ФИЛОСОФОВ' НА CLOJURE")
  (println "==============================================")

  ;; Эксперимент 1: Четное количество философов (без deadlock)
  (run-experiment 4 50 50 10 :normal)

  ;; Эксперимент 2: Нечетное количество философов (без deadlock)
  (run-experiment 5 50 50 10 :normal)

  ;; Эксперимент 3: Четное количество философов с возможностью deadlock
  (run-experiment 4 20 20 5 :deadlock-prone)

  ;; Эксперимент 4: Нечетное количество философов с возможностью deadlock
  (run-experiment 5 20 20 5 :deadlock-prone)

  ;; Эксперимент 5: Много философов
  (run-experiment 10 30 30 5 :normal)

  ;; Эксперимент 6: Много философов с возможностью deadlock
  (run-experiment 10 10 10 3 :deadlock-prone))

;; Функция для создания философа без deadlock (иерархия ресурсов)
(defn create-philosopher-no-deadlock
  "Философ, который никогда не deadlock'нет"
  [id forks think-time eat-time max-meals total-philosophers]
  (future
   (try
     (let [start-time (System/currentTimeMillis)
           ;; Определяем порядок взятия вилок (всегда сначала меньший индекс)
           [first-fork second-fork] (sort [id (mod (inc id) total-philosophers)])
           fork1 (forks first-fork)
           fork2 (forks second-fork)]

       (loop [meal-count 0]
         (when (< meal-count max-meals)
               (Thread/sleep think-time)

               (let [success (atom false)]
                 (while (not @success)
                        (swap! restart-counter inc)

                        (try
                          (dosync
                           ;; Берем вилки в определенном порядке
                           (when (< @fork1 2)
                                 (alter fork1 inc)
                                 (when (< @fork2 2)
                                       (alter fork2 inc)
                                       (reset! success true))))
                          (catch Exception e
                            nil))

                        (when-not @success
                                  (Thread/sleep 1)))

                 ;; Едим
                 (Thread/sleep eat-time)

                 ;; Возвращаем вилки
                 (dosync
                  (alter fork1 dec)
                  (alter fork2 dec))

                 (recur (inc meal-count)))))

       (let [end-time (System/currentTimeMillis)]
         (swap! execution-times assoc id (- end-time start-time))
         (swap! philosophers-done inc)))

     (catch Exception e
       (println "Философ" id "ошибся:" (.getMessage e))))))

;; Сравнительный тест стратегий
(defn compare-strategies []
  (println "\n" (apply str (repeat 70 "=")))
  (println "СРАВНЕНИЕ СТРАТЕГИЙ ДЛЯ 5 ФИЛОСОФОВ")
  (println (apply str (repeat 70 "=")))

  (println "\n1. Стратегия с возможностью deadlock:")
  (let [res1 (run-experiment 5 30 30 5 :deadlock-prone)]
    (println "   Перезапусков на операцию:" (/ (:restarts res1) (* 5 5)))
    (Thread/sleep 2000))

  (println "\n2. Стратегия без deadlock (иерархия ресурсов):")
  (reset! restart-counter 0)
  (reset! philosophers-done 0)
  (reset! execution-times {})

  (let [n-philosophers 5
        think-time 30
        eat-time 30
        max-meals 5
        forks (vec (repeatedly n-philosophers #(ref 0)))
        start-time (System/currentTimeMillis)

        philosophers (doall (map #(create-philosopher-no-deadlock % forks
                                   think-time eat-time max-meals n-philosophers)
                                 (range n-philosophers)))]

    ;; Ждем завершения
    (loop [timeout 0]
      (when (and (< @philosophers-done n-philosophers) (< timeout 10000))
            (Thread/sleep 100)
            (recur (+ timeout 100))))

    (let [end-time (System/currentTimeMillis)
          total-time (- end-time start-time)]

      (println "   Общее время:" total-time "мс")
      (println "   Перезапусков:" @restart-counter)
      (println "   Завершили:" @philosophers-done "/" n-philosophers)
      (println "   Перезапусков на операцию:" (/ @restart-counter (* n-philosophers max-meals)))

      ;; Отменяем фьючерсы
      (doseq [p philosophers]
        (when (not (future-done? p))
              (future-cancel p))))))

;; Основная функция для запуска
(defn -main []
  (println "ПРОГРАММА 'ОБЕДАЮЩИЕ ФИЛОСОФЫ' НА CLOJURE")
  (println "==========================================")
  (println "Автоматический запуск всех экспериментов...")

  ;; Запускаем серию экспериментов
  (run-experiment-series)

  ;; Запускаем сравнение стратегий
  (compare-strategies)

  ;; Запускаем дополнительные демонстрации
  (println "\n" (apply str (repeat 70 "=")))
  (println "ДОПОЛНИТЕЛЬНЫЕ ЭКСПЕРИМЕНТЫ")
  (println (apply str (repeat 70 "=")))

  (println "\nЭксперимент 7: 3 философа, минимальные времена (демонстрация livelock):")
  (run-experiment 3 10 10 3 :deadlock-prone)

  (println "\nЭксперимент 8: 2 философа, стратегия без deadlock:")
  (reset! restart-counter 0)
  (reset! philosophers-done 0)
  (reset! execution-times {})
  (let [n 2
        forks (vec (repeatedly n #(ref 0)))
        start-time (System/currentTimeMillis)
        philosophers (doall (map #(create-philosopher-no-deadlock % forks 30 30 5 n)
                                 (range n)))]

    ;; Ждем завершения
    (loop [timeout 0]
      (when (and (< @philosophers-done n) (< timeout 10000))
            (Thread/sleep 100)
            (recur (+ timeout 100))))

    (let [end-time (System/currentTimeMillis)]
      (println "   Общее время:" (- end-time start-time) "мс")
      (println "   Перезапусков:" @restart-counter)
      (println "   Завершили:" @philosophers-done "/" n))

    ;; Отменяем фьючерсы
    (doseq [p philosophers]
      (when (not (future-done? p))
            (future-cancel p))))

  (println "\n✅ Все эксперименты завершены!"))

;; Автоматический запуск при выполнении файла
(println "Начало выполнения программы 'Обедающие философы'...")
(println "Пожалуйста, подождите, выполняется серия экспериментов...")
(println "")

;; Вызываем основную функцию
(-main)
(System/exit 0)