(ns statute.facts-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [statute.facts :as facts]))

(deftest tto-has-spec-basis
  (let [sb (facts/spec-basis "TTO")]
    (is (= 3 (count sb)))
    (is (every? #(str/starts-with? (:statute/url %) "https://") sb))
    (is (every? :statute/law-number sb))))

(deftest unknown-jurisdiction-has-no-spec-basis
  (is (nil? (facts/spec-basis "ATL")))
  (is (nil? (facts/spec-basis "ZZZ"))))

(deftest coverage-is-honest
  (let [c (facts/coverage ["TTO" "JPN" "ATL"])]
    (is (= 3 (:requested c)))
    (is (= 1 (:covered c)))
    (is (= ["ATL" "JPN"] (:missing-jurisdictions c)))))

(deftest by-topic-filters
  (is (= ["tto.industrial-relations-act"]
         (mapv :statute/id (facts/by-topic "TTO" :labor))))
  (is (= ["tto.foreign-investment-act"]
         (mapv :statute/id (facts/by-topic "TTO" :foreign-investment))))
  (is (empty? (facts/by-topic "TTO" :environment)))
  (is (empty? (facts/by-topic "ATL" :labor))))
