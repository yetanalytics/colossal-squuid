(ns com.yetanalytics.squuid-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.yetanalytics.squuid :as squuid]
            [com.yetanalytics.squuid.uuid :refer [before? equals?]]
            [com.yetanalytics.squuid.test-support :refer [check-validate]]))

(deftest squuid-test
  (testing "squuid gentests"
    (is (nil? (check-validate `squuid/generate-squuid*)))
    (is (nil? (check-validate `squuid/generate-squuid))))
  (testing "squuid monotonicity"
    (is
     (loop [squuid-seq  (repeatedly 10000 squuid/generate-squuid)
            squuid-seq' (rest squuid-seq)]
       (if (empty? squuid-seq')
         true
         (let [prev-squuid (first squuid-seq)
               next-squuid (first squuid-seq')]
           (if (before? prev-squuid next-squuid)
             (recur squuid-seq' (rest squuid-seq'))
             false))))))
  (testing "squuid monotonicity (sort)"
    (let [squuid-seq  (repeatedly 1000 squuid/generate-squuid)
          squuid-seq' (sort (fn [u1 u2] (.compareTo u1 u2)) squuid-seq)]
      (is (every? (fn [[u1 u2]] (equals? u1 u2))
                  (map (fn [u1 u2] [u1 u2]) squuid-seq squuid-seq'))))))
