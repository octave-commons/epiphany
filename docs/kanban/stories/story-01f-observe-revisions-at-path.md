---
id: "01900d7c-7f3a-7e8b-9c4d-000000001106"
title: "ENG-001F: Persist revision-at-path observations"
status: "done"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/adrs/adr-001-git-backed-resource-identity.md"
points: 2
labels: ["phase-1", "archaeological-ledger", "engineering-breakdown"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001105"]
---

# ENG-001F: Persist revision-at-path observations

Persist immutable file-at-commit observations that link selected tree entries to their Git evidence.

## Acceptance criteria

- Records distinguish add/modify/delete evidence from a claim of document continuity.
- Each record retains repository family/instance context, commit/tree/blob OIDs, exact path, mode, parent comparison context where available, observed time, and schema version.
- Idempotent reruns do not duplicate the same observation.
- A reader can retrieve source bytes through Git-object access from the recorded blob evidence, without a historical checkout.

---
Implementation complete. Created domain/revision_at_path.clj (evidence-type, revision-at-path, deduplicate), added observation/revision-at-path-v1 schema. 20 tests covering evidence types, idempotency, blob retrieval without checkout. 113 tests, 350 assertions, 0 failures. Ready for review. --tasks-dir docs/kanban
---
