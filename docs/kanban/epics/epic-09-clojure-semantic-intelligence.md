---
id: 01900d7c-7f3a-7e8b-9c4d-000000000010
title: "Epic 9: Clojure Semantic Intelligence"
status: incoming
type: epic
priority: high
phase: 2
design: docs/notes/design/phase-2-code-comprehension.md
size: 8
labels: [clojure, clj-kondo, semantic-analysis, dependencies]
---

# Epic 9: Clojure Semantic Intelligence

Make Clojure and ClojureScript the first deeply understood language layer using `clj-kondo`, compiler-aware metadata where appropriate, and Clojure-specific domain modeling.

## User outcome

“I can ask what a namespace provides, what symbols it consumes, where a var is defined and used, how macros affect the analysis, and which namespaces form a coherent subsystem.”

## Scope

- Run `clj-kondo` project-wide and ingest its analysis export/cache-derived facts.
- Capture namespace declarations, `:require`, `:use`, `:import`, aliases, refer clauses, var definitions, var usages, keywords, protocol definitions/implementations, multimethods/methods, macros/macro usages, test declarations, linter findings, and source locations.
- Ingest project configuration from `.clj-kondo/config.edn`.
- Treat macro-heavy or dynamically resolved behavior as explicitly incomplete rather than falsely resolved.
- Build Clojure-specific relationship types: `:namespace/requires`, `:var/defines`, `:var/references`, `:protocol/implemented-by`, `:multimethod/implemented-by`, `:macro/expands-into`, `:test/verifies`, `:config/affects-analysis`.
- Link Clojure docstrings, comments, namespace names, and keyword vocabularies to Phase 1 concepts.

## Research question enabled

“Did this namespace become conceptually incoherent because its responsibilities drifted, because it acquired too many dependency directions, or because the original domain boundary was never expressed in code?”

## Acceptance criteria

- Every Clojure semantic edge identifies its analyzer version and source location.
- Users can navigate definition → references → containing namespace → dependents.
- Namespace dependency views distinguish explicit requires from actual resolved symbol usage.
- Protocol, multimethod, macro, and test relationships are visible as different edge types.
- Dynamic/unresolved references are visibly marked as unknown or partial, not silently omitted.
- `clj-kondo` lint diagnostics are searchable and traceable to historical revisions.
- The system can compare namespace dependency structure across Git revisions.

## Next step

Set up `clj-kondo` analysis export ingestion and define the semantic fact schema.
