(ns com.yetanalytics.squuid-test
  #?@(:clj [(:require
             [clojure.test :refer [deftest testing is]]
             [clojure.spec.test.alpha :refer [check instrument]]
             [com.yetanalytics.squuid :as squuid]
             [com.yetanalytics.squuid.uuid :refer [compare-uuid]]
             [com.yetanalytics.squuid.time :as t])]
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
                 `squuid/time->uuid
                 `squuid/uuid->time])
    (is (every? #(-> % :clojure.spec.test.check/ret :pass?)
                (check [`squuid/generate-squuid*
                        `squuid/generate-squuid
                        `squuid/time->uuid
                        `squuid/uuid->time])))))

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
                  (map (fn [u1 u2] [u1 u2]) squuid-seq squuid-seq')))))
  ;; Test for: https://github.com/yetanalytics/colossal-squuid/issues/10
  #?(:clj ; Rip cljs and its single thread.
     (testing "squuid monotonicity (multi-threaded)"
       (with-redefs [t/current-time #(java.time.Instant/ofEpochMilli 100)]
         (is (->> (fn []
                    (let [_       (squuid/reset-all!)
                          [f1 f2] (pmap
                                   (fn [f] (future (f)))
                                   (repeat 2 squuid/generate-squuid))
                          max-1   (last (sort [@f1 @f2]))
                          new-u   (squuid/generate-squuid)
                          max-2   (last (sort [max-1 new-u]))]
                      [new-u max-2]))
                  (repeatedly 30)
                  (every? (partial apply =))))))))

(deftest uuid->time-test
  (testing
   (is (let [id (squuid/generate-squuid*)
             time (squuid/uuid->time (:squuid id))]
         (= (:timestamp id) time)))))
