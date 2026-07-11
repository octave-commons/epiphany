---
id: 01900d7c-7f3a-7e8b-9c4d-000000000019
title: "Epic 17: Research Knowledge Graph and Evidence Ranking"
status: icebox
type: epic
priority: medium
phase: 3
design: docs/notes/design/phase-3-research-operations.md
size: 8
labels: [graph, research, evidence, ranking]
---

# Epic 17: Research Knowledge Graph and Evidence Ranking

Link external research components to the Phase 1/2 local corpus while preserving differences in authority, time, and evidence type.

## User outcome

“I can trace a local design idea to prior art, implementations, datasets, and criticism—and distinguish a verified link from a semantic suggestion.”

## Scope

Build graph relationships such as:

- `:paper/studies` → task/problem
- `:paper/proposes` → method
- `:paper/evaluates-on` → dataset
- `:paper/measures-with` → metric
- `:paper/reports` → result
- `:paper/acknowledges` → limitation
- `:repository/implements` → method
- `:dataset/supports` → task
- `:model/trained-on` → dataset
- `:artifact/cites` → artifact
- `:local-concept/has-prior-art` → external component
- `:local-code/implements-similar-method` → external method
- `:external-claim/conflicts-with` → local/external claim
- `:research-gap/suggested-by` → evidence set

Use hybrid retrieval plus graph traversal to discover candidates. Require evidence and review state for high-value cross-domain links.

## Acceptance criteria

- A local concept can return relevant papers, repos, models, datasets, and explicit evidence spans.
- A paper/method can return local notes, code, and experiments that resemble or build on it.
- Search results disclose source class, trust tier, publication/revision date, extraction confidence, and evidence status.
- Graph traversals are bounded by relationship types, provenance filters, and time.
- The user can distinguish explicit citation/link, lexical/semantic resemblance, shared method component, human-accepted lineage, and LLM-proposed hypothesis.
- The system produces an evidence packet for any “prior art” claim rather than a bare similarity score.

## Next step

Design the research relationship vocabulary and the evidence-ranking algorithm.
