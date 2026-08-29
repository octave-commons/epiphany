---
slug: clio-event-ledger-contract-comparison-2026-08-12
uuid: d8f83538-69ca-41ce-9bed-1cf67e53a099
kind: note
status: draft
description: "Revision-scoped comparison of eta-mu Clio and the standalone open-hax/event-ledger contract."
labels: [eta-mu, clio, event-ledger, event-sourcing, contracts, cross-repo, triage]
created: "2026-08-12"
sources:
  - "https://github.com/open-hax/eta-mu/blob/337a1404673b5ce0a488305528f5d3615010882d/packages/clio/README.md"
  - "https://github.com/open-hax/event-ledger/blob/ada7374b7f4e1c3b0ab4e6bbe996f10f06e9b93a/README.md"
  - "https://github.com/open-hax/eta-mu/blob/2afdb0208dd614bbcc87a6096db938e17b96426a/ROADMAP.md"
informs:
  - "docs/notes/eta-mu-clio-event-kernel-triage-2026-08-10.md"
---

# Clio ↔ standalone event-ledger contract comparison — 2026-08-12

## Purpose

Complete the bounded comparison requested by the Clio triage note without
promoting either implementation into cross-repository ownership.

This is revision-scoped synthesis. It compares the public contracts documented
at the immutable source revisions listed above; it is not an ADR, migration
authorization, or claim that either repository supersedes the other.

## Observed overlap

Both systems currently provide an append boundary around Malli-validated events
and treat `:event/id` as a durable identity used to recognize retries or
collisions. Both distinguish durable event data from derived/read-side behavior.
That is real semantic overlap and explains why eta-mu's roadmap ownership line is
now ambiguous.

The overlap stops well short of interchangeability.

## Standalone event-ledger — observed contract

`open-hax/event-ledger` is an operational MongoDB-backed event spine. Its public
README describes:

- a permissive version-1 envelope whose only required incoming field is
  `:event/type`, with defaults added for UUID, time, causal root, session and
  delivery mode;
- optional actor/principal, organization, run, episode, contract-revision and
  delivery references;
- a global monotonic `:ledger/seq` allocated from a Mongo counters document;
- idempotent insert keyed by `:event/id`;
- Mongo change-stream watchers;
- TTL expiration and per-type TTL overrides;
- a compatibility merge-read with the legacy `events` collection;
- CLJS and JS/ESM consumption surfaces;
- host-supplied MongoDB as the persistence transport.

Its causal fields are correlation/audit fields. The documented public contract
does not make a causal DAG, physical-partition invariance, topological replay,
stream-slot continuity, schema-catalog Merkle identity, or pure projection folds
part of the store's semantics.

## Clio — observed contract

`@eta-mu/clio` is an event-sourcing/canonicalization kernel. Its public README
describes:

- a closed event schema whose complete historical validator is identified by a
  catalog Merkle root plus schema id and schema-leaf hash;
- immutable event identity where exact duplicates dedupe and same-id/different
  data is corruption;
- explicit `:event/causes` edges and complete-history missing-parent checks;
- `[stream, seq]` as a contested stream slot with contiguous history rules;
- arbitrary physical ledger files as semantically irrelevant partitions;
- deterministic topological replay while preserving graph-incomparable
  concurrency separately from the tie-break order;
- projections as disposable pure folds over canonical order;
- append admission as runtime-neutral law;
- newline-delimited EDN persistence with an OS-backed advisory lock protocol.

The first slice deliberately requires complete histories beginning at stream
sequence 1 and does not yet define trusted prefixes, checkpoints, snapshots,
compaction, or partial-history replay.

## Contract differences that prevent substitution

### Envelope

The standalone ledger accepts an additive operational envelope centered on
routing, attribution, delivery, session/run correlation and compatibility.
Clio requires stream, sequence, causes, actor, subject, schema provenance and a
closed historical event validator.

Neither envelope is a strict subset of the other.

### Ordering and causality

Standalone `event-ledger` assigns one global Mongo sequence and exposes causal
references for querying/audit. Clio derives semantic order from stream slots and
a causal DAG; physical append order has no authority.

These are different ordering models, not merely different storage adapters.

### Versioning

Standalone `event-ledger` exposes an additive integer envelope marker and
versioned resource references. Clio deliberately has no hand-maintained event
version; exact schema history is content-addressed through catalog-root and leaf
hashes.

### Storage lifecycle

Standalone `event-ledger` is a long-lived Mongo service surface with change
streams, TTL expiry, indexes, and a legacy bridge. Clio's current storage is
append-only local EDN partitions whose event set is expected to remain available
for complete-history canonicalization.

TTL deletion from a canonical Clio history would violate its current
complete-history assumptions unless a future checkpoint/prefix law explicitly
made that deletion safe.

### Read model

Standalone `event-ledger` provides watches and cursor-paginated operational
reads. Clio provides canonicalization plus projection folds. Neither currently
implements the other's complete read contract.

## Working interpretation

The evidence supports a narrower ownership statement than eta-mu's current
ROADMAP wording.

`event-ledger` is presently the operational Open-Hax event-store/transport
implementation and compatibility surface. Clio is presently eta-mu's stronger
semantic event-sourcing law/canonicalization experiment and executable kernel.

That split is an interpretation, not an accepted ownership decision. It may
prove durable, or Clio may eventually supply laws consumed by a Mongo-backed
transport, or one project may supersede/absorb the other. Current code does not
establish which path is intended.

## Promotion candidates

The comparison strengthens these candidates for later cross-center
normalization because they are semantic laws rather than transport choices:

1. event identity must distinguish exact retry from same-id/different-data
   corruption;
2. persistence order and causal order should not be conflated;
3. projection state should remain reconstructible from durable authority;
4. append admission belongs in law rather than a first transport;
5. schema provenance should make historical validation reproducible.

The precise Clio mechanisms — complete-history requirement, stream sequence law,
Merkle catalog protocol, EDN partitions and lock protocol — remain eta-mu
implementation evidence until broader use or explicit acceptance establishes
otherwise.

## Roadmap implication

Eta-mu's current ownership-table sentence that standalone `event-ledger` owns
"the append-only envelope contract and its storage" is now too broad to use
without qualification: Clio also has a merged append/envelope contract with
stronger event-sourcing semantics.

A safe roadmap correction would distinguish **operational event-store/transport
ownership** from **event-sourcing semantic-law incubation**, while leaving the
final durable ownership relation explicitly unresolved.

## Disposition

- standalone `event-ledger`: current operational implementation evidence;
- Clio: current eta-mu semantic-kernel implementation evidence;
- exact replacement/subsumption relation: unresolved;
- roadmap ownership wording: stale/ambiguous enough to require qualification;
- cross-center laws: promotion candidates only.
