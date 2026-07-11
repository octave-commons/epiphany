---
title: Phase 2 — Code Comprehension and Architectural Archaeology
slug: phase-2-code-comprehension
created: 2026-07-11
source: docs/inbox/clojure Natural Language Processing.md
kind: design
parent: docs/notes/design/knowledge-platform-overview.md
---

# Phase 2 — Code Comprehension and Architectural Archaeology

## Objective

Given a symbol, namespace/module, or subsystem, show what it does, what it depends on, what concepts it implements, which files historically evolved with it, where its real boundaries are, and which organizational changes are worth human review.

## In scope

- Clojure and ClojureScript first; then Go, Python, Bash, YAML, EDN, JSON, Lua, Ruby, and C through a common parsing contract.
- Immutable source revisions from Phase 1.
- Language-aware ASTs and semantic facts.
- Symbol/reference/dependency/co-change/runtime-evidence graphs.
- Concept-to-code links.
- Multi-view clusters and boundary recommendations.
- Code maps and comprehension workflows.
- Human-reviewed architecture refactoring plans.

## Out of scope

- Automatic large-scale file moves or namespace rewrites.
- Replacing language-native compilers, linters, type checkers, or build tools.
- Treating AST similarity as proof of equivalent behavior.
- Whole-program semantic analysis for every language from day one.
- General-purpose vulnerability scanning.
- Agent-authored refactors without a human-approved plan.
- Making code organization conform to a generic style ideology.

## Epics

| Epic | Name | Goal |
|---|---|---|
| Epic 7 | Polyglot Source Ledger | Extend the artifact ledger from Markdown to source and configuration revisions with parser provenance. |
| Epic 8 | Syntax Forest and Structural Features | Parse source files into lossless-ish concrete syntax representations and derive structural features. |
| Epic 9 | Clojure Semantic Intelligence | Make Clojure/ClojureScript the first deeply understood language layer using `clj-kondo`. |
| Epic 10 | Program Relationship Graph | Build a versioned, multi-layer graph of structural, semantic, temporal, and conceptual relationships. |
| Epic 11 | Concept-to-Code Grounding | Link Markdown concepts to code/config structures that implement, mention, test, or contradict them. |
| Epic 12 | Architectural Boundary Inference | Identify candidate subsystem and namespace boundaries using multiple independent views. |
| Epic 13 | Code Archaeology Workbench | Extend the workbench with source and architectural-comprehension workflows. |
| Cross-cutting | Analyzer quality and evaluation | Ensure Phase 2 is a research instrument rather than an attractive graph of plausible nonsense. |

## Epic 7: Polyglot Source Ledger

**Goal:** Extend the Phase 1 artifact ledger from Markdown revisions to source and configuration revisions, with language identity, parser provenance, and a stable source-unit model.

**User outcome:** “I can see every source file, configuration file, and historical revision in the corpus, and I know exactly which parser and extraction version produced every derived fact.”

### Supported languages

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

### Common source-unit contract

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

### Domain rule

A file is a historical container, not the principal unit of understanding. Preserve file topology but project meaningful units into the graph.

## Epic 8: Syntax Forest and Structural Features

**Goal:** Parse source files into lossless-ish concrete syntax representations and derive language-neutral structural features for retrieval, comparison, and visualization.

**User outcome:** “I can inspect a function or config block as a tree, search for recurring structural patterns, and compare implementation shapes across the corpus without pretending different languages are identical.”

### Scope

- Use Tree-sitter as the default CST/AST parser substrate where grammars are suitable.
- Store raw tree, normalized node stream, node types, parent/child/sibling relationships, source spans, comments/docstrings, and parser errors.
- Produce versioned structural representations: preorder node-type sequences, rooted subtree hashes, parent-child edge n-grams, selected AST paths, declaration shape, and control-flow/data-literal summaries where parser support permits.
- Define an AST n-gram vocabulary per language family rather than one universal bag of node names.
- Index structural features at declaration/function and module level.
- Build a visual tree inspector with source synchronization.

### Non-goal

Do not assume AST similarity implies domain similarity. Structural signals are one input to later multi-view clustering.

## Epic 9: Clojure Semantic Intelligence

**Goal:** Make Clojure and ClojureScript the first deeply understood language layer using `clj-kondo`, compiler-aware metadata where appropriate, and Clojure-specific domain modeling.

**User outcome:** “I can ask what a namespace provides, what symbols it consumes, where a var is defined and used, how macros affect the analysis, and which namespaces form a coherent subsystem.”

### Scope

- Run `clj-kondo` project-wide and ingest its analysis export/cache-derived facts.
- Capture namespace declarations, `:require`, `:use`, `:import`, aliases, refer clauses, var definitions, var usages, keywords, protocol definitions and implementations, multimethods and methods, macros and macro usages, test declarations, linter findings, and source locations.
- Ingest project configuration from `.clj-kondo/config.edn`.
- Treat macro-heavy or dynamically resolved behavior as explicitly incomplete rather than falsely resolved.
- Build Clojure-specific relationship types: `:namespace/requires`, `:var/defines`, `:var/references`, `:protocol/implemented-by`, `:multimethod/implemented-by`, `:macro/expands-into`, `:test/verifies`, `:config/affects-analysis`.
- Link Clojure docstrings, comments, namespace names, and keyword vocabularies to Phase 1 concepts.

### Research question enabled

> “Did this namespace become conceptually incoherent because its responsibilities drifted, because it acquired too many dependency directions, or because the original domain boundary was never expressed in code?”

## Epic 10: Program Relationship Graph

**Goal:** Build a versioned, multi-layer graph of structural, semantic, temporal, and conceptual relationships across source artifacts.

**User outcome:** “I can traverse from a concept to notes, then to code symbols, dependent namespaces, tests, co-changing files, and historical implementation decisions.”

### Relationship layers

| Layer | Example edges | Evidence source |
|---|---|---|
| Containment | Repository → revision → namespace → var | Parser/analyzer |
| Syntax | Declaration → AST subtree / structural fingerprint | Tree-sitter |
| Dependencies | Namespace → requires → namespace | `clj-kondo`, language analyzers |
| References | Symbol → calls/references → symbol | Semantic analysis |
| Configuration | Service → reads → EDN/YAML/JSON key | Parser + semantic adapter |
| Verification | Test → verifies → function/namespace | Test/analyzer conventions |
| Temporal | Revision → changed-with → revision | Git history |
| Co-change | File/symbol → co-changes-with → file/symbol | Commit projections |
| Conceptual | Note concept → described-by/implemented-by → code unit | Hybrid retrieval + review |
| Runtime | Service/function → observed-to-interact-with → service/function | Future traces; optional Phase 2 ingest |

### Domain rule

A graph edge is not automatically an architectural claim. Most edges are evidence. Architecture is the reviewed interpretation of many edges.

## Epic 11: Concept-to-Code Grounding

**Goal:** Link the human concepts expressed in Markdown to the code and configuration structures that implement, mention, test, or contradict them.

**User outcome:** “I can start with an idea in my notes and find its implementation, tests, relevant configs, and historical transitions—or learn that it was never implemented.”

### Candidate relation types

- `:implements`
- `:describes`
- `:tests`
- `:configures`
- `:depends-on-concept`
- `:obsolete-implementation-of`
- `:contradicts-design`
- `:possibly-related`

### Evidence signals

- Lexical overlap
- Embeddings
- Docstrings/comments
- Namespace/module naming
- Keyword/configuration vocabulary
- Git temporal proximity
- Commit-message overlap
- Explicit links
- User labels

## Epic 12: Architectural Boundary Inference

**Goal:** Identify candidate subsystem and namespace boundaries using multiple independent views of the codebase, then turn them into human-reviewable architectural hypotheses.

**User outcome:** “I can see why a group of files belongs together conceptually, why a namespace is likely misplaced, and what a low-risk reorganization would look like.”

### Signals for multi-view clustering

- Directed namespace/import dependency
- Resolved symbol reference
- Shared protocol/multimethod participation
- Shared domain vocabulary
- Docstring/comment embeddings
- AST structural patterns
- Shared config keys
- Shared tests
- Co-change history
- Temporal co-emergence
- User labels and accepted concept-to-code links
- Complexity/lint facts as weak diagnostic signals

### Candidate boundary relations

- `:belongs-to-subsystem`
- `:bridge-module`
- `:adapter`
- `:boundary-violation`
- `:cyclic-coupling`
- `:misplaced-by-concept`
- `:overloaded-namespace`
- `:candidate-extraction`
- `:candidate-merge`

### Critical principle

The filesystem is one coordinate system. Dependency topology, semantic vocabulary, tests, runtime behavior, and time are other coordinate systems. A useful boundary recommendation appears where those views converge—or where their mismatch reveals hidden architectural debt.

## Epic 13: Code Archaeology Workbench

**Goal:** Extend the Phase 1 research workbench with source and architectural-comprehension workflows.

**User outcome:** “I can explore the codebase as a living historical system rather than a directory tree, and move from a question to evidence to a reviewable design hypothesis.”

### Core views

- **Namespace map:** directed dependency graph, layering, cycles, inbound/outbound pressure.
- **Symbol explorer:** definition, references, callers/callees, historical changes, tests, docs.
- **Concept-to-code view:** Markdown concepts and implementation candidates with evidence/status.
- **Co-change timeline:** files, namespaces, and symbols that changed together across commits.
- **Structural motif explorer:** AST pattern search and matching subtrees.
- **Boundary map:** candidate clusters, bridges, exceptions, and signal breakdown.
- **Historical architecture slider:** select a commit/time range and compare dependency/cluster structure.
- **Refactor review packet:** proposed investigation, impacted units, evidence, tests, and rollback strategy.

## Cross-cutting epic: Analyzer quality and evaluation

**Goal:** Ensure Phase 2 is a research instrument rather than an attractive graph of plausible nonsense.

### Evaluation dataset

Curate an “architecture archaeology” benchmark from the corpus:

- 20 known namespace/module boundaries
- 20 known concept-to-code links
- 10 intentional bridges/adapters
- 10 historical moves/renames
- 10 examples of co-change that does not mean conceptual cohesion
- 10 intentionally dynamic Clojure patterns where static resolution is incomplete
- 10 known stale/orphan notes or implementations

### Required metrics

- Parser coverage and error rate by language.
- Semantic-resolution coverage and unknown-rate by language/tool.
- Precision/recall for reviewed concept-to-code links.
- Precision/recall for accepted boundary recommendations.
- Cluster stability across commits and indexing/model versions.
- False-positive rate for “misplaced module” suggestions.
- Search Recall@k / nDCG for code comprehension queries.
- Time-to-evidence for a user investigation.
- Human-review acceptance, rejection, and “insufficient evidence” rates.

## Delivery sequence

1. Epic 7: Polyglot Source Ledger
2. Epic 8: Syntax Forest and Structural Features
3. Epic 9: Clojure Semantic Intelligence
4. Epic 10: Program Relationship Graph
5. Epic 13: Minimal Code Archaeology Workbench
6. Epic 11: Concept-to-Code Grounding
7. Epic 12: Architectural Boundary Inference
8. Analyzer quality/evaluation hardening throughout

## Phase-two exit test

Phase 2 is complete when you can choose one Clojure subsystem—such as OpenPlanner ingestion/data layer or Graph-Weaver—and produce an evidence-backed architectural investigation that includes:

- its namespaces, symbols, dependencies, protocols, multimethods, tests, and relevant configuration;
- the Git timeline that shows how the subsystem formed;
- links from design notes and concepts to implementation units, with accepted versus candidate status;
- co-change, structural, semantic, and dependency signals;
- an explanation of which namespaces form a cohesive subsystem, which are bridges, and which may be historically misplaced;
- at least one human-reviewed boundary/refactor hypothesis with affected units, tests, evidence, and a reversible rollout plan;
- a clear accounting of unknowns caused by macro expansion, dynamic resolution, generated code, or incomplete analysis.
