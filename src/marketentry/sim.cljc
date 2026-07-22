(ns marketentry.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean engagement
  through intake -> jurisdiction assessment -> filing draft
  (escalate/approve/commit) -> filing submit (escalate/approve/
  commit), then shows HARD-hold scenarios, including BOTH paths of the
  Foreign Investment Act dual-path flagship check and its negative
  controls."
  (:require [langgraph.graph :as g]
            [marketentry.store :as store]
            [marketentry.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :market-entry-operator :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- assess-draft-approve! [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-assess") {:op :jurisdiction/assess :subject subject} operator)
  (approve! actor (str tid-prefix "-assess"))
  (exec-op actor (str tid-prefix "-draft") {:op :filing/draft :subject subject} operator)
  (approve! actor (str tid-prefix "-draft")))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== engagement/intake eng-1 (TTO, clean) ==")
    (println (exec-op actor "t1" {:op :engagement/intake :subject "eng-1"
                                  :patch {:id "eng-1" :operator "Piarco Logistics Ltd"}} operator))

    (println "== jurisdiction/assess eng-1 (escalates -- human approves) ==")
    (println (exec-op actor "t2" {:op :jurisdiction/assess :subject "eng-1"} operator))
    (println (approve! actor "t2"))

    (println "== filing/draft eng-1 (always escalates -- actuation/draft-filing) ==")
    (let [r (exec-op actor "t3" {:op :filing/draft :subject "eng-1"} operator)]
      (println r)
      (println "-- human market-entry operator approves --")
      (println (approve! actor "t3")))

    (println "== filing/submit eng-1 (always escalates -- actuation/submit-filing) ==")
    (let [r (exec-op actor "t4" {:op :filing/submit :subject "eng-1"} operator)]
      (println r)
      (println "-- human market-entry operator approves --")
      (println (approve! actor "t4")))

    (println "== jurisdiction/assess eng-2 (no spec-basis -> HARD hold) ==")
    (println (exec-op actor "t5" {:op :jurisdiction/assess :subject "eng-2" :no-spec? true} operator))

    (println "== engagement-fee-mismatch (eng-3) ==")
    (assess-draft-approve! actor "t6pre" "eng-3")
    (println "== filing/submit eng-3 (fee mismatch -> HARD hold) ==")
    (println (exec-op actor "t7" {:op :filing/submit :subject "eng-3"} operator))

    (println "== foreign-investment-noncompliant, PRIVATE path (eng-4: foreign investor, s.4 notice not filed) ==")
    (assess-draft-approve! actor "t8pre" "eng-4")
    (println "== filing/submit eng-4 (private s.4 notice unfiled -> HARD hold) ==")
    (println (exec-op actor "t9" {:op :filing/submit :subject "eng-4"} operator))

    (println "== bir-number-unverified (eng-5) ==")
    (assess-draft-approve! actor "t10pre" "eng-5")
    (println "== filing/submit eng-5 (BIR File Number unverified -> HARD hold) ==")
    (println (exec-op actor "t11" {:op :filing/submit :subject "eng-5"} operator))

    (println "== foreign-investment-noncompliant, PUBLIC path (eng-6: 45% foreign shareholding, s.5(2) licence not held) ==")
    (assess-draft-approve! actor "t12pre" "eng-6")
    (println "== filing/submit eng-6 (public 45% foreign shareholding, no licence -> HARD hold) ==")
    (println (exec-op actor "t13" {:op :filing/submit :subject "eng-6"} operator))

    (println "== negative control: PUBLIC company BELOW 30% threshold, no licence held (eng-7) ==")
    (assess-draft-approve! actor "t14pre" "eng-7")
    (println "== filing/submit eng-7 (20% foreign shareholding is below s.5(2)'s 30% threshold -- clean, escalates for human sign-off only) ==")
    (let [r (exec-op actor "t15" {:op :filing/submit :subject "eng-7"} operator)]
      (println r)
      (println (approve! actor "t15")))

    (println "== negative control: PRIVATE foreign investor, s.4 notice IS filed (eng-8) ==")
    (assess-draft-approve! actor "t16pre" "eng-8")
    (println "== filing/submit eng-8 (s.4 notice on file -- clean, escalates for human sign-off only) ==")
    (let [r (exec-op actor "t17" {:op :filing/submit :subject "eng-8"} operator)]
      (println r)
      (println (approve! actor "t17")))

    (println "== filing/draft eng-1 AGAIN (double-draft -> HARD hold) ==")
    (println (exec-op actor "t18" {:op :filing/draft :subject "eng-1"} operator))

    (println "== filing/submit eng-1 AGAIN (double-submit -> HARD hold) ==")
    (println (exec-op actor "t19" {:op :filing/submit :subject "eng-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft records ==")
    (doseq [r (store/draft-history db)] (println r))

    (println "== submit records ==")
    (doseq [r (store/submit-history db)] (println r))))
