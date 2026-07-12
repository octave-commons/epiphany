---
id: "01900d7c-7f3a-7e8b-9c4d-000000001108"
title: "ENG-001H: Record possible history-replacement evidence"
status: "done"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/adrs/adr-001-git-backed-resource-identity.md"
points: 2
labels: ["phase-1", "archaeological-ledger", "engineering-breakdown"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001104", "01900d7c-7f3a-7e8b-9c4d-000000001107"]
---

# ENG-001H: Record possible history-replacement evidence

Detect changed selected-ref reachability and record it as an auditable observation.

## Acceptance criteria

- A ref whose prior observed target is no longer reachable creates a rewrite/replacement observation with old/new evidence.
- No repository family is silently split or merged.
- Prior observations remain retained and queryable.
- Diagnostics clearly label the condition as observed ref/history evidence, not a semantic conclusion.

---
Completed. Added history-replacement-v1 schema, domain/history_replacement.clj with detect-replacements/detect-missing-refs/make-replacement-record, 8 tests (23 assertions). Total: 151 unit tests + 11 integration tests, 483 assertions, 0 failures.
---
