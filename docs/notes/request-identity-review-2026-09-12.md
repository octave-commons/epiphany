---
slug: request-identity-review-2026-09-12
uuid: c1af4b79-fb4f-4c1d-a8f5-fc9f7afc3c04
kind: report
status: open
description: "Explicit command IDs, HTTP profile parity and durable retry review repairs."
labels: [clio, review, identity, verification]
---

# Explicit command identity and profile review

The isolated source base is `81ddbce198e0963e270ddc2b0532c58db57e6f93`.
Its tree `ee1b6aea670ac107f9d9cd78647288cab4e6168b` matches published
`a9166b2db3e272fe025339de72e28aab43267510`, verified against the actual GitHub
commit. The shared Foresight checkout and remote branch were not changed by this
repair. The dependency remains canonical Clio at
`2b7bfbefa580d512262ca18f9163ecba43e54cc5`; test/static commands disclose their
explicit local override. The shipped build, CLI and native launcher resolve the
existing declared sibling path to that same immutable checkout.

| Actual review | Repaired behavior |
| --- | --- |
| [Codex 3997282605](https://github.com/octave-commons/epiphany/pull/18#discussion_r3997282605) | HTTP header and percent-encoded query profiles accept the documented leading colon. Explicit configured-provider dispatch and invalid-profile refusals remain. |
| [CodeRabbit 3997300601](https://github.com/octave-commons/epiphany/pull/18#discussion_r3997300601) | Registration requires a caller-owned UUID in the named command schema and at direct application entry. CLI/HTTP do not manufacture defaults. Invalid requests reach no Git, metadata or observation port. |
| [CodeRabbit review 5187731466, candidate seeding](https://github.com/octave-commons/epiphany/pull/18#pullrequestreview-5187731466) | Durable diff seeding requires `--request-id`; retries return the original persisted candidate UUID. Changed relation or evidence under the same ID is refused. Read-only diff needs no ID. |
| [The same review, workflow credentials](https://github.com/octave-commons/epiphany/pull/18#pullrequestreview-5187731466) | Workflow permissions grant only contents read; all four checkout steps disable persisted credentials. Existing checkout depth and execution steps remain. |

The first actual behavioral RED had **6 tests, 51 assertions, 26 failures and no
errors**. An initial focused repair passed **6/52**. The first broader run found
one successful raw-EDN fixture that still omitted the newly required ID:
**809/2544, one failure**. That fixture and other successful-call fixtures now
supply explicit IDs; their original behavioral assertions remain.

Independent review then checked the retry paths themselves. The new candidate
read shortcut needed the adapter's no-op write to reaffirm durability after an
uncertain force. A real Clio ledger with injected force refusal produced
**1 test, 3 assertions, 2 failures** before that adjustment. A misplaced require
in the first attempt caused a separate test compilation error; it was corrected
before recording this behavioral RED.

Registration had two older retry gaps: it accepted a different path under the
same UUID and returned before the durability fence. A real Clio regression
produced **1 test, 5 assertions, 3 failures**. The retry now compares the exact
persisted path before calling the no-op writer. Both registration and candidate
retries return the stored identity, retain unchanged history and propagate a
failed durability fence. The final read-only peer found no remaining concrete
issue in these boundaries.

The final full unit suite passed **811 tests, 2552 assertions, zero failures**.
Configured full lint reported **zero errors and zero warnings**; formatting,
layer boundaries and the interop ratchet passed. The actual `bin/build` and
shipped `bin/ep --help` both exited zero. Parsed YAML checks confirmed all four
checkout settings and the workflow-level permissions. A first formatting command
incorrectly appended `fix` to the alias's built-in `check`; an explicit temporary
alias using the same pinned formatter corrected only the owned files. No tool
version, dependency policy or warning suppression changed.

The unchanged canonical native gate completed real separate-JVM replay, lexical
search, incremental ingestion and concurrent durable writes. Its functional
checks passed, but **the process gate exited 1** because the current Lucene/JDK
combination emitted native-access and vector-module warnings. The original
streams and failure status are retained. The previously attempted supported
flags and remaining incubator-module warning conflict are documented in
[the earlier warning investigation](clio-restore-review-2026-09-12.md). This
repair neither suppresses diagnostics nor claims a passing native warning gate.

[Exact commands, final source hashes and raw receipts](evidence/request-identity-review.json)
retain every final gate and the behavioral failures. Earlier native or service
receipts remain attached to their original source.
