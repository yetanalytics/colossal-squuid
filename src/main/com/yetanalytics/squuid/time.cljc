(ns com.yetanalytics.squuid.time
  #?(:clj (:import [java.time Instant])))

(def ^:private max-time-emsg
  (str "Cannot generate SQUUID past August 2, 10889."
       " The timestamp would have exceeded 48 bits."))

(def max-seconds
  "The maximum underlying value of a 48-bit timestamp."
  0x0000FFFFFFFFFFFF)

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
    (assert (<= curr-seconds max-seconds)
            max-time-emsg)
    #?(:clj (Instant/ofEpochMilli curr-seconds)
       :cljs (js/Date. curr-seconds))))

(defn before?
  "Does `time1` occur strictly before `time2`?"
  #?(:clj ([^Instant time1 ^Instant time2] (.isBefore time1 time2))
     :cljs ([time1 time2] (< time1 time2))))

#?(:clj
   (defn ms->Instant
     "Convenience function returning a java.time.Instant object 
      given `ms` from the beginning of the UNIX epoch."
     [ms]
     (Instant/ofEpochMilli ms)))
