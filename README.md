# cloud-itonami-iso3166-tto

Open ISO 3166 Blueprint for **TTO**: Trinidad and Tobago -- **`:implemented`**.

This repository designs **and implements** a forkable OSS business for
an independent public-sector market-entry consultant: an already-
incorporated operator (e.g. a `cloud-itonami-cofog-{code}`,
`cloud-itonami-isco-{code}`, `cloud-itonami-unspsc-{segment}` or
`cloud-itonami-{ISIC}` blueprint fork) gets a Compliance Advisor +
independent **Market-Entry Compliance Governor** to navigate public-
procurement registration, local business/tax registration, Foreign
Investment Act screening, and regulatory-compliance rules in Trinidad
and Tobago, so the operator can win and service a government contract
without hiring a full in-house compliance department.

## Official surface (curl/WebFetch-verified 2026-07-23 -- `oprtt.org`, `agla.gov.tt`, `ird.gov.tt`, `globaltrinidadandtobago.com` fetched cleanly this session; `rgd.legalaffairs.gov.tt` returned a live 503 throughout, Internet Archive Wayback Machine used as the documented fallback for its own PDFs)

- Procurement: `oprtt.org`, the Office of Procurement Regulation (OPR)'s
  own site -- a body corporate established under s.9 of the Public
  Procurement and Disposal of Public Property Act, 2015 (Act No. 1 of
  2015, assented 14 January 2015, fully proclaimed 26 April 2023 by
  Legal Notice 106 of 2023, amended by Act No. 5 of 2016 / No. 3 of 2017
  / No. 27 of 2020 / No. 13 of 2023). `opr.gov.tt` does NOT resolve --
  confirmed via DNS lookup, disclosed so nobody else repeats the
  mistake. Suppliers/contractors register on the OPR's own Procurement
  Depository; ProcureTT is the public procurement/disposal notice
  board.
- Business registration: the Registrar General's Department (RGD)
  Companies Registry, under the Office of the Attorney General and
  Ministry of Legal Affairs (AGLA) -- issues a Certificate of
  Incorporation under the Companies Act, Chap. 81:01 (Act 35 of 1995,
  as amended), s.12. Fully digital via the Companies Registry Online
  System (CROS), launched February 2023.
- Tax: the Board of Inland Revenue (BIR), Inland Revenue Division,
  Ministry of Finance -- issues a BIR File Number ("All companies are
  required to register with the Board of Inland Revenue to obtain a
  BIR File Number", ird.gov.tt's own text, verbatim).
- Foreign investment: the Foreign Investment Act, Chap. 70:07 (Act 16
  of 1990, as amended) -- a foreign investor incorporating a PRIVATE
  company must notify the Minister (s.4); a foreign investor acquiring
  30% or more of a local PUBLIC company's cumulative shareholding must
  hold a licence (s.5(2)). InvesTT, the task's own named investment-
  promotion agency, is now a legacy brand folded (2024) into Global
  Trinidad and Tobago (`globaltrinidadandtobago.com`) -- disclosed
  honestly rather than cited as if still standalone.

## Implementation (R0)

| Piece | Location |
|---|---|
| Actor namespaces | `src/marketentry/*` |
| Governor | `:market-entry-compliance-governor` |
| Ops | `:engagement/intake` · `:jurisdiction/assess` · `:filing/draft` · `:filing/submit` |
| Flagship HARD check | `foreign-investment-noncompliant` (Foreign Investment Act Chap. 70:07 dual-path: s.4 private-company Minister-notification, or s.5(2) public-company 30%-cumulative-foreign-shareholding licence -- independently recomputed from the engagement's own declared company type/shareholding, see `docs/adr/0001-architecture.md`) |
| Compliance catalog | `src/statute/facts.cljc` -- Companies Act (Chap. 81:01), Industrial Relations Act (Chap. 88:01), Foreign Investment Act (Chap. 70:07) |
| Tests | `clojure -M:dev:test` |
| Demo | `clojure -M:dev:run` |
| Architecture ADR | [`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) |

`:filing/submit` is never in any phase's `:auto` set -- human sign-off
is structural, not a rollout milestone.

## No robotics premise -- digital/data service exemption

Market-entry and procurement-compliance navigation is a pure data/software
service with no physical-domain work (portal registration, document
checklists, regulatory-change monitoring) -- the same exemption class as
`cloud-itonami-6310` (HR SaaS replacement) and `cloud-itonami-gtin-*`.
`blueprint.edn` sets `:itonami.blueprint/robotics false` and
`:required-technologies` lists only real capabilities (`:identity`,
`:forms`, `:dmn`, `:bpmn`, `:audit-ledger`), no `:robotics`.

## Core Contract

```text
operator intake + prior filing history
        |
        v
Compliance Advisor -> Market-Entry Compliance Governor -> filing draft, or human sign-off
        |
        v
gated portal registration / filing submission + audit ledger
```

No automated proposal can submit a portal registration or filing the
governor refuses, suppress a compliance record, or claim a legal/tax
conclusion the governor has not cleared. `:filing/submit` is never in any
phase's `:auto` set -- it always requires human sign-off.

## What this is NOT

- **Not the government of Trinidad and Tobago.** This blueprint is an
  independent operator the government contracts with or that bids into
  its procurement -- never the government itself, and never an official
  channel.
- **Not legal or tax advice.** Every regulatory claim must cite the
  official source and route final filings to Trinidad-and-Tobago-
  licensed counsel or a registered agent where the law requires licensed
  representation.

## Capability layer

Required capabilities (`blueprint.edn`):

- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.

## Culture catalog

Alongside the market-entry / statute catalogs, this repo carries a
**country-level regional-culture catalog** (ADR-2607171400 addendum 2,
`cloud-itonami-municipality-culture-catalog` Wave 1, in
`com-junkawasaki/root`) — national dishes, protected products, beverages,
crafts, festivals and heritage sites for Trinidad and Tobago:

- `src/culture/facts.cljc` — the catalog, source of truth (keyed by
  uppercase ISO3, mirroring `statute.facts`).
- `schema/culture.edn` — DataScript schema.
- `data/culture-tx.edn` — derived DataScript tx-data (regenerated from
  the catalog, never hand-edited).

City-level counterparts live in the `cloud-itonami-municipality-*` repos.
Same provenance discipline as the compliance catalogs: every entry cites a
source URL that was actually fetched and read on `:culture/retrieved-at`;
summaries state only what the cited source confirms. An item not in
`culture.facts/catalog` has no spec-basis — never fabricate one.
