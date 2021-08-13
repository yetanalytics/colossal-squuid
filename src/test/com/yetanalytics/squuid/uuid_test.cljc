(ns com.yetanalytics.squuid.uuid-test
  (:require [clojure.test :refer [deftest testing is are]]
            [com.yetanalytics.squuid.uuid :as u]))

(deftest inc-uuid-test
  (testing "incrementing UUIDs"
    (is (thrown-with-msg?
         #?(:clj clojure.lang.ExceptionInfo
            :cljs js/Error)
         #"Cannot increment UUID 00000000-0000-4fff-bfff-ffffffffffff any further."
         (u/inc-uuid #uuid "00000000-0000-4FFF-BFFF-FFFFFFFFFFFF")))
    (are [u1 u2] (= u1 u2)
      #uuid "00000000-0000-0000-0000-000000000001"
      (u/inc-uuid u/zero-uuid)
      #uuid "12345678-1234-4234-8234-123456789ABD"
      (u/inc-uuid #uuid "12345678-1234-4234-8234-123456789ABC")
      #uuid "0000-0000-4000-8100-000000000000"
      (u/inc-uuid #uuid "00000000-0000-4000-80FF-FFFFFFFFFFFF")
      #uuid "00000000-0000-4000-8000-000000000010"
      (u/inc-uuid #uuid "00000000-0000-4000-8000-00000000000F")
      #uuid "00000000-0000-4000-8000-F00000000000"
      (u/inc-uuid #uuid "00000000-0000-4000-8000-EFFFFFFFFFFF")
      #uuid "00000000-0000-4000-8001-000000000000"
      (u/inc-uuid #uuid "00000000-0000-4000-8000-FFFFFFFFFFFF")
      #uuid "00000000-0000-4000-8100-000000000000"
      (u/inc-uuid #uuid "00000000-0000-4000-80FF-FFFFFFFFFFFF")
      #uuid "00000000-0000-4000-9000-000000000000"
      (u/inc-uuid #uuid "00000000-0000-4000-8FFF-FFFFFFFFFFFF")
      #uuid "00000000-0000-4001-8000-000000000000"
      (u/inc-uuid #uuid "00000000-0000-4000-BFFF-FFFFFFFFFFFF")
      #uuid "00000000-0000-4F00-8000-00000000000"
      (u/inc-uuid #uuid "00000000-0000-4EFF-BFFF-FFFFFFFFFFFF")
      #uuid "00000000-0000-4FFF-BFFF-FFFFFFFFFFFF"
      (u/inc-uuid #uuid "00000000-0000-4FFF-BFFF-FFFFFFFFFFFE"))))
