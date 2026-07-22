(ns marketentry.registry
  "Pure-function market-entry filing-draft + filing-submit record
  construction -- an append-only market-entry book-of-record draft.

  Like every sibling actor's registry, there is no single international
  reference-number standard for a public-procurement market-entry
  filing -- every jurisdiction assigns its own format. This namespace
  does NOT invent one; it builds a jurisdiction-scoped sequence number
  and validates the record's required fields, the same honest,
  non-fabricating discipline `marketentry.facts` uses.

  `engagement-fee-matches-claim?` is an HONEST reapplication of the
  SAME ground-truth-recompute DISCIPLINE sibling actors use (verify a
  claimed monetary total against the entity's own recorded quantity x
  unit fields), reapplied to a market-entry engagement fee line.

  `foreign-investment-noncompliant?` grounds this vertical's flagship
  governor check -- the Foreign Investment Act, Chapter 70:07 (Act 16 of
  1990, as amended; own primary text, see `marketentry.facts`), which
  this session read directly via the Internet Archive Wayback Machine
  (`rgd.legalaffairs.gov.tt` itself returned a live 503 throughout this
  session). The Act's own text establishes a genuinely TWO-TIER regime,
  and this namespace deliberately implements BOTH tiers rather than
  picking one and ignoring the other:

    - s.4 (PRIVATE companies): a foreign investor incorporating a
      private company, or acquiring shares in one already incorporated,
      must FIRST 'supply the Minister with such information as is
      prescribed in the First Schedule' -- a NOTIFICATION duty,
      unconditional on any shareholding percentage. This applies to
      EVERY foreign-invested private-company engagement, the same
      'always applies once the gate condition is met' shape this
      family's Bulgaria/Azerbaijan/Armenia checks use for their own
      (different) gate conditions.
    - s.5(2) (local PUBLIC companies): 'A foreign investor may not
      acquire shares in a local public company without obtaining a
      licence where the holding of such shares by him either directly
      or indirectly results in THIRTY PER CENT OR MORE of the total
      cumulative shareholding of the company being held by foreign
      investors' -- a numeric LICENSING threshold, not a notification
      duty. This is a genuinely different shape from every prior
      sibling this catalog's family has implemented that this iteration
      directly read: not a date-recompute (Barbados's forward-looking
      registration-validity window, Grenada's backward-looking
      conviction-disqualification lookback), not a boolean registry-
      membership read (Guyana's Companies-Act-1991 'carrying on an
      undertaking' trigger set), not a sector-gated staffing-ratio rule
      (Guyana's Local Content Act 2021) -- it is a PERCENTAGE THRESHOLD
      independently recomputed against the engagement's own declared
      cumulative foreign shareholding.

  `foreign-investment-noncompliant?` is TRUE only when the applicable
  path's own precondition is met (private + is-a-foreign-investor, OR
  public + shareholding >= 30%) AND the corresponding ground-truth flag
  (`:foreign-investment-notice-filed?` / `:foreign-investor-licence-
  held?`) is not true -- it must NEVER fire for a wholly domestic
  private-company engagement, and must NEVER fire for a public-company
  engagement whose foreign shareholding is honestly below 30%, exactly
  mirroring the sector/trigger conditionality discipline this family's
  Guyana sibling established for its own (different) conditional
  checks.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real Ministry of Finance or Office of Procurement
  Regulation system, no host date API. It builds the RECORD an operator
  would keep, not the act of submitting a portal registration itself
  (that is `marketentry.operation`'s `:filing/submit`, always
  human-gated -- see README Actuation)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the market-entry operator's act, not this actor's."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn compute-engagement-fee
  "The ground-truth engagement fee for `engagement`'s own `:base-fee`
  and `:monitoring-months` x `:monthly-rate` -- a single flat
  base + months x rate calculation, not a full pricing engine."
  [{:keys [base-fee monthly-rate monitoring-months]}]
  (+ (double base-fee)
     (* (double monthly-rate) (double monitoring-months))))

(defn engagement-fee-matches-claim?
  "Does `engagement`'s own `:claimed-fee` equal the independently
  recomputed `compute-engagement-fee`?"
  [{:keys [claimed-fee] :as engagement}]
  (== (double claimed-fee) (compute-engagement-fee engagement)))

;; --------------- Foreign Investment Act Chap. 70:07 (dual-path) ---------------

(def foreign-shareholding-licence-threshold-pct
  "Foreign Investment Act Chap. 70:07 s.5(2)'s own numeric threshold: a
  foreign investor may not acquire shares in a local PUBLIC company
  without a licence where the acquisition results in this percentage or
  more of the company's total cumulative shareholding being held by
  foreign investors."
  30)

(defn foreign-investment-private-notice-required?
  "Does `engagement` fall under Foreign Investment Act Chap. 70:07 s.4's
  PRIVATE-company notification duty -- i.e. is the operator's own
  declared `:operator-company-type` `:private` AND is it (or a
  controlling interest in it) a declared `:foreign-investor?`? This gate
  is UNCONDITIONAL on any percentage -- s.4 applies to ANY foreign
  investor incorporating, or acquiring shares in, a private company."
  [{:keys [operator-company-type foreign-investor?]}]
  (boolean (and (= operator-company-type :private) (true? foreign-investor?))))

(defn foreign-investment-public-licence-required?
  "Does `engagement` fall under Foreign Investment Act Chap. 70:07
  s.5(2)'s PUBLIC-company licensing threshold -- i.e. is the operator's
  own declared `:operator-company-type` `:public` AND does its own
  declared `:foreign-shareholding-pct` meet or exceed
  `foreign-shareholding-licence-threshold-pct` (30)? A public-company
  engagement honestly below the threshold is NOT required to hold a
  licence -- this fn returns false for it."
  [{:keys [operator-company-type foreign-shareholding-pct]}]
  (boolean (and (= operator-company-type :public)
                (some? foreign-shareholding-pct)
                (>= foreign-shareholding-pct foreign-shareholding-licence-threshold-pct))))

(defn foreign-investment-noncompliant?
  "TRUE only when a Foreign Investment Act Chap. 70:07 obligation
  actually applies to `engagement` (either path above) AND the
  corresponding ground-truth flag is not true:
    - private path: `foreign-investment-private-notice-required?` true
      AND `:foreign-investment-notice-filed?` not true (s.4 breach).
    - public path: `foreign-investment-public-licence-required?` true
      AND `:foreign-investor-licence-held?` not true (s.5(2) breach,
      s.9 offence -- fine $100,000.00).
  For a wholly-domestic private engagement, or a public engagement
  honestly below the 30% threshold, ALWAYS returns false regardless of
  the notice/licence flags -- the conditionality itself is the fact
  under test here, the same discipline this family's Guyana sibling
  applies to its own (different) conditional checks."
  [{:keys [foreign-investment-notice-filed? foreign-investor-licence-held?] :as engagement}]
  (boolean
   (or (and (foreign-investment-private-notice-required? engagement)
            (not (true? foreign-investment-notice-filed?)))
       (and (foreign-investment-public-licence-required? engagement)
            (not (true? foreign-investor-licence-held?))))))

;; ----------------------------- filing records -----------------------------

(defn register-draft
  "Validate + construct the FILING-DRAFT registration DRAFT -- the
  market-entry operator's own act of preparing a portal registration
  package. Pure function -- does not touch any real procurement
  system."
  [engagement-id jurisdiction sequence]
  (when-not (and engagement-id (not= engagement-id ""))
    (throw (ex-info "draft: engagement_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "draft: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "draft: sequence must be >= 0" {})))
  (let [draft-number (str (str/upper-case jurisdiction) "-DFT-" (zero-pad sequence 6))
        record {"record_id" draft-number
                "kind" "filing-draft"
                "engagement_id" engagement-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "draft_number" draft-number
     "certificate" (unsigned-certificate "FilingDraft" draft-number draft-number)}))

(defn register-submit
  "Validate + construct the FILING-SUBMIT registration DRAFT -- the
  market-entry operator's own act of actually submitting a portal
  registration (always human-gated upstream)."
  [engagement-id jurisdiction sequence]
  (when-not (and engagement-id (not= engagement-id ""))
    (throw (ex-info "submit: engagement_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "submit: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "submit: sequence must be >= 0" {})))
  (let [submit-number (str (str/upper-case jurisdiction) "-SUB-" (zero-pad sequence 6))
        record {"record_id" submit-number
                "kind" "filing-submit"
                "engagement_id" engagement-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "submit_number" submit-number
     "certificate" (unsigned-certificate "FilingSubmit" submit-number submit-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
