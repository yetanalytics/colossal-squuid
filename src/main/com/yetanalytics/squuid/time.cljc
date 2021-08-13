(ns com.yetanalytics.squuid.time
  #?(:clj (:import [java.time Instant])))

(def ^:private max-time-emsg
  (str "Cannot generate SQUUID past August 2, 10889."
       " The Date timestamp would have exceeded 48 bits."))

(def max-num-seconds 0x0000FFFFFFFFFFFF)

(defn- assert-valid-time
  [t]
  (when-not (<= t max-num-seconds)
    (throw (ex-info max-time-emsg
                    {:type ::exceeded-max-time
                     :time t}))))

(def zero-time
  #?(:clj (Instant/EPOCH)
     :cljs (js/Date. 0)))

(defn current-time
  []
  (let [curr-seconds #?(:clj (System/currentTimeMillis)
                        :cljs (.now js/Date))]
    (assert-valid-time curr-seconds)
    #?(:clj (Instant/ofEpochMilli curr-seconds)
       :cljs (js/Date. curr-seconds))))

(defn before?
  [t1 t2]
  #?(:clj (.isBefore t1 t2)
     :cljs (< t1 t2)))

(defn after?
  [t1 t2]
  #?(:clj (.isAfter t1 t2)
     :cljs (> t1 t2)))
