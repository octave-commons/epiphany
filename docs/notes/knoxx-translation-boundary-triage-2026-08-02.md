---
slug: knoxx-translation-boundary-triage-2026-08-02
uuid: 7cbf3e32-f8a8-4df0-a7ad-6da6417da3aa
title: "Knoxx Translation Boundary Triage — 2026-08-02"
kind: note
status: draft
description: "Revision-scoped triage of Knoxx translation persistence, tenant authority, worker identity, and OpenPlanner boundary changes after PRs #210 and #211."
created: "2026-08-02"
labels: [triage, knoxx, translation, tenancy, identity, openplanner, mongo, provenance]
sources:
  - "open-hax/knoxx#210"
  - "open-hax/knoxx@035860262c7685376d2defe34acc47df28045288"
  - "open-hax/knoxx#211"
  - "open-hax/knoxx@5df786bcbebb5fe79c5b7ede620d1c4082612a05"
  - "octave-commons/epiphany:PROCESS.md@68ab489353c95f8e6d5fa9b2b501c5774e31d4d6"
informs:
  - "docs/notes/session-persistence-and-knowledge-service-boundary.md"
---

# Knoxx Translation Boundary Triage — 2026-08-02

## Scope

This note records a bounded state change in Knoxx's translation architecture.
It distinguishes merged implementation evidence from design interpretation and
from decisions that remain unaccepted. It does not rewrite the source PRs or
promote their implementation shape into permanent cross-repository authority.

## Observed implementation state

Knoxx PR #210 moved translation persistence and domain operations out of the
OpenPlanner REST path and into Knoxx ClojureScript operating directly against
the shared Mongo data plane. The merged slice covers translation segments,
documents, reviews, labels, manifests, SFT exports, graph-memory updates, and
batch operations. Other OpenPlanner-backed data-plane operations remain on the
existing SDK boundary.

PR #211 then repaired material authority and integrity defects discovered after
#210 merged. At revision `5df786b`, the current implementation includes:

- nonblank organization scope on direct translation contracts;
- membership-bound batch ownership and worker execution context;
- system-admin-only batch claiming, with explicit cross-organization scope;
- fail-closed handling for batches whose principal cannot be resolved safely;
- bounded and validated manifests, pagination, and SFT exports;
- restart-safe tenant index migration and tenant/project backfill;
- atomic or compensating label, review, and graph-memory writes;
- unique graph element identifiers required for concurrency correctness;
- focused Mongo namespaces behind a compatibility facade;
- pinned reusable-workflow logic and narrowed secret propagation.

These are merged implementation facts. They establish current behavior at the
cited revision, not permanent architectural acceptance.

## Derived interpretation

The current implementation implies a sharper service boundary than the earlier
"Knoxx calls OpenPlanner for translation storage" model:

```text
Knoxx
  = authentication, membership, tenant authority, translation orchestration,
    translation-domain persistence, and worker policy enforcement

OpenPlanner
  = remaining graph/search/projection capabilities reached through explicit
    SDK or service boundaries

shared Mongo
  = data plane, not independent authority
```

The important correction is not merely removal of a REST hop. Translation
writes require Knoxx-resolved organizational and membership authority. Direct
database access therefore remains subordinate to the authenticated service
boundary; possession of the shared database does not confer translation-domain
or tenant authority.

## Unresolved design questions

The merged work does not by itself settle:

1. whether translation persistence is permanently owned by Knoxx or is an
   interim module pending extraction into a dedicated capability;
2. whether approved translation graph-memory writes should continue targeting
   OpenPlanner-compatible collections directly or pass through a projection
   protocol;
3. which translation events belong in canonical event-ledger envelopes rather
   than only Mongo operational state;
4. how translation batch identity should compose with Axxium principal
   bindings across repositories;
5. whether compensating multi-document writes are sufficient long-term or
   should become transaction-backed when deployment topology permits;
6. which legacy translation batches may remain valid without membership
   attribution, and when that compatibility path should expire.

These remain proposals or inquiry targets until an authorized decision records
scope, basis, and acceptance.

## Actionable design drift

Any documentation that describes OpenPlanner as the active translation storage
or transaction authority is stale for Knoxx revision `5df786b`. A narrower
statement remains true: OpenPlanner still supplies selected projection and
compatibility capabilities, while Knoxx now owns the authenticated translation
operation boundary and its direct persistence implementation.

The implementation also demonstrates a reusable authority rule:

```text
data-plane reachability != tenant authority
worker capability       != caller membership
service ownership       != database ownership
```

This rule is an interpretation supported by the merged regressions. It is not a
new accepted charter or ADR.

## Verification targets

A focused follow-up should verify:

- current Knoxx architecture and translation documentation for stale
  OpenPlanner-authority claims;
- every translation collection and index for explicit tenant scope;
- the exact projection contract for graph-memory writes;
- whether batch and translation lifecycle events are represented in
  event-ledger;
- removal criteria for safe legacy batches without membership attribution.

## Disposition

`note` / revision-scoped implementation finding.

Preserve PRs #210 and #211 as the authoritative change and review record. Use
this note to guide documentation correction and architecture review, but do not
promote the inferred service boundary or unresolved migration choices without
explicit operator acceptance.
