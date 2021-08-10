(ns com.yetanalytics.squuid.clj.uuid
  (:import [java.util UUID]))

(set! *warn-on-reflection* true)

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

(set! *warn-on-reflection* false)
