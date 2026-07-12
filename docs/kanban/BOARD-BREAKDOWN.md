# Board Breakdown: Epiphany Phase 0–1 Delivery Map

Triaged and fully broken down 2026-07-11. Every implementable phase-1 card is `ready`, ≤ 5 points, with explicit dependencies. Decomposed parents (US-000–021, ENG-001B) sit in `breakdown` — never implement them directly. Phase ≥ 2 epics are iceboxed.

## Gate 0 — Executable baseline (Phase 0)

| Card | Title | Pts | Depends on |
|------|-------|-----|------------|
| US-000A | Scaffold, entry point, green tests | 4 | — |
| US-000B | Profiles, ports, in-memory adapters | 5 | 000A |
| US-000C | Service manifest + readiness diagnostics | 4 | 000B |

## Gate 1 — Registration is trustworthy (Epic 1)

| Card | Title | Pts | Depends on |
|------|-------|-----|------------|
| ENG-001C | Registration observation schemas | 4 | 000A |
| ENG-001B1 | Git-local identity (normal/bare/worktree) | 4 | 000A |
| ENG-001A | Mongo location-observation adapter | 5 | 001C, 000B, 000C |
| ENG-001B2 | Registration service + `ep register` | 5 | 001B1, 001A, 001C |

## Gate 2 — Git facts are observable (Epic 1)

| Card | Title | Pts | Depends on |
|------|-------|-----|------------|
| ENG-001D | Bounded reachable commit graph | 3 | 001C |
| ENG-001E | Markdown tree-entry selection policy | 2 | 001D |
| ENG-001F | Revision-at-path observations | 2 | 001E |

## Gate 3 — Runs recover (Epic 1)

| Card | Title | Pts | Depends on |
|------|-------|-----|------------|
| ENG-001G | Ingestion runs + projection checkpoints | 3 | 001D, 001F |
| ENG-001H | History-replacement evidence | 2 | 001D, 001G |
| ENG-001I | Git-diff file-lineage candidates | 2 | 001F |
| ENG-001J | Ledger outcomes + recovery evidence | 3 | 001G, 001H |

## Gate 4 — Evidence extraction (Epic 2)

| Card | Title | Pts | Depends on |
|------|-------|-----|------------|
| ENG-002A | Markdown → typed tree with source spans | 4 | 000A |
| ENG-002B | Versioned section-extraction records | 4 | 002A, 001F, 001C, 000C |
| ENG-002C | Checkpointed extraction projection | 3 | 002B, 001G |
| ENG-002D | Deterministic continuity features | 5 | 002B |
| ENG-002E | Path-repurpose boundary proposals | 3 | 002D |

## Gate 5 — Retrieval (Epic 3)

| Card | Title | Pts | Depends on |
|------|-------|-----|------------|
| ENG-003A | Lucene lexical section index | 5 | 002B |
| ENG-003B | Ollama embedding projection | 4 | 002B, 001G |
| ENG-003C | KNN vector search | 3 | 003B |
| ENG-003D | Hybrid search query service | 4 | 003A, 003C |
| ENG-003E | `ep search` CLI | 2 | 003D |
| ENG-003F | Retrieval benchmark harness | 3 | 003D |

## Gate 6 — Lineage (Epic 4)

| Card | Title | Pts | Depends on |
|------|-------|-----|------------|
| ENG-004A | Evidence reader + `ep show` | 4 | 002B |
| ENG-004B | Compare expressions + `ep diff` | 3 | 004A |
| ENG-004C | Deterministic candidate lineage links | 5 | 003D, 002D |
| ENG-004D | Trace chronology + `ep trace` | 4 | 004C |

## Gate 7 — Review (Epic 5)

| Card | Title | Pts | Depends on |
|------|-------|-----|------------|
| ENG-005A | Append-only review decision events | 3 | 004C |
| ENG-005B | Review inbox + `ep inbox` | 3 | 005A |
| ENG-005C | Redundancy/contradiction candidates | 4 | 003D |
| ENG-005D | Concepts + research questions | 3 | 005A |
| ENG-005E | Research-gap surfacing | 3 | 004D, 005A |
| ENG-005F | Evidence packet export + `ep export` | 3 | 004D, 005D |

## Gate 8 — Workbench (Epic 6)

| Card | Title | Pts | Depends on |
|------|-------|-----|------------|
| ENG-006A | HTTP API adapter `/api/v1` | 4 | 003D, 004A |
| ENG-006B | Workbench: search + evidence drawer | 5 | 006A, 004B |
| ENG-006C | Workbench: timeline + inbox + health | 5 | 006B, 004D, 005B |

## Ops (US-020/021 slices)

| Card | Title | Pts | Depends on |
|------|-------|-----|------------|
| ENG-020A | Cross-stage `ep status` | 2 | 001G, 002C, 003A, 003B |
| ENG-021A | Backup/restore + rebuild drill | 3 | 001G, 003A, 003C |

## Critical path

```
000A → 000B → 000C
  └→ 001C ─┬→ 001A → 001B2 → (register works)
  └→ 001B1 ┘
001C → 001D → 001E → 001F → 001G (ledger ingests, resumes)
002A + 001F → 002B → 002C (sections exist)
002B → 003A + 003B → 003C → 003D → 003E (search works)
003D + 002D → 004C → 004D (lineage traces)
004C → 005A → 005B (review loop closes)
003D + 004A → 006A → 006B → 006C (workbench ships)
```

Parallelizable early: after 000A lands, ENG-001C, ENG-001B1, and ENG-002A have no other prerequisites.

## Totals

Phase 0+1 ready lane: **33 cards, ~120 points**, every card ≤ 5. Done: 7 cards, 23 pts. WIP limits: 2 in progress, 1 in review. Definition of Done lives in `PROCESS.md` and the root `AGENTS.md` quality gate.
