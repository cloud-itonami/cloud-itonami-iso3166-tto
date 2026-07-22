# ADR-0001: Architecture — Trinidad and Tobago market-entry compliance actor (`marketentry`)

**Status**: accepted
**Date**: 2026-07-23

## Context

`cloud-itonami-iso3166-tto` was published as a `:blueprint` (docs +
`blueprint.edn` + `deps.edn`, then a country-level `culture.facts`
catalog in a separate Wave 1 batch) but carried ZERO `src/marketentry`
or `src/statute` content -- its `:public-sector/market-entry-
compliance` domain, declared in `blueprint.edn`, was unimplemented.
This ADR closes that gap, following the pattern established by
`cloud-itonami-iso3166-jpn` (origin) and the Caribbean/CARICOM siblings
`cloud-itonami-iso3166-guy` (Guyana) and `cloud-itonami-iso3166-grd`
(Grenada). `cloud-itonami-iso3166-vct` (Saint Vincent and the
Grenadines) was checked first and confirmed to carry ONLY the same
`src/culture/` stub this repo started with -- it is not yet a
structural model for this pattern.

## Decision

Build the full governed-actor architecture for `marketentry`, mirroring
GUY/GRD's harness verbatim (StateGraph node names, governor hard/
escalate contract, phase 0-3 rollout, `Store` protocol with MemStore +
DatomicStore parity) and researching Trinidad and Tobago's own real
market-entry rules from scratch for the country-specific content.

- **Store**: `marketentry.store`, MemStore + DatomicStore, proven parity
  via contract test.
- **Registry**: `marketentry.registry`, pure DRAFT-certificate
  construction via `unsigned-certificate`, jurisdiction-scoped sequence
  numbering (`TTO-DFT-000000`, `TTO-SUB-000000`), plus the flagship
  Foreign Investment Act dual-path recompute (see below).
- **Governor**: `:market-entry-compliance-governor` (family keyword from
  `blueprint.edn`).
- **Entity shape**: `engagement`, sequential draft -> submit on the same
  record. `high-stakes` = `#{:actuation/draft-filing
  :actuation/submit-filing}`.
- **Phase**: 0->3; `:filing/draft` and `:filing/submit` NEVER auto-
  commit at any phase.

### Office of Procurement Regulation -- exact Act name/year verified directly, `opr.gov.tt` does NOT resolve

The task's own hint suggested checking `opr.gov.tt` and the "Public
Procurement and Disposal of Public Property Act, 2015 (as amended)".
Direct DNS lookup confirmed `opr.gov.tt` does not resolve (NXDOMAIN).
The real domain, `oprtt.org`, was found by fetching the live Ministry
of Finance procurement-unit page (`www.finance.gov.tt/divisions/
procurement-unit/`), whose own text names "Office of the Procurement
Regulation (OPRTT)" and links to it. `oprtt.org`'s own About page,
fetched and read directly, states verbatim: "The Office of Procurement
Regulation (OPR) is a body corporate established pursuant to an Act of
Parliament, namely the Public Procurement and Disposal of Public
Property Act, 2015... The Act was assented to on January 14, 2015, and
was partially proclaimed by way of Legal Notice 150 of 2015... The Act
was fully proclaimed on April 26, 2023, by way of Legal Notice 106 of
2023." The Act's own primary text (downloaded from `oprtt.org` and read
via `pdftotext`) independently confirms s.9 ("There is hereby
established as a body corporate the Office of Procurement Regulation")
and s.10 (a Board chaired by the "Procurement Regulator"). `oprtt.org`'s
own Legislative Framework page lists the Act as "Act No. 1 of 2015"
plus four amendment Acts (No. 5 of 2016, No. 3 of 2017, No. 27 of 2020,
No. 13 of 2023).

### Flagship HARD check: Foreign Investment Act Chap. 70:07 dual-path -- the task's own suggested research target, independently verified

The task suggested checking "whether there's a specific licensing/
notification threshold for foreign investment in land or specific
sectors" via the Foreign Investment Act (Chap. 70:07). `rgd.
legalaffairs.gov.tt` (Trinidad and Tobago's own consolidated-law host)
returned a genuine live `503 Service Temporarily Unavailable` on every
attempt this session -- confirmed by reading the response body (a
plain nginx-style outage page, NOT a bot-detection/CAPTCHA challenge).
This ADR's catalog therefore used the Internet Archive Wayback Machine
as the documented fallback, per this task's own instructions, fetching
a `web.archive.org` snapshot of `rgd.legalaffairs.gov.tt/laws2/
alphabetical_list/lawspdfs/70.07.pdf` and reading it via `pdftotext`.

It genuinely IS the Foreign Investment Act, Chapter 70:07 (Act 16 of
1990, amended by 6 of 1991, 33 of 1995, 4 of 1997, 2 of 2005, 17 of
2007). Its own primary text establishes a genuinely TWO-TIER regime,
and `marketentry.registry` implements BOTH tiers:

- **s.4 (PRIVATE companies)**: a foreign investor incorporating a
  private company, or acquiring shares in one, must FIRST "supply the
  Minister with such information as is prescribed in the First
  Schedule" -- a NOTIFICATION duty, unconditional on any shareholding
  percentage.
- **s.5(2) (local PUBLIC companies)**: "A foreign investor may not
  acquire shares in a local public company without obtaining a licence
  where the holding of such shares by him either directly or indirectly
  results in THIRTY PER CENT OR MORE of the total cumulative
  shareholding of the company being held by foreign investors" -- a
  numeric LICENSING threshold.
- s.6/s.7 permit up to 1 acre of land for residence, or up to 5 acres
  for trade/business, without a licence.
- s.9 makes an unlicensed vesting of land or shares an offence, fine
  $100,000.00.

`marketentry.registry/foreign-investment-noncompliant?` independently
recomputes, from the engagement's own declared `:operator-company-type`
/ `:foreign-investor?` / `:foreign-shareholding-pct`, whether either
path's own obligation applies, and cross-checks the corresponding
ground-truth flag (`:foreign-investment-notice-filed?` /
`:foreign-investor-licence-held?`). This is a genuinely different check
SHAPE from every prior iso3166 sibling this ADR's author directly read
in full: not a date-recompute (Grenada's backward-looking 2-year
disqualification lookback, Barbados's forward-looking registration-
validity window per Grenada's own ADR), not a boolean registry-
membership read or sector-gated staffing ratio (Guyana's Companies Act
1991 "carrying on an undertaking" trigger set / Local Content Act
2021) -- it is a DUAL-PATH check that branches on the operator's own
declared company type and, for the public-company path only,
independently recomputes a NUMERIC PERCENTAGE THRESHOLD (30%).

### InvesTT is a stale premise -- honestly disclosed, not papered over

The task named InvesTT as the investment-promotion agency to check.
`investt.co.tt` redirects (confirmed via `curl -L`) to
`globaltrinidadandtobago.com`, whose own About page states verbatim:
"Global Trinidad & Tobago is the nation's premier trade and investment
promotion agency, formed in 2024 through the amalgamation of three
legacy organizations: InvesTT (Investment Promotion), ExporTT (Export
Development), CreativeTT (Creative Industries Growth)." This ADR's
catalog cites the live successor agency, not the stale standalone name.

### Business registration / tax registration

The Registrar General's Department (RGD) Companies Registry, under the
Office of the Attorney General and Ministry of Legal Affairs (AGLA),
administers the Companies Act, Chap. 81:01 (Act 35 of 1995, as
amended) -- confirmed both via the Act's own primary text (Wayback
snapshot, s.12 "Certificate of incorporation") and via AGLA's own live
site, which states verbatim: "The Registrar General is... also the
Registrar of Companies for the purposes of the Companies Act, Ch.
81:01." Registration is fully digital via the Companies Registry Online
System (CROS), launched February 2023 -- independently confirmed by
BOTH the RGD's own site and Trinidad and Tobago's investment-promotion-
agency site. Tax registration is confirmed via `ird.gov.tt`'s own live
text, verbatim: "All companies are required to register with the Board
of Inland Revenue to obtain a BIR File Number."

### `statute.facts` (second, orthogonal catalog)

Three Trinidad and Tobago statutes: the Companies Act, Chap. 81:01
(confirmed via Wayback snapshot of `rgd.legalaffairs.gov.tt`, s.12), the
Industrial Relations Act, Chap. 88:01 (Act 23 of 1972, as amended,
confirmed via the same Wayback fallback -- s.4 establishes the
Industrial Court, s.10(4)-(5) empowers it to order reinstatement or
compensation for a dismissal "harsh and oppressive or not in accordance
with" good industrial relations), and the Foreign Investment Act, Chap.
70:07 (the same Act grounding the flagship check above, also listed
here because it is simultaneously a general compliance statute, not
solely a market-entry mechanism).

Not independently confirmed this session, honestly omitted rather than
guessed: a Trinidad and Tobago Data Protection Act citation (no working
search path this session to independently verify its chapter number),
and any general procurement de-minimis dollar threshold beyond the
Foreign Investment Act's own 30%/1-acre/5-acre figures.

## Consequences

- `src/` now genuinely exists with real, tested, curl/pdftotext-cited
  content for this blueprint's declared domain (`:public-sector/
  market-entry-compliance`) -- moves this repo's
  `manifest/itonami-fleet-audit.edn` `:prod-ready?` signal from `:stub`
  to `:active`.
- The existing `culture.facts` catalog (Wave 1, unrelated batch) is
  untouched.
- The OPR's own Ineligibility Proceedings mechanism (LN 27 of 2022, a
  1-10-year variable-duration debarment sanction plus a 6-month
  conditional-non-debarment tier) is genuine, independently confirmed
  (regulation PDF + live FAQ page), but used here as descriptive
  grounding for `rep-spec-basis` only, not as a second flagship check --
  a genuine, NOT-yet-implemented extension point for a future
  iteration, per this vertical's single-flagship design.
- Sibling country blueprints can continue forking JPN/GUY/GRD/TTO and
  swapping in their own genuinely-researched `marketentry.facts` /
  `statute.facts` content and whichever flagship check their own law
  actually supports -- this ADR is itself further evidence that even a
  "specific licensing/notification threshold" hint can resolve to a
  genuinely different check shape (a dual-path percentage-threshold
  recompute) than any prior sibling's date-recompute or boolean-
  registry-membership shape.
