---
slug: clio-edn-development
uuid: 21d8a09a-b7f0-4cec-81fc-76e18c79e4a2
kind: design
status: implemented
description: "Durable local observations through the canonical Clio event kernel."
labels: [development, clio, persistence]
requires-decisions: ["ADR-000", "ADR-001"]
dependencies: []
dependendents: []
epics: []
---

# Clio EDN development profile

## Restore retry and command review follow-up

The programmatic restore entry point is now `epiphany.infra.backup/restore-drill`,
migrated from `epiphany.domain.backup/restore-drill` because effect orchestration
cannot depend upward from the pure domain layer. Its three-argument legacy clear
contract and four-argument UUID contract remain. Existing payload validation and
file export/import functions remain at their existing public names. The two
internal restore callers were migrated; historical review records remain intact.

An identified drill durably retains the first snapshot in `backup.edn`, with its
`:restore/command-id`, before clearing observations. A failed import can be retried
with the same directory and UUID after reopening the ledger. The original bytes
are reused and synchronized again. A different command, or an unidentified legacy
drill, cannot replace that backup. Use a new directory and UUID for a new drill.
The directory lock spans the full drill; the original snapshot is published by
an exclusive hard link after forcing a complete temporary file. Corrupt snapshots
and failed synchronization stop before clear. Clio clear retries keep their
existing event semantics and never rewrite history.

All profile options accept both `edn` and `:edn` spellings, likewise for `local`
and `services`. Registration generates one request ID before CLI/HTTP command
decoding when the caller omits one, and returns the same ID stored on its
observation. Retry with the returned `--request-id`; a new invocation without
that ID represents a new command, not an inferred identity match.

`bin/verify-clio-edn` now runs the full native proof through
`bin/with-zero-warnings`. It preserves stdout, stderr and nonzero command status;
any warning also fails the gate. The operations companion is not a substitute
for the gated entry point. Current JDK 21 and Lucene 10.5 require explicit warning
disclosure: `--enable-native-access=ALL-UNNAMED` and
`--add-modules=jdk.incubator.vector` enable the documented native/vector paths,
but the JDK itself prints `WARNING: Using incubator modules: jdk.incubator.vector`.
An actual 32-element SIMD dot product returned 64.0 with 512-bit vectors and FMA
enabled while that warning remained. Omitting the vector module instead produces
Lucene's fallback warning. No diagnostic suppression, disabled vector capability,
or changed JDK is used to manufacture a passing zero-warning result. Until a
supported warning-free configuration is demonstrated, this process gate remains
a blocker even when its functional checks pass. The configuration guidance is
documented by [Lucene VectorUtil](https://lucene.apache.org/core/10_5_0/core/org/apache/lucene/util/VectorUtil.html)
and [MMapDirectory](https://lucene.apache.org/core/10_5_0/core/org/apache/lucene/store/MMapDirectory.html).

The user explicitly authorized a development provider that removes Mongo as a
prerequisite while retaining existing providers. This scoped decision extends
ADR-000's earlier Mongo durability wording: Git remains canonical for source,
Clio events become canonical for observations in the selected `:edn` profile,
and Lucene remains a rebuildable index. No production data is migrated.

The observation port already supplies finite, validated operations and an
in-memory reference implementation. The Clio adapter reconstructs that reference
from accepted operations on every read and write. It appends only a successful
operation that changes reference state. Rejected commands and no-op retries do
not append. Clear and import operations change the projection through new events;
they never edit or erase the ledger. Existing per-method retry semantics remain
the authority; this does not invent a Mongo collection interpreter.

`epiphany.law.clio-observations` declares the finite invocation set and versioned
event catalog. `epiphany.extern.clio-observations` owns directory configuration
and the JVM transaction lock. `epiphany.infra.adapters.clio` owns replay and port
composition. All events and schema snapshots use `eta-mu/packages/clio`.
The sibling dependency is deliberate: Foresight shares one source checkout.
Standalone CI fetches only that package at the explicit revision in
`bin/fetch-clio-source`; it refuses to replace an existing sibling checkout.

Each complete read/decision/append cycle holds a separate OS-backed lock file.
Clio independently validates and locks the canonical ledger during append.
New revision observations are admitted by their resource, commit and exact path
identity while the operation lock is held. Repeated imports remove identical
accepted facts before staging; changed-content reuse and malformed duplicates
are rejected. This admission affects new commands only, so historical operation
replay retains its original meaning. A logical no-op calls Clio's durability
barrier before returning: visible bytes from a previously failed force are not
treated as proof that a retry has become durable.
Direct record writes now return an explicit transient
`{:observation/write-status :accepted}` or `:duplicate` result after that locked
admission and durability fence. Revision and extraction counters consume this
result, and a deduplicated extraction's discarded UUID never enters the index.
Legacy adapters retain their existing nil acknowledgement contract. The durable
operation event still records the reference implementation's nil result, so
historical replay and schema identities retain their meaning. A later index
failure remains observable without retracting the successful observation count.
Readers rebuild from full historical schema snapshots and verify each recorded
result, canonical before/after state hashes and state change. Missing history, malformed events, absent historical
schemas and semantic replay conflicts fail visibly. Opening a store validates
old history before materializing today's schema, preventing a missing historical
snapshot from being silently regenerated. There is no fallback to empty memory.

The profile also uses the existing Git-local identity file and durable Lucene
adapter. Lexical registration, ingestion, search, status and review storage need
no Mongo or inference process. Semantic operations still require a configured
real embedding provider; EDN storage does not fabricate embeddings. This recovery
does not claim restoration of the lost historical optional embedding adapter.

HTTP servers carry their configured default profile. A request selecting another
profile is dispatched only when that profile has explicitly supplied adapters;
otherwise it receives an unavailable response before any handler uses storage.
The server never acknowledges an EDN selection while writing through another
provider. JGit resolves normal, bare and linked-worktree common directories for
registration and CLI/HTTP parity without requiring a Git command subprocess.

This intentionally favors inspectability over speed. Replaying and validating
the whole ledger on every call is unsuitable for large production workloads.
Store directories must remain on a filesystem supported by Clio's explicit
locking and durability contract. An I/O refusal propagates and never publishes
the disposable staged projection.
