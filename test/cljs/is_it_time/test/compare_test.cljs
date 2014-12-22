(ns is-it-time.test.compare-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [is-it-time.compare :as compare]))

(deftest version-element-compare-test
  (testing "Comparing version elements"
    (is (= 1 (compare/version-element-compare 1 nil)))
    (is (= -1 (compare/version-element-compare 1 4)))
    (is (= -1 (compare/version-element-compare "SNAPSHOT" 0)))
    (is (= 1 (compare/version-element-compare 1 "SNAPSHOT")))
    (is (= -1 (compare/version-element-compare nil 1)))
    (is (= 0 (compare/version-element-compare nil 0)))))
