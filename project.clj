(defproject is-it-time "0.1.0-SNAPSHOT"
  :description "Tiny web application that checks dependencies in your project.clj for newer versions."
  :url "https://is-it-time.herokuapp.com/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2496" :scope "provided"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 ;; Server
                 [ring "1.3.2"]
                 [liberator "0.12.2"]
                 [compojure "1.3.1"]
                 [http-kit "2.1.19"]
                 [enlive "1.1.5"]

                 ;; Client
                 [om "0.8.0-beta3"]
                 [sablono "0.2.22"]
                 [figwheel "0.1.6-SNAPSHOT"]
                 [cljs-ajax "0.2.6"]

                 [environ "1.0.0"]
                 [com.cemerick/piggieback "0.1.3"]
                 [weasel "0.4.3-SNAPSHOT"]
                 [leiningen "2.5.0"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
            [lein-environ "1.0.0"]]

  :min-lein-version "2.5.0"

  :uberjar-name "is-it-time.jar"

  :cljsbuild {:builds {:is-it-time {:source-paths ["src/cljs"]
                                    :compiler {:output-to     "resources/public/js/is_it_time.js"
                                               :output-dir    "resources/public/js/out"
                                               :source-map    "resources/public/js/out.js.map"
                                               :preamble      ["react/react.min.js"]
                                               :externs       ["react/externs/react.js"]
                                               :optimizations :none
                                               :pretty-print  true}}
                       :test {:source-paths ["src/cljs" "test/cljs"]
                              :notify-command ["phantomjs" "phantom/unit-test.js" "phantom/unit-test.html"]
                              :compiler {:output-to     "target/testable.js"
                                         :preamble      ["react/react.min.js"]
                                         :externs       ["react/externs/react.js"]
                                         :optimizations :whitespace
                                         :pretty-print  true}}}
              :test-commands {"test" ["phantomjs" "phantom/unit-test.js" "phantom/unit-test.html"]}}

  :profiles {:dev {:repl-options {:init-ns is-it-time.server
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :plugins [[lein-figwheel "0.1.4-SNAPSHOT"]]

                   :figwheel {:http-server-root "public"
                              :port 3449
                              :css-dirs ["resources/public/css"]}

                   :env {:is-dev true}

                   :cljsbuild {:builds {:is-it-time {:source-paths ["env/dev/cljs"]}}}}

             :uberjar {:hooks [leiningen.cljsbuild]
                       :env {:production true}
                       :omit-source true
                       :aot :all
                       :cljsbuild {:builds {:is-it-time
                                            {:source-paths ["env/prod/cljs"]
                                             :compiler
                                             {:optimizations :advanced
                                              :pretty-print false}}}}}})
