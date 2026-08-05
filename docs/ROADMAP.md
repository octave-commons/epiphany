---
slug: epiphany-roadmap
kind: roadmap
status: provisional
description: "Navigation hub for Epiphany's accepted architecture, active delivery map, and longer-view phase structure."
labels: [architecture, roadmap, jvm, cross-repository]
---

# Epiphany roadmap hub

This document is a navigation and synthesis hub. It does not replace the
sources it links, promote proposals into decisions, or imply acceptance from
merge status or implementation alone.

## Authority map

Use the following sources according to their declared scope:

1. [`PROCESS.md`](../PROCESS.md) — constitutional process guidance, evidence
   tiers, authority, promotion, and acceptance.
2. [`docs/adrs/`](adrs/) — accepted architectural decisions within their stated
   scope.
3. [`README.md`](../README.md) — current product purpose, Phase 1 boundary,
   architecture overview, and stated non-goals.
4. [`docs/kanban/AGENTS.md`](kanban/AGENTS.md) — active board contract and the
   Phase 0–4 theme map.
5. [`docs/kanban/BOARD-BREAKDOWN.md`](kanban/BOARD-BREAKDOWN.md) — current
   Phase 0–1 delivery gates, dependencies, and critical path.
6. [`docs/designs/`](designs/) — design proposals and implementation-oriented
   reasoning; designs are not decisions unless accepted through the applicable
   authority.
7. [`docs/kanban/epics/`](kanban/epics/) — phase-level outcome records and
   longer-horizon work inventory.

When these sources disagree, preserve the contradiction and apply the authority
order in `PROCESS.md`; do not silently reconcile them in this hub.

## Current center

**Observed repository scope:** Epiphany is a Clojure-first, JVM-based,
local-first Git knowledge-archaeology system. Phase 1 is the active product
boundary: trustworthy repository registration, Git and Markdown observation,
evidence extraction, retrieval, lineage, review, and a workbench.

**Current delivery source:** `docs/kanban/BOARD-BREAKDOWN.md` owns the detailed
Phase 0–1 gate sequence and critical path. This document intentionally does not
copy card state or totals because those facts become stale quickly.

## Long-view phase topology

The current board contract records this directional phase map:

| Phase | Theme | Current disposition |
| --- | --- | --- |
| 0 | Executable local baseline | active |
| 1 | Markdown corpus archaeology | active |
| 2 | Code archaeology | icebox |
| 3 | Governed external research and bounded research agents | icebox |
| 4 | Simulation laboratory | icebox |

The phase map is directional planning evidence, not a delivery promise or an
accepted architecture for every future component. Consult the owning epic,
design, ADR, and current code before making a consequential claim.

## Relationship to eta-mu

The following is a **working architectural interpretation**, not an accepted ADR:

- `open-hax/eta-mu` is the executable coordination center for the ClojureScript
  runtime and related CLJS work.
- Epiphany is the JVM-specific center and a long-view normalization environment
  for evidence-preserving architecture, research, and promotion decisions.
- Runtime shapes validated in eta-mu may become promotion candidates for
  Epiphany; they do not become Epiphany-owned merely through repetition, merge,
  or implementation.
- JVM discoveries in Epiphany may create pressure to revise cross-runtime
  contracts in eta-mu. The flow is bidirectional rather than a fixed upstream /
  downstream hierarchy.

A durable division of authority between the centers requires explicit operator
acceptance and, where consequential, an ADR or equivalent accepted decision.

## Cross-repository navigation questions

When reviewing Knoxx, services, Muse, OpenPlanner, eta-mu, or another connected
repository, record:

1. Which center currently owns the relevant runtime or architectural concern?
2. Which document is the authoritative source for that claim?
3. Is the evidence observed implementation, interpretation, proposal, accepted
   decision, stale evidence, contradiction, or unavailable coverage?
4. Does the work expose a reusable shape that should be considered for explicit
   promotion, normalization, or rejection?
5. What acceptance authority would be required before ownership changes?

## Maintenance rule

Keep this file stable and link-oriented. Update it when the source topology,
phase topology, or explicitly accepted center relationship changes. Do not turn
it into a duplicate board, design catalog, or implementation status report.
