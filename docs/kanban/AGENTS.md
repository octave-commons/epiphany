# Epiphany Kanban Board — Agent Contract

Markdown-backed board managed by `eta-mu kanban`. Every card is one file with YAML frontmatter. This file is the contract for any agent (human or automated) working the board. ADRs in `docs/adrs/` are architecturally authoritative; cards never override them.

## Layout

```
docs/kanban/
  stories/          implementable work (US-* product stories, ENG-* engineering slices)
  epics/            phase-level outcome cards (not directly implementable)
  chores/           maintenance work
  board.json        GENERATED snapshot — never edit by hand
  BOARD-BREAKDOWN.md  delivery map (phases, gates, critical path)
  AGENTS.md         this contract
```

## CLI

Run from the repo root (the config `openhax.kanban.json` lives there) or pass `--tasks-dir` explicitly:

```bash
eta-mu kanban count  --tasks-dir docs/kanban
eta-mu kanban list   --tasks-dir docs/kanban
eta-mu kanban find <slug> --tasks-dir docs/kanban
eta-mu kanban frontmatter <slug> status in_progress --tasks-dir docs/kanban
eta-mu kanban comment <slug> "Progress note" --tasks-dir docs/kanban
eta-mu kanban board snapshot --tasks-dir docs/kanban --out docs/kanban/board.json
```

Card slugs are derived from titles (e.g. `eng-001a-persist-idempotent-repository-location-observations`).

## Frontmatter contract

```yml
id: "01900d7c-...-000000001101"   # stable identity; children suffix the parent id (-a, -1)
title: "ENG-001A: ..."            # PREFIX-NNN: imperative outcome
status: ready                     # see FSM below
type: "story"                     # story | epic | chore | planning-record
priority: "P0"                    # P0 (now) … P3 (someday)
phase: 1                          # see phase map below
epic: "<epic id>"                 # owning epic, for stories
parent: "<card id>"               # set on cards produced by decomposition
design: "docs/adrs/adr-001-...md" # the authoritative design input
points: 3                         # honest estimate; REQUIRED before leaving incoming
labels: [phase-1, ...]
category: "stories"               # stories | epics | chores
dependency: ["<card id>", ...]    # ids of cards that must be done first
```

## Status FSM

`icebox → incoming → accepted → breakdown → ready → todo → in_progress → review → document → done` (plus `rejected` from anywhere).

- **icebox** — real but not now. All phase ≥ 2 epics live here.
- **incoming** — captured, untriaged. Points may be a guess.
- **accepted** — triaged: honest points, dependencies listed, design link present.
- **breakdown** — two meanings, both terminal for implementation:
  - a card actively being split, or
  - a **decomposed parent** (label `decomposed`, body has a "Decomposed into" section). Never implement these directly; work their children.
- **ready** — implementable as specified. Gate: **points ≤ 5**, acceptance criteria present, every `dependency` either `done` or itself `ready`-or-later in the same delivery gate.
- **in_progress / review / document / done** — execution states. WIP limit: 2 in progress, 1 in review.

## Hard rules

1. **No card over 5 points may be `ready`.** Split it; give children `parent:` and the parent a "Decomposed into" section, label `decomposed`, status `breakdown`.
2. **Points are honest estimates, not placeholders.** A board full of `points: 1` is unestimated, not cheap. Scale: 1 ≈ an hour or two, 2–3 ≈ half a day to a day, 5 ≈ a couple of days, 8/13 ≈ too big to be ready.
3. **Estimate the card, not the ceremony.** Don't double-count work owned by another card (schemas, ports, scaffolding); reference the owning card in `dependency` instead.
4. **Schemas/contracts precede adapters.** Contract cards depend only on scaffolding; adapter cards depend on contract cards.
5. **Append, don't rewrite.** Record triage/progress/decisions as card comments (`eta-mu kanban comment`) or "revised" sections; never silently rewrite history. Same rule the product itself follows.
6. **`board.json` is generated.** After editing cards, regenerate the snapshot; never hand-edit it.

## How an agent picks work

1. `eta-mu kanban list --tasks-dir docs/kanban`, filter `status: ready`.
2. Discard cards whose `dependency` ids are not `done`.
3. Order by priority (P0 first), then phase, then smallest points.
4. Set the card `in_progress`, do the work, comment outcomes, move through `review`/`document` to `done`.
5. If a card turns out bigger than its points, stop, comment why, set it back to `breakdown`, and split it.

## Phase map

| Phase | Theme | Epics | State |
|-------|-------|-------|-------|
| 0 | Executable local baseline | US-000 family | active |
| 1 | Markdown corpus archaeology (ledger → extraction → retrieval → lineage → review → workbench) | 1–6 | active |
| 2 | Code archaeology (polyglot ledger, syntax forest, semantics, graphs, boundaries) | 7–13 | icebox |
| 3 | Governed external research + bounded research agents | 14–21 | icebox |
| 4 | Simulation laboratory | 22–30 | icebox |

Phase N+1 epics leave the icebox only when the phase-N workbench outcome is demonstrable, not before. The auto-research agents of phase 3 consume this same contract — which is why triage hygiene (honest points, real dependencies, append-only decisions) matters more than ceremony.

## Current delivery order

All phase 0/1 work is broken down: 33 cards `ready`, 7 cards `done`, across nine gates (baseline → registration → Git facts → recovery → extraction → retrieval → lineage → review → workbench, plus ops). The full gate tables and critical path live in `BOARD-BREAKDOWN.md`. First moves: US-000A, then ENG-001C / ENG-001B1 / ENG-002A in parallel.
