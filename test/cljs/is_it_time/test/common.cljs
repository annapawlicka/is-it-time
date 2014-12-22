(ns is-it-time.test.common)

(defn container-div
  "Creates div with a unique id and returns a vector of that new div element
  and id."
  []
  (let [id  (str "container-" (gensym))
        div (.createElement js/document "div")]
    (aset div "id" id)
    [div (str  id)]))

(defn append-container
  "Appends a given container to html's body."
  [container]
  (-> js/document .-body (.appendChild container)))

(defn create-container
  "Creates and returns new container."
  []
  (let [[n id] (container-div)]
    (append-container n)
    (.getElementById js/document id)))
