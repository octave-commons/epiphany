---
slug: clio-event-ledger-ownership-adjudication-2026-08-14
uuid: c4192c28-72ab-42d1-bbea-7ca59f52a26e
kind: note
status: draft
description: "Operator adjudication that Clio owns the event-ledger domain; legacy event-ledger implementations are migration sources, not peer authorities."
labels: [eta-mu, clio, event-ledger, event-sourcing, ownership, migration, cross-repo, triage]
created: "2026-08-14"
sources:
  - "https://github.com/open-hax/eta-mu/commit/5dfa8e97099cc473584e451e3847fedcf7b0c7e6"
  - "https://github.com/open-hax/eta-mu/commit/0a009f4686fd81df0759b9946407fc3f9a7496c2"
  - "https://github.com/open-hax/openplanner/blob/b473034df697ea26d472101b0c0c4b0d3609d24f/packages/services/graphics-svg-pipeline/src/cljs/graphics_svg_pipeline/ledger.cljs"
  - "https://github.com/open-hax/eta-mu/blob/2afdb0208dd614bbcc87a6096db938e17b96426a/ROADMAP.md"
supersedes:
  - "docs/notes/clio-event-ledger-contract-comparison-2026-08-12.md#working-interpretation"
  - "docs/notes/eta-mu-clio-event-kernel-triage-2026-08-10.md#unresolved-questions"
informs:
  - "ROADMAP.md"
---

# Clio event-ledger ownership adjudication — 2026-08-14

## Decision source

The operator explicitly adjudicated the previously unresolved Clio ↔ standalone event-ledger ownership question:

> "no clio take it all. i dont think anything is even using the event ledger."

This is an explicit operator ownership decision. It is not inferred from merge status, implementation volume, or repository age.

## Accepted ownership

Clio is the accepted destination and owner for the event-ledger domain being developed across these repositories, including the semantic contract that had previously been split or ambiguously described across eta-mu, standalone `open-hax/event-ledger`, and OpenPlanner's embedded `packages/event-ledger` implementation.

The intended direction is:

```text
Clio
  ├─ event laws
  ├─ envelope / identity semantics
  ├─ schema and historical-validation provenance
  ├─ append admission
  ├─ causal DAG / canonicalization
  ├─ projection semantics
  └─ persistence/runtime adapters as required

legacy standalone or embedded event-ledger implementations
  └─ migration sources only -> retire after live consumers are moved
```

This decision does not require every legacy implementation detail to survive. A behavior is migrated only when a live consumer or an independently accepted requirement justifies it against Clio's laws.

## Observed source dependency requiring deployment/runtime verification

A repository-wide source search found at least one concrete source dependency on OpenPlanner's embedded event-ledger package:

`packages/services/graphics-svg-pipeline/src/cljs/graphics_svg_pipeline/ledger.cljs` requires `promethean.event-ledger.core`.

That source-level observation revises the tentative belief that no code
references the old implementation. It does not establish deployment or runtime
reachability, and it does **not** revise the ownership decision. The graphics
pipeline dependency requires deployment/runtime verification; if active, its
required behavior should be identified, represented through Clio or a
Clio-backed adapter, cut over, and then the legacy dependency can be retired.

The remaining event-ledger package files, roadmaps, epics, compatibility bridges, Mongo/REST surfaces, TTL policy, and other historical implementation artifacts are not evidence of ongoing authority by existence alone.

## Superseded interpretations

The earlier notes correctly established semantic overlap and implementation differences, but their disposition is now stale where they say the durable ownership relation is unresolved.

Preserve those notes as revision-scoped provenance. Read this adjudication as the later authority for ownership:

- Clio ownership/destination: **accepted by operator**.
- standalone `open-hax/event-ledger`: **legacy migration source**, not peer architectural authority.
- OpenPlanner `packages/event-ledger`: **legacy migration source**, not peer architectural authority.
- live legacy consumers: **migration obligations**, not arguments for split ownership.
- Clio laws becoming Epiphany/JVM standards: **still a separate promotion decision**; this adjudication does not automatically promote implementation-specific mechanisms into Epiphany policy.

## Roadmap implication

Eta-mu's roadmap wording that assigns standalone `event-ledger` ownership of the append-only envelope/storage contract is superseded by this operator decision and should be revised when the roadmap is next edited.

The roadmap should distinguish historical migration sources from current ownership and should point event-ledger semantic work toward Clio.

## Migration proof required before deletion

Before removing a legacy event-ledger implementation, verify at minimum:

1. direct imports and package dependencies;
2. deployment/runtime references;
3. persistence data or compatibility reads that must remain accessible;
4. watchers, TTL, REST, attribution, or sequencing behavior actually depended upon by a live caller;
5. a Clio or Clio-backed replacement for every retained behavior;
6. an executable cutover check demonstrating the caller no longer reaches the legacy implementation;
7. conversion of every retained legacy event into a Clio envelope, with schema,
   causal, ordering, and version constraints validated;
8. replay and projection equivalence over retained history, including Clio's
   current complete-history and sequence-1 requirements; or
9. an explicit approved exception identifying data that will not be retained
   and why conversion and replay proof do not apply.

Absence of a search hit is evidence only for the search performed, not proof of non-use.

## Disposition

- ownership ambiguity: **resolved**;
- accepted owner/destination: **Clio**;
- legacy implementations: **migration/retirement candidates**;
- OpenPlanner graphics pipeline: **observed source dependency requiring deployment/runtime verification**;
- Epiphany normalization of portable Clio laws: **promotion candidate only**, independently governed by Epiphany's process and explicit acceptance rules.
