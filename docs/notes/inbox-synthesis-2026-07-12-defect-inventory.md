---
slug: inbox-synthesis-2026-07-12-defect-inventory
uuid: 9057f23b-be59-43d1-84ca-c0ad7239c62e
title: "Verification Defect Inventory: Boundary and Failure-Mode Claims, Verified"
kind: finding
status: draft
description: "Agent code-review claims from the 2026-07-12 inbox batch, re-verified against the working tree at e92e389; each claim carries its epistemic tier, evidence, and disposition."
labels: [finding, verification, assurance, boundaries, security, adr-004]
created: "2026-07-12"
sources:
  - "docs/inbox/2026.07.12.10.17.56.md"
  - "docs/inbox/2026.07.12.06.12.04.md"
informs:
  - "docs/designs/verification-architecture.md"
  - "docs/kanban/stories/engineering-assurance-*.md"
---

# Verification Defect Inventory

## Bounded claim

The agent code review preserved in `docs/inbox/2026.07.12.10.17.56.md` asserts
a set of boundary, failure-mode, and honesty defects in this codebase. This
finding re-verifies those assertions against the working tree at `e92e389`
(2026-07-12) and separates **observed** defects (inspected directly, with
locations) from **claimed** defects (plausible, from the source conversation,
not independently re-verified in this pass).

Method: direct reading and targeted grep of the named namespaces, CI workflow,
and config files. No runtime reproduction was performed; that is a stated
limitation, not evidence of absence.

## Observed defects (verified this pass)

### 1. Untrusted HTTP EDN bodies parsed with `clojure.core/read-string`

`src/epiphany/infra/http.clj:97` — request bodies with content-type
`application/edn` are passed to bare `read-string`; the namespace does not
require `clojure.edn`. Same pattern at `http.clj:338` and `http.clj:342`
(fallback re-parse), and `src/epiphany/infra/adapters/lucene.clj:143` reads
the Lucene version file with `read-string`. With default `*read-eval*`, EDN
request bodies can trigger reader evaluation (`#=(...)`) — arbitrary code
execution on any exposed HTTP profile — and even with read-eval off,
`read-string` accepts reader behavior a data-only parser must reject.

**Consequence:** remote code execution risk plus undefined parse behavior at a
public boundary. **Disposition:** highest-priority fix; replace with
`clojure.edn/read-string` (no default readers), map parse failure to a 400
problem response. No existing card covers this. `plan-work` candidate, ~2pts.

### 2. `restore-drill` claims five stages, performs one

`src/epiphany/domain/backup.clj:81-108` — the docstring promises export →
drop → import → re-export/compare → inaccessible-source check; the body
exports, prints, and returns `{:drill-status :export-complete}`. A restore
drill that never restores is a false capability claim of exactly the kind
`PROCESS.md` forbids ("not implemented ... must not be represented as a
successful result").

**Disposition:** either implement the drill or rename to
`export-for-restore-drill` and surface `:not-implemented` for the remaining
stages; add a test that fails unless all documented stages execute.
`plan-work` candidate; relates to ENG-017F (read/import integrity) but is not
covered by it.

### 3. In-memory observation adapter accepts anything

`src/epiphany/infra/adapters/in_memory.clj:55-105` — `:record-ingestion-run!`,
checkpoints, section extractions, revisions-at-path are bare `swap!` appends
with no schema validation, uniqueness, or idempotency-conflict enforcement.
This is the root of the false-green unit suite the batch is about.

**Disposition:** already the subject of ENG-017A–D (`incoming`). No new work
needed beyond executing that lane; recorded here as the verified anchor
observation for it.

### 4. CI exercises only the unit suite

`.github/workflows/test.yml:27` — the only test step is
`clojure -M:unit-test`. `deps.edn` defines `:integration-test`
(kaocha `integration`) and a full `:test` alias; no CI job runs them. Combined
with defect 3, CI green currently certifies persistence behavior that
production adapters may reject.

**Disposition:** covered by ENG-017E/H/J CI-gate work; verify ephemeral-Mongo
availability before marking ENG-017E ready (open question in the source
conversation).

### 5. Workbench placeholders return empties indistinguishable from data

`src/epiphany/infra/workbench.clj:352` ("With real data, we'd query lineage
here. For now return placeholder.") and `:456` (candidates/decisions) return
placeholder/empty results from unimplemented queries. Under the charter,
*empty evidence is a claim*; a feature that has not queried its source must
report unavailable/not-implemented, not an empty result.

**Disposition:** `plan-work` candidate (small): make these routes return an
explicit `{:status :unavailable}` / 501-style result. Partially adjacent to
ENG-017F but interface-level; also a good first fixture for the
"placeholder-empty" adversarial test class in ADR-004.

### 6. No static-analysis configuration exists

No `.clj-kondo/config.edn` (only `.clj-kondo/imports/`), no `.splint.edn`, no
`:lint` alias in `deps.edn`. The "Chore-000: static analysis" draft exists
only inside inbox item `06.12.04`.

**Disposition:** covered by ENG-017H (`incoming`); the chore draft (splint,
clj-kondo, heretic, cloverage) should fold into that card's rework rather
than becoming a separate card.

## Claimed defects (from source; not re-verified this pass)

Tier: derived-from-source, `provisional`. Each needs a short inspection before
becoming a card.

- **Backup import lacks integrity verification** — `import-from-file`
  checks only `:format`; no manifest version, expected collections, counts, or
  checksum; possible logical-vs-physical collection-name mismatch making
  `inaccessible-sources` silently inspect an empty list (`10.17.56`).
- **Non-atomic metadata writes** — repository and Lucene version metadata
  written with direct `spit`; crash/race can leave partial files; concurrent
  registration may race on "missing then write" (`10.17.56`).
- **HTTP boundary permissiveness** — `:limit` unvalidated (type/bounds);
  parse failures become 500s instead of 400s; `wrap-exceptions` returns
  exception messages (path/topology disclosure); `Accept` matched by
  substring; `text/plain` uses `(str data)` (`10.17.56`).
- **Mongo validates only repository-location observations** — other record
  kinds persist unvalidated (`06.16.53`). `mongo.clj` does require
  `epiphany.law.registry` and validates *something*; completeness unverified.
- **Domain-layer JVM leakage** — direct `UUID`/`Date`/digest calls in domain
  namespaces; interop concentration figures (690 dot calls, 106 type hints;
  Mongo 238, Lucene 65, Git 47) are the conversation's grep counts, not
  re-measured here (`06.12.04`).

## What this finding does not establish

- That the observed list is complete — this pass verified the source
  conversation's claims; it did not independently audit the codebase.
- Severity ordering beyond the obvious (defect 1 is the only remote-execution
  risk found).
- That any fix is accepted work — cards require triage under the kanban
  policy; this finding only supplies evidence and boundaries.

## Confidence

High for the six observed defects (direct inspection, locations cited).
Moderate for the claimed set (consistent with observed code style and partially
corroborated, but unverified).

## Disposition summary

Propose three new small cards (EDN parse boundary; restore-drill honesty;
workbench unavailable-vs-empty), fold Chore-000 into ENG-017H's rework, and
treat the claimed set as a checklist for a follow-up inspection pass —
ideally executed as part of ENG-017F/G rework rather than as separate cards.
