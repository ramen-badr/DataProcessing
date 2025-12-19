(defn strings-no-repeats [alphabet n]
  (if (<= n 0)
    [""]
    (let [chars (map str alphabet)]
      (reduce
       (fn [acc _]
         (mapcat
          (fn [s]
            (map
             (fn [c] (str s c))
             (filter
              (fn [c] (not= (str (last s)) c))
              chars)))
          acc))
       chars
       (range 1 n)))))

(println (strings-no-repeats ["a" "b" "c"] 2))
(System/exit 0)