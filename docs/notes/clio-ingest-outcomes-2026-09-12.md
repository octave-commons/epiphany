---
slug: clio-ingest-outcomes-2026-09-12
uuid: 1695a730-30db-433b-bec0-c69056511826
kind: report
status: open
description: "Failure-first recovery of truthful concurrent ingestion outcomes and Clio runtime durability."
labels: [clio, development, verification, ingestion]
---

# Concurrent ingestion admission

Actual [Codex finding 3996503279](https://github.com/octave-commons/epiphany/pull/18#discussion_r3996503279)
identified a valid bug at restored source
`7c157fb3a824cf6ff23205486ccf8e285ea78fba`: two overlapping ingestion operations
could propose the same revisions and extractions before either locked append.
Clio deduplicated persistence, but both callers counted their proposals, and the
loser's discarded extraction UUID still reached the index.

Three new native tests use a literal committed Git repository and independent
Clio adapter handles. A barrier makes both readers capture the empty relevant
snapshot before either writes; no storage result is mocked. The initial result
was **3 tests / 13 assertions / 5 failures**: revision overcount, extraction
overcount, two index inconsistencies, and loss of an accepted count after an
index failure. The last case independently confirms that the canonical record
already exists when its disposable index refuses.

Direct Clio observation writes now return an explicit transient
`{:observation/write-status :accepted}` or `:duplicate` acknowledgement only
after the locked write or durability fence succeeds. Every direct record method
participates. A registered Malli contract validates the complete result before
consumers count or index it. Existing providers' nil return retains their earlier
acknowledgement semantics. Clear/import return values and persisted operation
`:result nil` remain unchanged; old events are replayed under their original
contract. Strict Clio law tests require exact accepted/duplicate outcomes, while
every original observation law remains active.

Revision counts now measure acknowledged new records. A losing extraction does
not enter the index. A newly accepted extraction remains counted if its index
operation fails, with an explicit `index-failed` record. This does not add an
automatic index-repair command: the existing ingestion filter skips already
accepted extractions on retry. Durable accepted facts remain available for a
separate rebuild, and missing-index repair remains a follow-up rather than a
claimed feature of this correction.

The combined slice also adopts the Clio runtime's snapshot-before-schema and
reopen durability helpers; its separate native **8 failures → 2 tests / 18
assertions passing** evidence is in
[the runtime adoption report](clio-runtime-adoption-2026-09-12.md). The immutable
dependency is `ff48f090965be5c17cca1e3662d7402b383510c1`, now also pinned by the
standalone fetch script. The original advertised fetch command successfully
materialized that exact sparse checkout. No root gitlink was promoted.

## Verification and obstacles

The initial focused combined result was **10 tests / 39 assertions / 0 failures**.
An intermediate wider adapter run occurred during fixture migration and failed
old nil-result expectations; it is recorded by the runtime report and is not
counted as green coverage. New result-contract cases also reject malformed and
ambiguous acknowledgements and verify legacy nil compatibility.

The first full unit run reached **796 tests / 2316 assertions / 1 failure**. Its
interop ratchet caught a new Java exception type hint in the shared failure
formatter. Replacing the Java accessor and hint with Clojure core `ex-message`
removed that direct interop; no baseline, warning level, or test was suppressed.
The final advertised unit run passed **796 tests / 2316 assertions / 0 failures**.
Configured lint passed **0 errors / 0 warnings**, formatting and layer boundaries
passed, and the final JVM AOT build passed.

The actual shipped `bin/verify-clio-edn` proof passed registration, ingestion,
Lucene lexical search, repeated ingestion with zero newly stored records, and
two independent JVMs appending and reopening durable observations. This proof
completed before the final `ex-message` cleanup; its admission/persistence code
was unchanged. The final full unit and AOT checks cover the cleanup. No stronger
whole-source claim is transferred between those receipts.

The native CLI run emitted the previously recorded Java 21/Lucene startup
advisories about native linker access and the unavailable Vector API. Its exit
and functional proof passed; its total output was not warning-free. The raw
warnings remain in the committed transcript. The unit, configured lint,
formatting, boundary and AOT results above are separate receipts and do not
silently erase those runtime advisories.

All Clojure commands selected the immutable Clio checkout explicitly through
`-Sdeps`; the native shell script used a task-local launcher adding that same
override, preserving the repository's script body. Java 21, CLI 1.12.5.1654,
the project's dependency pins and original gates remained in use. Fresh Maven
dependencies were restored through per-session proxy settings and the dedicated
truststore with TLS verification enabled.

Exact commands/logs, source digest and checksums are in
[clio-ingest-outcomes.json](evidence/clio-ingest-outcomes.json). The real Mongo,
S3rver and MiniLM service run requires a clean commit and remains pending at
this checkpoint. Canonical review acceptance, remote review convergence and
root promotion are separate unfinished steps. Scoped peer review found no
confirmed implementation blocker and requested the acknowledgement/index-repair
limitations above be stated explicitly.

## Follow-up: actual services and repaired board projection

The clean checkpoint `0e3950bf2d54043203885f12ccbbd2836a5710fe`
(tree `20664b20df0f91afa22b8668556d8560e94078e9`) subsequently passed the
actual root service supervisor against immutable Clio `ff48f090`: Mongo's
command-protocol ping, S3rver create/put/get preserving literal EDN bytes, and
the JVM service suite **22 tests / 108 assertions / 0 failures / no skips**.
Embedding used real `Xenova/all-MiniLM-L6-v2`, 384 dimensions, pinned revision
`751bff37182d3f1213fa05d7196b954e230abad9` and model artifact
`sha256:afdb6f1a0e45b715d0bb9b11772f032c399babd23bfc31fed1c170afc848bdb1`.
The service suite transcript contains no warnings. Its exact source digest and
argv are retained in `evidence/ingest-outcomes-service-integration-02.json`;
that receipt describes the checkpoint above, not later documentation changes.

The first service attempt failed before reaching the JVM suite: Mongo never
answered its ping. Inspection found the old runtime path again held a
154,213,376-byte partial executable, SHA-256
`df5aa165038ea3f576b01c36afa918b89d77693b8f59ff0e1aea3eca1643adca`, despite an
earlier recorded complete installation. The cause of that path's changed
contents is unknown; this observation does not establish pruning or external
interference. The failed attempt is preserved separately and is not counted as
test coverage.

Recovery extracted the verified official MongoDB 8.0.13 archive into a new
unique task-owned path, awaited extraction, verified the executable checksum,
and started services sequentially in the same supervisor. The executable at
`target/verified-mongo.89vmmkFa/mongod` remained 219,690,440 bytes with SHA-256
`6262c72cd9697832100641bce3613c45909f4afdf67ca2efb54b76be7da0f078` before and
after the successful run. Task activation now selects that verified path. The
earlier failed binary remains untouched; a previously rejected cleanup was not
retried. Every service process and client ran within the same network namespace.

Self-review also found that the board snapshot in `0e3950bf` lost plain YAML
labels while retaining all cards. The old eta-mu router resolved its old Rheos
companion, which predates the reviewed plain/mixed label parser. For example,
the byte-identical Epic 10 card declares `[graph, relationships, code, provenance]`,
but that generated snapshot held `[]`. Publication was held. No canonical card
or ledger was rewritten to repair a derived projection.

The corrected direct Rheos CLI from source
`c94e789070e15a4f7c9da51baa50ed520a6d1203`, executable SHA-256
`35eb33a238f4a869091302b1ec0c7aa7a46f7a2b0ab9f069899ee6539d5795d4`, regenerated
the snapshot with the actual project configuration. All 116 Markdown input
hashes were unchanged, all 116 tasks and 12 columns remained, and every task's
labels matched the original `7c157fb3` snapshot. Exact RED/GREEN and input hashes
are retained under `evidence/ingest-outcomes-board-*`. A subsequent canonical
progress comment records these results and regenerates the snapshot again.
The failed snapshot remains visible in Git history; it is not described as a
clean verification receipt. Remote review convergence and acceptance remain
separate from these observed local results.
