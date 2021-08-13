(ns com.yetanalytics.squuid.uuid
  (:require [clojure.string :as cstr :refer [join]]
            [com.yetanalytics.squuid.time :as t]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private hex-bitsize (js/BigInt 4))
(def ^:private hex-bitmask (js/BigInt 0xF))

(defn- bytes->hex-seq
  [byts num-hex]
  (loop [byts byts
         hlen num-hex
         hseq '()]
    (if-not (<= hlen 0)
      (recur (bit-shift-right byts hex-bitsize)
             (dec hlen)
             (cons (bit-and byts hex-bitmask) hseq))
      (map #(js/Number %) hseq))))

(defn- uuid->hex-seq
  [u]
  (let [ustr (.toString u)]
    (loop [idx  0
           hseq (transient [])]
      (if (< idx 36)
        (let [c (.charAt ustr idx)]
          (if (= "-" c)
            (recur (inc idx) hseq)
            (recur (inc idx) (conj! hseq (js/parseInt c 16)))))
        (persistent! hseq)))))

(defn hex-seq->uuid
  [raw-uuid]
  (let [raw-uuid'     (map #(.toString % 16) raw-uuid)
        [str-1 rst-1] (split-at 8 raw-uuid')
        [str-2 rst-2] (split-at 4 rst-1)
        [str-3 rst-3] (split-at 4 rst-2)
        [str-4 str-5] (split-at 4 rst-3)]
    (UUID. (join "-" (map join [str-1 str-2 str-3 str-4 str-5]))
           nil)))

(defn- rand-uuid-raw
  []
  (apply array
         (-> (repeatedly 32 #(rand-int 16))
             vec
             (assoc 12 4)
             (update 0 #(-> % (bit-clear 2) (bit-set 3))))))

(defn- rand-uuid
  []
  (random-uuid))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- inc-uuid*
  [u]
  (loop [u u
         i 31]
    (cond
      ;; Normal random hex is maxed out
      (or (< 16 i)
          (< 12 i 16))
      (if (= 15 (get u i))
        (recur (assoc u i 0) (dec i)) ; carry
        (update u i inc))

      ;; Hex with variant is maxed out
      (= 16 i)
      (if (= 11 (get u i))
        (recur (assoc u i 8) (dec i)) ; carry
        (update u i inc))

      ;; oh no
      :else
      nil)))

(defn- inc-uuid-2
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

(defn- make-squuid*
  [ts u]
  (let [ts'       (js/BigInt (t/time->millis ts))
        [msb lsb] (split-at 16 u)
        msb'      (concat (bytes->hex-seq ts' 12)
                          '(8)
                          (drop 13 msb))]
    (concat msb' lsb)))

(def ^:private bit-mask-16 (js/BigInt 0xFFFF))
(def ^:private bit-shift-16 (js/BigInt 16))

(defn- make-squuid-2
  ([ts]
   (make-squuid-2 ts (rand-uuid)))
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

(defn inc-uuid
  [u]
  (let [parsed-u (uuid->hex-seq u)]
    (if-some [inc-u (inc-uuid* parsed-u)]
      (hex-seq->uuid inc-u)
      (throw (ex-info (str "Cannot increment UUID " u " any further.")
                      {:type ::exceeded-max-uuid-node
                       :uuid u})))))

(defn make-squuid
  ([ts]
   (let [raw-base      (rand-uuid-raw)
         raw-squuid    (make-squuid* ts raw-base)
         cooked-squuid (hex-seq->uuid raw-squuid)]
     {:base-uuid (hex-seq->uuid raw-base)
      :squuid    cooked-squuid}))
  ([ts & [u]]
   (let [raw-base      (uuid->hex-seq u)
         raw-squuid    (make-squuid* ts raw-base)
         cooked-squuid (hex-seq->uuid raw-squuid)]
     {:base-uuid u
      :squuid    cooked-squuid})))

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

(comment

  (def ct (t/current-time))
  (def rv4 (rand-uuid))

  ;; ~40 ms
  (time
   (dotimes [_ 1000]
     (inc-uuid rv4)))
  
  ;; ~7 ms
  (time
   (dotimes [_ 1000]
     (inc-uuid-2 rv4)))
  
  ;; ~50 ms
  (time
   (dotimes [_ 1000]
     (:squuid (make-squuid ct rv4))))

  ;; ~2.5 ms
  (time
   (dotimes [_ 1000]
     (:squuid (make-squuid-2 ct rv4))))
  
  ;; ~80 ms
  (time
   (dotimes [_ 1000]
     (:squuid (make-squuid ct))))
  
  ;; ~7 ms
  (time
   (dotimes [_ 1000]
     (:squuid (make-squuid-2 ct))))

  )
