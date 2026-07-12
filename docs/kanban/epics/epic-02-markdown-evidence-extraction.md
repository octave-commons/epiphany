---
id: 01900d7c-7f3a-7e8b-9c4d-000000000002
title: "Epic 2: Markdown Evidence Extraction"
status: breakdown
type: epic
priority: high
phase: 1
design: docs/designs/phase-1-corpus-archaeology.md
size: 8
labels: [parsing, markdown, provenance, extraction, decomposed]
---

# Epic 2: Markdown Evidence Extraction

Turn every Markdown revision into stable, addressable evidence units without losing the ability to trace them back to exact text.

## User outcome

“I can search and inspect notes at the level of headings, paragraphs, lists, quotes, code blocks, and links—and every derived claim points to its source span.”

## Scope

- Parse Markdown into a normalized AST.
- Generate stable section IDs based on content/path/revision context.
- Extract document title, heading hierarchy, sections, paragraphs, lists, blockquotes, tables, code fences, links, tags, wiki-style references, front matter, and offsets.
- Preserve parent/child containment: repository → file lineage → revision → document → section → block → span.
- Emit canonical text for each retrieval unit.
- Define chunking as a pure, versioned function.

## Domain rule

A section is not “the concept.” It is an evidence-bearing expression of one or more concepts at a point in time.

## Acceptance criteria

- Every extracted unit has an exact revision ID and source offsets.
- Rendering a unit’s source span produces the corresponding source text from the immutable blob.
- Heading changes, section moves, and paragraph edits create new revision-level evidence rather than destroying the older form.
- Extraction is idempotent for the same input blob and extractor version.
- Extraction errors produce diagnostics and preserve the original artifact for retry.
- A user can navigate from a search result to its source revision, surrounding section, commit, and full file.

## Next step

Select or implement a Markdown parser with source-span preservation and design the section/chunk schema.

---
Decomposed into 5 stories (ENG-002A–ENG-002E). All children are ready. Epic moves to breakdown per board contract. --tasks-dir docs/kanban
---
