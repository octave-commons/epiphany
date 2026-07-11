---
id: "01900d7c-7f3a-7e8b-9c4d-000000001004"
title: "US-004: Discover Git-backed Markdown revisions"
status: "incoming"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 1
labels: [git, markdown, ingestion, provenance, history]
category: "stories"
---

# US-004: Discover Git-backed Markdown revisions

## Acceptance Criteria

- The ingestion policy can target one or more exact path patterns, initially including `**/docs/**.md` and configurable Markdown paths.
- For each observed commit/tree entry, the system records repository family, repository instance, commit OID, parent OIDs, exact path string, blob OID, file mode, and observed time.
- The system reads Git object content through Git object access, not by checking out every historical revision.
- The system treats Git commits, trees, blob IDs, and path-at-commit relationships as observed facts.
- Re-running the same ingestion range is idempotent.
- A malformed Markdown file does not halt discovery of other files; it produces a structured extraction diagnostic.
- The system can report counts for commits scanned, Markdown paths found, unique blobs found, duplicate blobs reused, and failures.

## Notes

**As a corpus owner,** I want the system to discover every eligible Markdown revision in selected Git history so that historical notes are not limited to the current checkout.
