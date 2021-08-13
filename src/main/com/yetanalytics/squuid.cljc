(ns com.yetanalytics.squuid
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.squuid.uuid :as u]
            [com.yetanalytics.squuid.time :as t]))

;; This library generates sequential UUIDs, or SQUUIDs, based on the draft RFC
;; for v8 UUIDS:
;; https://datatracker.ietf.org/doc/html/draft-peabody-dispatch-new-uuid-format

;; The original approach of generating a 48-bit timestamp and merging it into
;; a v4 UUID is taken from the Laravel PHP library's orderedUuid function:
;; https://itnext.io/laravel-the-mysterious-ordered-uuid-29e7500b4f8

;; The idea of incrementing the least significant bit on a timestamp collision
;; is taken from the ULID specification:
;; https://github.com/ulid/spec

(s/def ::base-uuid uuid?)
(s/def ::squuid uuid?)
(s/def ::timestamp (s/and inst? #(<= 0 (inst-ms %) t/max-seconds)))

;; The atom is private so that only generate-squuid(*) can mutate it.
;; Note that merging Instant/EPOCH with v0 UUID returns the v0 UUID again.
(def ^:private current-time-atom
  (atom {:timestamp t/zero-time
         :base-uuid u/zero-uuid
         :squuid    u/zero-uuid}))

(s/fdef generate-squuid*
  :args (s/cat)
  :ret (s/keys :req-un [::base-uuid ::timestamp ::squuid]))

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
        {curr-ts :timestamp} @current-time-atom]
    (if (t/before? curr-ts ts)
      ;; No timestamp clash - make new UUIDs
      (swap! current-time-atom (fn [m]
                                 (-> m
                                     (assoc :timestamp ts)
                                     (merge (u/make-squuid ts)))))
      ;; Timestamp clash - increment UUIDs
      (swap! current-time-atom (fn [m]
                                 (-> m
                                     (update :base-uuid u/inc-uuid)
                                     (update :squuid u/inc-uuid)))))))

(s/fdef generate-squuid
  :args (s/cat)
  :ret ::squuid)

(defn generate-squuid
  "Return a new v8 sequential UUID, or SQUUID. The most significant 48 bits
   are created from a timestamp representing the current time, which always
   increments, enforcing sequentialness. See `generate-squuid*` for more
   details."
  []
  (:squuid (generate-squuid*)))

(s/fdef time->uuid
  :args (s/cat :ts ::timestamp)
  :ret ::squuid)

(defn time->uuid
  "Convert a timestamp to a UUID. The upper 48 bits represent
   the timestamp, while the lower 80 bits are `8FFF-8FFF-FFFFFFFFFFFF`."
  [ts]
  (:squuid
   (u/make-squuid ts #uuid "00000000-0000-4FFF-8FFF-FFFFFFFFFFFF")))
