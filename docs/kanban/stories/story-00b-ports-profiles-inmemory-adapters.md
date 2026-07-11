---
id: "01900d7c-7f3a-7e8b-9c4d-000000000000-b"
title: "US-000B: Profile contract, storage ports, and in-memory adapters"
status: ready
type: "story"
priority: P0
phase: 0
parent: "01900d7c-7f3a-7e8b-9c4d-000000000000"
points: 5
labels: [bootstrap, clojure, ports, profiles, local-first, phase-0]
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
