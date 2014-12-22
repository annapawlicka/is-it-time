(ns test-runner
  (:require [cljs.test :refer-macros [run-tests]]
            [is-it-time.core-test]
            [is-it-time.compare-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
        (run-tests
         'is-it-time.core-test
         'is-it-time.compare-test))
    0
    1))
