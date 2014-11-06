(ns is-it-time.compare)

;; Taken from: https://github.com/xsc/version-clj
;; Should port it into cljs and release!

(defmulti normalize-element
  "Normalize an Element by class."
  type
  :default nil)

(defmethod normalize-element js/String
  [^String x]
  (try
    (.parseInt x)
    (catch js/Error e
      (.toLowerCase x))))

(defmethod normalize-element nil
  [x]
  x)

(defn normalize-version-seq
  "Normalize a version seq, creating a seq again."
  [x]
  (let [r (normalize-element x)]
    (if (seq? r) r (vector r))))

;; ## Split Points

(defprotocol SplitPoint
  "Protocol for Split Points."
  (split [this ^String s]))

(extend-type js/String
  SplitPoint
  (split [this s]
    (.split ^String s this)))

(extend-type js/Function
  SplitPoint
  (split [f s]
    (f s)))

;; ## Predefined Split Points

(def ^:const SPLIT-DOT
  "Split at `.` character."
  "\\.")

(def ^:const SPLIT-DASH
  "Split at `-` character."
  "-")

(defn SPLIT-COMPOUND
  "Split a given string into char-only and int-only parts."
  [^String v]
  (loop [^String v v
         result []]
    (if (seq v)
      (let [[c & rst] v
            split-rx (if (re-matches #"^\d+$" c) "[^0-9]" "[0-9]")
            split-result (.split v split-rx 2)
            first-part (first split-result)
            rest-part (.substring v (count first-part))]
        (recur rest-part (conj result first-part)))
      result)))

;; ## Splitting Algorithm
;;
;; We employ a special treatment of the first element of the last split vector.
;;
;;    "1.0-1-0.1-SNAPSHOT"
;;    -> ("1" "0-1-0" "1-SNAPSHOT")              # split using first split point
;;    -> (("1" "0-1-0") "1-SNAPSHOT")            # group into version/rest
;;    -> (("1" ("0" "1" "0")) ("1" "SNAPSHOT"))  # split using other split points
;;    -> (("1" ("0" "1" "0") "1") ("SNAPSHOT"))  # merge first of rest into version
;;    -> ((1 (0 1 0) 1) ("SNAPSHOT"))            # normalize
;;

(defn- first-split-at-point
  "Split using first split point. Creates a two-element vector consisting of the parts.
   The result should be interpreted as a version/qualifier data pair."
  [first-split-point ^String s]
  (let [parts (split first-split-point s)]
    (if (= (count parts) 1)
      (vector nil s)
      (vector (butlast parts) (last parts)))))

(defn- rest-split-at-points
  "Split version string recursively at the given split points."
  [split-points ^String s]
  (if-not (seq split-points)
    [s]
    (filter
      (complement empty?)
      (let [[p & rst] split-points
            parts (split p s)]
        (if (= (count parts) 1)
          (rest-split-at-points rst s)
          (map #(rest-split-at-points rst %) parts))))))

(defn version->seq
  "Split version string using the given split points, creating a two-element vector
   representing a version/qualifiers pair."
  ([^String s] (version->seq [SPLIT-DOT SPLIT-DASH SPLIT-COMPOUND] s))
  ([split-points ^String s]
   (if-not (seq split-points)
     (vector s)
     (let [[p & rst] split-points
           [v0 v1] (first-split-at-point p s)
           r0 (map #(rest-split-at-points rst %) v0)
           r1 (rest-split-at-points rst v1)]
       (if-let [p (first r1)]
         (let [r0 (normalize-version-seq (concat r0 [p]))
               r1 (normalize-version-seq (rest r1))]
           (if (seq r1)
             (vector r0 r1)
             (vector r0)))
         (vector (normalize-version-seq r0)))))))

(defmulti version-element-compare
  (letfn [(f [x]
            (cond (integer? x) :int
                  (string? x) :str
                  (nil? x) :nil
                  :else :lst))]
    (fn [e0 e1]
      (vector (f e0) (f e1)))))

;; ### List Comparison

(defmethod version-element-compare [:lst :lst]
  [v0 v1]
  (let [v0* (if (< (count v0) (count v1)) (concat v0 (repeat nil)) v0)
        v1* (if (< (count v1) (count v0)) (concat v1 (repeat nil)) v1)]
    (or
      (some
        (fn [[e0 e1]]
          (let [r (version-element-compare e0 e1)]
            (when-not (zero? r) r)))
        (map vector v0* v1*))
      0)))

(defmethod version-element-compare [:lst :nil]
  [v0 _]
  (version-element-compare v0 (repeat (count v0) nil)))

(defmethod version-element-compare [:nil :lst]
  [_ v1]
  (version-element-compare (repeat (count v1) nil) v1))

;; ### Integer Comparison

(defmethod version-element-compare [:int :int] [i0 i1] (compare i0 i1))
(defmethod version-element-compare [:int :nil] [i0 _] (if (zero? i0) 0 1))
(defmethod version-element-compare [:nil :int] [_ i1] (if (zero? i1) 0 -1))
(defmethod version-element-compare [:int :lst] [i0 v0] (version-element-compare [i0] v0))
(defmethod version-element-compare [:lst :int] [v0 i0] (version-element-compare v0 [i0]))
(defmethod version-element-compare [:int :str] [_ _] 1)
(defmethod version-element-compare [:str :int] [_ _] -1)

;; ### String Comparison

(def ^:private QUALIFIERS
  "Order Map for well-known Qualifiers."
  { "alpha"     0 "a"         0
    "beta"      1 "b"         1
    "milestone" 2 "m"         2
    "rc"        3 "cr"        3
    "snapshot"  5
    ""          6 "final"     6 "stable"    6 })

(defmethod version-element-compare [:str :lst] [s0 v0] (version-element-compare [s0] v0))
(defmethod version-element-compare [:lst :str] [v0 s0] (version-element-compare v0 [s0]))
(defmethod version-element-compare [:str :nil] [s0 _] (version-element-compare s0 ""))
(defmethod version-element-compare [:nil :str] [_ s0] (version-element-compare "" s0))
(defmethod version-element-compare [:str :str]
  [s0 s1]
  (let [m0 (get QUALIFIERS s0)
        m1 (get QUALIFIERS s1)]
    (cond (and m0 m1) (compare m0 m1)
          m0 1
          m1 -1
          :else (compare s0 s1))))

;; ## Wrappers

(defn version-seq-compare
  "Compare two version seqs."
  [v0 v1]
  (let [r (version-element-compare v0 v1)]
    (cond (pos? r) 1
          (neg? r) -1
          :else 0)))

(defn version-compare
  "Compare two Strings, using the default versioning scheme."
  [s0 s1]
  (version-seq-compare
   (version->seq s0)
   (version->seq s1)))
