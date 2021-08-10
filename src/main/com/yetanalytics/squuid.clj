(ns com.yetanalytics.squuid
  (:require [com.yetanalytics.squuid.uuid :as u]
            [com.yetanalytics.squuid.time :as t])
  (:import [java.time Instant]))

;; This library generates sequential UUIDs, or SQUUIDs, based on the draft RFC
;; for v8 UUIDS:
;; https://datatracker.ietf.org/doc/html/draft-peabody-dispatch-new-uuid-format

;; The original approach of generating a 48-bit timestamp and merging it into
;; a v4 UUID is taken from the Laravel PHP library's orderedUuid function:
;; https://itnext.io/laravel-the-mysterious-ordered-uuid-29e7500b4f8

;; The idea of incrementing the least significant bit on a timestamp collision
;; is taken from the ULID specification:
;; https://github.com/ulid/spec

(def ^{:private true :const true} bit-mask-12
  (unchecked-long 0x0000000000000FFF))
(def ^{:private true :const true} bit-mask-16
  (unchecked-long 0x000000000000FFFF))
(def ^{:private true :const true} bit-mask-48
  (unchecked-long 0x0000FFFFFFFFFFFF))
(def ^{:private true :const true} bit-mask-61
  (unchecked-long 0x1FFFFFFFFFFFFFFF))

(defn- inc-uuid
  [uuid]
  (let [uuid-msb (u/uuid-msb uuid)
        uuid-lsb (u/uuid-lsb uuid)]
    (cond
      ;; least significant bits not maxed out
      (not (zero? (bit-and-not bit-mask-61 uuid-lsb)))
      (u/bytes->uuid uuid-msb (inc uuid-lsb))
      ;; most significant bits not maxed out
      (not (zero? (bit-and-not bit-mask-12 uuid-msb)))
      (u/bytes->uuid (inc uuid-msb) uuid-lsb)
      ;; oh no
      :else
      (throw (ex-info (format "Cannot increment UUID %s any further."
                              uuid)
                      {:type ::exceeded-max-uuid-node
                       :uuid uuid})))))

(defn- make-squuid
  [ts]
  (let [;; Base UUID
        uuid      (u/rand-uuid)
        uuid-msb  (u/uuid-msb uuid)
        uuid-lsb  (u/uuid-lsb uuid)
        ;; Timestamp
        ts-long   (t/time->millis ts)
        ;; Putting it all together (and change version from v4 to v8)
        uuid-msb' (-> (bit-or (bit-shift-left ts-long 16)
                              (bit-and bit-mask-16 uuid-msb))
                      (bit-clear 14)
                      (bit-set 15))
        squuid    (u/bytes->uuid uuid-msb' uuid-lsb)]
    {:base-uuid uuid
     :squuid    squuid}))

;; The atom is private so that only generate-squuid(*) can mutate it.
;; Note that merging Instant/EPOCH with v0 UUID returns the v0 UUID again.
(def ^:private current-time-atom
    (atom {:timestamp (t/zero-time)
           :base-uuid (u/zero-uuid)
           :squuid    (u/zero-uuid)}))

(defn generate-squuid*
  "Return a map containing the following:
   :squuid     The v8 sequential UUID made up of a base UUID and timestamp.
   :base-uuid  The base v4 UUID that provides the lower 80 bits.
   :timestamp  The timestamp that provides the higher 48 bits.
   
   The sequential UUIDs have 7 reserved bits from the RFC 4122 standard;
   4 for the UUID version and 2 for the UUID variant. This leaves 74 random
   bits, allowing for about 18.9 sextillion random segments.
   
   The timestamp is coerced to millisecond resolution. Due to the 48 bit
   maximum on the timestamp, the latest time supported is February 11, 10332.
   
   In case that this function is called multiple times in the same millisecond,
   subsequent SQUUIDs are created by incrementing the base UUID and thus the
   random segment of the SQUUID. An exception is thrown in the unlikely case
   where all 72 (non-variant) random bits are 1s and incrementing can no
   longer occur."
  []
  (let [ts (t/current-time)
        {:keys [timestamp]} @current-time-atom]
    (if-not (t/after? ts timestamp)
      ;; Timestamp clash - increment UUIDs
      (swap! current-time-atom (fn [m]
                                 (-> m
                                     (update :base-uuid inc-uuid)
                                     (update :squuid inc-uuid))))
      ;; No timestamp clash - make new UUIDs
      (swap! current-time-atom (fn [m]
                                 (-> m
                                     (assoc :timestamp ts)
                                     (merge (make-squuid ts))))))))

(defn generate-squuid
  "Return a new v8 sequential UUID, or SQUUID. The most significant 48 bits
   are created from a timestamp representing the current time, which always
   increments, enforcing sequentialness. See `generate-squuid*` for more
   details."
  []
  (:squuid (generate-squuid*)))

(defn time->uuid
  "Convert a java.util.Instant timestamp to a UUID. The upper 48 bits represent
   the timestamp, while the lower 80 bits are `1FFF-8FFF-FFFFFFFFFFFF`."
  [^Instant ts]
  (let [ts-long  (t/time->millis ts)
        uuid-msb (bit-or (bit-shift-left ts-long 16)
                         0x8FFF)
        uuid-lsb (bit-or (bit-shift-left 0x8FFF 48)
                         bit-mask-48)]
    (u/bytes->uuid uuid-msb uuid-lsb)))
