---
slug: eta-mu-clio-event-kernel-triage-2026-08-10
kind: note
status: draft
description: "Revision-scoped synthesis of eta-mu's merged Clio event-sourcing kernel and its unresolved relationship to existing event-ledger authority."
labels: [eta-mu, clio, event-sourcing, ledger, schemas, cross-repo, triage]
created: "2026-08-10"
sources:
  - "https://github.com/open-hax/eta-mu/pull/280"
  - "https://github.com/open-hax/eta-mu/pull/282"
  - "https://github.com/open-hax/eta-mu/blob/main/packages/clio/README.md"
informs:
  - "https://github.com/open-hax/eta-mu/blob/main/ROADMAP.md"
---

# Eta-mu Clio event-kernel triage — 2026-08-10

## Purpose

Record the merged implementation evidence around eta-mu's new `@eta-mu/clio`
event-sourcing kernel without silently promoting it into cross-repository
architectural authority or treating it as an automatic replacement for the
existing standalone `event-ledger` project named by eta-mu's roadmap.

This note is revision-scoped working synthesis. It is not an ADR, ownership
decision, migration authorization, or statement that Epiphany has adopted the
same implementation.

## Observed implementation facts

### PR #280 — merged Clio kernel

`open-hax/eta-mu#280` merged a new `packages/clio` package described by its own
README as a small content-addressed event-sourcing kernel for eta-mu.

The merged package makes these implementation claims executable:

- authority is the immutable event set plus its causal/order graph rather than
  physical ledger-file order;
- physical ledger files are storage partitions rather than semantic streams;
- exact duplicate event IDs deduplicate while same-ID/different-data is
  corruption;
- `:event/causes` forms causal edges and missing parents are rejected for the
  complete-history canonicalization supported by this slice;
- `[stream, seq]` is an order-sensitive stream slot and competing claims are
  rejected;
- graph-incomparable events remain concurrent and receive only a deterministic
  replay tie-break;
- projections are disposable pure folds over canonical order;
- arbitrary physical partitioning is tested for canonicalization/projection
  invariance;
- event-schema identity is derived from a Merkle tree over logical namespace
  structure and Malli schema forms rather than a manually bumped version;
- each event records a schema catalog root, schema id, and leaf hash, while
  historical catalog snapshots remain addressable by root;
- reusable runtime-neutral code is organized as `law`, `shape`, and `domain`,
  Node/JS interop is isolated under `extern`, and runtime orchestration lives in
  `infra`.

The package deliberately limits its first canonicalizer to complete stream
histories beginning at sequence 1. Snapshot/partial-history replay is explicitly
left for a later trusted-prefix/checkpoint contract.

### PR #282 — append admission moved into law

`open-hax/eta-mu#282` followed the merge by moving append-admission decisions out
of `infra.ledger` and into `clio.law.ledger/append-admission`.

The pure law now classifies a candidate as:

- `:appendable`
- `:already-present`
- `:id-collision`
- `:stream-slot-conflict`

Infrastructure retains locking, reading, validation, and writing, but not the
policy that decides which of those semantic states applies. The external error
contract is described as unchanged.

This is direct implementation evidence for eta-mu's recurring construction-order
claim that domain policy should not live in infrastructure merely because the
first implementation discovered it there.

## Current roadmap tension

Eta-mu's root `ROADMAP.md` still names the standalone `event-ledger` as owner of
"the append-only envelope contract and its storage" and separately records that
the standalone event-ledger is ahead of the stale OpenPlanner copy.

Clio now implements a broader event-sourcing kernel inside eta-mu that includes:

- an event envelope;
- historical schema identity;
- append admission;
- physical ledger persistence;
- multi-ledger canonicalization;
- causal DAG construction;
- deterministic replay order;
- projection folds.

The observed facts therefore establish overlap. They do **not** establish which
artifact owns the durable cross-repository contract.

Treating Clio as an automatic replacement for `event-ledger` would infer an
ownership decision from merge status. Treating the older roadmap ownership table
as unchanged would ignore current implementation pressure. Both would overstate
the evidence.

## Working interpretation

Clio is currently best classified as a strong eta-mu incubation/promotion
candidate for event-ledger semantics.

The potentially reusable shapes are broader than its NBB/Shadow implementation:

1. immutable events plus causal graph as semantic authority;
2. physical-ledger partition invariance;
3. explicit separation of causality from deterministic replay tie-breaking;
4. content-derived schema revisions over logical namespace/schema structure;
5. append admission as pure law distinct from storage transport;
6. projections as rebuildable derived state.

Those shapes are plausible candidates for later normalization in Epiphany if
they survive use outside eta-mu and if an explicit ownership/acceptance path is
recorded. One merged package is not enough evidence to make them Epiphany policy.

## Cross-center relevance

### Eta-mu

Clio is merged executable evidence in the CLJS center. It is appropriate for
eta-mu to pressure-test these shapes through real runtime and storage behavior.

### Epiphany

Epiphany already distinguishes canonical evidence from projections and requires
explicit epistemic promotion. Clio's projection/replay and schema-provenance
choices are structurally compatible with those concerns, but no JVM adoption or
cross-center standardization is established here.

A future promotion should compare Clio's laws against Epiphany's existing event,
observation, decision, provenance, and projection models rather than importing
package names or runtime details wholesale.

## Unresolved questions

1. Does Clio supersede, absorb, consume, or merely experiment beyond the
   standalone `event-ledger` contract named by the eta-mu roadmap?
2. Which event-envelope fields are durable cross-repository contract and which
   are Clio-local implementation shape?
3. Is schema-root/leaf hashing intended to become shared contract vocabulary, or
   should other centers remain free to version schemas differently while
   preserving equivalent provenance guarantees?
4. What is the trusted-prefix/checkpoint contract for partial histories,
   snapshots, compaction, and bounded replay?
5. Which causal constraints belong to generic event law versus individual
   domain/stream policies?
6. Does the `law -> shape -> extern -> domain -> infra` construction order remain
   stable when the same semantics are implemented on the JVM without a JS
   boundary?
7. What migration evidence would be required before eta-mu's roadmap can replace
   its current standalone `event-ledger` ownership statement?

## Disposition

- `eta-mu#280`: **merged implementation evidence**; retain and observe.
- `eta-mu#282`: **merged implementation correction**; retain as evidence that
  append admission is being normalized upward from transport into law.
- Clio → standalone event-ledger ownership relation: **unresolved**.
- Clio laws → Epiphany normalization: **promotion candidate**, not accepted.
- Eta-mu roadmap ownership text: **requires bounded re-verification/update**, not
  silent reinterpretation.

## Highest-value next pass

Compare Clio's public event/schema laws against the standalone `event-ledger`
repository's current contract and identify exact identity, envelope, causal,
versioning, storage, and projection overlaps before proposing any ownership
change.
