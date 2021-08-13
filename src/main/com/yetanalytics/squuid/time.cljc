(ns com.yetanalytics.squuid.time
  #?(:clj (:import [java.time Instant])))

(def ^:private max-time-emsg
  (str "Cannot generate SQUUID past August 2, 10889."
       " The timestamp would have exceeded 48 bits."))

(def max-seconds 0x0000FFFFFFFFFFFF)

(defn- assert-valid-time
  [t]
  (when-not (<= t max-seconds)
    (throw (ex-info max-time-emsg
                    {:type ::exceeded-max-time
                     :time t}))))

(def zero-time
  "Return the timestamp corresponding to the beginning of the UNIX epoch,
   on Jan 1, 1970."
  #?(:clj Instant/EPOCH
     :cljs (js/Date. 0)))

(defn current-time
  "Return the timestamp corresponding to the current system time."
  []
  (let [curr-seconds #?(:clj (System/currentTimeMillis)
                        :cljs (.now js/Date))]
    (assert-valid-time curr-seconds)
    #?(:clj (Instant/ofEpochMilli curr-seconds)
       :cljs (js/Date. curr-seconds))))

(defn before?
  "Does `time1` occur strictly before `time2`?"
  #?(:clj ([^Instant time1 ^Instant time2] (.isBefore time1 time2))
     :cljs ([time1 time2] (< time1 time2))))
