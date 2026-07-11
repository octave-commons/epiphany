---
id: "01900d7c-7f3a-7e8b-9c4d-000000001010"
title: "US-010: Search current and historical notes"
status: "incoming"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000003"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 1
labels: [search, retrieval, lexical, semantic, hybrid]
category: "stories"
---

# US-010: Search current and historical notes

## Acceptance Criteria

- Search supports lexical retrieval across title/heading text, section text, path, front matter fields, tags, links, and commit messages where available.
- Search supports semantic/vector retrieval over the selected Phase 1 indexing unit.
- The default search result is a section expression, not an opaque whole-file match.
- Results can be filtered by repository family, repository instance, exact path prefix, date range, branch/ref where available, and observed/inferred status.
- Each result exposes source path, commit, date, heading path, exact excerpt, retrieval mode, and score components.
- The user can switch among lexical-only, semantic-only, and hybrid modes for diagnosis.
- Query logs capture sufficient metadata to evaluate retrieval quality without storing sensitive user text beyond the chosen local policy.

## Notes

**As a corpus owner,** I want to search my Markdown corpus by words, phrases, and meaning so that I can find relevant evidence even when the wording changed.
