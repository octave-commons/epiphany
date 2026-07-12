---
slug: verification-architecture
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000214
title: "Verification Architecture"
kind: design
status: open
requires-decisions: ["ADR-004"]
dependencies: ["docs/adrs/adr-004-contract-first-adversarial-verification.md"]
dependendents: []
epics: []
description: "Implementation design for contract-first, adversarial verification: schema enforcement, adapter laws, independent checks, and CI evidence."
labels: [architecture, quality, testing, schemas, verification]
created: "2026-07-12"
---

# Verification Architecture

## Purpose

ADR-004 establishes that no single green signal proves correctness. This design
turns that decision into a small set of explicit, testable mechanisms.

The primary objective is not a maximized test count. It is to make the system
reject invalid durable state, distinguish evidence states faithfully, and
produce reproducible evidence that a change was checked by several partly
independent mechanisms.

## Non-goals

This design does not choose permanent lint, mutation, or property-testing
tools. It does not define every domain schema, replace human acceptance of
continuity decisions, or require a JVM-wide ban on Java interop.

## Assurance model

A change is accepted only when all applicable integrity gates pass. Quality
signals inform review but cannot compensate for a failed integrity gate.

| Layer | Claim tested | Primary evidence | Required result |
|---|---|---|---|
| Contract | Data is structurally valid at a named boundary | Malli schema validation | Invalid values rejected |
| Port law | Implementations share observable behavior | In-memory and durable adapter law suite | Same acceptance and domain error categories |
| Domain | Rules preserve stated invariants | Example, property, metamorphic tests | Laws hold for cases and generated inputs |
| Integration | Real dependency mappings work | Mongo, Git, Lucene, HTTP integration tests | Expected real-adapter behavior |
| Structural | Source stays within architecture boundaries | Static analysis and dependency/interop checks | No unapproved boundary violation |
| Test adequacy | Tests detect meaningful behavior changes | Targeted mutation tests | Required mutants killed or reviewed |
| Evidence | Claimed checks actually occurred | CI-generated evidence artifact | Complete, replayable report |

## Schema ownership

`epiphany.law.*` owns versioned schema data and validators. It has no I/O and
does not depend on `domain`, `application`, or `infra`.

Each durable value has exactly one named schema for the version it claims to
be. Schemas are closed unless extensibility is explicit. A schema version is a
compatibility claim, not merely a positive integer; the operation selecting a
schema must check the record's `:observation/schema-version` against that
schema's known version.

The source of truth is an operation registry, not inference from an arbitrary
map:

```clojure
{:record-repository-location!
 {:input-schema "observation/repository-location-v1"
  :version 1
  :persistence :append-only}

 :record-ingestion-run!
 {:input-schema "observation/ingestion-run-v1"
  :version 1
  :persistence :append-only}

 :record-revision-at-path!
 {:input-schema "observation/revision-at-path-v1"
  :version 1
  :persistence :append-only}}
```

Adding a public write without an entry is an error. Adding a schema without an
owned operation is also an error unless it is explicitly marked read-only or
historical.

## Validation boundary

Create `epiphany.application.validation` as the only public validation gateway
for application contracts. Its responsibilities are:

- Resolve a named schema from `epiphany.law.registry`.
- Validate schema version where applicable.
- Raise a stable domain error with a safe explanation.
- Avoid putting raw document content, repository text, credentials, or paths
  beyond the necessary diagnostic in returned error data.

```clojure
(defn validate! [schema-name value]
  (if (registry/valid? schema-name value)
    value
    (throw (ex-info "Schema validation failed"
                    {:code :schema-validation-failed
                     :schema/name schema-name
                     :schema/explain (registry/explain schema-name value)}))))
```

The public error category is stable. The rendered explanation and redaction
policy are infrastructure concerns.

### Write path

Every public `:record-*` operation validates before any observable mutation.
The application composition root wraps raw ports in validating ports. Each
durable adapter also validates locally before encoding/writing as defense in
depth for direct use, scripts, and future callers.

```text
HTTP or CLI decoder
  -> command schema
  -> application service
  -> validating port wrapper
  -> adapter-local validation
  -> document encoding and durable write
```

A rejected write must leave state unchanged. This is a port-law requirement,
not an implementation detail.

### Read path

Every external decode validates after conversion into the domain map:

```text
Mongo document / Git observation / backup payload
  -> decode
  -> claimed-schema validation
  -> application/domain value
```

Malformed stored data, an unsupported future schema, and a dependency outage
are distinct outcomes. None may become an empty result.

## Reference adapter

The in-memory adapter is a reference implementation for the shared port
contract. It uses simple atom-backed storage but validates all commands and
records through the same operation registry. It enforces:

- Required schema and version validation.
- Idempotency behavior and conflict detection.
- Declared uniqueness and append-only rules.
- Rejection without mutation.
- Deterministic ordering where a port contract requires ordering.

It does not emulate BSON representation, Mongo transactions, indexes, query
planning, or network errors. Those belong to durable-adapter integration tests.

## Adapter law harness

Provide a reusable law-suite constructor, supplied an adapter factory and
capability declaration:

```clojure
(defn observations-laws [{:keys [make-port capabilities]}]
  ;; valid writes, invalid writes, no-state-change, idempotency,
  ;; conflict, read/decode normalization, and export/import laws
  )
```

The same law suite runs against the in-memory reference adapter and Mongo.
Tests compare normalized domain outcomes, not driver exceptions, generated
object IDs, raw BSON, timestamps, or incidental map order.

For reproducible comparisons, test composition injects `:now` and `:new-id`.
Production composition provides real time and ID sources. Domain code should
receive these capabilities rather than constructing `java.util.Date` or UUIDs
inside decision functions.

## Domain properties

Use examples for named regressions and property/metamorphic tests for stable
laws. Generated tests must print a replay seed and preserve a minimal failing
case when possible.

Required initial laws:

- Valid closed records become invalid when an undeclared key is added.
- Replacing required UUIDs, exact paths, or schema versions with incompatible
  values rejects the record.
- Replaying an equivalent request ID is idempotent; changing material content
  under that request ID is a conflict.
- Export then import then export yields equivalent canonical observation data.
- A corrupt, truncated, unrecognized, or future-version backup fails before
  it mutates durable state.
- Equivalent CLI and HTTP requests decode to the same application command and
  produce the same normalized outcome category.
- Search limit expansion preserves the ranked prefix, where the search
  contract claims deterministic ordering.
- Similarity-derived continuity stays provisional until an explicit accepted
  review decision exists.

## Static and structural checks

Static analysis has two jobs: general Clojure correctness and Epiphany-specific
architecture enforcement.

General linting covers unresolved symbols, invalid arity, unused bindings,
unused namespaces, duplicate requires, formatting, and suspicious forms.
Style/shape linting is advisory at first and may become a ratcheted required
check only for reviewed rules.

A project checker enforces these hard boundaries:

- `law` depends on neither `domain`, `application`, nor `infra`.
- `domain` and `application` do not require `infra`.
- `infra.http` and `infra.main` call application services/ports, not Mongo,
  Lucene, or Git adapters directly.
- Public persistence writes appear in the operation registry and pass the
  validation wrapper.
- Direct Java interop is prohibited in `law`, `domain`, and `application`
  except for an explicitly approved and recorded exception.

## Interop inventory

Track source-level Java interop by namespace and category: imports, type hints,
constructors/static calls, and dot calls. The inventory is a CI artifact and
its baseline is reviewed rather than silently reset.

The policy is a ratchet:

- No new unapproved direct interop in `law`, `domain`, or `application`.
- Infrastructure adapter interop is permitted, but new concentrated use needs
  an adapter-level test and a reason in the change evidence.
- CLI and HTTP are translation layers; their interop should remain shallow.

Runtime measurements instrument owned port crossings—Mongo, Lucene, Git,
filesystem, and embedding calls—with count and duration. This measures
architecturally relevant transitions, unlike an attempt to count every JVM
method dispatch.

## Mutation strategy

Mutation testing is targeted at pure and boundary-critical namespaces, not a
whole-repository score contest. Required mutants include removal of validation,
wrong schema selection, required-to-optional changes, bypassed conflict logic,
and collapse of unavailable/corrupt/not-implemented outcomes into empty data.

A surviving mutation is classified as one of: test gap, equivalent mutation,
irrelevant mutation, or tool limitation. Only an explicitly reviewed
classification can close it. The validation gateway and operation registry have
zero tolerated surviving meaningful mutants.

## CI matrix

Fast PR checks run deterministic and inexpensive gates. Durable and adversarial
checks run either on every merge queue or on a scheduled/approved workflow,
depending on their duration.

| Job | Scope | Failure blocks merge |
|---|---|---|
| `static` | Lint, formatting, architecture and interop ratchet | Yes |
| `unit-contract` | Unit, schemas, in-memory law suite, properties | Yes |
| `integration-contract` | Mongo and relevant real adapter law suites | Yes for persistence changes |
| `interface-parity` | CLI/HTTP decode and normalized outcome parity | Yes when interfaces change |
| `mutation` | Boundary-critical namespace mutation tests | Merge queue or scheduled; required for affected critical changes |
| `adversarial` | Private/rotated seeds, malformed inputs, regressions | Protected CI; required before release |

A private test failure must disclose enough seed, case identifier, and redacted
failure information for maintainers to reproduce it without exposing private
fixtures to untrusted contexts.

## CI evidence artifact

CI produces an EDN or JSON artifact per checked revision. The artifact records
observed execution, not a self-authored assertion that work is complete.

```clojure
{:revision "<git-oid>"
 :commands [{:name :unit-contract :exit 0}
            {:name :integration-contract :exit 0}]
 :properties [{:name :backup-round-trip :seed 814296 :trials 1000}]
 :adapters [:in-memory :mongo]
 :mutation {:targeted 37 :killed 37 :survived []}
 :interop {:baseline-revision "<git-oid>"
           :delta {:domain/dot-calls 0}}
 :generated-at "<instant>"}
```

This artifact is evidence of a particular run. It is not an observation of
Git history or a replacement for an accepted human review decision.

## Implementation slices

Each slice must remain at or below the board's implementation-size limit.

1. **Schema operation registry** — define named operation/schema/version data
   and completeness tests.
2. **Validating port wrapper** — enforce pre-mutation validation and stable
   schema errors for in-memory operations.
3. **Reference adapter parity** — add idempotency, uniqueness, and
   rejection-without-mutation rules.
4. **Mongo contract alignment** — apply the same validation and run the shared
   law suite against ephemeral Mongo.
5. **Read/import validation** — validate Mongo decodes and backup payloads;
   distinguish corruption, unsupported version, and unavailability.
6. **CLI/HTTP command contract** — extract common validated commands and add
   parity tests.
7. **Static boundary gate** — add linting, dependency checks, and the initial
   interop inventory baseline.
8. **Generative and metamorphic laws** — add generators and replayable seeds
   for identity, idempotency, backup, and epistemic-state laws.
9. **Mutation pilot** — evaluate a Clojure-CLI-compatible tool on validation
   gateway and registry; retain it only if it identifies deliberately removed
   validation.
10. **CI evidence and private evaluation** — generate the artifact, then add
    protected rotated cases after public laws are stable.

## Acceptance evidence

The design is implemented only when:

- Every public persistence operation is registry-covered and validates before
  mutation in both reference and durable adapters.
- The shared law harness proves rejection and state preservation for in-memory
  and Mongo adapters.
- CI has required static, contract, and applicable integration gates.
- At least one property suite and one targeted mutation suite exercise a
  boundary-critical contract.
- CLI and HTTP use a shared command contract where both interfaces exist.
- CI produces a reviewable evidence artifact for the checked revision.
- A maintainer can distinguish a passing public test, a passing private test,
  and an explicit human acceptance decision without conflating their tiers.
