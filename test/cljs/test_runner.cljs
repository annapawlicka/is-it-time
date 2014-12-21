(ns test-runner
  (:require [cljs.test :refer-macros [run-tests]]
            [is-it-time.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
        (run-tests
          'is-it-time.core-test))
    0
    1))
