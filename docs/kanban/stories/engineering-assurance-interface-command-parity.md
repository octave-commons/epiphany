---
id: "01900d7c-7f3a-7e8b-9c4d-000000001707"
title: "ENG-017G: Normalize CLI and HTTP command contracts"
status: "incoming"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000006"
design: "docs/designs/verification-architecture.md"
points: 5
labels: [quality, cli, http, contracts, phase-1]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001702", "01900d7c-7f3a-7e8b-9c4d-000000001601"]
---

# ENG-017G: Normalize CLI and HTTP command contracts

Refactor CLI argv and HTTP request handling into thin decoders over shared,
validated application command maps and outcome categories.

## Scope

- Define named command/input schemas for the current shared interface surface.
- Extract pure CLI and HTTP decoders plus encoders.
- Ensure handlers call application services/ports rather than direct adapters.
- Add parity tests using equivalent input through both interfaces.

## Acceptance criteria

- Equivalent CLI and HTTP input normalizes to the same command data.
- Invalid client input becomes a stable boundary error, not an internal error.
- HTTP and CLI preserve the same normalized accepted/rejected/unavailable outcome category.
- No HTTP handler directly accesses Mongo, Lucene, or Git adapters.
