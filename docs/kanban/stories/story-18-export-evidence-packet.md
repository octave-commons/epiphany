---
id: "01900d7c-7f3a-7e8b-9c4d-000000001018"
title: "US-018: Export an evidence packet"
status: "incoming"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000006"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 1
labels: [ui, workbench, export, evidence, packet, edn]
category: "stories"
---

# US-018: Export an evidence packet

## Acceptance Criteria

- The user can export selected results, timeline nodes, and review decisions as Markdown and EDN or JSON.
- Each export identifies repository resource ID, commit OID, exact path, source spans, timestamps, relation status, and extraction/index/model versions.
- The packet separates:
  - observed source facts;
  - inferred candidates;
  - accepted user interpretations;
  - unanswered questions.
- The export contains enough identifiers to reproduce evidence lookup locally.
- Exporting never includes a claim without an attached evidence reference or an explicit "interpretation/no direct source" label.

## Notes

**As a corpus owner,** I want to export a compact evidence-backed research packet so that I can use the result in planning, writing, or later agent-assisted work.
