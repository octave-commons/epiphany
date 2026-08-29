---
slug: clio-event-ledger-ownership-adjudication-2026-08-14
uuid: c4192c28-72ab-42d1-bbea-7ca59f52a26e
kind: note
status: draft
description: "Revision-scoped record of a reported operator instruction favoring Clio ownership; source recovery and authoritative acceptance remain open."
labels: [eta-mu, clio, event-ledger, event-sourcing, ownership, migration, cross-repo, triage]
created: "2026-08-14"
sources:
  - "https://github.com/open-hax/eta-mu/commit/5dfa8e97099cc473584e451e3847fedcf7b0c7e6"
  - "https://github.com/open-hax/eta-mu/commit/0a009f4686fd81df0759b9946407fc3f9a7496c2"
  - "https://github.com/open-hax/openplanner/blob/b473034df697ea26d472101b0c0c4b0d3609d24f/packages/services/graphics-svg-pipeline/src/cljs/graphics_svg_pipeline/ledger.cljs"
  - "https://github.com/open-hax/eta-mu/blob/2afdb0208dd614bbcc87a6096db938e17b96426a/ROADMAP.md"
  - "octave-commons/epiphany#12@0370d6ce6d4e1e9f90d17b252884fe8ee4970f76"
related:
  - "docs/notes/clio-event-ledger-contract-comparison-2026-08-12.md#working-interpretation"
  - "docs/notes/eta-mu-clio-event-kernel-triage-2026-08-10.md#unresolved-questions"
  - "octave-commons/epiphany#15"
informs:
  - "open-hax/eta-mu@2afdb0208dd614bbcc87a6096db938e17b96426a:ROADMAP.md"
---

# Clio event-ledger reported ownership direction — 2026-08-14

## Reported instruction and provenance gap

The 2026-08-14 working note recorded this operator instruction about the
previously unresolved Clio ↔ standalone event-ledger ownership question:

> "no clio take it all. i dont think anything is even using the event ledger."

The first recoverable repository artifact containing the quote is Epiphany PR
#12 head `0370d6ce6d4e1e9f90d17b252884fe8ee4970f76`. No dated source conversation
or session receipt is linked, and a search of the accessible PR timeline found
no earlier occurrence. This note therefore preserves a **reported operator
instruction**, not an accepted architectural decision. Epiphany issue #15 owns
source recovery or durable reconfirmation followed by an authoritative ADR.

## Reported ownership direction (provisional)

If confirmed, the reported direction would make Clio the destination and owner
for the event-ledger domain being developed across these repositories,
including the semantic contract previously split or ambiguously described
across eta-mu, standalone `open-hax/event-ledger`, and OpenPlanner's embedded
`packages/event-ledger` implementation. Until issue #15 produces the acceptance
source and ADR, this remains provisional and authorizes no migration or deletion.

The reported direction is:

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
  └─ proposed migration sources -> retire only after acceptance and proof
```

The reported direction would not require every legacy implementation detail to
survive. A behavior would be migrated only when a live consumer or an
independently accepted requirement justifies it against Clio's laws.

## Observed source dependency requiring deployment/runtime verification

A repository-wide source search found at least one concrete source dependency
on OpenPlanner's embedded event-ledger package:

`packages/services/graphics-svg-pipeline/src/cljs/graphics_svg_pipeline/ledger.cljs` requires `promethean.event-ledger.core`.

That source-level observation revises the tentative belief that no code
references the old implementation. It does not establish deployment or runtime
reachability, and it does **not** expand the reported ownership direction. The
graphics pipeline dependency requires deployment/runtime verification; if active, its
required behavior should be identified, represented through Clio or a
Clio-backed adapter, cut over, and then the legacy dependency can be retired.

The remaining event-ledger package files, roadmaps, epics, compatibility
bridges, Mongo/REST surfaces, TTL policy, and other historical implementation
artifacts are not evidence of ongoing authority by existence alone.

## Potential effect on earlier interpretations

The earlier notes correctly established semantic overlap and implementation
differences. Their unresolved disposition would become stale if the reported
instruction is durably confirmed and accepted.

Preserve those notes and this note as revision-scoped provenance. Until issue
#15 produces an accepted ADR, the following remains proposed direction rather
than ownership authority:

- Clio ownership/destination: **reported operator instruction; provisional**.
- standalone `open-hax/event-ledger`: **proposed legacy migration source**.
- OpenPlanner `packages/event-ledger`: **proposed legacy migration source**.
- live legacy consumers: **potential migration obligations requiring runtime
  proof**.
- Clio laws becoming Epiphany/JVM standards: **a separate promotion decision**;
  this note does not promote implementation-specific mechanisms into policy.

## Roadmap implication

Eta-mu's roadmap wording that assigns standalone `event-ledger` ownership of
the append-only envelope/storage contract would be superseded if the reported
instruction is confirmed and accepted.

The roadmap must not be revised solely from this draft note. An accepted ADR
from issue #15 should inform any later eta-mu roadmap change through eta-mu's
own repository process.

## Migration proof required before deletion

Before any later accepted decision authorizes removal of a legacy event-ledger
implementation, verify at minimum:

1. direct imports and package dependencies;
2. deployment/runtime references;
3. persistence data or compatibility reads that must remain accessible;
4. watchers, TTL, REST, attribution, or sequencing behavior actually depended
   upon by a live caller;
5. a Clio or Clio-backed replacement for every retained behavior;
6. an executable cutover check demonstrating the caller no longer reaches the
   legacy implementation;
7. conversion of every retained legacy event into a Clio envelope, with schema,
   causal, ordering, and version constraints validated;
8. replay and projection equivalence over retained history, including Clio's
   current complete-history and sequence-1 requirements; or
9. an explicit approved exception identifying data that will not be retained
   and why conversion and replay proof do not apply.

Absence of a search hit is evidence only for the search performed, not proof of non-use.

## Disposition

- ownership ambiguity: **unresolved pending durable confirmation and ADR**;
- reported owner/destination: **Clio, provisional**;
- legacy implementations: **proposed migration/retirement candidates; no
  deletion authorized**;
- OpenPlanner graphics pipeline: **observed source dependency requiring
  deployment/runtime verification**;
- Epiphany normalization of portable Clio laws: **promotion candidate only**,
  independently governed by Epiphany's process and explicit acceptance rules.
