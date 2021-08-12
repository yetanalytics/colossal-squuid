(ns com.yetanalytics.squuid.uuid
  (:require [clojure.string :refer [join]]
            [com.yetanalytics.squuid.time :as t]))

(defn- bytes->hex-seq
  [byts num-hex]
  (loop [byts byts
         hlen num-hex
         hseq '()]
    (if-not (<= hlen 0)
      (recur (bit-shift-right byts 4)
             (dec hlen)
             (cons (bit-and byts 0xF) hseq))
      hseq)))

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
  (-> (repeatedly 32 #(rand-int 16))
      vec
      (assoc 12 4)
      (update 0 #(-> % (bit-clear 2) (bit-set 3)))))

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

(defn- make-squuid*
  [ts u]
  (let [ts'       (t/time->millis ts)
        [msb lsb] (split-at 16 u)
        msb'      (concat (bytes->hex-seq ts' 12)
                          '(8)
                          (drop 13 msb))]
    (concat msb' lsb)))

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
   (let [raw-base (rand-uuid-raw)
         raw-squuid (make-squuid* ts raw-base)
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

(comment
  (hex-seq->uuid (uuid->hex-seq #uuid "a8b10dee-13a4-41f2-6eac-b60be801e95e"))
  (hex-seq->uuid [10 8 11 1 0 13 14 14 1 3 10 4 4 1 15 2 6 14 10 12 11 6 0 11 14 8 0 1 14 9 5 14])
  (make-squuid (js/Date. 0)))