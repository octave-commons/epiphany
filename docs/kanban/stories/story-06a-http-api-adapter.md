---
id: "01900d7c-7f3a-7e8b-9c4d-000000001601"
title: "ENG-006A: Expose the HTTP API adapter (`/api/v1`)"
status: ready
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000006"
design: "docs/kanban/epics/epic-06-temporal-research-workbench.md"
points: 4
labels: [phase-1, http, api, adapter]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001304", "01900d7c-7f3a-7e8b-9c4d-000000001401"]
---
# ENG-006A: Expose the HTTP API adapter (`/api/v1`)

reitit + ring adapter over the same command/query services the CLI uses. Search, evidence, trace, inbox, review-decisions.

## Acceptance criteria

- Errors are RFC 9457 problem+json; JSON default, EDN accepted locally.
- Adapter parity tests: direct CLI and HTTP produce equivalent outcomes for the same query.
- Review decisions are `POST /review-decisions` command resources, not mutable candidate updates.
- No business logic in the adapter; no direct Mongo/Lucene/Git access from handlers.
