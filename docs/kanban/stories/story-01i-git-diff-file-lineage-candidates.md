---
id: "01900d7c-7f3a-7e8b-9c4d-000000001109"
title: "ENG-001I: Produce revision-level file-lineage candidates from Git diff"
status: "done"
type: "story"
priority: "P2"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/adrs/adr-001-git-backed-resource-identity.md"
points: 2
labels: ["phase-1", "archaeological-ledger", "engineering-breakdown"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001106"]
---

# ENG-001I: Produce revision-level file-lineage candidates from Git diff

Generate inspectable path/rename/copy candidates from explicit parent-child Git comparisons.

## Acceptance criteria

- Candidate records retain both compared commits, paths, blob OIDs, Git diff settings/threshold, detector version, and generated time.
- Exact content relocation and threshold-based rename/copy signals remain distinct.
- All outputs have `PROVISIONAL` status unless they are directly an exact Git observation; none create permanent document identity.
- Re-running with a different policy creates a separately versioned candidate set.
