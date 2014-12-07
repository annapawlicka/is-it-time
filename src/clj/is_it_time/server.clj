(ns is-it-time.server
  (:require [clojure.java.io :as io]
            [is-it-time.dev :refer [is-dev? inject-devmode-html browser-repl start-figwheel]]
            [compojure.core :refer [GET POST defroutes routes]]
            [compojure.route :refer [resources] :as r]
            [compojure.handler :refer [api] :as h]
            [ring.middleware.reload :as reload]
            [environ.core :refer [env]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [liberator.core :refer [defresource]]
            [org.httpkit.server :refer [run-server] :as kit]
            [org.httpkit.client :as http]
            [clojure.edn :as reader]))

(defresource stats [ctx]
  :available-media-types ["application/edn"]
  :handle-ok (fn [ctx]
               (let [{:keys [status headers body error] :as resp} @(http/get "https://clojars.org/stats/all.edn")]
                 (if error (println error)
                     {:status 200
                      :body (apply merge (map (fn [[k v]]
                                                (hash-map (clojure.string/join "/" k) v))
                                              (reader/read-string body)))}))))

(deftemplate page
  (io/resource "index.html") [] [:body] (if is-dev? inject-devmode-html identity))

(defn index
  "Handle index page request. Injects session uid if needed."
  [req]
  {:status 200
   :body (page)})

(defroutes my-routes
  (-> (routes
       (GET  "/"   req (#'index req))
       (GET "/stats" [] stats)
       (resources "/")
       (resources "/react" {:root "react"})
       (r/not-found "<p>Page not found. I has a sad!</p>"))
      h/site))

(def http-handler
  (if is-dev?
    (reload/wrap-reload #'my-routes)
    #'my-routes))

(defn run [& [port]]
  (defonce ^:private server
    (do
      (if is-dev? (start-figwheel))
      (let [port (Integer. (or port (env :port) 10555))]
        (print "Starting web server on port" port ".\n")
        (run-server http-handler {:port port
                                  :join? false}))))
  server)

(defn -main [& [port]]
  (run port))
