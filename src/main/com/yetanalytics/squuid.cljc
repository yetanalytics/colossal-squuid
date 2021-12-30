(ns com.yetanalytics.squuid
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.squuid.uuid :as u]
            [com.yetanalytics.squuid.time :as t]
            #?(:clj [clojure.spec.gen.alpha :as sgen])))

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
(s/def ::timestamp
  #?(:clj (s/with-gen (partial instance? java.time.Instant)
            #(sgen/fmap (fn [ts] (t/ms->Instant (inst-ms ts)))
                        (s/gen inst?)))
     :cljs (s/with-gen (partial instance? js/Date)
             #(s/gen inst?))))

;; The atom is private so that only generate-squuid(*) can mutate it.
;; Note that merging Instant/EPOCH with v0 UUID returns the v0 UUID again.
(def ^:private current-time-atom
  (atom {:timestamp t/zero-time
         :base-uuid u/zero-uuid
         :squuid    u/zero-uuid}))

(defn reset-all!
  "Reset such that the starting timestamp and UUIDs are zeroed out. This
   function is intended for use in development/testing."
  []
  (reset! current-time-atom
          {:timestamp t/zero-time
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
   
   See `generate-squuid` for more details."
  []
  (let [ts (t/current-time)]
    (swap! current-time-atom
           (fn [m]
             (if (t/before? (:timestamp m) ts)
               (-> m
                   (assoc :timestamp ts)
                   (merge (u/make-squuid ts)))
               (-> m
                   (update :base-uuid u/inc-uuid)
                   (update :squuid u/inc-uuid)))))))

(s/fdef generate-squuid
  :args (s/cat)
  :ret ::squuid)

(defn generate-squuid
  "Return a new v8 sequential UUID, or SQUUID. The most significant 48 bits
   are created from a timestamp representing the current time, which always
   increments in value. The least significant 80 bits are derived from
   a base v4 UUID; since 6 bits are reserved (4 for the version and 2 for the
   variant), this leaves 74 random bits, allowing for about 18.9 sextillion
   random segments.
   
   The timestamp is coerced to millisecond resolution. Due to the 48 bit
   maximum on the timestamp, the latest time supported is August 2, 10889.
   
   In case that this function (or `generate-squuid*`) is called multiple times
   in the same millisecond, subsequent SQUUIDs are created by incrementing the
   base UUID and thus the random segment of the SQUUID. An exception is thrown
   in the unlikely case where all 74 random bits are 1s and incrementing can no
   longer occur."
  []
  (:squuid (generate-squuid*)))

(s/fdef time->uuid
  :args (s/cat :ts (s/and inst? #(<= 0 (inst-ms %) t/max-seconds)))
  :ret ::squuid)

(defn time->uuid
  "Convert a timestamp to a UUID. The upper 48 bits represent
   the timestamp, while the lower 80 bits are fixed at
   `8FFF-8FFF-FFFFFFFFFFFF`."
  [ts]
  (assert (inst? ts))
  (:squuid
   (u/make-squuid ts #uuid "00000000-0000-4FFF-8FFF-FFFFFFFFFFFF")))

(s/fdef uuid->time
  :args (s/cat :uuid ::squuid)
  :ret ::timestamp)

(defn uuid->time
  "Convert a previously generated `uuid` to its corresponding timestamp.
   Returns a java.time.Instant object in Clojure, #inst in ClojureScript."
  [uuid]
  (assert (uuid? uuid))
  #?(:clj
     (-> uuid
         (u/extract-ts-bytes)
         (t/ms->Instant))
     :cljs
     (-> uuid
         (u/extract-ts-bytes)
         (js/Date.))))
