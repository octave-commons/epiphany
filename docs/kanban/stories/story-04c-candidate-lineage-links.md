---
id: "01900d7c-7f3a-7e8b-9c4d-000000001403"
title: "ENG-004C: Generate deterministic candidate lineage links"
status: ready
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000004"
design: "docs/kanban/epics/epic-04-temporal-idea-lineage.md"
points: 5
labels: [phase-1, lineage, candidates, inference]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001304", "01900d7c-7f3a-7e8b-9c4d-000000001204"]
---
# ENG-004C: Generate deterministic candidate lineage links

Propose typed relations (:continues :refines :references :possibly-derived-from :near-duplicate :possibly-supersedes :possible-contradiction) from retrieval + continuity signals. LLM generation is a separate future card.

## Acceptance criteria

- Every candidate names source and target expressions, evidence spans, contributing features, generator version, and confidence.
- Candidates for the same pair/configuration merge idempotently or version — never endlessly re-create.
- Candidate generation mutates no document, section, or concept identity.
- Forks are representable: one source to many targets and many to one.
