(ns marketentry.governor
  "Market-Entry Compliance Governor -- the independent compliance layer
  that earns the MarketEntry-LLM the right to commit. The LLM has no
  notion of Trinidad and Tobago procurement law, whether the engagement's
  own declared company type/foreign-shareholding actually trips the
  Foreign Investment Act Chap. 70:07's private-notification or
  public-licence obligation, whether a claimed engagement fee actually
  equals base + months x rate, whether a BIR File Number has been
  verified for a filing that requires one, or when a draft stops being
  a draft and becomes a real-world oprtt.org Procurement Depository
  submission, so this MUST be a separate system able to *reject* a
  proposal and fall back to HOLD.

  `:itonami.blueprint/governor` is `:market-entry-compliance-governor`
  (shared family keyword on blueprints).

  This blueprint's own text (docs/business-model.md Trust Controls:
  'any actual portal registration or filing submission requires
  Market-Entry Compliance Governor clearance and always escalates to
  human sign-off'; 'a false or fabricated regulatory-requirement claim
  is a HARD hold') names exactly the checks below.

  Six checks, in priority order, ALL HARD violations: a human approver
  CANNOT override them. The confidence/actuation gate is SOFT: it asks
  a human to look (low confidence / actuation), and the human may
  approve -- but see `marketentry.phase`: for `:stake
  :actuation/draft-filing`/`:actuation/submit-filing` NO phase ever
  allows auto-commit either. Two independent layers agree that
  actuation is always a human call.

    1. Spec-basis                  -- did the jurisdiction proposal cite
                                       an OFFICIAL source
                                       (`marketentry.facts`), or invent
                                       one?
    2. Evidence incomplete         -- for `:filing/draft`/
                                       `:filing/submit`, has the
                                       jurisdiction actually been
                                       assessed with a full evidence
                                       checklist on file?
    3. Foreign investment
       noncompliant                  -- for `:filing/submit`,
                                       INDEPENDENTLY recompute, per the
                                       Foreign Investment Act Chap.
                                       70:07's own dual-path regime,
                                       whether the engagement's own
                                       declared company type/foreign-
                                       shareholding trips a s.4
                                       (private-company Minister-
                                       notification) or s.5(2) (public-
                                       company >=30% cumulative-
                                       shareholding licence) obligation,
                                       and whether the corresponding
                                       ground-truth flag is unmet --
                                       HARD-hold if so. FLAGSHIP genuinely
                                       new check for the iso3166 family
                                       (a percentage-threshold /
                                       notification-duty dual-path recompute,
                                       neither a date-recompute nor a
                                       boolean registry-membership read).
    4. Engagement fee mismatch     -- for `:filing/submit`,
                                       INDEPENDENTLY recompute whether
                                       the engagement's own `:claimed-
                                       fee` equals `base-fee +
                                       monthly-rate x monitoring-
                                       months` -- honest reapplication
                                       of the ground-truth-recompute
                                       discipline sibling actors use.
    5. BIR File Number unverified   -- for `:filing/submit`,
                                       INDEPENDENTLY check
                                       `:bir-number-verified?`,
                                       UNCONDITIONALLY (not gated behind
                                       a `:requires-bir-number?` flag):
                                       'All companies are required to
                                       register with the Board of
                                       Inland Revenue to obtain a BIR
                                       File Number' (ird.gov.tt's own
                                       text, verbatim) -- every
                                       engagement this actor exists for
                                       is, by definition, a company
                                       operating in Trinidad and Tobago.
    6. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:filing/draft`/
                                       `:filing/submit` (REAL acts)
                                       -> escalate.

  Two more guards, double-draft/double-submit prevention, are enforced
  off dedicated `:drafted?`/`:submitted?` facts (never a `:status`
  value)."
  (:require [marketentry.facts :as facts]
            [marketentry.registry :as registry]
            [marketentry.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Drafting a real portal package and submitting a real portal
  registration are the two real-world actuation events this actor
  performs."
  #{:actuation/draft-filing :actuation/submit-filing})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:jurisdiction/assess` (or `:filing/draft`/`:filing/submit`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's market-entry requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:jurisdiction/assess :filing/draft :filing/submit} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:filing/draft`/`:filing/submit`, the jurisdiction's required
  registration evidence must actually be satisfied."
  [{:keys [op subject]} st]
  (when (contains? #{:filing/draft :filing/submit} op)
    (let [e (store/engagement st subject)
          assessment (store/assessment-of st subject)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      (:jurisdiction e) (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(RGD Companies Registry登録/BIR File Number確認/Procurement Depository登録等)が充足していない状態での提案"}]))))

(defn- foreign-investment-noncompliant-violations
  "For `:filing/submit`, INDEPENDENTLY recompute, per the Foreign
  Investment Act Chap. 70:07's own dual-path regime, whether the
  engagement is noncompliant. FLAGSHIP check for this jurisdiction."
  [{:keys [op subject]} st]
  (when (= op :filing/submit)
    (let [e (store/engagement st subject)]
      (when (registry/foreign-investment-noncompliant? e)
        [{:rule :foreign-investment-noncompliant
          :detail (str subject " はForeign Investment Act (Chap. 70:07) の要件"
                      "(私企業=s.4大臣への事前通知 / 公開企業=s.5(2)外国人株式30%以上の"
                      "ライセンス取得)を充足していない -- 提出提案は進められない")}]))))

(defn- engagement-fee-mismatch-violations
  "For `:filing/submit`, INDEPENDENTLY recompute whether the
  engagement's own claimed fee equals base + months x rate."
  [{:keys [op subject]} st]
  (when (= op :filing/submit)
    (let [e (store/engagement st subject)]
      (when-not (registry/engagement-fee-matches-claim? e)
        [{:rule :engagement-fee-mismatch
          :detail (str subject " の申告手数料(" (:claimed-fee e)
                      ")が独立再計算値(" (registry/compute-engagement-fee e) ")と一致しない")}]))))

(defn- bir-number-unverified-violations
  "For `:filing/submit`, INDEPENDENTLY check `:bir-number-verified?`,
  UNCONDITIONALLY -- every engagement this actor handles is, by
  definition, a company operating in Trinidad and Tobago, and the Board
  of Inland Revenue's own published guidance makes a BIR File Number
  mandatory for exactly that (never gated behind a
  `:requires-bir-number?` flag)."
  [{:keys [op subject]} st]
  (when (= op :filing/submit)
    (let [e (store/engagement st subject)]
      (when-not (true? (:bir-number-verified? e))
        [{:rule :bir-number-unverified
          :detail (str subject " はBoard of Inland Revenue (BIR) の"
                      "BIR File Number確認が未完了 -- 提出提案は進められない")}]))))

(defn- already-drafted-violations
  "For `:filing/draft`, refuses to draft the SAME engagement twice."
  [{:keys [op subject]} st]
  (when (= op :filing/draft)
    (when (store/engagement-already-drafted? st subject)
      [{:rule :already-drafted
        :detail (str subject " は既にドラフト済み")}])))

(defn- already-submitted-violations
  "For `:filing/submit`, refuses to submit the SAME engagement twice."
  [{:keys [op subject]} st]
  (when (= op :filing/submit)
    (when (store/engagement-already-submitted? st subject)
      [{:rule :already-submitted
        :detail (str subject " は既に提出済み")}])))

(defn check
  "Censors a MarketEntry-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (foreign-investment-noncompliant-violations request st)
                           (engagement-fee-mismatch-violations request st)
                           (bir-number-unverified-violations request st)
                           (already-drafted-violations request st)
                           (already-submitted-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
