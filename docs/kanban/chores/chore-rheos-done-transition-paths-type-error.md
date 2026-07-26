---
labels: ["tooling", "kanban", "rheos", "bug", "phase-1"]
phase: "1"
type: "chore"
points: "2"
verification: ["unit-test"]
risk: "low"
title: "CHORE: rheos kanban_update_status document->done fails with 'paths[0] must be of type string, got object'"
priority: "P1"
status: "incoming"
id: "802cea43-4691-4e44-a9ca-5aefd16c78b9"
uuid: "802cea43-4691-4e44-a9ca-5aefd16c78b9"
created_at: "2026-07-25T00:00:00.000Z"
---

# CHORE: rheos kanban_update_status document->done fails with "paths[0] must be of type string, got object"

## Observed

Three-for-three reproductions on 2026-07-24, every time a card passes
`bin/kanban-done-gate` (exit 0) and the agent then calls the rheos MCP
`kanban_update_status` (project epiphany) document → done:

```
The "paths[0]" property must be of type string, got object
```

Same failure via the `eta-mu kanban frontmatter <slug> status done` CLI
(which additionally fails to resolve tasks by slug at all — "unknown
task" — while the MCP tools resolve them fine by uuid/slug).

Transitions that succeed through the same code path: ready → todo,
todo → in_progress, in_progress → review, review → document. Only
document → done fails, which points at the shared done-gate hook
(muse `plugins.kanban-gate`, reads `.ημ/kanban-done-gate.edn`) rather
than the FSM itself.

## Impact

Every done transition needs a manual frontmatter edit + audit comment
workaround (done 3x on 2026-07-24: ENG-003G, ENG-017E, ENG-017G2).
The hook's check itself works when invoked via `bin/kanban-done-gate`.

## Suspected area

The gate hook's invocation inside the status-transition path — likely
how it passes the task's file paths (a collection of strings vs
objects) to the gate check. Reproduce with any card at `document`:
`kanban_update_status(uuid, "done")`.
