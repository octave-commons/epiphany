---
slug: cross-repository-protocol-package-triage-2026-07-31
uuid: 7f0ee45e-26fd-4c0e-9301-ecfe83531f2a
title: "Cross-Repository Protocol Package Triage — 2026-07-31"
kind: note
status: draft
description: "Revision-scoped triage of eta-mu's merged Receipt River, Session Mycology, and Fork Tax package extraction."
created: "2026-07-31"
labels: [triage, eta-mu, receipt-river, session-mycology, fork-tax, provenance]
sources:
  - "open-hax/eta-mu:pull/161"
  - "open-hax/eta-mu@5af74786737c896beb288d15ae36982a972fa013"
  - "open-hax/eta-mu@dc75198469c4f00533d7727506b845e3b03a3cb1"
  - "octave-commons/epiphany:PROCESS.md"
implements:
  - "docs/process/inbox.md"
  - "docs/process/notes.md"
informs: []
---

# Cross-Repository Protocol Package Triage — 2026-07-31

## Purpose

This note records a material state transition discovered during recurring
cross-repository triage. It does not replace the source PR, package READMEs, or
future architectural decisions.

## Observed change

eta-mu PR #161 merged as commit
`5af74786737c896beb288d15ae36982a972fa013`. The merge added sibling packages
for Receipt River, Session Mycology, and Fork Tax, moved the corresponding
implementation behind package-owned APIs, and routed canonical and compatibility
CLI commands through those packages.

At repository revision `dc75198469c4f00533d7727506b845e3b03a3cb1`, the root
README describes those packages as owning their records, schema registries,
readers, writers, validators, and domain projections. The unscoped `eta-mu`
application composes versions and delegates commands rather than retaining
independent copies of that domain logic.

## Epistemic classification

### Implementation evidence

The package extraction, executable APIs, tests, version components, schemas,
and command delegation are merged implementation facts.

### Repository-local authority claim

Within eta-mu's current implementation, the package implementations are treated
as authoritative for their package-owned behavior and schemas. Existing
extension implementations are retained as historical behavior sources and
migration adapters.

### Not established by merge alone

The merge does not, by itself, establish operator acceptance of every durable
cross-repository semantic boundary. In particular, it does not settle:

- whether all three protocol package boundaries are permanent architecture;
- how their small shared event envelopes relate to the canonical event-ledger
  envelope over future incompatible revisions;
- whether Session Mycology spore state belongs entirely inside its package or
  partly in Epiphany's process authority;
- whether Fork Tax plans and handoff artifacts are observations, commitments,
  or executable actions in every host integration;
- when the extension adapters have reached sufficient parity to become thin
  delegates or be retired.

## Design interpretation

The merged implementation provides a useful ownership split:

```text
package          owns domain records, schemas, validation, readers, writers
eta-mu CLI       composes versions and delegates commands
extensions       retain migration adapters and historical behavior sources
event-ledger     remains the separate canonical append/envelope boundary where used
Epiphany         governs evidence, promotion, and process acceptance in its own scope
```

This is a derived interpretation of current repository behavior, not a new ADR.
A future decision record should be used if these package boundaries are to be
accepted as durable architecture across repositories.

## Related operational evidence

The same eta-mu revision line also added revision-bound sandbox bundles and
broadened the root validation gate. Those changes improve reproducibility and
verification coverage, but they are engineering evidence rather than acceptance
of the protocol semantics described above.

## Disposition

`note` / revision-scoped implementation synthesis.

Preserve this note until the package boundaries are either explicitly accepted,
revised, or superseded. Do not promote it to an architectural decision without
an identified accepting authority and a comparison against event-ledger,
Epiphany process ownership, and the remaining extension adapters.
