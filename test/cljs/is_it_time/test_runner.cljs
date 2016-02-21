(ns is-it-time-2.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [is-it-time-2.core-test]))

(enable-console-print!)

(doo-tests 'is-it-time-2.core-test)
