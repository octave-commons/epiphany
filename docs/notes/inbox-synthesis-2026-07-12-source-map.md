---
slug: inbox-synthesis-2026-07-12-source-map
uuid: e7c57101-a5eb-494e-b349-a17d13c8e7fe
title: "Inbox Batch 2026-07-12: Assurance Conversation Source Map and Dispositions"
kind: note
status: draft
description: "Intake, provenance, and per-item disposition record for the 2026-07-12 inbox batch: one assurance/governance conversation exported in overlapping segments, plus two earlier exports."
labels: [inbox, intake, provenance, synthesis, assurance, governance]
created: "2026-07-12"
sources:
  - "docs/inbox/ (all items listed below; originals preserved)"
implements: ["docs/process/inbox.md"]
---

# Inbox Batch 2026-07-12: Source Map and Dispositions

## What this note is

Intake and disposition record for the inbox batch processed on 2026-07-12
(working tree at `e92e389`). It preserves provenance for every inspected item,
maps the corpus onto the artifacts it has already produced, and lists the
residue that has not yet been routed. It is a working note, not authority for
any claim inside the source items.

Companion synthesized artifacts from this batch:

- [Verification defect inventory](inbox-synthesis-2026-07-12-defect-inventory.md)
  — `kind: finding`; claims verified against source.
- [Agent verification-gaming pattern](inbox-synthesis-2026-07-12-agent-gaming-pattern.md)
  — observation + derived pattern + proposal candidates.

## Corpus shape (observed)

41 markdown files were present in `docs/inbox/` (plus one editor lock symlink,
`.#2026.07.12.10.17.57.md`, which is ephemeral). Checksums show **26 unique
contents**. The batch is dominated by **one long Perplexity conversation**
(user + research assistant, "@GitHub"-tagged turns) about assurance,
verification, and process governance for this repository, exported in
overlapping segments — some segments re-exported minutes apart, some truncated
mid-message.

Exact duplicate pairs (identical md5): `10.17.55 = 10.19.54`,
`10.17.56 = 10.19.59`, `10.17.57 = 10.20.01`, `10.17.57-1 = 10.20.04`,
`10.17.58 = 10.20.07`, `10.17.59 = 10.20.08`. Additionally `10.17.57`,
`10.17.57-1`, `10.17.58` reproduce the earlier committed items `06.12.04`,
`06.16.53`, `06.19.25` with the originating user prompt prepended; `10.20.19`
reproduces `06.22.56`; `10.20.24` and `10.17.59` are truncated fragments of
content complete in `10.21.05` and `06.22.56` respectively; `10.17.59-1` and
`10.18.00` are 3-line scraps of a diagram from `06.22.56`.

Environment facts recorded in passing (from `10.17.43`/`10.17.55`): the
conversation's sandbox mirrored CI at **Temurin Java 21, Clojure CLI
1.12.2.1565**, `clojure -M:unit-test` as the CI baseline.

## The conversation arc, and where each part already landed

The conversation is a single continuous inquiry. Most of its outputs were
**already routed into the repository before this synthesis pass** — the inbox
items are the source record of how those artifacts came to be. Verified
against the working tree:

| Arc segment | Source item(s) | Routed artifact (verified present) |
|---|---|---|
| Sandbox/test setup | 10.17.43, 10.17.55 | none (environment facts only) |
| Code review: boundary/failure-mode defects | 10.17.56 | **not routed** → now [defect inventory](inbox-synthesis-2026-07-12-defect-inventory.md) |
| False-green in-memory oracle; static analysis; interop budget; CLI/HTTP untangling | 06.12.04 (= 10.17.57) | absorbed into ADR-004 + verification design; static-analysis chore **not realized** |
| Schema enforcement gateway | 06.16.53 (= 10.17.57-1) | `docs/designs/verification-architecture.md` |
| Splint / mutation testing placement | 06.19.25 (= 10.17.58) | design §mutation strategy; tool choice deferred to ENG-017H/J |
| Anti-gaming portfolio, metamorphic/differential tests | 06.22.56 (= 10.20.19; frags 10.17.59, 10.17.59-1, 10.18.00) | ADR-004 §5–8 + design |
| ADR drafting | 10.20.24 (frag), 10.21.05, 06.29.01 (draft text) | `docs/adrs/adr-004-contract-first-adversarial-verification.md` (revised from draft) |
| Verification design + 10-card scoping | 06.33.57 (byte-identical), 10.21.05 | design file + `docs/kanban/stories/engineering-assurance-*.md` (10 cards, `status: incoming`) |
| Card-quality critique → document governance need | 07.03.10 (= 10.21.21) | `docs/process/document-governance.md`; **card rework instruction not executed** |
| Research-as-activity governance | 10.21.42 | `docs/process/research.md` |
| Governance review; authority-drift table; charter plan | 10.21.53, 10.22.15 | `PROCESS.md` (charter rewrite) |
| Glossary, charter, kanban policy, research policy, doc-governance sequencing, STYLE kernel debate | 10.22.46 | `docs/process/glossary.md`, `PROCESS.md`, `docs/process/kanban.md`, `docs/process/research.md`, `STYLE.md` |
| Lisp construction form; pseudocode-as-data | 10.23.59, 10.24.27 | `STYLE.md` (§"The form is valid before it compiles", verified at line 256) |
| STYLE rewrite report | 10.24.20 | `STYLE.md` |
| Document governance λ | 10.24.33, 10.24.40 | `docs/process/document-governance.md` |
| Design practice δ | 10.24.46, 10.25.04 | `docs/process/design.md` |
| Unblocked-slice rule | 10.25.11 | `docs/process/design.md` (verified ~line 219) |
| Decision / review-acceptance / engineering policy drafts | 10.14.52 | `docs/process/decision.md`, `review-and-acceptance.md`, `engineering.md` |

Two items predate or sit beside this arc:

- **2026.07.11.09.27.05** — a clean product-overview essay (evidence tiers,
  Git identity model, continuity model, phase 1, interfaces, infrastructure).
  Candidate source for README/product-overview refresh. Disposition: `defer`.
- **2026.07.12.09.59.22** — an earlier Perplexity conversation on the notes
  lifecycle, an "inbox as contract", a deterministic perception layer
  ("unstructured text in, structured events out"), literature grounding
  (event-sourced personal knowledge graphs, schema-aware event extraction),
  and the wider runtime ecosystem (Rheos = kanban/flow, Knoxx = UI persona,
  OpenPlanner = memory, Sol/Katamorph). Its process ideas were realized as
  `docs/process/inbox.md` and `docs/process/notes.md`. Residue: a full
  `eta-mu-sol-notes-geometry` Clojure scaffold (deps.edn + domain/shape/law/
  infra namespaces) — reference material only, not Epiphany code; and the
  ecosystem vocabulary, which is useful cross-project context. Disposition:
  `extract` (this note) + `archive`.

## Per-item dispositions

| Item | Disposition | Basis |
|---|---|---|
| 2026.07.11.09.27.05 | `defer` | Product overview; candidate for docs refresh under document-governance |
| 06.12.04, 06.16.53, 06.19.25, 06.22.56 | `extract` → `archive` | Content absorbed into ADR-004/design; residue captured here and in defect inventory |
| 06.29.01 | `archive` | Superseded draft of ADR-004 (routed, revised) |
| 06.33.57 | `archive` | Byte-identical to `docs/designs/verification-architecture.md` |
| 07.03.10 | `extract` → `archive` | Origin of document-governance; card-rework instruction still open |
| 09.59.22 | `extract` + `archive` | See above |
| 10.14.52 | `archive` | Status report; policies verified present |
| 10.17.43, 10.17.55 | `closed-no-extraction` | Setup instructions; environment facts recorded above |
| 10.17.56 | `extract` | → defect inventory finding |
| 10.17.57, 10.17.57-1, 10.17.58 | `archive` (duplicate+prompt) | Prompts preserved in this map |
| 10.17.59, 10.17.59-1, 10.18.00, 10.19.54, 10.19.59, 10.20.01, 10.20.04, 10.20.07, 10.20.08, 10.20.19, 10.20.24 | `archive` (duplicate/truncated) | Checksummed against complete copies |
| 10.21.05 | `extract` | ENG-017 lane provenance; board.json not regenerated (eta-mu unavailable in sandbox) |
| 10.21.21 | `extract` | Card-rework-as-fixtures instruction (open) |
| 10.21.42, 10.21.53, 10.22.15, 10.22.46, 10.23.59, 10.24.20, 10.24.27, 10.24.33, 10.24.40, 10.24.46, 10.25.04, 10.25.11 | `extract` → `archive` | Routed to policies/STYLE as verified in the arc table |

No inbox originals were deleted or modified. "Archive" here is a recorded
disposition; physically relocating items into `docs/archives/` is left to the
board/curation workflow so this pass stays reversible.

## Residue: valuable and not yet routed

1. **Verified boundary defects with no cards** — unsafe `read-string` on HTTP
   EDN bodies, `restore-drill` stub, backup integrity, non-atomic metadata
   writes, workbench placeholder-empties. See the
   [defect inventory](inbox-synthesis-2026-07-12-defect-inventory.md).
2. **Static-analysis baseline not realized** — `06.12.04` opens with
   "Draft: Chore-000: Set up static code analysis (splint, clj-kondo, heretic,
   cloverage)". Verified: no `.clj-kondo/config.edn` (only an `imports/` dir),
   no `.splint.edn`, no `:lint` alias in `deps.edn`. ENG-017H covers this but
   is `incoming`.
3. **ENG-017 card rework instruction (open)** — the conversation itself ruled
   the 10 assurance cards "administratively valid, not executable" and said:
   do **not** promote them from `incoming`; build the story contract first,
   rewrite ENG-017A as the canonical exemplar, then revise the other nine.
   All 10 remain `incoming` and unreworked.
4. **`docs/standards/` corpus does not exist** — document-kind templates,
   examples, anti-examples, and the markdown contract checker are specified in
   `docs/process/document-governance.md` but unimplemented.
5. **Board transition enforcement** — see the
   [agent gaming pattern note](inbox-synthesis-2026-07-12-agent-gaming-pattern.md);
   the kanban FSM exists as policy but nothing mechanically rejects an illegal
   `ready -> done` jump or requires evidence at transitions.
6. **CI gate gap** — `.github/workflows/test.yml` runs only
   `clojure -M:unit-test`; the integration alias exists but no CI job uses it
   (verified). ENG-017E depends on ephemeral-Mongo CI availability.

## Highest-value next pass

Rework ENG-017A against the story minimum in
`docs/process/document-governance.md` as the exemplar story, then add the
missing defect cards from the inventory (each ≤ 5 points), then stand up the
static gate (`:lint` alias + `.clj-kondo/config.edn` baseline) so the lane has
an enforcement floor before more implementation lands.

## Open questions

- Should the assurance lane (ENG-017*) interrupt the Phase-1 ledger lane as
  P0? The conversation recommends reviewing this at triage; no decision is
  recorded anywhere in the corpus.
- Whether the duplicate/truncated exports should be physically archived or
  left in place until the board CLI can record the moves.
