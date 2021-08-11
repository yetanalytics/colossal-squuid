(ns com.yetanalytics.squuid.uuid
  (:require [com.yetanalytics.squuid.time :as t])
  (:import [java.util UUID]))

(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers + Java interop wrappers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^UUID zero-uuid
  "Generate a v0 UUID of all zeroes."
  []
  #uuid "00000000-0000-0000-0000-000000000000")

(defn ^UUID rand-uuid
  "Generate a random v4 UUID."
  []
  (UUID/randomUUID))

(defn ^Long uuid-msb
  "Get the most significant bytes of a UUID."
  [^UUID uuid]
  (.getMostSignificantBits uuid))

(defn ^Long uuid-lsb
  "Get the least significant bytes of a UUID."
  [^UUID uuid]
  (.getLeastSignificantBits uuid))

(defn bytes->uuid
  "Return a UUID given the most and least significant bytes."
  [^Long msb ^Long lsb]
  (UUID. msb lsb))

;; uuid<, uuid=, and uuid> from clj-uuid

(defn before?
  "Does `uuid1?` come before `uuid2`?"
  [^UUID uuid1 ^UUID uuid2]
  (neg-int? (.compareTo uuid1 uuid2)))

(defn equals?
  "Does `uuid1?` equal `uuid2`?"
  [^UUID uuid1 ^UUID uuid2]
  (zero? (.compareTo uuid1 uuid2)))

(defn after?
  "Does `uuid1?` come after `uuid2`?"
  [^UUID uuid1 ^UUID uuid2]
  (pos-int? (.compareTo uuid1 uuid2)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:private true :const true} bit-mask-12
  (unchecked-long 0x0000000000000FFF))
(def ^{:private true :const true} bit-mask-16
  (unchecked-long 0x000000000000FFFF))
(def ^{:private true :const true} bit-mask-61
  (unchecked-long 0x1FFFFFFFFFFFFFFF))

(defn inc-uuid
  "Increment the UUID by one bit. Throws an exception if there are no available
   bits left to increment."
  [uuid]
  (let [uuid-msb (uuid-msb uuid)
        uuid-lsb (uuid-lsb uuid)]
    (cond
      ;; least significant bits not maxed out
      (not (zero? (bit-and-not bit-mask-61 uuid-lsb)))
      (bytes->uuid uuid-msb (inc uuid-lsb))
      ;; most significant bits not maxed out
      (not (zero? (bit-and-not bit-mask-12 uuid-msb)))
      (bytes->uuid (inc uuid-msb) uuid-lsb)
      ;; oh no
      :else
      (throw (ex-info (format "Cannot increment UUID %s any further."
                              uuid)
                      {:type ::exceeded-max-uuid-node
                       :uuid uuid})))))

(defn make-squuid
  "Make a new v8 sequential UUID. Uses `uuid` as the base UUID if provided;
   otherwise uses a random v4 UUID as the base."
  ([ts]
   (make-squuid ts (rand-uuid)))
  ([ts uuid]
   (let [;; Base UUID
         uuid-msb  (uuid-msb uuid)
         uuid-lsb  (uuid-lsb uuid)
        ;; Timestamp
         ts-long   (t/time->millis ts)
        ;; Putting it all together (and change version from v4 to v8)
         uuid-msb' (-> (bit-or (bit-shift-left ts-long 16)
                               (bit-and bit-mask-16 uuid-msb))
                       (bit-clear 14)
                       (bit-set 15))
         squuid    (bytes->uuid uuid-msb' uuid-lsb)]
     {:base-uuid uuid
      :squuid    squuid})))

(set! *warn-on-reflection* false)
