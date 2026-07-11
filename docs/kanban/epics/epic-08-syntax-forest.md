---
id: 01900d7c-7f3a-7e8b-9c4d-000000000009
title: "Epic 8: Syntax Forest and Structural Features"
status: icebox
type: epic
priority: high
phase: 2
design: docs/notes/design/phase-2-code-comprehension.md
size: 8
labels: [parsing, ast, tree-sitter, structural-features]
---

# Epic 8: Syntax Forest and Structural Features

Parse source files into lossless-ish concrete syntax representations and derive language-neutral structural features for retrieval, comparison, and visualization.

## User outcome

“I can inspect a function or config block as a tree, search for recurring structural patterns, and compare implementation shapes across the corpus without pretending different languages are identical.”

## Scope

- Use Tree-sitter as the default CST/AST parser substrate where grammars are suitable.
- Store raw tree, normalized node stream, node types, parent/child/sibling relationships, source spans, comments/docstrings, and parser errors.
- Produce versioned structural representations: preorder node-type sequences, rooted subtree hashes, parent-child edge n-grams, selected AST paths, declaration shape, and control-flow/data-literal summaries.
- Define an AST n-gram vocabulary per language family rather than one universal bag of node names.
- Index structural features at declaration/function and module level.
- Build a visual tree inspector with source synchronization.

## Acceptance criteria

- Every extracted node can resolve back to an exact source span.
- A malformed file still yields partial parse structure and useful diagnostics where the grammar allows it.
- Structural fingerprint generation is deterministic and versioned.
- A user can query for recurring patterns within a language, e.g. “all Clojure `defmethod` forms with similar dispatch patterns” or “all YAML deployment objects with similar key structure.”
- Structural similarity searches show the matching subtrees, not merely a score.
- A language-specific adapter can add richer features without changing the common source-unit contract.

## Non-goal

Do not assume AST similarity implies domain similarity. Structural signals are one input to later multi-view clustering.

## Next step

Set up Tree-sitter grammars for the supported languages and define the normalized node stream schema.
