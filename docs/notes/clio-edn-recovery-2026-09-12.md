---
slug: clio-edn-recovery-2026-09-12
uuid: ee636742-247f-4246-a4d1-fb4da7577e6c
kind: report
status: open
description: "Fresh recovery evidence and remaining limitations for the local Clio profile."
labels: [development, clio, verification]
---

# Epiphany recovery

This report records the initial recovery checkpoint. Its 771-test result and
Clio dependency below are historical. The [review successor report](clio-review-recovery-2026-09-12.md)
contains the current fixes, immutable dependency rerun and remaining limits.

Scratch cleanup removed the former local Clio implementation. Commit
`17ec28f41a0d6274f8a09f11fd9b5350a4f42041` was unavailable from the remote, and
saved Git-tree exports contained no Epiphany source payload. Its old test logs
remain historical evidence only. This implementation was reconstructed from the
existing observation contracts on published main `643be698ea0d841dd19385506b272872306e456e`.

The first new adapter run failed because Malli interprets `[:= nil]` as a missing
schema child. The precise nil contract is `:nil`; after that fix, the real Clio
laws, restarts, corruption checks and concurrent-handle tests passed. A later
full run passed 768 tests and 2,129 assertions; that observation predates the
additional fault-injection tests and final Clio dependency update and is not the
final verification claim.

The original configured lint returned exit zero while printing 66 warnings.
Unused dependencies and dead private functions were removed, intentional unused
parameters were named explicitly, missing namespace requires were added, and
redundant nested lets were flattened without changing evaluation order. Lint now
fails at warning level. The formatter found 59 incorrect files, including 56
historical files; the pinned formatter corrected them. Boundary and interop
ratchets remained intact. The missing SLF4J provider was addressed by declaring
`slf4j-simple` 2.0.9, matching the already selected API and enabling real logging.

The old JSON board configuration used Rheos defaults that rejected the documented
`chore` type. An EDN configuration now explicitly declares the existing card
vocabulary and preserves the same FSM. Card creation, transitions and comments
were performed through Rheos; existing ledger facts were preserved.

A fresh process proof registered and ingested literal local Markdown, found it
through real Lucene lexical search, then observed zero new revisions/sections on
repeat ingestion. Two independent JVM writers both survived replay. That run
completed in 48.2 seconds. JVM 21 and Lucene emitted native-access and optional
Vector API advisories; those are recorded, not filtered or represented as zero
runtime warnings. Compiler/linter warnings are tracked separately.

The historical optional local embedding adapter is still absent;
lexical mode is fully independent of inference, while semantic mode remains
explicitly dependent on real embeddings.

Final verification on 2026-09-12 completed in 130.511 seconds: **771 tests,
2,134 assertions, zero failures**; lint had zero errors and zero warnings;
formatting, layer boundaries and interop ratchets passed. The same command then
AOT-compiled the JVM entry point, ran its help command, and passed the separate
JVM Clio/Lucene process proof. The final adapter also compares canonical
before/after state hashes during replay and refuses semantic drift.

The dependency is Clio revision `fd7cd25645d6acf80c3e5909f3ceecb65b7b1148`,
package tree `e6bb4d2f0cc2456a31c87a471f3a6d51db0cbf5c`. The primary sibling
checkout at `dbbe64154895e6e63af04054059c4bf34bc5a297` contains the same exact
Clio package tree. A fresh standalone sparse fetch of the pinned revision also
passed. [Command metadata](evidence/clio-edn-recovery.json) and
[actual output](evidence/clio-edn-recovery.txt) include the original red lint and
schema attempts as well as the final pass. Only trailing blank lines were
removed when collecting output; runtime advisories remain visible.

The legacy Mongo/S3/Ollama integration alias was not run for this checkpoint.
Its readiness fixtures can skip absent services, so it is not represented by the
unit and Clio process results above. Semantic search still requires real
embeddings. A newly reported upstream Clio uncertainty edge (append bytes may
exist after a failed fsync) also needs a kernel durability-barrier successor and
a corresponding no-op retry proof before this provider is ready to merge.

Rheos first rejected a move invocation without its required `--to` option.
The corrected review transition invoked an inherited parent `pnpm build`,
which refused because the root manifest's eta-mu gitlink had not yet been promoted
to the newly verified sibling checkout. The card remains `in_progress`; its
status was not edited around the gate. This does not replace the successful
Epiphany-owned commands above. Root pin promotion is coordinated separately.
