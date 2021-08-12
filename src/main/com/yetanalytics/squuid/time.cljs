(ns com.yetanalytics.squuid.time)

(def ^:private max-time-emsg
  (str "Cannot generate SQUUID past August 2, 10889."
       " The Date timestamp would have exceeded 48 bits."))

(defn- assert-valid-time
  [t]
  (when-not (< t 0x0000FFFFFFFFFFFF)
    (throw (ex-info max-time-emsg
                    {:type ::exceeded-max-time
                     :time t}))))

(defn zero-time [] (js/Date. 0))

(defn current-time
  []
  (let [curr-time (.now js/Date)]
    (assert-valid-time curr-time)
    (js/Date. curr-time)))

(defn before?
  [inst1 inst2]
  (< inst1 inst2))

(defn after?
  [inst1 inst2]
  (> inst1 inst2))

(defn time->millis
  [i]
  (.valueOf i))
