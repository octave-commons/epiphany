---
slug: clio-review-recovery-2026-09-12
uuid: 69041f7a-5faa-4ba1-a47b-4e8e1ebf4c63
kind: report
status: open
description: "Review-driven admission, profile routing and immutable Clio verification."
labels: [development, clio, verification]
---

# Clio review recovery

Epiphany executes its JVM unit, lint, formatting, boundary, interop, AOT build,
shipped launcher and durable Clio/Lucene process gates in the same sandbox.
The implementation checkpoint is `942934e69f38fc355a6db70dc20b2f29aa47b032`,
tree `0b9cce9e8183b81c3120f356375e8b071b7845b7`. Its source was unchanged
between the final gate run and that local checkpoint. Documentation and evidence
added afterward do not claim a later production-source test.

The final dependency was fetched separately at immutable Clio revision
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

After the immutable run, upstream Codex identified a canonical Clio read race
during a partial concurrent append. That successor is being fixed and tested in
a separate worktree. This report retains the exact tested revision and does not
claim that the unpublished kernel correction has passed these consumer gates.
The existing Rheos card remains `in_progress` because its inherited parent build
gate awaits coordinated root dependency promotion; no status was edited around
that gate. Remote reviews and the immutable kernel successor remain merge gates.
