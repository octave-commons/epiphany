# 
## Clojure House Rules (eta-mu-sol constitution)

### Architecture Paradigm: Categories vs. Contracts
When modeling domains, you must strictly differentiate between the grammar of motion and the enforcement of that motion.
- Categories: Describe the space of lawful possible transformations. They dictate "what kind of move this is" and define the state space, transition vocabulary, and general laws of composition for the runtime or a subsystem.
- Contracts: Decide whether a particular runtime entity, event, or transition is admissible under current obligations. They dictate "whether you are allowed to count it as a valid move right now" by defining guards, admissibility checks, evidence requirements, delivery expectations, and side-effect constraints.


### Zero Warnings
`clj-kondo`, type checks, and tests must all pass with zero warnings. Warnings
are failed contracts, not noise.

### Namespace Architecture
| Layer         | Pattern            | Rule                             |
|---------------|--------------------|----------------------------------|
| `domain.*`    | Business logic     | No I/O. Pure functions only.     |
| `infra.*`     | Transport/DB/Queue | No domain policy.                |
| `shape.*`     | Data morphisms     | Pure, domain-agnostic.           |
| `law.*`       | Contracts/Malli    | No I/O. Validators only.         |

### Clojure Construction Order
Regardless of the kanban process, ClojureScript is **built in a fixed order**, because the
order *is* the dependency DAG: each layer compiles against already-defined lower layers.

```
Discovery → ( Describe → specify → define → shape → extern → domain → infra )
```

- **Discovery** — survey what already exists before constructing. Opens every cycle and
  recurs *inside* every step (see the anomaly rule below). Output: a named inventory of the
  shapes in play, including the ones that are already there.
- **Describe** — state the projection's intent in prose (a note, a kanban card body).
- **specify** — pin acceptance criteria / exit signals (the kanban "Clarify & Scope" pass).
- **define** — author the `law.*` that **describes** the shape (μ). A law is a description of
  what a valid instance is, not the data and not the transform; that is why it has no
  dependencies and comes first. (`define` and `shape` are complementary roles, not one
  artifact moved between layers — the law describes; the shape is the morphism it describes.)
- **shape** — `shape.*` pure morphisms that produce/consume the described shapes (parse,
  enrich, (de)serialize). Depends only on `law`.
- **extern** — `extern.*` raw JS / Node / browser / SDK boundaries, decoding foreign data
  into defined shapes at the edge. Nothing above `extern` touches a raw host object.
- **domain** — `domain.*` pure decisions over shaped data. Depends on `shape` + `law`.
- **infra** — `infra.*` effect orchestration composing `extern` adapters and `domain`
  decisions, returning CLJS data. Depends on everything below it.

## Invariants
- `receipts.edn` is append only
- The a `receipt` edn record must be appended to the `receipts.edn` ledger at the end of every significant turn, and prior to significant decisions made over the course of a turn
- Read recent receipts from the `receipt-river` SKILL `receipts.edn` event ledger
  - at the start of every turn,
  - before appending new receipts
- Read recent session mycology lessons
- Ground work in specs, always.
- Never cut corners
  - no fallback
  - no stop gaps
  - fail loudly and gracefully
- Correct is better than easy
- Keep documentation up to date with the latest operational realities
  - PROCESS.md
  - AGENTS.md
  - README.md
  - CLAUDE.md
  - kanban/
  - docs/research
  - docs/designs
  - docs/adrs
  - docs/notes
- use the claude memory tool

#### The anomaly rule (a surprise at every step)
Every step is also a discovery step. You will keep finding shapes you didn't realize were
already there — in this package or a sibling.

> If the discovery does **not** invalidate the shape of your targeted projections, you
> **describe the anomaly** (location + whether it's already-there reuse or a contradiction)
> and **keep going**. If it **does** invalidate a target, stop and re-`describe` that projection.

Anomalies are logged, not silently absorbed. A worked example of a Discovery pass and its
anomaly log: `docs/rheos-chat-ui-shape-discovery.md`.

### Modern ClojureScript
Always use `^:async` metadata (ClojureScript ≥ 1.12.145). Never use
`core.async` channels or Promise chains in new code.

```clojure
(defn ^:async fetch-data [url]
  (await (js/fetch url)))

(deftest ^:async fetch-test
  (is (some? (await (fetch-data "https://example.com")))))
```

### Idioms
- `when-let` over nested `let` + `if`
- `->` / `->>` over nested `let` forms
- No `utils` namespaces
- No broad `:refer :all`
- Custom macros registered in `.clj-kondo/config.edn` on day one

## Kanban Agile Workflow

All feature work, and significant chores require a task file to have worked through the
@PROCESS.md to the and be in the `todo` status to be eligible for
implementation work and the transition to the `in_progress` status

If you are asked to work on a task that is not marked `todo` *YOU MUST* read @PROCESS.md and focus on moving the card through the process,
one status transition at a time until you are either blocked, or the you make the transition from `in_progress` to `review` after
all tests pass, and lint passes with 0 errors, or warnings.

We pick up after each other, resolve warnings even if it's not your fault.
Do you like having to swim through trash to do your job?

```md
---
uuid: "shared-kondo-config-wire-axxium"
title: "Wire shared clj-kondo config into axxium"
status: "done"
priority: "P1"
labels: ["tasks", "lint", "clj-kondo", "infra", "1sp"]
created_at: "2026-06-15T00:00:00Z"
source: "kanban/epics/shared-kondo-config-install.md"
points: 1
category: "tasks"
---
```


- **Work from a card.** Never work off-board. Anchor every implementation slice on a kanban task and record the scoped plan on the card before moving to implementation.
- **Move cards with the Rheos CLI.** Run commands from the **repo root** so the board resolves correctly:
  - `eta-mu kanban list` — current board.
  - `eta-mu kanban count` — column counts.
  - `eta-mu kanban comment <uuid> "note"` — append provenance to a card.
  - `eta-mu kanban frontmatter <uuid> status <new-status>` — lawful status change.
  - `node packages/Rheos/dist/cli.cjs status-update <uuid> --to <status>` — FSM-enforced move (also runs build-gate when required).
- **No direct frontmatter edits.** The file watcher treats hand-edited frontmatter as drift and stamps a `drift: true` indicator on the card. Use the CLI so the ledger records a `write-id` and the provenance is auditable.
- **Walk lawful hops.** There are no shortcut edges. To move a card multiple columns forward, step through each lawful transition in order. The direct `in_progress → review` edge exists only when the build-gate passes.
- **Regenerate snapshots when needed.** The web UI and `kanban/.kanban/board.json` are generated snapshots; the source of truth is the task files plus the ledger in `kanban/.events/ledger.edn`. If a snapshot is stale, regenerate it from the CLI or the web UI.
- All other guidelines live in the living `PROCESS.md` document, 
