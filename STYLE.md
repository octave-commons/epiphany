# Clojure House Rules (eta-mu-sol constitution)

## Architecture Paradigm: Categories vs. Contracts

When modeling domains, you must strictly differentiate between the grammar of motion and the enforcement of that motion.
- Categories: Describe the space of lawful possible transformations. They dictate "what kind of move this is" and define the state space, transition vocabulary, and general laws of composition for the runtime or a subsystem.
- Contracts: Decide whether a particular runtime entity, event, or transition is admissible under current obligations. They dictate "whether you are allowed to count it as a valid move right now" by defining guards, admissibility checks, evidence requirements, delivery expectations, and side-effect constraints.


## Zero Warnings
`clj-kondo`, type checks, and tests must all pass with zero warnings. Warnings
are failed contracts, not noise.

## Namespace Architecture
| Layer         | Pattern            | Rule                             |
|---------------|--------------------|----------------------------------|
| `domain.*`    | Business logic     | No I/O. Pure functions only.     |
| `infra.*`     | Transport/DB/Queue | No domain policy.                |
| `shape.*`     | Data morphisms     | Pure, domain-agnostic.           |
| `law.*`       | Contracts/Malli    | No I/O. Validators only.         |

## Clojure Construction Order
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

#### The anomaly rule (a surprise at every step)
Every step is also a discovery step. You will keep finding shapes you didn't realize were
already there — in this package or a sibling.

> If the discovery does **not** invalidate the shape of your targeted projections, you
> **describe the anomaly** (location + whether it's already-there reuse or a contradiction)
> and **keep going**. If it **does** invalidate a target, stop and re-`describe` that projection.

Anomalies are logged, not silently absorbed. A worked example of a Discovery pass and its
anomaly log: `docs/rheos-chat-ui-shape-discovery.md`.
