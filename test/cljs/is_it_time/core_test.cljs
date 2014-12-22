(ns is-it-time.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [is-it-time.core :as core]))

(deftest split-into-map-test
  (testing "Split into map"
    (let [project-file '(defproject "is-it-time" "0.1.0-SNAPSHOT"
                         :description "Foo"
                         :url "Bar"
                         :license {:name "Eclipse Public License"
                                   :url "http://www.eclipse.org/legal/epl-v10.html"}
                         :source-paths ["src/clj" "src/cljs"]
                         :dependencies [["org.clojure/clojure" "1.6.0"]
                                        ["org.clojure/clojurescript" "0.0-2505" :scope "provided"]
                                        ["org.clojure/core.async" "0.1.346.0-17112a-alpha"]
                                        ["ring" "1.3.2"]]
                         :plugins [["lein-cljsbuild" "1.0.4-SNAPSHOT"]
                                   ["lein-environ" "1.0.0"]]
                         :min-lein-version "2.5.0"
                         :uberjar-name "is-it-time.jar"
                         :cljsbuild {:builds {:is-it-time {:source-paths ["src/cljs"]
                                                           :compiler {:output-dir "resources/public/js/out"
                                                                      :optimizations :none
                                                                      :output-to "resources/public/js/is_it_time.js"
                                                                      :source-map "resources/public/js/out.js.map"
                                                                      :pretty-print true}}}})]
      (is (= {:description "Foo"
              :url "Bar"
              :license {:name "Eclipse Public License"
                        :url "http://www.eclipse.org/legal/epl-v10.html"}
              :source-paths ["src/clj" "src/cljs"]
              :dependencies [["org.clojure/clojure" "1.6.0"]
                             ["org.clojure/clojurescript" "0.0-2505" :scope "provided"]
                             ["org.clojure/core.async" "0.1.346.0-17112a-alpha"]
                             ["ring" "1.3.2"]]
              :plugins [["lein-cljsbuild" "1.0.4-SNAPSHOT"]
                        ["lein-environ" "1.0.0"]]
              :min-lein-version "2.5.0"
              :uberjar-name "is-it-time.jar"
              :cljsbuild [{:builds {:is-it-time {:source-paths ["src/cljs"]
                                                 :compiler {:output-dir "resources/public/js/out"
                                                            :optimizations :none
                                                            :output-to "resources/public/js/is_it_time.js"
                                                            :source-map "resources/public/js/out.js.map"
                                                            :pretty-print true}}}}]}
             (->> project-file (drop 3) (core/split-into-map keyword?)))))))
