(ns com.yetanalytics.squuid.time-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.yetanalytics.squuid.time :as t]))

(deftest max-time-test
  (testing "before max time assertion"
    (is (<= #?(:clj (System/currentTimeMillis)
               :cljs (.now js/Date))
            t/max-seconds))
    (is (<= 0x0000FFFFFFFFFFFF t/max-seconds))
    (is (not (<= 0x0001000000000001 t/max-seconds)))
    ;; clj does not play well with 5-digit years
    #?(:cljs (= #inst "10889-08-01T05:31:50.655Z"
                (js/Date. t/max-seconds)))))
(deftest before-test
  (testing "before? function"
    (let [curr-time (t/current-time)]
      (is (t/before? t/zero-time curr-time))
      (is (not (t/before? curr-time t/zero-time)))
      (is (not (t/before? t/zero-time t/zero-time)))
      (is (not (t/before? curr-time curr-time))))))
