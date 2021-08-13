(ns com.yetanalytics.squuid.uuid
  #?(:clj (:import [java.util UUID])
     :cljs (:require [clojure.string :refer [join]])))

#?(:clj (set! *warn-on-reflection* true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private constants
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:clj
   (def ^{:private true :const true} bit-mask-12
     (unchecked-long 0x0000000000000FFF)))

#?(:clj
   (def ^{:private true :const true} bit-mask-16
     (unchecked-long 0x000000000000FFFF)))

#?(:clj
   (def ^{:private true :const true} bit-mask-61
     (unchecked-long 0x1FFFFFFFFFFFFFFF)))

#?(:cljs
   (def ^:private bit-mask-16 (js/BigInt 0xFFFF)))

#?(:cljs
   (def ^:private bit-shift-16 (js/BigInt 16)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helper vars and functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def zero-uuid
  "The v0 UUID of all zeroes."
  #uuid "00000000-0000-0000-0000-000000000000")

(defn rand-uuid
  "Generate a random v4 UUID."
  []
  #?(:clj (UUID/randomUUID)
     :cljs (random-uuid)))

(defn compare-uuid
  "Returns:
   - (< uuid1 uuid2): -1
   - (= uuid1 uuid2): 0
   - (> uuid1 uuid2): 1"
  [^UUID u1 ^UUID u2]
  #?(:clj
     (.compareTo u1 u2)
     :cljs
     (let [u1 (.toString u1)
           u2 (.toString u2)]
       (cond
         (< u1 u2) -1
         (= u1 u2) 0
         (> u1 u2) 1))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Major functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- throw-inc-uuid-error
  [u]
  (throw (ex-info (str "Cannot increment UUID " u " any further.")
                  {:type ::exceeded-max-uuid-node
                   :uuid u})))

(defn inc-uuid
  "Increment the UUID by one bit. Throws an exception if there are no available
   bits left to increment."
  [^UUID u]
  #?(:clj ; Use bit operations
     (let [uuid-msb (.getMostSignificantBits u)
           uuid-lsb (.getLeastSignificantBits u)]
       (cond
         ;; least significant bits not maxed out
         (not (zero? (bit-and-not bit-mask-61 uuid-lsb)))
         (UUID. uuid-msb (inc uuid-lsb))
         ;; most significant bits not maxed out
         (not (zero? (bit-and-not bit-mask-12 uuid-msb)))
         (UUID. (inc uuid-msb) uuid-lsb)
        ;; oh no
         :else
         (throw-inc-uuid-error u)))

     :cljs ; Use string operations
     (let [u-char-arr (js/Array.from (.toString u))
           inc-char   (fn [c] (.toString (inc (js/parseInt c 16)) 16))
           ret-uuid   (fn [char-arr] (UUID. (join char-arr) nil))]
       (loop [i 35] ; start from the back and inch forwards
         (cond
           ;; Regular hexes: 0x0 to 0xF
           (or (< 23 i)
               (< 19 i 23)
               (< 14 i 18))
           (let [c (aget u-char-arr i)]
             (if (or (identical? "F" c) (identical? "f" c))
               (do (aset u-char-arr i "0")
                   (recur (dec i)))
               (do (aset u-char-arr i (inc-char c))
                   (ret-uuid u-char-arr))))

           ;; Variant hexes: 0x0 to 0xB
           (= 17 i)
           (let [c (aget u-char-arr i)]
             (if (or (identical? "B" c) (identical? "b" c))
               (do (aset u-char-arr i "0")
                   (recur (dec i)))
               (do (aset u-char-arr i (inc-char c))
                   (ret-uuid u-char-arr))))

           ;; Dashes: ignore
           (#{18 23} i)
           (recur (dec i))

           :else
           (throw-inc-uuid-error u))))))

(defn make-squuid
  "Make a new v8 sequential UUID. Uses `uuid` as the base UUID if provided;
   otherwise uses a random v4 UUID as the base. Returns a map containing
   `:base-uuid` and `:squuid`."
  ([ts]
   (make-squuid ts (rand-uuid)))
  ([ts ^UUID u]
   #?(:clj ; Use bit operations
      (let [;; Base UUID
            uuid-msb  (.getMostSignificantBits u)
            uuid-lsb  (.getLeastSignificantBits u)
            ;; Timestamp
            ts-long   (inst-ms ts)
            ;; Putting it all together (and change version from v4 to v8)
            uuid-msb' (-> (bit-or (bit-shift-left ts-long 16)
                                  (bit-and bit-mask-16 uuid-msb))
                          (bit-clear 14)
                          (bit-set 15))
            squuid    (UUID. uuid-msb' uuid-lsb)]
        {:base-uuid u
         :squuid    squuid})

      :cljs ; Use string operations
      (let [make-padding
            (fn [max-len s] (join (repeat (- max-len (count s)) "0")))
            ;; Timestamp manips
            ts'   (js/BigInt (inst-ms ts))
            ts-hi (bit-shift-right ts' bit-shift-16)
            ts-lo (bit-and ts' bit-mask-16)
            ts-hs (.toString ts-hi 16)
            ts-ls (.toString ts-lo 16)
            ts-hp (make-padding 8 ts-hs)
            ts-lp (make-padding 4 ts-ls)
            ;; Base UUID manips
            u-str  (subs (.toString u) 15)
            ;; Cook some SQUUID
            raw-squuid    (str ts-hp ts-hs
                               "-"
                               ts-lp ts-ls
                               "-"
                               "8" u-str)
            cooked-squuid (UUID. raw-squuid nil)]
        {:base-uuid u
         :squuid    cooked-squuid}))))

#?(:clj (set! *warn-on-reflection* false))
