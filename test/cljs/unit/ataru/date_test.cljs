(ns ataru.date-test
  (:require [ataru.date :as date]
            [cljs.test :refer-macros [deftest is testing]]
            [cljs-time.core :refer [local-date]]))

(deftest test-is-minor
  (testing "Person is minor"
    (is (false? (date/years-between? (local-date 2010 10 1) (local-date 2021 4 3) 18)) "18 years should not have passed")
    (is (false? (date/years-between? (local-date 2003 10 1) (local-date 2021 4 3) 18)) "18 years should not have passed")
    (is (false? (date/years-between? (local-date 2003 4 4) (local-date 2021 4 3) 18)) "18 years should not be passed"))
  (testing "Person is full age"
    (is (true? (date/years-between? (local-date 2000 10 1) (local-date 2021 4 3) 18)) "18 years should have passed")
    (is (true? (date/years-between? (local-date 2003 4 1) (local-date 2021 4 3) 18)) "18 years should have passed")
    (is (true? (date/years-between? (local-date 2003 4 3) (local-date 2021 4 3) 18)) "18 years should have passed")))
