---
id: 01900d7c-7f3a-7e8b-9c4d-000000000018
title: "Epic 16: Research Component Extraction"
status: icebox
type: epic
priority: medium
phase: 3
design: docs/notes/design/phase-3-research-operations.md
size: 8
labels: [extraction, research, papers, components]
---

# Epic 16: Research Component Extraction

Convert papers, repositories, model cards, dataset cards, and documentation into structured, evidence-linked research components.

## User outcome

“I can ask: what did this work claim, assume, evaluate, use, and release—and inspect the exact text, code, or metadata supporting each answer.”

## Extract and link

- Research question/problem
- Claimed contribution
- Hypothesis and assumptions
- Methods/models/algorithms
- Datasets and data splits
- Metrics, baselines, controls, and ablations
- Reported results and uncertainty
- Hardware/resource claims
- Threats to validity and stated limitations
- Reproducibility assets: code, config, seeds, environment, license
- Citations, implementation references, model/dataset lineage
- Repository signals: maintenance activity, release history, open issues, test/config evidence
- Dataset signals: card quality, license, task/domain, schema, split, sample statistics, limitations

## Acceptance criteria

- Every extracted component links to at least one source artifact and evidence span.
- The system distinguishes author-reported claims from independently observed repository/dataset metadata.
- Extraction records model/prompt/extractor version and confidence.
- Low-confidence extraction becomes a review candidate, not an accepted fact.
- A user can compare components across papers/repositories.
- The system retains the original artifact even when a later extraction model revises the interpretation.
- Extraction quality is evaluated against a curated gold set.

## Domain rule

Do not flatten a paper into a single “summary.” Its claims, methods, datasets, results, limitations, and evidence must be separately addressable.

## Next step

Design the research component schema and extraction pipeline.
