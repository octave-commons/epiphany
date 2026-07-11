---
id: "01900d7c-7f3a-7e8b-9c4d-000000001011"
title: "US-011: Inspect search evidence"
status: "incoming"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000003"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 1
labels: [search, evidence, ui, provenance]
category: "stories"
---

# US-011: Inspect search evidence

## Acceptance Criteria

- Opening a result shows the source text from the exact blob/revision, not only a current working-tree file.
- The evidence reader displays repository instance/family, commit OID, author and commit times, exact path, heading path, and byte/line span.
- It provides surrounding section context without changing the cited source span.
- It can display raw Markdown and a rendered Markdown view.
- It links to the commit and direct parent/child revisions where available.
- If the Git object is currently inaccessible, the UI identifies the missing source state rather than fabricating an excerpt.
- The user can copy/export a stable evidence reference packet.

## Notes

**As a corpus owner,** I want every search hit to open at the exact historical source span so that I can decide whether the result actually supports my question.
