---
slug: clio-review-recovery-2026-09-12
uuid: 69041f7a-5faa-4ba1-a47b-4e8e1ebf4c63
kind: report
status: open
description: "Review-driven admission, profile routing and immutable Clio verification."
labels: [development, clio, verification]
---

# Clio review recovery

## Current extraction and clear command correction

Actual Codex 3996355207 found that concurrent ingestion commands could persist
the same section extraction under independently generated observation IDs.
Prospective admission now keys an extraction by resource, accepted revision,
blob and extractor version. Accepted observation/request IDs also remain bound
to their content. Equivalent retries ignore regenerated command envelopes and
timestamps; a changed payload refuses before append. A new extractor version
with new envelope IDs creates a new fact. Historical replay retains the original
facts, including any older duplicates. Every envelope ID in those accepted facts
is indexed when comparing later writes. IDs from an unpersisted duplicate retry
are not claimed as durable reservations.

The actual concurrent regression first failed six of eleven assertions; the
corrected focused adapter suite passed 18 tests / 114 assertions. A subsequent
self-review found that equivalent historical duplicates could lose secondary
envelope aliases during admission indexing. Both failure-first assertions failed;
the reduction now keeps those aliases, including comparisons within one import
batch. The recursive Markdown regression also now changes the actual dispatch
key `:block/type` rather than adding an unrelated unknown key.

CodeRabbit's clear-command finding reproduced an actual lost-acknowledgement
hazard: a second zero-argument clear removed a later observation and appended
another event. Both assertions failed. Prospective EDN clears now require one
caller UUID, stored as `:command-id` in canonical event data. Reuse that UUID
on retry. The locked admission decision binds it to operation and arguments,
returns without re-executing an accepted clear, and still forces durability.
An initially empty clear records its command too, so its later retry cannot
erase intervening writes. Invalid IDs or extra command material refuse before
mutation. Replay rejects duplicate accepted command IDs.

The event catalog adds an optional command field while retaining old schema
snapshots. Identified clears may preserve the same before/after state; historical
unidentified operations retain the original state-change requirement. A fixture
contains the exact catalog evaluated from committed `3156a5d652d4771b64406627821c8c06c42413fa`.
The compatibility test writes through that old runtime, then checks that the
new reader preserves both original ledger and schema bytes. The first version
of this additional fixture omitted its owned parent directory and failed before
writing; explicitly creating the directory repaired that harness error.

The explicit durable drill call is
`(backup/restore-drill observations git backup-directory command-id)`.
The historical three-argument drill remains available for legacy in-memory and
Mongo adapters. EDN refuses a missing command ID. Focused regressions cover clear
retry after another write, empty-first-clear, restart, failed fsync, altered
command arguments, duplicate command corruption and the real backup drill.

The standalone dependency pin is independently fetched Clio
`6c5af6077d069620583b29a180eb925a0805e94b`, tree
`c590a1ce1abbf970a4263c70f6e39f56cff434aa`, which adds shared read locks for
read-only ledgers. Fresh complete gates for this source and kernel are recorded
separately below; earlier results do not transfer to this revision.

### Exact verification and self-review

The complete application checkpoint
`c433d63f43cefd626c1d17152cc5cb4be3c6eeb5`, tree
`a71c00d1746afb643e0ba5de7249202b7baa4152`, passed **790 unit tests / 2,273
assertions**, lint with zero errors/warnings, formatting, boundary and interop
checks, AOT build, the separate-JVM Clio/Lucene process proof, and the actual
shipped launcher. The real Mongo/S3rver/MiniLM integration also passed **22 tests /
108 assertions**, with no skips and matching digests for all 455 tracked source
files and symlink targets.

Self-review then moved only invocation normalization into
`shape.observation-invocation`, leaving the law as a validator returning the
original arguments. Final source `4c40cb93c689f328782671eb512b405be64aa5f7`, tree
`2d0ebee0bcee3c540437f551e2a4989624858518`, passed the unchanged focused **33 tests /
186 assertions**, all static gates, a fresh AOT build and actual launcher.
The real service integration was repeated on this final source and passed
**22 tests / 108 assertions**, no skips, with matching digests for all 456 files
and symlink targets. These are distinct receipts: the full 790-test and process
results remain attached to `c433d63`, while the final wiring has its own focused
and integration proof. Both used exactly the independently fetched Clio revision
above. [Exact gate metadata](evidence/clio-command-identity.json) and
[actual command output](evidence/clio-command-identity.txt) include the behavioral
failures, corrected results, source hashes and visible runtime advisories.

The new clear test initially needed one closing delimiter repaired during lint.
The first extraction failure run reused a just-imported observation ID while
trying to represent a new extractor version; assigning that new fact its own
envelope IDs removed the unrelated fixture error. Neither harness error is
presented as the behavioral failure proof. The historical schema fixture's
directory repair is described above; its final six assertions pass. Independent
peer review found no confirmed blocker in the clear transaction and requested
that stronger old-schema proof. Actual remote reviewers and root dependency
promotion remain pending; source-only review is not reported as a test run.

## Earlier direct-write and real service verification

Source `d06a08b88e940204ce4575fee8a34837eaf0a2a0`, tree
`0eac2c8d734913ecb973b996478e006638da706b`, passed **783 unit tests / 2,217
assertions**, zero lint errors/warnings, formatting, boundary and interop checks,
AOT build, the multi-process Clio/Lucene gate, and the actual shipped launcher.
Every gate used independently fetched immutable Clio
`690aad83ff54ef5225a1f1533b4a7bd0eaef3561`; source stayed unchanged throughout.
[Historical gate metadata](evidence/clio-direct-review.json) and
[actual gate output](evidence/clio-direct-review.txt) retain exact provenance and
the visible JVM/Lucene advisories.

Actual Codex 3996289134 found that direct writes still inherited historical
first-write-wins behavior: a retry could silently acknowledge changed material
content. Every prospective Clio record now validates before the same locked
identity comparison used by import. Identical semantic retries preserve the
original accepted bytes; changed material content throws `:idempotency-conflict`.
Revision identity excludes its generated observation/revision IDs and time;
ingestion, checkpoint and extraction identity retains its observation ID while
ignoring observation time. Request-keyed repository, review and lineage retries
ignore regenerated envelope IDs/time; review decision IDs/times and lineage
candidate IDs/generation times are also retry-generated by their constructors.
Their actual decision, target, confidence, path and other material fields remain
part of the comparison.

The failure-first regression reported ten failures across 35 assertions, including
malformed duplicate writes that formerly bypassed schema validation. The corrected
focused suite passed 17 tests / 103 assertions. Clio's shared law run explicitly
selects stronger conflict obligations for all seven record kinds, using material
payload changes rather than regenerated envelope IDs. Historical event replay
still invokes its original reference unchanged. The legacy Mongo/in-memory
first-write-wins contract has not been migrated by this change. An independent
read-only peer review found no confirmed introduced defect.

The formerly pending service integration alias now also passes **22 tests / 108
assertions with no skips**. The Foresight `devtools/epiphany-integration.mjs`
supervisor starts native MongoDB, npm S3rver and real offline MiniLM in the same
process/network context as the JVM caller. It proves an actual Mongo command ping
and S3 EDN create/put/get before the suite; the embeddings adapter makes real HTTP
requests and checks 384-dimensional results. From the Foresight root, run
`pnpm --filter @foresight/devtools test:epiphany-integration` with the documented
`EPIPHANY_CLIO_SOURCE`, `FORESIGHT_MODEL_CACHE` and installed runtime settings.
[Integration evidence](evidence/clio-local-services.json) records clean source
and matching digests of all 450 source files and symlink targets before/after.

The fixtures now require a dedicated Mongo test URI and use isolated collections;
they no longer select the historical hardcoded connection or silently skip. Model,
URL, dimensions and digest are explicit test settings; original Ollama/nomic/768
defaults remain available. A readiness regression first failed one of two
assertions and now passes both. The supervisor's initial startup attempts reused
a Mongo client after its failed connection closed the topology. Recreating and
closing the client on each bounded attempt fixed that race. Its first complete
integration run was an intermediate dirty-source result; the final clean-source
run additionally repaired source hashing for a directory symlink. The final
supervisor exited zero after stopping its owned children and deleting its data.

Remote reviews, coordinated root pin promotion, and any newer immutable Clio
kernel correction remain gates. These results are not relabeled as coverage for
a future kernel revision.

## Earlier immutable successor verification

The earlier implementation checkpoint was
`152b8f5c88fe597c723425788d7fab06b916d962`, tree
`757a7ada2c2fdbc0cb6340e6d894d67f99ac770d`. It passed every owned gate below
against separately fetched Clio `690aad83ff54ef5225a1f1533b4a7bd0eaef3561`,
repository tree `8799304eb2cba6975d925cbd9dd27e87f1e10fea`. This kernel locks
canonical per-file snapshots during concurrent appends. A classpath probe checked
the exact selected source before testing; the application source remained unchanged
through the final launcher check.

The full unit suite passed **781 tests / 2,180 assertions**. Lint reported zero
errors or warnings; formatting, architecture boundaries, interop inventory, AOT
build, multi-process Clio/Lucene verification, and the actual shipped launcher
all passed. [Historical revision metadata](evidence/clio-final-review.json) and
[actual command output](evidence/clio-final-review.txt) retain the separate gate
durations and visible runtime advisories. These results supersede the earlier
37b720 consumer run recorded below; the legacy service integration alias remains
pending and is not included in this claim.

Two subsequent actual review findings were reproduced and corrected:

- Codex 3996201045 prompted a retry check for timestamped ingestion, checkpoint,
  and section-extraction observations. The existing direct observation writer
  already kept the first record; the affected backup-import admission instead
  rejected identical facts with a newer observation timestamp. Its failure-first
  test produced three idempotency-conflict errors. Comparison now ignores that
  observation timestamp for those three collections while retaining material-field
  conflict detection, original accepted facts, and byte-identical no-op imports.
  The focused corrected suite passed eight tests / 36 assertions. Before the
  behavioral RED, an incorrect top-level dependency override selected the old
  kernel, and one test delimiter failed to parse; neither is counted as evidence
  of the admission defect. The corrected invocation puts `:override-deps` inside
  the selected `:verified-clio` alias.
- CodeRabbit 3996200917 identified that a trailing slash in a nonexistent fetch
  destination made staging creation fail before Git ran. The script now removes
  trailing separators and creates its owned staging directory beside the intended
  destination. Injected fetch failures with zero, one, and three trailing slashes
  all reached the fetch step and left no partial destination or staging directory.
  A real public fetch with three trailing slashes produced the exact 690aad kernel
  above. The script now pins that immutable revision.

Root dependency promotion, actual reviewer clearance, and the explicitly pending
legacy service integration gate remain separate work. The card remains under its
existing canonical state transition requirements.

## Previous immutable checkpoint and obstacle history

Epiphany executes its JVM unit, lint, formatting, boundary, interop, AOT build,
shipped launcher and durable Clio/Lucene process gates in the same sandbox.
The implementation checkpoint is `942934e69f38fc355a6db70dc20b2f29aa47b032`,
tree `0b9cce9e8183b81c3120f356375e8b071b7845b7`. Its source was unchanged
between the final gate run and that local checkpoint. Documentation and evidence
added afterward do not claim a later production-source test.

That checkpoint's dependency was fetched separately at immutable Clio revision
`37b720ddded5dbb83a2d55fabbe0415dc7302b1e`, repository tree
`00e8c694f70f456f8d03c3c9a86fca825b2e6d63`. Explicit `:override-deps`
selected this checkout. An earlier shared worktree changed during verification;
those runs are not presented as immutable dependency coverage. Every owned gate
was repeated against the independent fetch after discovering that mismatch.

| Actual gate | Observed result |
| --- | --- |
| `clojure -M:unit-test` | 780 tests, 2,165 assertions, zero failures |
| `clojure -M:lint` | Zero errors and warnings; informational findings remain visible |
| `clojure -M:cljfmt` | All source formatted |
| `clojure -M:boundary-check` | Layer boundary check passed |
| `clojure -M:interop-inventory` | Interop ratchet passed |
| `bash bin/build` | JVM entry point AOT compiled |
| Actual `bin/ep --help` | Published launcher source ran from an independent caller directory |
| `bash bin/verify-clio-edn` | Separate JVM replay, lexical search, repeat ingestion and concurrent writes passed |

[Revision metadata](evidence/clio-review-recovery.json) accompanies the
[actual command output](evidence/clio-review-recovery.txt). The launcher proof
used a disposable copy of the same source with its normal sibling dependency
layout and the same JVM runtime. The main workspace's older sibling gitlink is
promoted separately; no launcher implementation or language was substituted.

The real review findings led to these changes:

- HTTP profile selection now reaches explicitly configured adapters. An
  unavailable selected profile returns 503 before storage access. Two actual
  handler regressions exercise header and query selection.
- Revision identity admission runs inside the complete operation lock. Two
  handles racing with distinct generated UUIDs persist one natural revision.
  Repeated imports of every append collection preserve both bytes and state;
  malformed duplicates and changed-content identity reuse fail before mutation.
- An injected native file-force failure first demonstrated that visible append
  bytes could incorrectly make a retry succeed. The retry now calls canonical
  Clio `ensure-durable!` while retaining the operation lock. It keeps refusing
  while force fails and later acknowledges without appending duplicate facts.
- Recursive Markdown schemas return to the central registry. Lucene results
  carry resource identity, and hybrid scoring keeps different resources and
  commits distinct. Exact configured storage paths retain parent segments and
  Unicode. JGit common-directory resolution handles ordinary repositories,
  bare repositories and linked worktrees in registration and parity tests.
- The standalone Clio fetch uses owned staging and atomic no-clobber publication.
  Two injected fetch failures leave no partial destination and can be retried;
  an existing destination remains untouched. A real pinned public fetch passed.
- The canonical board snapshot now includes the active recovery card. A Rheos
  parser bug had erased inline YAML labels from projections. The corrected
  CLI at source `1659ec9173c39999bb33bb9aaba503716f3864e0` regenerated all 116
  cards with original labels. Existing card bytes and ledger history were
  preserved. Historical abbreviated ledger references were clarified by new
  comments rather than editing past events.

The initial focused review tests reported two errors and seven failures. The
durability fault regression separately failed before the barrier was added.
The first full unit run had 21 failures because the recovered Git clone lacked
the `HEAD~1` and `HEAD~3` history used by existing fixtures. Fetching its real
history fixed that environmental cause; the fixture assertions were retained.
One launcher experiment still selected the caller project's old dependency;
running the actual launcher from a separate caller directory with the verified
sibling layout resolved the classpath conflict. An initial fetch-test harness
tried to execute a non-executable script directly; invoking its declared Bash
interpreter corrected the harness before the meaningful failure injection.

The JVM process proof emits native-access and optional Vector API advisories.
These diagnostics remain in the published output. Native access can be declared
with `--enable-native-access=ALL-UNNAMED`. Inspection of Lucene 10.5's official
`VectorizationProvider` source found no explicit scalar-provider selector that
avoids its advisory: the default scalar fallback logs when the incubator module
is unavailable. Adding `--add-modules=jdk.incubator.vector` instead produces
the JVM's incubator-module warning. This checkpoint does not claim zero runtime
advisories or hide them. The primary source is
[Lucene's provider implementation](https://github.com/apache/lucene/blob/releases/lucene/10.5.0/lucene/core/src/java/org/apache/lucene/internal/vectorization/VectorizationProvider.java).

The legacy Mongo/S3/Ollama integration alias remains a separate pending gate:
its readiness fixtures may skip unavailable services, and its embedding fixtures
hard-code Ollama, `nomic-embed-text` and 768 dimensions. The local Clio/Lucene
proof uses no Mongo or inference and does not represent those integrations as
passed. The root workspace's real 384-dimension MiniLM provider can support a
future explicit embedding configuration; its shape must not be silently
substituted for the old fixture contract.

After that earlier immutable run, upstream Codex identified a canonical Clio read
race during a partial concurrent append. The separately fixed successor is the
690aad kernel tested in the current verification above; the older 780-test result
is retained here as historical evidence rather than relabeled as successor coverage.
The existing Rheos card remains `in_progress` because its inherited parent build
gate awaits coordinated root dependency promotion; no status was edited around
that gate. Remote reviews and the remaining integration work remain merge gates.
