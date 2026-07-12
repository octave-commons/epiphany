---
id: "01900d7c-7f3a-7e8b-9c4d-000000001303"
title: "ENG-003C: Serve KNN vector search over section embeddings"
status: "done"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000003"
design: "docs/kanban/epics/epic-03-retrieval-substrate.md"
points: 3
labels: ["phase-1", "search", "vector", "knn"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001302"]
---

# ENG-003C: Serve KNN vector search over section embeddings

Nearest-neighbor retrieval over the embedding projection (Lucene KNN vectors preferred — one index technology).

## Acceptance criteria

- A query embedding returns ranked sections with similarity scores and the model version used.
- Results never mix vectors from different model versions.
- The vector index is rebuildable from stored embeddings.
