---
uuid: "a51d82f0-be84-4fa5-95aa-d777143d7856"
title: "ENG-017N: Assurance-kernel hardening (prod wrapper wiring, write-op classifier guard, law-suite generalization)"
status: "incoming"
priority: "P2"
labels: ["quality", "schemas", "verification", "hardening", "phase-1"]
created_at: "2026-07-21T20:06:07.378Z"
parent: "eng-017b-enforce-schemas-through-validating-observation-ports"
points: "3"
phase: "1"
type: "story"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
verification: ["unit-test"]
risk: "low"
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
write-id: "1784664367378-0.kpqf9ctilqek1a87820"
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

