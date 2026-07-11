---
id: "01900d7c-7f3a-7e8b-9c4d-000000001013"
title: "US-013: Generate candidate lineage links"
status: "incoming"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000004"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 1
labels: [lineage, graph, inference, candidates, provenance]
category: "stories"
---

# US-013: Generate candidate lineage links

## Acceptance Criteria

- The system can generate candidates such as:
  - `:continues`;
  - `:refines`;
  - `:references`;
  - `:possibly-derived-from`;
  - `:near-duplicate`;
  - `:possibly-supersedes`;
  - `:possible-contradiction`.
- Every candidate identifies source and target section/revision expressions.
- Every candidate includes evidence spans, retrieval/similarity features, timestamps, generator version, and confidence/score.
- Candidate generation never mutates document, section, or concept identity.
- The initial candidate generator may use deterministic retrieval and continuity signals; LLM-generated relations are deferred unless separately designed and evaluated.
- Duplicate candidates for the same relation pair/configuration are idempotently merged or versioned rather than endlessly re-created.

## Notes

**As a corpus owner,** I want the system to propose plausible historical relationships between note sections so that I can rapidly discover what an idea became.
