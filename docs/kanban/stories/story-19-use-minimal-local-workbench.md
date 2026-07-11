---
id: "01900d7c-7f3a-7e8b-9c4d-000000001019"
title: "US-019: Use a minimal local workbench"
status: "incoming"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000006"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 1
labels: [ui, workbench, local, interface, visualization]
category: "stories"
---

# US-019: Use a minimal local workbench

## Acceptance Criteria

- The first interface may be a local web application backed by CLI/EDN-compatible operations.
- It has at least:
  - search;
  - evidence reader;
  - comparison view;
  - timeline/lineage view;
  - review inbox;
  - ingestion/projection status.
- Every screen shows clear source/projection status where relevant.
- The UI supports Unicode paths without transliteration.
- The UI has no dependency on external SaaS access for the local corpus workflow.
- The UI works when only one strong machine is online, subject to the availability of its local source repositories and indices.

## Notes

**As a corpus owner,** I want one local interface that unifies search, evidence, timeline, and review so that corpus archaeology is a practical loop rather than a collection of admin tools.
