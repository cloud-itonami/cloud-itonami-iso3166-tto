(ns marketentry.facts-test
  (:require [clojure.test :refer [deftest is testing]]
            [marketentry.facts :as facts]))

(deftest tto-has-spec-basis
  (let [sb (facts/spec-basis "TTO")]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (some? (facts/corporate-number-spec-basis "TTO")))
    (is (some? (facts/business-registration-spec-basis "TTO")))
    (is (some? (facts/foreign-investment-spec-basis "TTO")))))

(deftest tto-rep-spec-basis-is-populated
  (testing "OPR Ineligibility Proceedings mechanism -- genuinely populated, descriptive grounding for the evidence checklist"
    (is (some? (facts/rep-spec-basis "TTO")))))

(deftest unknown-jurisdiction-has-no-spec-basis
  (is (nil? (facts/spec-basis "ATL")))
  (is (nil? (facts/spec-basis "ZZZ"))))

(deftest required-evidence-satisfied
  (let [sb (facts/spec-basis "TTO")
        all (:required-evidence sb)]
    (is (true? (facts/required-evidence-satisfied? "TTO" all)))
    (is (not (facts/required-evidence-satisfied? "TTO" (take 1 all))))
    (is (nil? (facts/required-evidence-satisfied? "ATL" all)))))

(deftest coverage-is-honest
  (let [c (facts/coverage ["TTO" "USA" "ATL"])]
    (is (= 3 (:requested c)))
    (is (= 2 (:covered c)))
    (is (= ["ATL"] (:missing-jurisdictions c)))))
