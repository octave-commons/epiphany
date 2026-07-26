---
labels: ["quality", "schemas", "verification", "hardening", "phase-1"]
parent: "eng-017b-enforce-schemas-through-validating-observation-ports"
phase: "1"
type: "story"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
write-id: "1784943216531-0.mggrv5myts690tiyy6"
points: "3"
verification: ["unit-test"]
risk: "low"
title: "ENG-017N: Assurance-kernel hardening (prod wrapper wiring, write-op classifier guard, law-suite generalization)"
priority: "P2"
status: "done"
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
uuid: "a51d82f0-be84-4fa5-95aa-d777143d7856"
created_at: "2026-07-21T20:06:07.378Z"
---

# ENG-017N: Assurance-kernel hardening

## Intent

Three non-blocking findings from the 2026-07-21 independent re-reviews of
ENG-017A/017B/017D. None is a correctness hole today; each is a latent gap or
a place where a proven guarantee is not actually exercised on the paths that
matter. Bundled here so the assurance kernel's guarantees are live and
drift-proof, not merely provable.

## Scope

### 1. Wire the production CLI through the validating composition root (from 017B) — PRIORITY
`src/epiphany/infra/main.clj` builds adapters DIRECTLY via `in-memory/make`
(run-register/run-search/run-serve, ~lines 94, 339, 430) and the Mongo path,
bypassing `infra/profile.clj`'s `resolve-adapters` and therefore the ENG-017B
`validating-observations-port` wrapper. This is not an unvalidated hole —
both adapters enforce adapter-local `validate-write!`/`validate!` as
defense-in-depth (ADR-004 §2) — but the composition-root wrapper (017B's
actual deliverable) is never exercised on shipped command paths; its
protection is proven only via `resolve-adapters` and tests. Retarget the CLI
onto `resolve-adapters`/`profile/resolve` so BOTH ADR-004 layers are live in
production, with a test asserting the CLI-built port is the wrapped one.

### 2. Guard the name-based write-op classifier (from 017A/017B)
`law/operations.clj` `write-operation?` classifies port ops as writes by name
(`:record-*!` / `:import-all`). A future durable write named otherwise
(`:append!`, `:save-observation!`) would be silently excluded from the derived
`port-write-operations` AND from 017B's eager unregistered-op check — both
share this oracle. Add either a naming-convention assertion over the port
schema (every write-shaped op matches the convention, or is explicitly
listed) or a documented rule + lint, so a non-conforming write can't slip past
validation unnoticed.

### 3. Generalize the ENG-017D law harness beyond repository-location (from 017D)
`test/epiphany/law_suite/observations_laws.clj` fixtures only
repository-location observations; the other write ops (revision-at-path,
ingestion-run, checkpoint, section-extraction, lineage-candidate) are not
driven by any law. Make the harness record-type-parametric (a fixture per
schema) so every persisted record kind is judged by the same contract laws —
so partial coverage isn't mistaken for full port-contract coverage. Overlaps
ENG-017E/017I; coordinate scope.

## Non-goals

- No schema/behavior changes to the record kinds themselves.
- `:import-all` record-level validation stays ENG-017F's job.

## Acceptance criteria

- CLI commands build their observations port through the validating wrapper;
  a test proves a CLI-constructed port rejects an invalid write at the wrapper.
- A non-conforming write-op name cannot silently escape registration/validation
  (guard test or lint + documented convention).
- The law harness exercises every observations write op, not just
  repository-location (or the remaining kinds are explicitly, visibly listed
  as pending with a reason).
- Full unit suite green.

## Completion evidence

Test output, `git diff --stat`, reviewer named at done.

---
TRIAGE 2026-07-24: incoming -> accepted. Points 3 honest across the three bundled hardening items. Parent ENG-017B done; reviewed cards 017A/017D done. Acceptance criteria present, ADR link present. Scope coordination note: item 3 (law-harness generalization) overlaps ENG-017E/017I — implementer must check those cards' status before writing fixtures and take only the harness-generalization part, leaving adapter-specific runs to 017E. Also note: ENG-003G (in review) added more direct adapter construction in main.clj (make-ingest-adapters, make-durable-index-ports) — item 1's retarget onto resolve-adapters must cover these new call sites too.

IMPLEMENTED 2026-07-24 (commit 06cd710).

Item 1 (composition root): profile/resolve-raw-adapters :services now composes real adapters — requires explicit :mongo-conn (lifecycle stays with caller) and :index-dir, real Git-local repository.edn metadata port (ADR-001), Mongo observations, durable Lucene index, Ollama embeddings. main.clj retargeted: run-register, run-status (make-status-adapters), run-ingest (make-ingest-adapters), run-serve (both profiles), with-observations-adapter. Both ADR-004 layers are now live on production CLI paths. Tests: with-redefs spy proves validating-observations-port is invoked by register/inbox-decide/status CLI paths; :services composition proven with a fake conn map (construction is I/O-free); invalid write rejected through the wrapper on a CLI-constructed port.

Item 2 (classifier guard): law/operations gains read-operation? (:find-*/:list-*/:export-all), destructive-port-operations (explicit #{:clear-all!}), and unclassified-port-operations — a port op matching no class fails the completeness suite. A future :append!-style write can no longer slip past the registry AND the wrapper silently; guard test proves the teeth (:append! classifies as nothing).

Item 3 (harness generalization): law suite is now record-type-parametric — op-fixtures for all 7 write ops. Universal laws (valid-write-accepted, invalid-write-rejected, rejection-leaves-state-unchanged) judge every op. Idempotency laws are kind-parametric: :full (repository-location: replay nil + changed-content conflict map), :first-write-wins (review-decision, lineage-candidate: any replay nil, original retained — matches BOTH adapters' actual semantics, verified differentially), :none (others). Mongo law suite re-run: all ops pass, including the new coverage. Scope coordination honored: no adapter-specific enforcement changes (017E), no schema changes.

Gates: clojure -M:unit-test => 712 tests, 1876 assertions, 0 failures. clojure -M:integration-test => 17 tests, 96 assertions, only the 2 pre-existing baseline failures. boundary-check clean. Interop baseline regenerated.

AUDIT 2026-07-25 (ultra-code wave, .ημ/workflows/eng-017n-review.edn): 3 lenses, 18 skeptic votes, quorum 2. 2 confirmed findings (same root, both should-fix): the generalized law suite's judged ops defaulted to the hand-maintained op-fixtures keys — a future registered write op would silently escape the harness, reintroducing the exact drift item 3 kills. FIXED in e339537: judged ops derive from operations/registered-write-operations; a fixtureless registered op is a loud :fail (:fixture-present) with a guard test. 7 other findings refuted by quorum. Gates after fix: 724 tests, 1903 assertions, 0 failures (unit); integration unchanged (20/99, 2 pre-existing).

REVIEW 2026-07-25: approve. Both confirmed findings from the wave fixed in e339537 with a guard test proving the harness can no longer silently skip a registered write op. No blockers. Gates: 724 tests, 1903 assertions, 0 failures.

GATE 2026-07-25: bin/kanban-done-gate exit 0. document->done via rheos MCP failed server-side ('paths[0]' type error — known bug, chore card exists). Status advanced by direct frontmatter edit with this audit trail.
---