---
id: "01900d7c-7f3a-7e8b-9c4d-000000001003"
title: "US-003: Recognize shared history"
status: "breakdown"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 8
labels: [git, ingestion, provenance, shared-history, deduplication]
category: "stories"
---

# US-003: Recognize shared history

## Acceptance Criteria

- When a newly registered repository shares one or more commit OIDs with an existing repository family, it joins that family automatically.
- The new path remains a separate repository instance with independent availability and ref observations.
- Commits and blobs already observed in the family are not re-recorded as new facts merely because another instance exposes them.
- Matching remote URLs, matching folder names, or similar content alone never auto-merge repository families.
- The registration result explains whether the instance created a new family or joined an existing family and cites the overlapping commit evidence.

## Notes

**As a corpus owner,** I want clones, mirrors, and diverged local representations with shared commits to share historical indexing work.
