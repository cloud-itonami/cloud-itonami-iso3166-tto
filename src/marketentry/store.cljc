(ns marketentry.store
  "SSoT for the Trinidad and Tobago (TTO) market-entry compliance actor,
  behind a `Store` protocol so the backend is a swap, not a rewrite --
  the same seam every prior cloud-itonami actor in this fleet uses.

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store.

  Both implement the same protocol and pass the same contract
  (test/marketentry/store_contract_test.clj).

  The primary entity here is an `engagement` -- filing-draft and
  filing-submit actuation events apply SEQUENTIALLY to the SAME
  engagement record (draft first, submit later). Dedicated
  double-actuation-guard booleans (`:drafted?`/`:submitted?`, never a
  `:status` value).

  `:operator-company-type` / `:foreign-investor?` /
  `:foreign-shareholding-pct` / `:foreign-investment-notice-filed?` /
  `:foreign-investor-licence-held?` are ground truth for the
  `foreign-investment-noncompliant?` flagship check (Foreign Investment
  Act Chap. 70:07 s.4 private-company notification / s.5(2) public-
  company 30%-shareholding licence). `:bir-number-verified?` is ground
  truth for the BIR File Number check (ird.gov.tt's own text: 'All
  companies are required to register with the Board of Inland Revenue
  to obtain a BIR File Number').

  The ledger stays append-only on every backend."
  (:require [marketentry.registry :as registry]
            [langchain.db :as d]
            [langchain-store.core :as ls]))

(defprotocol Store
  (engagement [s id])
  (all-engagements [s])
  (assessment-of [s engagement-id] "committed jurisdiction assessment, or nil")
  (ledger [s])
  (draft-history [s] "the append-only filing-draft history")
  (submit-history [s] "the append-only filing-submit history")
  (next-draft-sequence [s jurisdiction])
  (next-submit-sequence [s jurisdiction])
  (engagement-already-drafted? [s engagement-id])
  (engagement-already-submitted? [s engagement-id])
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-engagements [s engagements] "replace/seed the engagement directory"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained engagement set covering both actuation
  lifecycles (draft, submit) plus the governor's own checks, including
  BOTH paths of the Foreign Investment Act dual-path flagship check and
  the negative controls that prove its conditionality:

    - eng-1 clean, wholly-domestic private company (not a foreign
      investor at all), BIR-number-verified -- the walkthrough-happy-
      path engagement.
    - eng-2 no spec-basis jurisdiction (\"ATL\").
    - eng-3 engagement-fee-mismatch.
    - eng-4 PRIVATE company, IS a foreign investor, but
      `:foreign-investment-notice-filed?` is false -- s.4 breach,
      `foreign-investment-noncompliant` HARD hold (PRIVATE path).
    - eng-5 BIR File Number unverified -- `bir-number-unverified` HARD
      hold.
    - eng-6 PUBLIC company, 45% cumulative foreign shareholding
      (>= the 30% threshold), but `:foreign-investor-licence-held?` is
      false -- s.5(2) breach, `foreign-investment-noncompliant` HARD
      hold (PUBLIC/licence path).
    - eng-7 PUBLIC company, 20% cumulative foreign shareholding
      (BELOW the 30% threshold), no licence held -- proves the
      threshold conditionality: this must NOT hold on
      `foreign-investment-noncompliant`, because s.5(2) does not yet
      require a licence below 30%.
    - eng-8 PRIVATE company, IS a foreign investor, and
      `:foreign-investment-notice-filed?` is true -- proves the
      satisfied-path does not misfire even though the gate condition
      (foreign-investor? true) is met."
  []
  {:engagements
   {"eng-1" {:id "eng-1" :operator "Piarco Logistics Ltd" :portal "oprtt.org"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :claimed-fee 860000.0
             :operator-company-type :private :foreign-investor? false
             :foreign-shareholding-pct 0
             :foreign-investment-notice-filed? nil :foreign-investor-licence-held? nil
             :bir-number-verified? true
             :drafted? false :submitted? false
             :jurisdiction "TTO" :status :intake}
    "eng-2" {:id "eng-2" :operator "Atlantis LLC" :portal "oprtt.org"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :claimed-fee 860000.0
             :operator-company-type :private :foreign-investor? false
             :foreign-shareholding-pct 0
             :foreign-investment-notice-filed? nil :foreign-investor-licence-held? nil
             :bir-number-verified? true
             :drafted? false :submitted? false
             :jurisdiction "ATL" :status :intake}
    "eng-3" {:id "eng-3" :operator "San Fernando Builders Co" :portal "oprtt.org"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :claimed-fee 999000.0
             :operator-company-type :private :foreign-investor? false
             :foreign-shareholding-pct 0
             :foreign-investment-notice-filed? nil :foreign-investor-licence-held? nil
             :bir-number-verified? true
             :drafted? false :submitted? false
             :jurisdiction "TTO" :status :intake}
    "eng-4" {:id "eng-4" :operator "Chaguanas Import Export Ltd" :portal "oprtt.org"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :claimed-fee 860000.0
             :operator-company-type :private :foreign-investor? true
             :foreign-shareholding-pct 100
             :foreign-investment-notice-filed? false :foreign-investor-licence-held? nil
             :bir-number-verified? true
             :drafted? false :submitted? false
             :jurisdiction "TTO" :status :intake}
    "eng-5" {:id "eng-5" :operator "Scarborough Marine Services" :portal "oprtt.org"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :claimed-fee 860000.0
             :operator-company-type :private :foreign-investor? false
             :foreign-shareholding-pct 0
             :foreign-investment-notice-filed? nil :foreign-investor-licence-held? nil
             :bir-number-verified? false
             :drafted? false :submitted? false
             :jurisdiction "TTO" :status :intake}
    "eng-6" {:id "eng-6" :operator "Point Lisas Energy Supply Co" :portal "oprtt.org"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :claimed-fee 860000.0
             :operator-company-type :public :foreign-investor? true
             :foreign-shareholding-pct 45
             :foreign-investment-notice-filed? nil :foreign-investor-licence-held? false
             :bir-number-verified? true
             :drafted? false :submitted? false
             :jurisdiction "TTO" :status :intake}
    "eng-7" {:id "eng-7" :operator "Arima Agro Processing PLC" :portal "oprtt.org"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :claimed-fee 860000.0
             :operator-company-type :public :foreign-investor? true
             :foreign-shareholding-pct 20
             :foreign-investment-notice-filed? nil :foreign-investor-licence-held? false
             :bir-number-verified? true
             :drafted? false :submitted? false
             :jurisdiction "TTO" :status :intake}
    "eng-8" {:id "eng-8" :operator "Couva Manufacturing Ltd" :portal "oprtt.org"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :claimed-fee 860000.0
             :operator-company-type :private :foreign-investor? true
             :foreign-shareholding-pct 100
             :foreign-investment-notice-filed? true :foreign-investor-licence-held? nil
             :bir-number-verified? true
             :drafted? false :submitted? false
             :jurisdiction "TTO" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- draft-filing!
  [s engagement-id]
  (let [e (engagement s engagement-id)
        seq-n (next-draft-sequence s (:jurisdiction e))
        result (registry/register-draft engagement-id (:jurisdiction e) seq-n)]
    {:result result
     :engagement-patch {:drafted? true
                        :draft-number (get result "draft_number")}}))

(defn- submit-filing!
  [s engagement-id]
  (let [e (engagement s engagement-id)
        seq-n (next-submit-sequence s (:jurisdiction e))
        result (registry/register-submit engagement-id (:jurisdiction e) seq-n)]
    {:result result
     :engagement-patch {:submitted? true
                        :submit-number (get result "submit_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (engagement [_ id] (get-in @a [:engagements id]))
  (all-engagements [_] (sort-by :id (vals (:engagements @a))))
  (assessment-of [_ engagement-id] (get-in @a [:assessments engagement-id]))
  (ledger [_] (:ledger @a))
  (draft-history [_] (:draft-records @a))
  (submit-history [_] (:submit-records @a))
  (next-draft-sequence [_ jurisdiction] (get-in @a [:draft-sequences jurisdiction] 0))
  (next-submit-sequence [_ jurisdiction] (get-in @a [:submit-sequences jurisdiction] 0))
  (engagement-already-drafted? [_ engagement-id] (boolean (get-in @a [:engagements engagement-id :drafted?])))
  (engagement-already-submitted? [_ engagement-id] (boolean (get-in @a [:engagements engagement-id :submitted?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :engagement/upsert
      (swap! a update-in [:engagements (:id value)] merge value)

      :assessment/set
      (swap! a assoc-in [:assessments (first path)] payload)

      :engagement/mark-drafted
      (let [engagement-id (first path)
            {:keys [result engagement-patch]} (draft-filing! s engagement-id)
            jurisdiction (:jurisdiction (engagement s engagement-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:draft-sequences jurisdiction] (fnil inc 0))
                       (update-in [:engagements engagement-id] merge engagement-patch)
                       (update :draft-records registry/append result))))
        result)

      :engagement/mark-submitted
      (let [engagement-id (first path)
            {:keys [result engagement-patch]} (submit-filing! s engagement-id)
            jurisdiction (:jurisdiction (engagement s engagement-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:submit-sequences jurisdiction] (fnil inc 0))
                       (update-in [:engagements engagement-id] merge engagement-patch)
                       (update :submit-records registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-engagements [s engagements] (when (seq engagements) (swap! a assoc :engagements engagements)) s))

(defn seed-db
  "A MemStore seeded with the demo engagement set."
  []
  (->MemStore (atom (assoc (demo-data)
                           :assessments {}
                           :ledger [] :draft-sequences {} :draft-records []
                           :submit-sequences {} :submit-records []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  {:engagement/id                   {:db/unique :db.unique/identity}
   :assessment/engagement-id        {:db/unique :db.unique/identity}
   :ledger/seq                      {:db/unique :db.unique/identity}
   :draft-record/seq                {:db/unique :db.unique/identity}
   :submit-record/seq               {:db/unique :db.unique/identity}
   :draft-sequence/jurisdiction     {:db/unique :db.unique/identity}
   :submit-sequence/jurisdiction    {:db/unique :db.unique/identity}})

(defn- engagement->tx [{:keys [id operator portal base-fee monthly-rate monitoring-months claimed-fee
                               operator-company-type foreign-investor? foreign-shareholding-pct
                               foreign-investment-notice-filed? foreign-investor-licence-held?
                               bir-number-verified?
                               drafted? submitted?
                               jurisdiction status draft-number submit-number]}]
  (cond-> {:engagement/id id}
    operator                              (assoc :engagement/operator operator)
    portal                                (assoc :engagement/portal portal)
    base-fee                              (assoc :engagement/base-fee base-fee)
    monthly-rate                          (assoc :engagement/monthly-rate monthly-rate)
    monitoring-months                     (assoc :engagement/monitoring-months monitoring-months)
    claimed-fee                           (assoc :engagement/claimed-fee claimed-fee)
    operator-company-type                 (assoc :engagement/operator-company-type operator-company-type)
    (some? foreign-investor?)             (assoc :engagement/foreign-investor? foreign-investor?)
    (some? foreign-shareholding-pct)      (assoc :engagement/foreign-shareholding-pct foreign-shareholding-pct)
    (some? foreign-investment-notice-filed?) (assoc :engagement/foreign-investment-notice-filed? foreign-investment-notice-filed?)
    (some? foreign-investor-licence-held?)   (assoc :engagement/foreign-investor-licence-held? foreign-investor-licence-held?)
    (some? bir-number-verified?)          (assoc :engagement/bir-number-verified? bir-number-verified?)
    (some? drafted?)                      (assoc :engagement/drafted? drafted?)
    (some? submitted?)                    (assoc :engagement/submitted? submitted?)
    jurisdiction                          (assoc :engagement/jurisdiction jurisdiction)
    status                                (assoc :engagement/status status)
    draft-number                          (assoc :engagement/draft-number draft-number)
    submit-number                         (assoc :engagement/submit-number submit-number)))

(def ^:private engagement-pull
  [:engagement/id :engagement/operator :engagement/portal :engagement/base-fee :engagement/monthly-rate
   :engagement/monitoring-months :engagement/claimed-fee
   :engagement/operator-company-type :engagement/foreign-investor? :engagement/foreign-shareholding-pct
   :engagement/foreign-investment-notice-filed? :engagement/foreign-investor-licence-held?
   :engagement/bir-number-verified?
   :engagement/drafted? :engagement/submitted?
   :engagement/jurisdiction :engagement/status :engagement/draft-number :engagement/submit-number])

(defn- pull->engagement [m]
  (when (:engagement/id m)
    {:id (:engagement/id m) :operator (:engagement/operator m) :portal (:engagement/portal m)
     :base-fee (:engagement/base-fee m) :monthly-rate (:engagement/monthly-rate m)
     :monitoring-months (:engagement/monitoring-months m) :claimed-fee (:engagement/claimed-fee m)
     :operator-company-type (:engagement/operator-company-type m)
     :foreign-investor? (boolean (:engagement/foreign-investor? m))
     :foreign-shareholding-pct (:engagement/foreign-shareholding-pct m)
     :foreign-investment-notice-filed? (:engagement/foreign-investment-notice-filed? m)
     :foreign-investor-licence-held? (:engagement/foreign-investor-licence-held? m)
     :bir-number-verified? (boolean (:engagement/bir-number-verified? m))
     :drafted? (boolean (:engagement/drafted? m)) :submitted? (boolean (:engagement/submitted? m))
     :jurisdiction (:engagement/jurisdiction m) :status (:engagement/status m)
     :draft-number (:engagement/draft-number m) :submit-number (:engagement/submit-number m)}))

(defrecord DatomicStore [conn]
  Store
  (engagement [_ id]
    (pull->engagement (d/pull (d/db conn) engagement-pull [:engagement/id id])))
  (all-engagements [_]
    (->> (d/q '[:find [?id ...] :where [?e :engagement/id ?id]] (d/db conn))
         (map #(pull->engagement (d/pull (d/db conn) engagement-pull [:engagement/id %])))
         (sort-by :id)))
  (assessment-of [_ engagement-id]
    (ls/dec* (d/q '[:find ?p . :in $ ?eid
                   :where [?a :assessment/engagement-id ?eid] [?a :assessment/payload ?p]]
                 (d/db conn) engagement-id)))
  (ledger [_] (ls/read-stream conn :ledger/seq :ledger/fact))
  (draft-history [_] (ls/read-stream conn :draft-record/seq :draft-record/record))
  (submit-history [_] (ls/read-stream conn :submit-record/seq :submit-record/record))
  (next-draft-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :draft-sequence/jurisdiction ?j] [?e :draft-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-submit-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :submit-sequence/jurisdiction ?j] [?e :submit-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (engagement-already-drafted? [s engagement-id]
    (boolean (:drafted? (engagement s engagement-id))))
  (engagement-already-submitted? [s engagement-id]
    (boolean (:submitted? (engagement s engagement-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :engagement/upsert
      (d/transact! conn [(engagement->tx value)])

      :assessment/set
      (d/transact! conn [{:assessment/engagement-id (first path) :assessment/payload (ls/enc payload)}])

      :engagement/mark-drafted
      (let [engagement-id (first path)
            {:keys [result engagement-patch]} (draft-filing! s engagement-id)
            jurisdiction (:jurisdiction (engagement s engagement-id))
            next-n (inc (next-draft-sequence s jurisdiction))]
        (d/transact! conn
                     [(engagement->tx (assoc engagement-patch :id engagement-id))
                      {:draft-sequence/jurisdiction jurisdiction :draft-sequence/next next-n}
                      {:draft-record/seq (count (draft-history s)) :draft-record/record (ls/enc (get result "record"))}])
        result)

      :engagement/mark-submitted
      (let [engagement-id (first path)
            {:keys [result engagement-patch]} (submit-filing! s engagement-id)
            jurisdiction (:jurisdiction (engagement s engagement-id))
            next-n (inc (next-submit-sequence s jurisdiction))]
        (d/transact! conn
                     [(engagement->tx (assoc engagement-patch :id engagement-id))
                      {:submit-sequence/jurisdiction jurisdiction :submit-sequence/next next-n}
                      {:submit-record/seq (count (submit-history s)) :submit-record/record (ls/enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (ls/append-blob! conn :ledger/seq :ledger/fact (count (ledger s)) fact)
    fact)
  (with-engagements [s engagements]
    (when (seq engagements) (d/transact! conn (mapv engagement->tx (vals engagements)))) s))

(defn datomic-store
  ([] (datomic-store {}))
  ([{:keys [engagements]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-engagements s engagements))))

(defn datomic-seed-db
  []
  (datomic-store (demo-data)))
