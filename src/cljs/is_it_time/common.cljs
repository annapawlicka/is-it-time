(ns is-it-time.common)

(defn log [& s]
  (.log js/console (apply str s)))
