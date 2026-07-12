---
id: "01900d7c-7f3a-7e8b-9c4d-000000001103"
title: "ENG-001C: Define append-only Git registration observation schemas"
status: "done"
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/adrs/adr-001-git-backed-resource-identity.md"
points: 4
labels: ["phase-1", "archaeological-ledger", "engineering-breakdown", "schemas", "malli"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000000000-a"]
---

# ENG-001C: Define append-only Git registration observation schemas

Define Malli contracts and Mongo record shapes for registration and Git-ref observations before walking history.

## Acceptance criteria

- Schemas distinguish Git-observed commit/ref values from platform observation metadata.
- A registration record retains repository instance, family assessment state, exact path, common Git directory, request ID, observed time, and adapter/schema version.
- Schema validation rejects normalized/re-written paths and missing provenance fields.
- Versioned fixtures document the EDN/JSON shapes used by direct-mode output and Mongo adapters.

## Breakdown (revised)

Re-scored during board triage. Dependency inverted: schemas are contracts and come **before** the ENG-001A adapter, not after it — this card also owns the repository-location observation schema that ENG-001A's original subtask #1 duplicated. Honest scope at 4 points:

| # | Task | Points | Dependencies |
|---|------|--------|--------------|
| 1 | Malli schemas: Git-observed commit/ref values, registration observation (incl. repository-location), platform observation metadata | 2 | None |
| 2 | Validation rules: reject normalized/rewritten paths, require provenance fields | 1 | #1 |
| 3 | Versioned EDN/JSON fixtures + schema validation tests | 1 | #1, #2 |

### Notes

- Schemas are contracts; changes require version bumps.
- Git-observed values are immutable facts; platform metadata is append-only.
- Fixtures document expected shapes for direct-mode output and Mongo adapters.

---
ENG-001C implemented 2026-07-11 — REVIEW-READY, held in_progress because review WIP (1) is occupied by US-000A. New law/ quadrant: epiphany.law.git (git/oid, git/commit-oid, git/blob-oid, git/object-format, git/ref-name, git/ref), epiphany.law.observation (path/raw, path/observed provenance map, observation/registration-v1, observation/repository-location-v1, observation/git-ref-v1, exact-path? comparison contract), epiphany.law.registry (merged registry + memoized validators). Design choices: Git-observed values nest under :git/observed so the git-fact/platform-metadata distinction is structural; all record maps closed so normalized-path variant keys and unknown keys are rejected; provenance envelope (:observation/id, observed-at, adapter-version, schema-version, request-id) required; schema bodies are pure EDN (string regex patterns, quoted 'inst? symbol) with a test proving the registry round-trips through EDN. Versioned fixtures: test/epiphany/law/fixtures/{registration,repository_location,git_ref}_observation_v1.{edn,json} — EDN = direct-mode shape (validated in tests), JSON twin documents the Mongo document shape (documentation-only: no JSON parser dep is sanctioned until ENG-006A; Mongo adapter round-trip tests belong to ENG-001A). Evidence: clojure -M:test = 29 tests/78 assertions/0 failures; clj-kondo 0 warnings; cljfmt clean. Anomaly: bare Greek ημ has no NFD decomposition, so the Unicode-normalization rejection test uses ή/é (decomposable) — .ημ exact-path preservation is still covered by fixtures. --tasks-dir docs/kanban

Final review approved. All acceptance criteria met, 61 tests, 192 assertions, 0 failures. No ADR-001 deviations. Moving to done. --tasks-dir docs/kanban
---
