---
id: 01900d7c-7f3a-7e8b-9c4d-000000000015
title: "Phase 2 Cross-Cutting: Analyzer Quality and Evaluation"
status: incoming
type: cross-cutting
priority: high
phase: 2
design: docs/notes/design/phase-2-code-comprehension.md
size: 8
labels: [evaluation, quality, benchmarks, metrics]
---

# Phase 2 Cross-Cutting: Analyzer Quality and Evaluation

Ensure Phase 2 is a research instrument rather than an attractive graph of plausible nonsense.

## Evaluation dataset

Curate a manually curated “architecture archaeology” benchmark from the corpus:

- 20 known namespace/module boundaries
- 20 known concept-to-code links
- 10 intentional bridges/adapters
- 10 historical moves/renames
- 10 examples of co-change that does not mean conceptual cohesion
- 10 intentionally dynamic Clojure patterns where static resolution is incomplete
- 10 known stale/orphan notes or implementations

## Required metrics

- Parser coverage and error rate by language.
- Semantic-resolution coverage and unknown-rate by language/tool.
- Precision/recall for reviewed concept-to-code links.
- Precision/recall for accepted boundary recommendations.
- Cluster stability across commits and indexing/model versions.
- False-positive rate for “misplaced module” suggestions.
- Search Recall@k / nDCG for code comprehension queries.
- Time-to-evidence for a user investigation.
- Human-review acceptance, rejection, and “insufficient evidence” rates.

## Next step

Create the evaluation dataset and define the metrics dashboard before analyzer development proceeds.
