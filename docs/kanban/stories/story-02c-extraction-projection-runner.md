---
id: "01900d7c-7f3a-7e8b-9c4d-000000001203"
title: "ENG-002C: Run extraction as a checkpointed projection"
status: ready
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000002"
design: "docs/kanban/epics/epic-02-markdown-evidence-extraction.md"
points: 3
labels: [phase-1, extraction, projection, checkpoint]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001202", "01900d7c-7f3a-7e8b-9c4d-000000001107"]
---
# ENG-002C: Run extraction as a checkpointed projection

Drive ENG-002B over discovered Markdown revisions as a resumable projection.

## Acceptance criteria

- Interrupted runs resume from the projection checkpoint without duplicating records.
- A malformed file produces a per-item diagnostic and does not halt the run.
- The run reports counts: revisions scanned, sections extracted, failures.
- Replay into an empty projection works from Mongo observations + Git objects.
