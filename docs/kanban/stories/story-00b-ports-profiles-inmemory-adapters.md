---
id: "01900d7c-7f3a-7e8b-9c4d-000000000000-b"
title: "US-000B: Profile contract, storage ports, and in-memory adapters"
status: "done"
type: "story"
priority: "P0"
phase: 0
parent: "01900d7c-7f3a-7e8b-9c4d-000000000000"
points: 5
labels: ["bootstrap", "clojure", "ports", "profiles", "local-first", "phase-0"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000000000-a"]
---

# US-000B: Profile contract, storage ports, and in-memory adapters

Second slice of US-000. Defines how the application composes against infrastructure before any real infrastructure exists.

## Scope

- Define the explicit profile contract: `:local` is in-process/direct mode, `:services` uses locally provisioned adapters, and no profile silently falls back to another.
- Add ports/interfaces for durable observations/events, blob storage, job dispatch, and Git access.
- Implement in-memory adapters sufficient for direct mode.
- Add a bootstrap test proving the application can construct its direct-mode dependency graph using in-memory adapters.

## Out of scope

The MongoDB/S3 service manifest and health diagnostics (US-000C). Any real adapter implementation (ENG-001A onward).

## Acceptance criteria

- `:local` and `:services` selection is explicit in diagnostics and command output.
- Failure to reach a selected service yields `UNAVAILABLE`, never a silent in-memory or alternate-target fallback.
- The initial storage ports distinguish append-only observed facts/commands from rebuildable projections, consistent with `AGENTS.md`.
- Service connection settings are obtained only from a documented profile/config boundary; credentials and machine-specific paths are not committed.
- The bootstrap test passes under `clojure -M:unit-test`.

---
US-000B implemented 2026-07-11. New files: law/ports.clj (Malli schemas for git, repository-metadata, observations, and composite application-ports), infra/adapters/in_memory.clj (isolated in-memory adapter constructor), infra/profile.clj (:local/:services profile contract with explicit selection and UNAVAILABLE semantics). Updated law/registry.clj to register port schemas. Tests: 126 tests, 380 assertions, 0 failures. Clj-kondo 0 warnings on new files. Bootstrap test proves :local profile composes in-memory adapters with registration layer; :services profile throws UNAVAILABLE; adapter worlds are independent. Acceptance criteria met: :local/:services explicit, UNAVAILABLE on missing service, storage ports distinguish append-only observations from rebuildable projections, credentials not committed, bootstrap test passes. --tasks-dir docs/kanban

AUDIT 2026-07-12: status=done graded B+. Completion evidence recorded and verified: law/ports.clj, infra/profile.clj, in_memory.clj exist; UNAVAILABLE semantics and profile explicitness hold; 126 tests at the time. The defect is in the acceptance criteria, not the claim: nothing required the in-memory adapters to enforce record contracts, so this card legitimately shipped the permissive test oracle behind the false-green problem (in_memory.clj:55-105 bare swap! appends). Gate that would have caught it: ENG-017C (contract-enforcing reference adapter) — now reworked to cite this card. Lesson: criteria quality is part of card quality; 'done per criteria' can still institutionalize a defect. --tasks-dir docs/kanban
---
