(ns is-it-time.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om  :include-macros true]
            [cljs.core.async :refer [<! chan put! alts!]]
            [sablono.core :refer-macros [html]]
            [cljs.reader :as reader]
            [ajax.core :refer (GET)]
            [is-it-time.compare :as compare]
            [is-it-time.common :refer (log)]
            [clojure.string :as str])
  (:import [goog.events EventType]))

(enable-console-print!)

(def app-model (atom {:dependencies []
                      :file []
                      :spinner {:event :none}
                      :alert {}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers

(defn by-id [id]
  (.getElementById js/document id))

(defn split-into-map [key-fn items]
  (when-not (key-fn (first items))
    (throw (ex-info "first item is not a key" {:items items})))
  (loop [result {}
         current-parts []
         items items]
    (let [item (first items)]
      (cond
       (and (nil? item) (empty? items))
       (assoc result (first current-parts) (vec (rest current-parts)))
       (key-fn item)
       (recur (if (seq current-parts)
                (assoc result (first current-parts) (first (rest current-parts)))
                result)
              [item]
              (rest items))
       :else
       (recur result (conj current-parts item) (rest items))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Alerting

(defmulti spinner (fn [cursor owner] (:event cursor)))

(defmethod spinner :fetching [cursor owner]
  (om/component
   (html
    [:div {:class "fa fa-spinner fa-spin"
           :style {:color "yellow" :font-size "50px" :text-align "center"}}])))

(defmethod spinner :processing [cursor owner]
  (om/component
   (html
    [:div {:class "fa fa-cog fa-spin"
           :style {:color "yellow" :font-size "50px" :text-align "center"}}])))

(defmethod spinner :none [cursor owner]
  (om/component
   (html
    [:div])))

(defn alert [cursor owner]
  (om/component
   (let [{:keys [status text class]} cursor]
     (html
      [:div {:style {:display (if status "block" "none")}}
       [:div {:class class}
        [:button.close {:type "button"
                        :onClick (fn [_] (om/update! cursor :status false))}
         [:span {:class "fa fa-times"}]]
        text]]))))

(defn process-file [file cursor]
  (try
    (om/update! cursor :spinner {:event :processing})
    (let [project-map (->> (reader/read-string file)
                           (drop 3)
                           (split-into-map keyword?))
          dependencies (mapv (fn [[jar version]] (let [[group artifact] (str/split jar #"/")]
                                                   (if-not artifact
                                                     {:jar (str group "/" group) :version version}
                                                     {:jar jar :version version})))
                             (:dependencies project-map))]
      (om/update! cursor :spinner {:event :none})
      (om/update! cursor :file dependencies))
    (catch :default e
        (om/update! cursor :alert {:status true
                                   :class "alert alert-warning"
                                   :text "Cannot parse your dependencies. Are you sure you dropped project.clj?"}))))

(defn handle-file-select [cursor evt]
  (.stopPropagation evt)
  (.preventDefault evt)
  (let [files (.-files (.-dataTransfer evt))]
    (dotimes [i (.-length files)]
      (let [rdr (js/FileReader.)
            the-file (aget files i)]
        (set! (.-onload rdr)
              (fn [e]
                (let [file-content (.-result (.-target e))
                      file-name (if (= ";;; " (.substr file-content 0 4))
                                  (let [idx (.indexOf file-content "\n\n")]
                                    (.log js/console idx)
                                    (.slice file-content 4 idx))
                                  (.-name the-file))]
                  (process-file file-content cursor))))
        (.readAsText rdr the-file)))))

(defn handle-drag-over [evt]
  (.stopPropagation evt)
  (.preventDefault evt)
  (set! (.-dropEffect (.-dataTransfer evt)) "copy"))

;; TODO use core.async channels instead
(defn set-up-drop-zone [el cursor]
  (.addEventListener el "dragover" handle-drag-over false)
  (.addEventListener el "drop" (partial handle-file-select cursor) false)
  (.addEventListener js/window "dragover" (fn [e] (.stopPropagation e)
                                            (.preventDefault e)))
  (.addEventListener js/window "drop" (fn [e] (.stopPropagation e)
                                        (.preventDefault e))))

(defn drop-zone [cursor owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (set-up-drop-zone (by-id "drop-zone") cursor))
    om/IRender
    (render [_]
      (html
       [:p "Drop your project.clj here"]))))

;; TODO Better parsing of versions: at the moment 0.3.0 > 0.10.0 !
(defn latest-version [current versions]
  (filter #(< (compare/version-compare current %) 0) versions))

(defn check-status [dependency response]
  (let [versions   (get response (str (:jar dependency)))
        latest     (latest-version (:version dependency) (keys versions))
        status     (cond
                     (and (seq versions) (seq latest)) {:label {:class "label label-danger" :text "Yes, it is!"} :latest latest}
                     (and (seq versions) (empty? latest)) {:label {:class "label label-success" :text "No, you're fine."}}
                     :else {:label {:class "label label-default" :text "No data."}})]
    (assoc dependency :status status)))

(defn dependencies-list [dependencies owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div
        [:div.well.well-lg
         [:table.table
          [:thead
           [:tr
            [:th "Dependency"] [:th "Version"] [:th "Status"] [:th "Newer versions"]]]
          [:tbody
           (for [dependency (js->clj dependencies)]
             (let [{:keys [jar version status]} dependency
                   {:keys [label latest]} status]
               [:tr
                [:td (str jar)]
                [:td (str version)]
                [:td [:span {:class (:class label)} (:text label)]]
                [:td (interpose ", " latest)]]))]]]]))))

(defn is-it-time-view [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! cursor :spinner {:event :fetching})
      (GET "/stats"
           {:handler (fn [response]
                       (om/update! cursor :dependencies (:body response))
                       (om/update! cursor :spinner {:event :none}))
            :error-handler (fn [{:keys [status status-text]}]
                             (om/update! cursor :alert {:status true
                                                        :class "alert alert-danger"
                                                        :text status-text}))}))
    om/IRender
    (render [_]
      (html
       [:div
        [:div.class.page-header
         [:h1 "Is it time" [:small " ...to upgrade?"]]]
        [:div.row.col-md-12.col-centered {:id "spinner"} (om/build spinner (:spinner cursor))]
        [:div {:id "alert"}
         (om/build alert (:alert cursor))]
        (if-not (seq (:file cursor))
          [:div {:id "drop-zone"}
           [:div.well.well-lg
            (om/build drop-zone cursor)]]
          [:div
           (om/build dependencies-list (:file cursor) {:fn (fn [file]
                                                             (mapv #(check-status % (:dependencies cursor))
                                                                   file))})])]))))
(defn main []
  (om/root is-it-time-view app-model
           {:target (.getElementById js/document "app")}))
