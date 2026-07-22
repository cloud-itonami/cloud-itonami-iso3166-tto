# Business Model: Independent Public-Sector Market-Entry & Procurement Compliance Service — Trinidad and Tobago

## Classification

- Repository: `cloud-itonami-iso3166-tto`
- ISO 3166: `TTO` (Trinidad and Tobago)
- Activity: public-procurement market-entry and ongoing regulatory-
  compliance navigation for an already-incorporated operator

## Customer

- an already-incorporated `cloud-itonami-cofog-{code}` /
  `cloud-itonami-isco-{code}` / `cloud-itonami-unspsc-{segment}` /
  `cloud-itonami-{ISIC}` operator wanting to bid on a Trinidad and
  Tobago public contract
- a foreign SME or civic-tech vendor entering the public sector in
  Trinidad and Tobago for the first time, including one that is itself
  (or is controlled by) a foreign investor under the Foreign Investment
  Act Chap. 70:07
- a `cloud-itonami-M6910` client that has just completed incorporation
  and now needs public-sector market access

## Offer

- registration walkthrough for the Office of Procurement Regulation
  (OPR)'s Procurement Depository (oprtt.org), the OPR's own supplier/
  contractor registration and reporting system (ss.13, 24, 26, 29, 58 of
  the Public Procurement and Disposal of Public Property Act, 2015),
  plus awareness of ProcureTT (the public notice board for procurement/
  disposal opportunities)
- business/tax registration checklist: Certificate of Incorporation
  from the Registrar General's Department (RGD) Companies Registry
  (Companies Act, Chap. 81:01, via the Companies Registry Online System
  (CROS)), followed by BIR File Number registration with the Board of
  Inland Revenue (BIR)
- **Foreign Investment Act Chap. 70:07 dual-path screening** --
  independent verification of whether the operator's own declared
  company type and (where public) cumulative foreign shareholding trips
  a s.4 Minister-notification duty (private company) or a s.5(2) 30%-
  shareholding licence requirement (public company), before any filing
  submission
- ongoing regulatory-change monitoring subscription
- compliance-audit export package for the client's own records

## Revenue

- per-engagement market-entry fee (one-time registration + checklist
  completion)
- recurring regulatory-change monitoring subscription
- compliance-audit export package

## Trust Controls

- any actual portal registration or filing submission requires
  Market-Entry Compliance Governor clearance and always escalates to
  human sign-off (`:filing/submit` is never automated at any phase)
- a false or fabricated regulatory-requirement claim is a HARD hold that
  cannot be overridden by human approval alone -- it must be corrected
  against a cited official source first
- a Foreign Investment Act Chap. 70:07 obligation (s.4 notification for
  a private company, or s.5(2) licence for a public company at or above
  30% cumulative foreign shareholding) that is unmet, independently
  recomputed from the engagement's own declared company type and
  shareholding, is a HARD hold on `:filing/submit` -- never trusted from
  a self-reported "compliant" claim
- this service does **not** provide legal or tax advice; characterization
  and filing on the client's behalf beyond checklist/draft assistance
  routes to Trinidad-and-Tobago-licensed counsel or a registered agent

## Boundary with adjacent actors (read before forking)

- **`cloud-itonami-M6910`**: helps a client BECOME a legal entity
  (incorporation, ISIC 6910) -- a prior, different regulatory phase
  (company law). This blueprint assumes incorporation is already done and
  handles public-procurement market entry (a different regulatory domain).
- **`cloud-itonami-cofog-{code}`**: a jurisdiction-agnostic operator
  template for ONE public function. This blueprint is the orthogonal
  jurisdiction-specific axis -- the two compose (fork a COFOG-function
  blueprint AND this one to operate in Trinidad and Tobago).
