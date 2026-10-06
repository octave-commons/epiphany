---
slug: clio-restore-review-2026-09-12
uuid: 5221f1f5-8df4-4eeb-bd83-94692127bd07
kind: report
status: in_progress
description: "Failure-first restore retry and command fixes, with explicit JVM warning gate evidence."
labels: [clio, backup, review, evidence]
---

# PR 18 restore and command review

The source base is published Epiphany `b7e0832fb846357e82d30494b7541e593d9ebcbe`,
tree `143ae8e49d2d4897ac8a076097059417ee672c6e`. Work is isolated in
`epiphany-review-restored`; the root checkout and its earlier service proof were
left untouched. Source compilation uses the explicit local dependency override
`eta-mu/clio` at clean combined revision
`2b7bfbefa580d512262ca18f9163ecba43e54cc5`, package `packages/clio`.

| Actual finding | Repair and verification |
| --- | --- |
| Codex `3997047801`, original backup overwritten on retry | The first identified snapshot is complete, forced, exclusively published and command-bound before clear. Retry reuses and re-forces it. A real Clio clear followed by injected import failure and a reopened handle lost three assertions on the old source; the repaired retry restores the original observation and leaves original backup bytes unchanged. |
| Codex `3997047802`, native proof accepts warnings | The canonical executable now wraps the entire native proof in a gate that retains both streams and command status, and independently fails on a warning. Actual incubator/native warning spellings on either stream are covered. The Lucene/JDK configuration blocker is retained below. |
| CodeRabbit `3997056204`, observation policy in shape | `write-invocation` moved unchanged to `epiphany.domain.observation-invocation`; its adapter caller and existing clear-command tests retain the same operation/result contract. |
| CodeRabbit outside-diff profile parsing | All nine profile option sets accept plain and colon-prefixed spellings; eight previously failed. The actual option tables produced 32 failing assertions before the fix. |
| CodeRabbit outside-diff generated registration request ID | CLI and HTTP assign the ID before decoding. Registration preserves that ID in its result and observation. A real JGit fixture and Clio ledger prove the generated ID is visible and explicit retry leaves history unchanged; four assertions failed before repair. |

The first new test attempt also contained one fixture error: it called an absent
`:list-repository-locations` port. Replacing that probe with the real `:export-all`
contract removed the fixture error before recording the actual RED result:
**3 tests, 121 assertions, 39 failures, 0 errors**. No production source changed
between those two RED attempts.

An initial repaired focused run passed **35 tests, 243 assertions, 0 failures**.
Additional native tests then proved a failed snapshot durability fence prevents
clear, retry preserves the already-published bytes, a different UUID or legacy
drill cannot overwrite an identified snapshot, and malformed/nil EDN cannot turn
into apparent absence. The full final unit suite passed **803 tests, 2489
assertions, 0 failures**. The pinned lint, formatting, boundary and interop gates
all returned zero; AOT compiled the shipped JVM entry point.

The first full suite had a separate native startup warning: another execution's
JVM already owned `/tmp/hsperfdata_root/26`. We left that file untouched. The
installed Temurin 21.0.12.1 accepts `-XX:+PerfDisableSharedMem`; actual flag output
shows that setting true and `UsePerfData` still true. OpenJDK's
[flag declaration](https://github.com/openjdk/jdk21u/blob/master/src/hotspot/share/runtime/globals.hpp)
and [POSIX implementation](https://github.com/openjdk/jdk21u/blob/master/src/hotspot/os/posix/perfMemory_posix.cpp)
confirm private standard memory replaces shared performance-data memory. The
full suite rerun with that scoped setting passed the same **803/2489** with no
warning. External tools that require the shared jvmstat file lose that attachment
path; counters and application/vector behavior stay enabled. This is an explicit
resource-ownership setting, not a diagnostic filter. The earlier attempted Linux
source-file URL returned 404; the current POSIX path above succeeded.

The supported Lucene flags `--enable-native-access=ALL-UNNAMED` and
`--add-modules=jdk.incubator.vector` produced an actual SIMD dot product of 64.0,
512-bit preferred vectors and enabled FMA. They still emitted the JDK warning
`Using incubator modules`. The Lucene 10.5 provider bytecode was also inspected:
absence of that module explicitly emits its own warning and chooses the default
provider. Changing logging levels, filtering stderr, disabling vectors, test-only
hooks or a different JDK were not used. A supported zero-warning native Lucene
configuration has **not** been established; a nonzero process gate is the honest
outcome until that blocker is resolved.

Root explicitly authorized moving `restore-drill` to `epiphany.infra.backup` after
reviewing the static rule and STYLE.md prohibition on domain-to-infra dependencies.
A same-name forwarding facade or dynamic namespace resolution would violate that
rule. Its only two preexisting callers were internal tests, now migrated. Both
arities and the report shape remain; the four-argument form requires an actual
UUID. Existing public backup payload validation and export/import names remain.
The new native file effects are in `epiphany.extern.backup-files`; command binding
and ownership decisions stay in domain.

An independent read-only Clio-lane review inspected those three files and the
underlying filesystem functions. It found no confirmed durability, data-loss or
report-shape regression. This scoped result is not a substitute for current remote
Codex, eta-mu and CodeRabbit review or final acceptance.

The canonical Rheos CLI appended the scoped work record and regenerated the
board. All 116 cards and 12 columns remain; every label is unchanged. Apart from
generated timestamps and this checkout's source paths, only the assigned card's
content changed. The generated board was not hand-edited.

Raw RED, initial/full reruns, lint, formatting, boundary, interop, AOT and native
configuration receipts accompany this report under `evidence/restore-review-*`.
The native process and service integration follow-up must cite a committed clean
candidate separately; the earlier root integration result is not reassigned to
this source.

## Committed-candidate process follow-up

All following runs used clean candidate
`2796d24fa2ca5e5023c0c4e41c653f8f8ad466f8`, tree
`164c2de05abae5e7e54f74be63c96b90b2cb01b1`, and the unchanged combined Clio
revision recorded above. This follow-up changes documentation and evidence only.

The canonical `bin/verify-clio-edn` completed its real separate-JVM replay,
lexical search, incremental ingestion and concurrent durable-write assertions.
It then correctly exited **1** because native linker and Lucene vector-module
warnings were emitted. The companion operations still completed; their final
PASS is not relabeled as a passing zero-warning gate.

A second actual JVM probe used both documented Lucene launch flags and the scoped
performance-memory setting. MMap wrote and read integer 42, and the SIMD dot
product returned 64.0. Only the JDK incubator warning remained, and the wrapper
again correctly exited **1**. An independent identity-lane reviewer inspected the
matching installed `java.base/jdk/internal/module/ModuleBootstrap.java`: startup
calls `checkIncubatingStatus`, its `WARN_INCUBATING` branch emits the diagnostic,
and archived boot layers exclude incubator modules. The installed Lucene provider
also warns when that module is absent. No supported warning-free configuration
for this same-JDK classpath launch was found. This limitation is corroborated by
native execution and source inspection, not an assertion about all conceivable
future launchers or versions.

The first real service launch reached Mongo's ping and S3's byte-for-byte EDN
roundtrip, then stopped before JVM tests. Clojure needed the declared sibling
`../eta-mu/packages/clio` during initial discovery even with the supervisor's
override alias. It was absent in the isolated worktree layout. No `result.json`
was published by that failed run. Root authorized creating only the missing
shared sibling symlink to the clean, verified combined checkout; no path was
replaced, source copied, root pin promoted, or dependency cache duplicated.

The unchanged supervisor, SHA-256
`3555b6d5d899381844641f2a18675743b058c1c87aab7ac903614f94986320ee`, then passed
the retry with **22 tests, 108 assertions, 0 failures**, no skips and no warning.
It used native Mongo, S3rver and the real cached `Xenova/all-MiniLM-L6-v2` model at
revision `751bff37182d3f1213fa05d7196b954e230abad9`, producing 384-dimensional
embeddings. It observed child-process closure with exit zero and completed owned
cleanup before publishing evidence. The 506-file clean Epiphany source digest
`1c7b0899d14c7e906fe28f27c9660ffd8a027f097df3fb93b9e7eed4e0b28419` and the Clio
revision stayed unchanged throughout. Mongo's executable SHA-256 stayed
`6262c72cd9697832100641bce3613c45909f4afdf67ca2efb54b76be7da0f078` before and
after both attempts. The original failed Mongo directory and binary were never
touched.

An earlier source-fetch attempt over the configured SSH remote failed DNS
resolution. Fetching the exact published commit over HTTPS succeeded without
changing the remote. Initial source inspection used an obsolete OpenJDK Linux
path that returned 404, then the POSIX implementation path succeeded; raw web
access also failed, so the successful direct official-source download and the
matching installed source supplied the evidence.

The raw native outcomes, failed service attempt, successful integration transcript,
supervisor receipt and SHA manifest are committed as
`evidence/restore-review-process-outcomes.json` and its referenced receipts.
The implementation defects are repaired and the actual service stack passes.
The native zero-warning gate remains an explicit unresolved acceptance blocker;
this report does not grant approval, move the card to done, or claim current
remote reviewers have exhausted their findings.
