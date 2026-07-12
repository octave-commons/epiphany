---
id: "01900d7c-7f3a-7e8b-9c4d-000000001902"
title: "ENG-021A: Prove backup, restore, and rebuild"
status: "in_progress"
type: "story"
priority: "P1"
phase: 1
parent: "story-21-recover-corpus-archaeology-view"
design: "docs/kanban/stories/story-21-recover-corpus-archaeology-view.md"
points: 3
labels: ["phase-1", "operations", "recovery", "durability"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001107", "01900d7c-7f3a-7e8b-9c4d-000000001301", "01900d7c-7f3a-7e8b-9c4d-000000001303"]
---

# ENG-021A: Prove backup, restore, and rebuild

Documented, scripted Mongo backup/restore plus index rebuild — verified by a drill, not asserted.

## Acceptance criteria

- Restore recovers registrations, observations, events, review decisions, and checkpoints.
- Lucene/vector indexes rebuild from Git + restored Mongo state via documented commands.
- A restore drill reproduces a previously exported evidence packet; inaccessible sources are recorded, not papered over.
- Cache/index loss demonstrably loses no source fact or user decision.
