---
slug: cross-repository-triage-2026-07-27
uuid: 5d705f30-e8ef-49e3-a6e6-c1e8b23dd4a4
title: "Cross-Repository Triage — Muse Boundary and Axxium Identity"
kind: note
status: draft
description: "Revision-scoped follow-up recording the accepted Muse/Keryx boundary, its first implementation evidence, and the new Axxium runtime-principal binding slice."
created: "2026-07-27"
labels: [triage, synthesis, muse, keryx, axxium, actors, provenance]
sources:
  - "docs/notes/cross-repository-notes-synthesis-2026-07-26.md"
  - "open-hax/eta-mu@62ae1cd6d36206a5ab844a33a8dcecc0558e6b3b"
  - "octave-commons/muse@e9f3f0fb056f9b0413868923ddfd0c18e16b7cee"
  - "octave-commons/muse@76c57712a48ef48100259231a2e9d54069c2b14a"
  - "open-hax/eta-mu@342cc85acade1e78c996b8d2179c8e454d9f8c95"
implements:
  - "docs/process/inbox.md"
  - "docs/process/notes.md"
informs: []
---

# Cross-Repository Triage — 2026-07-27

## Scope

This is an additive follow-up to the 2026-07-26 cross-repository source map. It records only material changes observed after that synthesis. It does not replace the accepted architecture records in eta-mu or Muse.

## Observation 1: Muse/Keryx is no longer unresolved

The eta-mu reconciliation note was promoted from `decision candidate` to `accepted` at commit `62ae1cd6d36206a5ab844a33a8dcecc0558e6b3b`.

The accepted boundary is:

- Muse owns portable declaration linking, compatibility profiles, and target compilation;
- Muse's OpenCode adapter owns OpenCode-specific projection behavior;
- eta-mu runtime modules own native session, actor, event, and workflow semantics;
- Keryx remains historical vocabulary and requirement lineage, not an active compiler authority.

**Disposition:** accepted decision, externally owned by eta-mu/Muse. The earlier Epiphany source map's unresolved wording is now stale historical state.

## Observation 2: implementation now supports the accepted direction

Muse commit `e9f3f0fb056f9b0413868923ddfd0c18e16b7cee` replaced generated blocking actor monitoring with durable non-blocking watch registration, status, cancellation, resumable pending watches, and terminal event recording across OpenCode, Claude, and MCP projections.

Muse commit `76c57712a48ef48100259231a2e9d54069c2b14a` separated semantic capability descriptors, runtime implementation descriptors, and target exposure descriptors. It retained `deftool` and the legacy flat tool shape as compatibility projections.

These commits are implementation evidence for the accepted Muse boundary. They do not complete the migration.

Remaining verified gaps include:

- canonical Katamorph resource integration;
- explicit target-loss diagnostics;
- cross-target artifact-parity fixtures;
- replacement or isolation of Muse-local embedded runtime implementations;
- eventual removal of legacy flat-tool compatibility only after evidence exists.

## Observation 3: Axxium identity authority became executable

eta-mu commit `342cc85acade1e78c996b8d2179c8e454d9f8c95` added Axxium-owned runtime principal bindings for Sol and event-ledger attribution, organization-scope constraints, authenticated principal resolution, closed schemas, canonical JSON wire keys, PostgreSQL integration tests, and read-only CI.

**Interpretation:** the earlier authority model is gaining executable support: Axxium owns actor/principal identity, while Sol and event-ledger consume resolved attribution rather than inventing identity locally.

**Limit:** this commit establishes a runtime binding slice, not complete cross-repository adoption. Consumers still need to use the binding consistently and preserve provenance at host boundaries.

## Contradictions and drift

1. The 2026-07-26 Epiphany source map still names Muse/Keryx reconciliation as unresolved and code inspection as the highest-value next pass. That statement is now stale but remains valid as historical provenance for the earlier revision.
2. Muse currently implements local capability/implementation/exposure descriptor records while Katamorph remains the intended semantic authority. This is an explicit migration gap, not yet an ownership contradiction.
3. Muse's durable watch implementation contains actor/event runtime behavior inside the compatibility repository. The accepted design classifies such code as bootstrap, conformance, or migration residue; future work should prevent this implementation convenience from becoming authority drift.

## Research disposition

No external research was required for this pass. The material questions were repository-specific ownership and implementation claims, for which current code and accepted design records are the primary evidence.

## Next bounded review

Inspect whether Muse's local descriptor schemas can map losslessly to current Katamorph resources and identify the smallest compatibility adapter or schema extension required. Keep the result as a proposal until explicitly accepted by the operator and both repository boundaries.