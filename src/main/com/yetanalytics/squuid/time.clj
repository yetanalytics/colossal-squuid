(ns com.yetanalytics.squuid.clj.time
  (:import [java.time Instant]))

(set! *warn-on-reflection* true)

(def ^:private max-time-emsg
  (str "Cannot generate SQUUID past August 2, 10889."
       " The java.time.Instant timestamp would have exceeded 48 bits."))

(defn- assert-valid-time
  [t]
  (when-not (< t 0x0000FFFFFFFFFFFF)
    (throw (ex-info max-time-emsg
                    {:type ::exceeded-max-time
                     :time t}))))

(defn ^Instant zero-time
  "Return the Instant instance representing the beginning of the UNIX epoch."
  []
  (Instant/EPOCH))

(defn ^Instant current-time
  "Return the Instant instance for the current system time. Throws an exception
   if the underlying long exceeds 48 bits."
  []
  (let [curr-time (System/currentTimeMillis)]
    (assert-valid-time curr-time)
    (Instant/ofEpochMilli curr-time)))

(defn before?
  "Does `inst1` come before `inst2`?"
  [^Instant inst1 ^Instant inst2]
  (.isBefore inst1 inst2))

(defn after?
  "Does `inst1` come after `inst2`?"
  [^Instant inst1 ^Instant inst2]
  (.isAfter inst1 inst2))

(defn time->millis
  "Given an Instant instance `inst`, convert it into the number of milliseconds
   since the start of the UNIX epoch."
  [^Instant inst]
  (.toEpochMilli inst))

(set! *warn-on-reflection* false)
