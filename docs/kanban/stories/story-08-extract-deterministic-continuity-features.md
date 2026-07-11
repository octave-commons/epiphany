---
id: "01900d7c-7f3a-7e8b-9c4d-000000001008"
title: "US-008: Extract deterministic continuity features"
status: "incoming"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000002"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 1
labels: [markdown, extraction, continuity, features, provenance]
category: "stories"
---

# US-008: Extract deterministic continuity features

## Acceptance Criteria

- For relevant adjacent revision transitions, the system stores raw measurements for:
  - text similarity;
  - front matter stability/change;
  - explicit-link overlap/change;
  - time gap;
  - named-entity overlap/change.
- The resulting Markdown continuity score identifies the versioned policy/configuration used.
- The score is stored separately from observed Git facts.
- The UI can display every raw signal alongside the score.
- Recomputing the policy creates a new derived result; it does not rewrite prior results or review decisions.
- Markdown and code have separate policy namespaces from the outset, even though code extraction is deferred.

## Notes

**As a corpus owner,** I want the system to calculate inspectable Markdown continuity features so that I can understand why it thinks a document changed gradually or discontinuously.
