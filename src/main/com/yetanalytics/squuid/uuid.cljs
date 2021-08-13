(ns com.yetanalytics.squuid.uuid
  (:require [clojure.string :as cstr :refer [join]]
            [com.yetanalytics.squuid.time :as t]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- rand-uuid
  []
  (random-uuid))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn inc-uuid
  [u]
  (let [u-char-arr (js/Array.from (.toString u))
        inc-char   (fn [c] (.toString (inc (js/parseInt c 16)) 16))
        ret-uuid   (fn [char-arr] (UUID. (join char-arr) nil))]
    (loop [i 35]
      (cond
        ;; Regular hexes: 0-F
        (or (< 23 i)
            (< 19 i 23)
            (< 14 i 18))
        (let [c (aget u-char-arr i)]
          (if (or (identical? "F" c) (identical? "f" c))
            (do (aset u-char-arr i "0")
                (recur (dec i)))
            (do (aset u-char-arr i (inc-char c))
                (ret-uuid u-char-arr))))

        ;; Variant hexes: 0-B
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
        (throw (ex-info (str "Cannot increment UUID " u " any further.")
                        {:type ::exceeded-max-uuid-node
                         :uuid u}))))))

(def ^:private bit-mask-16 (js/BigInt 0xFFFF))
(def ^:private bit-shift-16 (js/BigInt 16))

(defn make-squuid
  ([ts]
   (make-squuid ts (rand-uuid)))
  ([ts u]
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
      :squuid    cooked-squuid})))

(defn zero-uuid
  []
  #uuid "00000000-0000-0000-0000-000000000000")

(defn before?
  [u1 u2]
  (< u1 u2))

(defn equals?
  [u1 u2]
  (= u1 u2))

(defn after?
  [u1 u2]
  (> u1 u2))

(defn compare-uuid
  [u1 u2]
  (cond
    (< u1 u2) -1
    (= u1 u2) 0
    (> u1 u2) 1))
