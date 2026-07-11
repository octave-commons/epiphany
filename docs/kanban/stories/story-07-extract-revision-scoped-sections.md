---
id: "01900d7c-7f3a-7e8b-9c4d-000000001007"
title: "US-007: Extract revision-scoped sections"
status: "breakdown"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000002"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 8
labels: [markdown, extraction, parsing, provenance, sections, decomposed]
category: "stories"
---

# US-007: Extract revision-scoped sections

## Acceptance Criteria

- Given a Markdown blob, the system creates a versioned extraction record.
- The extractor represents a document revision, heading-delimited sections, and blocks within sections.
- A section records exact heading path, ordinal, byte span, line span, content hash, extractor version, and source revision-at-path ID.
- Nested headings preserve hierarchy.
- Front matter is represented separately from the body and remains linked to the same revision.
- Paragraphs, lists, tables, quotes, and fenced code blocks are represented as typed blocks or diagnostics where unsupported.
- The exact raw source slice can be recovered from the Git blob plus recorded byte span.
- The system never assumes a section in one revision is the same persistent section as one in another revision.

## Notes

**As a corpus owner,** I want Markdown revisions split into inspectable sections so that retrieval and evidence point to meaningful source regions rather than whole files.

## Decomposed into

Product-outcome card; do not implement directly. The engineering slices:

- **ENG-002A** `story-02a-parse-markdown-blob-typed-tree.md` — typed tree + source spans (4)
- **ENG-002B** `story-02b-persist-section-extraction-records.md` — versioned section records (4)
- **ENG-002C** `story-02c-extraction-projection-runner.md` — checkpointed projection runner (3)
