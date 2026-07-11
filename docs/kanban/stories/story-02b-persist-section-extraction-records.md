---
id: "01900d7c-7f3a-7e8b-9c4d-000000001202"
title: "ENG-002B: Persist versioned section-extraction records"
status: ready
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000002"
design: "docs/kanban/epics/epic-02-markdown-evidence-extraction.md"
points: 4
labels: [phase-1, markdown, extraction, mongodb]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001201", "01900d7c-7f3a-7e8b-9c4d-000000001106", "01900d7c-7f3a-7e8b-9c4d-000000001103", "01900d7c-7f3a-7e8b-9c4d-000000000000-c"]
---
# ENG-002B: Persist versioned section-extraction records

Persist heading-delimited sections and blocks per revision, linked to their revision-at-path evidence.

## Acceptance criteria

- A section records heading path, ordinal, byte/line span, content hash, extractor version, and source revision-at-path ID.
- Re-extracting the same blob with the same extractor version is idempotent; a new extractor version creates new records, never overwrites.
- No record implies a section in one revision is the same section in another revision.
- Malli schemas for extraction records live in law/ and are validated on write.
