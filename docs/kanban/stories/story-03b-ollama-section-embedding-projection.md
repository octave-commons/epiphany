---
id: "01900d7c-7f3a-7e8b-9c4d-000000001302"
title: "ENG-003B: Embed sections via Ollama as a versioned projection"
status: "done"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000003"
design: "docs/kanban/epics/epic-03-retrieval-substrate.md"
points: 4
labels: ["phase-1", "embeddings", "ollama", "projection"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001202", "01900d7c-7f3a-7e8b-9c4d-000000001107"]
---

# ENG-003B: Embed sections via Ollama as a versioned projection

Batch-embed sections through the local Ollama HTTP API; checkpointed, resumable, model-pinned.

## Acceptance criteria

- Every embedding records model name, model version/digest, and embedding config.
- The projection resumes from its checkpoint and reports counts and failures.
- Changing the model creates a new projection version; it never silently overwrites vectors.
- Ollama unavailability is an explicit UNAVAILABLE diagnostic, not a hang.
