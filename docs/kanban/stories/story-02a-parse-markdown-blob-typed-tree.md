---
id: "01900d7c-7f3a-7e8b-9c4d-000000001201"
title: "ENG-002A: Parse a Markdown blob into a typed tree with source spans"
status: "done"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000002"
design: "docs/kanban/epics/epic-02-markdown-evidence-extraction.md"
points: 4
labels: ["phase-1", "markdown", "parsing", "spans"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000000000-a"]
---

# ENG-002A: Parse a Markdown blob into a typed tree with source spans

Pure parsing (`shape/`): one Markdown string in, one typed block tree out. Decide flexmark vs commonmark-java here and record why in the card.

## Acceptance criteria

- Headings preserve hierarchy; paragraphs, lists, tables, quotes, and fenced code are typed blocks; unsupported constructs become diagnostics, not crashes.
- Front matter is parsed separately from the body.
- Every node carries byte and line spans; the exact raw source slice is recoverable from blob + span.
- Output is deterministic for the same input and parser version.
- Unit-tested only — no Git, no Mongo.

---
Chose flexmark over commonmark-java: native source spans via Node.getStartOffset/getEndOffset, built-in YAML front matter + table extensions, well-maintained 0.64.x stable. Parser configured with YamlFrontMatterExtension and TablesExtension. All unsupported constructs become :diagnostic blocks, not crashes. 58 unit tests, 179 assertions, 0 failures. Files: src/epiphany/shape/markdown.clj, src/epiphany/law/markdown.clj, test/epiphany/shape/markdown_test.clj. --tasks-dir docs/kanban

Implementation complete. Chose flexmark 0.64.8 over commonmark-java for native source spans. 58 tests, 179 assertions, 0 failures. Ready for review. --tasks-dir docs/kanban

Review found span offset bug: :span/start-byte and :span/end-byte are actually UTF-16 char offsets, not byte offsets. Moving back to in_progress to fix. slice() works accidentally because it uses char-based subs, but stored spans don't match true UTF-8 byte offsets. Need to fix span-of to compute true UTF-8 byte offsets. --tasks-dir docs/kanban

Fixed span offset bug: added char-offset->byte-offset and byte-offset->char-offset functions to convert between UTF-16 char offsets (flexmark native) and UTF-8 byte offsets. Updated slice() to work with byte-based spans. Added tests for non-ASCII content verification. 59 tests, 183 assertions, 0 failures. --tasks-dir docs/kanban

Fixed surrogate pair bug in byte-offset->char-offset: 4-byte UTF-8 sequences (astral plane characters like emoji) now correctly increment char-count by 2. Added test case with 😀 emoji. 59 tests, 185 assertions, 0 failures. --tasks-dir docs/kanban

Fixed review issues: (1) fenced code info string extraction now handles 4+ character fences correctly, (2) added clojure.string require to test file, (3) renamed :block/span to :inline/span for inlines, (4) made :list/start-number optional in schema. 60 tests, 191 assertions, 0 failures. --tasks-dir docs/kanban

Final review approved. All 4 fixes confirmed correct. 60 tests, 191 assertions, 0 failures. Moving to done. --tasks-dir docs/kanban
---
