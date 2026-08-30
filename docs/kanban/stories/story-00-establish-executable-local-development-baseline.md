---
id: 01900d7c-7f3a-7e8b-9c4d-000000000000
title: "US-000: Establish executable local development baseline"
status: breakdown
type: story
priority: P0
phase: 0
blocks: ["US-001", "US-004", "US-005", "US-007", "US-010", "US-019", "US-020", "US-021"]
points: 13
labels: [bootstrap, clojure, build, local-first, testing, service-contracts, decomposed]
category: "stories"
dependency: []
---
# US-000: Establish executable local development baseline

## User outcome

"I can clone Epiphany, start a REPL, run the full test command, run the executable, and select an explicit local service profile before an implementation story relies on infrastructure."

## Problem

Phase 1 stories name MongoDB, object storage, jobs, search, and a CLI/TUI boundary, but the repository currently has no executable Clojure project, dependency manifest, process entry point, test baseline, or declared local-service contract. Those are implementation prerequisites, not details to be improvised by each feature story.

## Scope

- Add `deps.edn` as the canonical Clojure CLI dependency and alias manifest.
- Establish the initial source topology: `src/domain`, `src/infra`, `src/law`, `src/shape`, `test`, `dev`, and `bin`.
- Provide a single executable entry point for `epiphany` and its `ep` alias; initially it may only report version/help and selected profile.
- Provide `:test`, `:unit-test`, `:integration-test`, `:repl`, `:dev`, and `:run` aliases.
- Add a test runner and one passing smoke test, so `clojure -M:test` is the green baseline.
- Define an explicit profile contract: `:local` is in-process/direct mode, `:services` uses locally provisioned adapters, and no profile silently falls back to another.
- Add a checked-in local service manifest for MongoDB and S3-compatible object storage, plus documented lifecycle commands and non-secret defaults.
- Add ports/interfaces for durable observations/events, blob storage, job dispatch, and Git access. The baseline may use in-memory adapters where a story does not require an external service.
- Add health/readiness diagnostics that name unavailable required services without creating partial durable state.
- Document the supported JDK/Clojure versions and the exact bootstrap, test, and run commands.

## Out of scope

- Implementing repository registration, Git traversal, Markdown extraction, search, or a durable event schema.
- Selecting a production deployment platform, orchestrator, hosted MongoDB provider, or cloud object store.
- Starting HTTP, TUI, vector, graph, queue, or browser services merely to make the skeleton look complete.

## Acceptance criteria

- On a supported JDK, a fresh clone succeeds with `clojure -M:test` without manually editing local files.
- `clojure -M:run -- --help` succeeds and identifies the canonical executable as `epiphany`; `ep` invokes the same entry point.
- `clojure -M:repl` starts a usable REPL with production code on its classpath and does not require external services.
- `clojure -M:unit-test` runs tests that require no Docker or network access; `clojure -M:integration-test` either runs against explicitly started local services or exits with clear availability diagnostics.
- The checked-in service manifest can start MongoDB and S3-compatible object storage using documented commands, with data directories ignored by Git.
- Service connection settings are obtained only from a documented profile/config boundary; credentials and machine-specific paths are not committed.
- `:local` and `:services` selection is explicit in diagnostics and command output; failure to reach a selected service yields `UNAVAILABLE`, never a silent in-memory or alternate-target fallback.
- The initial storage ports distinguish append-only observed facts/commands from rebuildable projections, consistent with `AGENTS.md`.
- A bootstrap test proves that the application can construct its direct-mode dependency graph using in-memory adapters.
- CI can execute formatting/linting (if configured) and the unit-test baseline headlessly.

## Implementation notes

Start from Truth's useful conventions—`deps.edn`, alias-based commands, explicit test groups, thin `bin/` wrappers, and a `domain`/`infra`/`law`/`shape` split—but retain only dependencies needed for the Phase 1 bootstrap. The first manifest should not pre-install an embedding provider, HTTP stack, TUI, vector database, or queue.

The service manifest is a reproducible local adapter environment, not a canonical source of Git facts. Git remains canonical for Git-originated objects; MongoDB is durable for Epiphany observations/events/decisions/jobs/checkpoints; object storage retains immutable source blobs; derived indexes remain rebuildable.

## Decomposed into

This card exceeded the 5-point cap (its own subtask total was 14). Do not implement it directly. The work lives in:

- **US-000A** `story-00a-project-scaffold-and-test-baseline.md` — scaffold, entry point, green test baseline (4 pts)
- **US-000B** `story-00b-ports-profiles-inmemory-adapters.md` — profile contract, ports, in-memory adapters (5 pts)
- **US-000C** `story-00c-local-service-manifest-and-diagnostics.md` — service manifest, readiness diagnostics (4 pts)

Acceptance criteria above remain the definition of done for the set; each child card carries its own slice.
