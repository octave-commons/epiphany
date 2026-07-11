---
id: 01900d7c-7f3a-7e8b-9c4d-000000000008
title: "Epic 7: Polyglot Source Ledger"
status: incoming
type: epic
priority: high
phase: 2
design: docs/notes/design/phase-2-code-comprehension.md
size: 8
labels: [ingestion, source, polyglot, provenance]
---

# Epic 7: Polyglot Source Ledger

Extend the Phase 1 artifact ledger from Markdown revisions to source and configuration revisions, with language identity, parser provenance, and a stable source-unit model.

## User outcome

“I can see every source file, configuration file, and historical revision in the corpus, and I know exactly which parser and extraction version produced every derived fact.”

## Supported languages

- Clojure: `.clj`, `.cljc`, `.cljs`
- Go: `.go`
- Python: `.py`
- C: `.c`, `.h`
- Lua: `.lua`
- Ruby: `.rb`
- Bash: `.sh`, executable scripts
- YAML: `.yaml`, `.yml`
- EDN: `.edn`
- JSON: `.json`

## Common source-unit contract

- file
- module/namespace
- declaration
- callable
- type/schema
- import/require
- invocation/reference
- literal/configuration key
- comment/docstring
- parse diagnostic

## Acceptance criteria

- A source revision can be retrieved exactly by repository, commit, path, and blob hash.
- Every revision has a detected language plus parser/extractor provenance.
- Unsupported or malformed files are retained as artifacts and produce diagnostics rather than disappearing.
- The system records whether each semantic fact came from syntax-only extraction, a language-native analyzer, or an inferred model.
- The same source blob and parser configuration always produce the same normalized extraction.
- Replaying the source ledger into an empty index reproduces the same file/revision records.

## Domain rule

A file is a historical container, not the principal unit of understanding. Preserve file topology but project meaningful units into the graph.

## Next step

Design the language-detection and parser-strategy registry.
