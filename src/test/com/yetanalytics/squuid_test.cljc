(ns com.yetanalytics.squuid-test
  #?@(:clj [(:require
             [clojure.test :refer [deftest testing is]]
             [clojure.spec.test.alpha :refer [check instrument]]
             [com.yetanalytics.squuid :as squuid]
             [com.yetanalytics.squuid.uuid :refer [compare-uuid]])]
      :cljs [(:require
              [goog.math]
              [clojure.test.check]
              [clojure.test.check.properties]
              [cljs.test :refer [deftest testing is]]
              [cljs.spec.test.alpha]
              [com.yetanalytics.squuid :as squuid]
              [com.yetanalytics.squuid.uuid :refer [compare-uuid]])
             (:require-macros
              [cljs.spec.test.alpha :refer [check instrument]])]))

(deftest squuid-gentest
  (testing "squuid gentests"
    (instrument [`squuid/generate-squuid*
                 `squuid/generate-squuid
                 `squuid/time->uuid])
    (is (every? #(-> % :clojure.spec.test.check/ret :pass?)
                (check [`squuid/generate-squuid*
                        `squuid/generate-squuid
                        `squuid/time->uuid])))))

(deftest squuid-monotone-test
  (testing "squuid monotonicity"
    (is
     (loop [squuid-seq  (repeatedly 10000 squuid/generate-squuid)
            squuid-seq' (rest squuid-seq)]
       (if (empty? squuid-seq')
         true
         (let [prev-squuid (first squuid-seq)
               next-squuid (first squuid-seq')]
           (if (neg-int? (compare-uuid prev-squuid next-squuid))
             (recur squuid-seq' (rest squuid-seq'))
             false))))))
  (testing "squuid monotonicity (sort)"
    (let [squuid-seq  (repeatedly 1000 squuid/generate-squuid)
          squuid-seq' (sort (fn [u1 u2] (compare-uuid u1 u2)) squuid-seq)]
      (is (every? (fn [[u1 u2]] (zero? (compare-uuid u1 u2)))
                  (map (fn [u1 u2] [u1 u2]) squuid-seq squuid-seq'))))))
