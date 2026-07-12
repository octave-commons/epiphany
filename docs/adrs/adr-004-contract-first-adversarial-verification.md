---
status: draft
title: "ADR-004: Contract-First Adversarial Verification"
id: 4
---

# ADR-004: Contract-First Adversarial Verification

## Context

Epiphany is a Git-backed knowledge system. Its durable records distinguish
observed facts, derived outputs, provisional candidates, and explicitly
accepted decisions. A malformed persisted record, a schema-validation bypass,
or an empty result that actually means unavailable or not implemented can make
false claims about evidence or continuity.

The system has multiple adapter profiles. In-memory adapters make unit tests
fast, but a permissive double can accept a record that a durable adapter
rejects. A green unit suite therefore does not by itself demonstrate that a
command produces a valid durable observation.

Visible checks are vulnerable to optimization without fulfillment of their
underlying intent. A single fixed test suite, coverage threshold, linter,
mutation score, or agent-authored report is not sufficient assurance. The
verification system must preserve the difference between observed successful
execution and a claim that the intended behavior is established.

## Decision

Epiphany adopts a contract-first, multi-oracle verification architecture.

1. Every externally received command, durable observation, persisted-document
decode, backup import, and public result has a named, versioned data contract.

2. Schema validation is mandatory at application and adapter boundaries. A
public persistence operation must not rely only on its caller remembering to
validate. Composition-time validation wrappers are permitted; adapter-local
validation remains required as defense in depth.

3. The in-memory adapter is a contract-enforcing reference implementation. It
must not accept a state that a durable adapter rejects under their shared port
contract. It need not reproduce database implementation details.

4. Every adapter implementation passes the same contract-law suite for its
shared behavior. Durable-adapter integration tests remain required for
implementation-specific behavior, including BSON mapping, indexes, filesystem
failure, Git object access, and service availability.

5. Required correctness evidence is a portfolio of partly independent checks:
schema validation, static analysis, example tests, property and metamorphic
tests, adapter differential tests, targeted mutation testing, and integration
tests. The portfolio may evolve, but no one check is the sole authority.

6. Non-negotiable integrity constraints are gates, not weighted-score inputs.
Invalid records must not persist; rejected writes must not change state;
unknown schema versions must not be interpreted as known versions; and empty,
unavailable, corrupt, and not-implemented outcomes must remain distinct.

7. CI-generated evidence is authoritative over agent-authored completion
claims. Required commands, generated-test seeds, adapter-contract outcomes,
and mutation results must be produced or verified by CI.

8. Stable laws may be public. Their concrete generated inputs, random seeds,
fixtures, and adversarial cases may be rotated or private. This prevents
memorizing a finite fixture set while preserving reproducibility for a specific
failing run.

## Consequences

### Positive

- Fast unit tests and durable adapters share an explicit behavioral contract.
- Schema failures occur near the boundary responsible for them rather than late
  in Mongo, Lucene, HTTP, CLI, or backup behavior.
- A new persistence operation requires an explicit contract, enforcement
  registration, and adapter-law coverage.
- Verification claims are corroborated by multiple evidence sources instead of
  a single green result.
- The system protects its epistemic distinctions: missing, malformed, and
  unavailable evidence cannot silently become negative evidence or identity.

### Costs

- More test infrastructure, fixture/version maintenance, and slower
  integration and mutation jobs.
- New commands, observation kinds, and adapter operations need schemas,
  validation, reference behavior, and tests before becoming usable.
- Private or rotated checks require controlled CI access and a reproducibility
  mechanism for failures.
- Independence is not automatic: checks derived from the same implementation
  can share the same defect and must not be presented as independent evidence.

## Required invariants

- Each public persistence-write operation has a named versioned input schema.
- Every public read/decode validates the schema version it claims to represent.
- Rejected writes leave adapter state unchanged.
- In-memory and durable adapters agree on acceptance/rejection and
  domain-level failure categories for their shared contract.
- Candidate, accepted, rejected, unknown, unavailable, corrupt, empty, and
  not-implemented outcomes are not collapsed into one another.
- A passing metric or aggregate score cannot waive a failed integrity gate.
- A durable identity or accepted review decision is never inferred solely from
  similarity, a test outcome, or an unavailable source.

## Non-decisions

This ADR does not select:

- A permanent linter, mutation-testing engine, property-testing library, or
  CI provider.
- A project-wide static type system.
- A zero-Java-interop policy. Java interop remains permitted in explicitly
  infrastructure-owned adapters; layer-boundary rules govern where it belongs.
- Individual domain schemas, migrations, or storage-index definitions.
- A replacement for explicit human acceptance of identity and continuity
  decisions.

## Alternatives considered

### Trust production-only validation

Rejected. It permits a permissive unit adapter to certify behavior that cannot
persist in the selected durable adapter.

### Make the in-memory adapter emulate Mongo exactly

Rejected. The reference adapter must enforce shared semantics, but duplicating
Mongo internals creates a fragile second database implementation.

### One comprehensive end-to-end suite

Rejected. It is slow, difficult to diagnose, and remains dependent on one
execution path and its oracles.

### Coverage as the primary gate

Rejected. Executing a line does not show that relevant behavior or epistemic
outcomes were asserted.

### A single weighted quality score

Rejected. It allows a material integrity failure to be offset by superficial
wins elsewhere.

## Follow-up design work

- Define a schema-boundary wrapper and operation-to-schema registry.
- Define the adapter contract-law harness and differential test normalization.
- Define the CI assurance matrix, including required, private, and rotated
  checks.
- Define layer-boundary and Java-interop inventory checks.
- Trial static-analysis, generative-testing, and mutation-testing tools against
  small, boundary-critical namespaces.
- Define a machine-readable CI evidence artifact.
