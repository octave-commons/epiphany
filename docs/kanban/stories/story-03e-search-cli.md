---
id: "01900d7c-7f3a-7e8b-9c4d-000000001305"
title: "ENG-003E: Expose search on the CLI (`ep search`)"
status: done
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000003"
design: "docs/kanban/epics/epic-03-retrieval-substrate.md"
points: 2
labels: [phase-1, cli, search]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001304"]
---
# ENG-003E: Expose search on the CLI (`ep search`)

Wire the hybrid query service to the canonical executable.

## Acceptance criteria

- `ep search <query>` with flags for mode, filters, and limit; results to stdout, diagnostics to stderr.
- `--format edn|json` emits machine output with full provenance fields.
- Verbose output names index/projection versions consulted.
