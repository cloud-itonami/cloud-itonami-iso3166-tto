(ns marketentry.registry-test
  (:require [clojure.test :refer [deftest is testing]]
            [marketentry.registry :as registry]))

(deftest engagement-fee-recompute
  (let [e {:base-fee 500000 :monthly-rate 30000 :monitoring-months 12 :claimed-fee 860000.0}]
    (is (== 860000.0 (registry/compute-engagement-fee e)))
    (is (true? (registry/engagement-fee-matches-claim? e))))
  (let [bad {:base-fee 500000 :monthly-rate 30000 :monitoring-months 12 :claimed-fee 999000.0}]
    (is (false? (registry/engagement-fee-matches-claim? bad)))))

(deftest register-draft-and-submit
  (let [d (registry/register-draft "eng-1" "TTO" 0)
        s (registry/register-submit "eng-1" "TTO" 0)]
    (is (= "TTO-DFT-000000" (get d "draft_number")))
    (is (= "TTO-SUB-000000" (get s "submit_number")))
    (is (nil? (get-in d ["certificate" "proof"])))
    (is (= "draft-unsigned" (get-in s ["certificate" "status"])))))

(deftest register-requires-ids
  (is (thrown? Exception (registry/register-draft "" "TTO" 0)))
  (is (thrown? Exception (registry/register-submit "eng-1" "" 0))))

(deftest foreign-investment-private-notice-required
  (testing "PRIVATE company + foreign investor -> s.4 notice duty applies, unconditional on percentage"
    (is (true? (registry/foreign-investment-private-notice-required?
                {:operator-company-type :private :foreign-investor? true})))
    (is (true? (registry/foreign-investment-private-notice-required?
                {:operator-company-type :private :foreign-investor? true :foreign-shareholding-pct 1}))
        "even a tiny shareholding trips s.4 -- no percentage gate on the private path"))
  (testing "wholly domestic private company -> s.4 does not apply"
    (is (false? (registry/foreign-investment-private-notice-required?
                 {:operator-company-type :private :foreign-investor? false}))))
  (testing "public company -> the private-path gate never applies, regardless of foreign-investor?"
    (is (false? (registry/foreign-investment-private-notice-required?
                 {:operator-company-type :public :foreign-investor? true})))))

(deftest foreign-investment-public-licence-required
  (testing "PUBLIC company at or above the 30% threshold -> s.5(2) licence required"
    (is (true? (registry/foreign-investment-public-licence-required?
                {:operator-company-type :public :foreign-shareholding-pct 30})))
    (is (true? (registry/foreign-investment-public-licence-required?
                {:operator-company-type :public :foreign-shareholding-pct 45}))))
  (testing "PUBLIC company below the 30% threshold -> no licence required"
    (is (false? (registry/foreign-investment-public-licence-required?
                 {:operator-company-type :public :foreign-shareholding-pct 20}))))
  (testing "PRIVATE company -> the public-path gate never applies, regardless of percentage"
    (is (false? (registry/foreign-investment-public-licence-required?
                 {:operator-company-type :private :foreign-shareholding-pct 90})))))

(deftest foreign-investment-noncompliant
  (testing "PRIVATE path: foreign investor, s.4 notice not filed -> noncompliant"
    (is (true? (registry/foreign-investment-noncompliant?
                {:operator-company-type :private :foreign-investor? true
                 :foreign-investment-notice-filed? false}))))
  (testing "PRIVATE path: foreign investor, s.4 notice filed -> compliant"
    (is (false? (registry/foreign-investment-noncompliant?
                 {:operator-company-type :private :foreign-investor? true
                  :foreign-investment-notice-filed? true}))))
  (testing "PRIVATE path: not a foreign investor at all -> never noncompliant regardless of notice flag"
    (is (false? (registry/foreign-investment-noncompliant?
                 {:operator-company-type :private :foreign-investor? false
                  :foreign-investment-notice-filed? false}))))
  (testing "PUBLIC path: >=30% foreign shareholding, no licence held -> noncompliant"
    (is (true? (registry/foreign-investment-noncompliant?
                {:operator-company-type :public :foreign-shareholding-pct 45
                 :foreign-investor-licence-held? false}))))
  (testing "PUBLIC path: >=30% foreign shareholding, licence held -> compliant"
    (is (false? (registry/foreign-investment-noncompliant?
                 {:operator-company-type :public :foreign-shareholding-pct 45
                  :foreign-investor-licence-held? true}))))
  (testing "PUBLIC path: below 30% threshold -> never noncompliant even without a licence"
    (is (false? (registry/foreign-investment-noncompliant?
                 {:operator-company-type :public :foreign-shareholding-pct 20
                  :foreign-investor-licence-held? false})))))
