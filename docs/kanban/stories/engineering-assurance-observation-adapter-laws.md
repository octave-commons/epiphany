---
id: "01900d7c-7f3a-7e8b-9c4d-000000001704"
title: "ENG-017D: Establish reusable observation adapter contract laws"
status: "incoming"
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
points: 5
labels: [quality, contract-tests, adapters, differential-testing, phase-1]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001703"]
---

# ENG-017D: Establish reusable observation adapter contract laws

Create one reusable law suite for every observations-port implementation, with
normalized data-level outcomes rather than driver-specific exceptions.

## Scope

- Build a parameterized adapter factory/law harness.
- Cover valid write, invalid rejection, no-state-change, idempotent replay,
  conflicting replay, list/query ordering where contracted, and export/import behavior.
- Run it against the in-memory reference adapter.

## Out of scope

- Mongo-specific lifecycle setup and document encoding; those are ENG-017E.

## Acceptance criteria

- The law suite accepts an adapter factory and capabilities declaration.
- Failures identify the law and normalized expected/actual outcome category.
- At least one deliberately permissive adapter implementation fails the suite.
- The suite is runnable by the standard unit-test alias.
