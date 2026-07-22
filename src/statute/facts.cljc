(ns statute.facts
  "General-law compliance catalog for Trinidad and Tobago (TTO) --
  extends this repo's existing `marketentry.facts` (public-procurement
  market-entry only, narrow scope) with a second, orthogonal catalog of
  statutes a company operating in this jurisdiction must generally
  track for compliance. Mirrors cloud-itonami-iso3166-jpn/-deu/-bgr/
  -aze/-alb/-arm/-atg/-brb/-dma/-grd/-guy's `statute.facts`
  (ADR-2607141700, cloud-itonami-compliance-fact-federation).

  Every entry cites an OFFICIAL Trinidad and Tobago government-hosted
  URL -- never fabricated. `rgd.legalaffairs.gov.tt` (the Ministry of
  the Attorney General and Legal Affairs' own consolidated-law host,
  'LAWS OF TRINIDAD AND TOBAGO ... www.legalaffairs.gov.tt' per every
  PDF's own header) returned a genuine live `503 Service Temporarily
  Unavailable` on every direct attempt this session (a plain
  nginx-style outage page, confirmed by reading the response body --
  NOT a bot-detection/CAPTCHA challenge, so this catalog used the
  Internet Archive Wayback Machine as the documented fallback per this
  task's own instructions, rather than attempting any bypass). Both
  entries below were fetched via `web.archive.org` snapshots of the
  exact `rgd.legalaffairs.gov.tt/laws2/alphabetical_list/lawspdfs/
  <chapter>.pdf` URL pattern and read via `pdftotext -layout` -- neither
  required OCR, both carried a genuine text layer:

  - Companies Act, Chapter 81:01 (Act 35 of 1995, amended by 5 of 1997,
    6 of 1999, 12 of 2003, 2 of 2012, 2 of 2013, 2 of 2015 and 7 of
    2016) -- confirmed via its own primary text (Wayback snapshot of
    `rgd.legalaffairs.gov.tt`, read via `pdftotext`): s.12 'Certificate
    of incorporation' reads 'Upon receipt of articles of incorporation
    which comply with the provisions of this Act, the Registrar shall
    issue a certificate of incorporation in accordance with section 481
    and the certificate is conclusive proof of the incorporation of the
    company named in the certificate' -- the same certificate-of-
    incorporation shape this catalog's other siblings each independently
    document for their own (different-numbered) Companies Acts.
    Independently corroborated by the live Registrar General's
    Department (RGD) site
    (`agla.gov.tt/registrar-general/registrar-general-companies-registry-2/`,
    fetched directly, HTTP 200), which states verbatim: 'The Registrar
    General is... also the Registrar of Companies for the purposes of
    the Companies Act, Ch. 81:01.' Business registration in Trinidad
    and Tobago is now fully digital via the Companies Registry Online
    System (CROS), launched February 2023 -- independently confirmed by
    BOTH the RGD's own site and Trinidad and Tobago's investment-
    promotion-agency site (`globaltrinidadandtobago.com/
    how-to-register-a-business/`).
  - Industrial Relations Act, Chapter 88:01 (Act 23 of 1972, as
    amended) -- confirmed via its own text (Wayback snapshot of
    `rgd.legalaffairs.gov.tt`): s.4 establishes the Industrial Court;
    s.10(4)-(5) empowers the Court, in any dispute concerning the
    dismissal of a worker, to order re-employment, reinstatement, or
    the payment of compensation or damages, where the worker 'has been
    dismissed in circumstances that are harsh and oppressive or not in
    accordance with' the principles and practices of good industrial
    relations.
  - Foreign Investment Act, Chapter 70:07 (Act 16 of 1990, as amended)
    -- confirmed via its own text (Wayback snapshot of
    `rgd.legalaffairs.gov.tt`); this same Act grounds
    `marketentry.facts/foreign-investment-spec-basis` and this
    vertical's flagship governor check
    (`marketentry.registry/foreign-investment-noncompliant?`) -- see
    `marketentry.facts` and `marketentry.registry` for the full s.4/
    s.5(2) dual-path detail. Listed here too because it is
    simultaneously a GENERAL compliance statute (it governs land and
    company-share acquisition by any foreign investor in Trinidad and
    Tobago, not solely public-procurement market entry).

  Not independently confirmed this session, honestly omitted rather
  than guessed: a Trinidad and Tobago Data Protection Act citation
  (this session could not reach a working search path to independently
  verify its chapter number and declined to guess one -- see
  `marketentry.facts` namespace docstring).

  A law not in this table has NO spec-basis, full stop; extend
  `catalog`, do not invent an id/url.")

(def catalog
  "iso3 -> vector of statute entries. `:statute/url` + `:statute/law-number`
  are the citation the governor requires before any compliance-fact
  proposal referencing this law can commit."
  {"TTO"
   [{:statute/id "tto.companies-act"
     :statute/title "Companies Act"
     :statute/jurisdiction "TTO"
     :statute/kind :law
     :statute/law-number "Chapter 81:01 (Act 35 of 1995, amended by 5 of 1997, 6 of 1999, 12 of 2003, 2 of 2012, 2 of 2013, 2 of 2015 and 7 of 2016)"
     :statute/url "https://web.archive.org/web/20250312180132/https://rgd.legalaffairs.gov.tt/laws2/alphabetical_list/lawspdfs/81.01.pdf"
     :statute/url-provenance :wayback-machine-fallback
     :statute/enacted-date "1995-01-01"
     :statute/retrieved-at "2026-07-23"
     :statute/topic #{:corporate-governance :incorporation}}
    {:statute/id "tto.industrial-relations-act"
     :statute/title "Industrial Relations Act"
     :statute/jurisdiction "TTO"
     :statute/kind :law
     :statute/law-number "Chapter 88:01 (Act 23 of 1972, as amended)"
     :statute/url "https://web.archive.org/web/20220123231141/https://rgd.legalaffairs.gov.tt/laws2/alphabetical_list/lawspdfs/88.01.pdf"
     :statute/url-provenance :wayback-machine-fallback
     :statute/enacted-date "1972-01-01"
     :statute/retrieved-at "2026-07-23"
     :statute/topic #{:labor :employment :industrial-relations :termination}}
    {:statute/id "tto.foreign-investment-act"
     :statute/title "Foreign Investment Act"
     :statute/jurisdiction "TTO"
     :statute/kind :law
     :statute/law-number "Chapter 70:07 (Act 16 of 1990, as amended)"
     :statute/url "https://web.archive.org/web/20260111135612/https://rgd.legalaffairs.gov.tt/laws2/alphabetical_list/lawspdfs/70.07.pdf"
     :statute/url-provenance :wayback-machine-fallback
     :statute/enacted-date "1990-08-17"
     :statute/retrieved-at "2026-07-23"
     :statute/topic #{:foreign-investment :land :corporate-governance}}]})

(defn spec-basis
  "The jurisdiction's statute vector, or nil -- nil means NO spec-basis
  for that jurisdiction yet."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report, same shape/discipline as `marketentry.facts/coverage`:
  never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-iso3166-tto statute.facts Wave 0 (ADR-2607141700): "
                 (count (get catalog "TTO")) " TTO statutes seeded with an "
                 "official government-hosted citation. Extend "
                 "`statute.facts/catalog`, never fabricate a law-id or URL.")})))

(defn by-topic
  "Statutes for `iso3` tagged with `topic` (e.g. :labor, :foreign-investment)."
  [iso3 topic]
  (filterv #(contains? (:statute/topic %) topic) (spec-basis iso3)))
