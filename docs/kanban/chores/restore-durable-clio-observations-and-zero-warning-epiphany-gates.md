---
category: "chores"
dependency: []
type: "chore"
write-id: "1789234096986-0.4e2u33pu422fo1jobzh"
points: "5"
title: "Restore durable Clio observations and zero-warning Epiphany gates"
priority: "P0"
status: "in_progress"
uuid: "restore-durable-clio-observations-and-zero-warning-epiphany-gates"
created_at: "2026-09-12T09:56:20.132Z"
---

## Outcome

Restore the user-authorized Clio EDN observation provider in the existing JVM
Clojure environment, retaining explicit Mongo and memory profiles and rebuildable
Lucene indexes. Fix the actual lint warnings without suppressing checks.

## Scope and basis

The former local commit 17ec28f41a0d6274f8a09f11fd9b5350a4f42041 is unavailable
after scratch cleanup. Published main has no replacement implementation. Saved
historical logs establish intent, not fresh executable evidence. The user directs
Clio ownership and removal of external-service requirements for local work; this
explicit development-provider authorization supersedes the older Mongo-only
durability wording in ADR-000 within this scope. Git remains canonical source;
Lucene remains rebuildable; no production provider is removed.

Dependency justification: eta-mu/packages/clio is the user's designated canonical
event kernel, consumed as the sibling JVM local/root source dependency.

## Acceptance criteria

- Explicit EDN profile implements every observation-port operation through
  validated canonical Clio events and rebuilds state after process restart.
- Rejected writes and exact no-op retries append nothing; corruption, missing
  history and unavailable storage fail visibly without memory fallback.
- Concurrent processes serialize read/decision/append without losing writes.
- Actual CLI register, ingest, lexical search and repeat ingest run without
  Mongo or inference; observation laws and full unit tests pass.
- Configured lint and relevant static gates pass with zero warnings; all failed
  attempts, workarounds and remaining limitations are recorded.

## Exclusions

No environment/language change, no remote model prompts, no fabricated embeddings,
no production migration, and no historical evidence promoted to a current pass.

---
The old JSON board config cannot express the documented chore type in current Rheos; an EDN config now declares the existing epic/story/chore/planning-record vocabulary without changing the FSM. All lifecycle transitions used Rheos. Clio sibling local/root is the user-designated event dependency. Add slf4j-simple 2.0.9 to match the existing SLF4J API and provide actual logging instead of the missing-provider/NOP warning. Fresh full unit result is 768 tests, 2129 assertions, 0 failures; all 66 source/test lint warnings are resolved and the lint command now fails on warnings. First Clio test run rejected an invalid nil schema form; :nil fixed that contract before successful replay proof.

Final actual verification: 771 tests, 2134 assertions, 0 failures. Lint 0 errors/0 warnings; formatting, boundary, interop, JVM AOT build and CLI help passed. The same 130.511s command verified separate JVM registration, ingestion, Lucene lexical search, incremental no-op ingestion and concurrent durable writers against current Clio fd7cd256. Durable command metadata/output live in docs/notes/evidence/clio-edn-recovery.{json,txt}. Runtime native-access/Vector API advisories remain recorded. Legacy Mongo/S3/Ollama suite is unrun. Upstream failed-fsync retry barrier follow-up remains open and blocks merge; current adapter review requested.

Review transition attempt: corrected missing --to syntax, then canonical build gate inherited parent pnpm build and refused the not-yet-promoted eta-mu root gitlink (a7 recorded, dbbe actual). Card remains in_progress; no manual status change or parent pin mutation. Epiphany-owned unit/static/AOT/CLI/process gates above remain verified; root assembly promotion and upstream Clio uncertainty barrier are separate pending dependencies.

Resumed PR18 review at immutable 816531ecadcaee2e3856f43a258ebc193ae301be after workspace recovery. Adjudicate Codex profile routing, concurrent revision identity, import idempotency, generated board snapshot and partial source-fetch cleanup; CodeRabbit schema registry, hybrid result identity, JGit resolver, exact configured path and documentation metadata. Preserve prior event bytes. Canonical Clio failed-fsync successor remains a separate dependency. Implement portable operation decisions before adapters and meaningful concurrency/restart/HTTP tests, then rerun all JVM gates. Current baseline evidence is historical until the new checkout executes.

Correction of abbreviated identifiers in earlier immutable comments: the then-current Clio revision was fd7cd25645d6acf80c3e5909f3ceecb65b7b1148. The root eta-mu gitlink was a7b19825fb5d7c624c38f1d41043c42e92d7f0c3; the actual recovered eta-mu checkout was dbbe64154895e6e63af04054059c4bf34bc5a297. These facts append to history without rewriting prior events. Current failed-fsync tests use canonical Clio f22199ee8a8903b4b69eceee9a18ae4d3e7b3c32, complete tree d2278e9d4dd0a53518b5afc53081a44e9479d2dd, which the standalone fetch also verified from the public source. HTTP now dispatches to explicitly configured profile adapters and refuses unconfigured selections; 42 focused HTTP/Clio tests and 83 assertions pass. Full updated gates are in progress.

Review recovery checkpoint 942934e69f38fc355a6db70dc20b2f29aa47b032: immutable Clio37b720ddded5dbb83a2d55fabbe0415dc7302b1e full unit780/2165, lint0/0, formatting, boundary, interop, AOT, actual launcher and separate-JVM durable Lucene proof pass. Revision admission and repeated import are locked and idempotent; no-op retries use canonical force barrier; HTTP uses selected configured adapters or refuses. Fetch failure cleanup and metadata fixes pass. Corrected Rheos1659ec9173c39999bb33bb9aaba503716f3864e0 retains inline labels in116-card snapshot. JVM advisories, legacy service integrations, parent promotion and newer upstream read-lock successor remain explicit limits in docs/notes/clio-review-recovery-2026-09-12.md.

Actual follow-up reviews reproduced and corrected timestamp-only backup import retries (Codex 3996201045) and trailing-slash Clio fetch staging (CodeRabbit 3996200917). Source checkpoint 152b8f5c88fe597c723425788d7fab06b916d962, tree 757a7ada2c2fdbc0cb6340e6d894d67f99ac770d, passed full 781 tests / 2180 assertions, lint 0 errors / 0 warnings, format, boundaries, interop, AOT, multiprocess Clio/Lucene and actual shipped launcher against separately fetched immutable Clio 690aad83ff54ef5225a1f1533b4a7bd0eaef3561 (tree 8799304eb2cba6975d925cbd9dd27e87f1e10fea). Classpath and unchanged-source proof retained. Runtime advisories remain visible. This supersedes the historical 37b720 consumer gate; legacy Mongo/S3/Ollama integration and root promotion remain pending. Updated report and self-contained actual output: docs/notes/clio-review-recovery-2026-09-12.md and evidence/clio-final-review.*. Snapshot uses the corrected linear Rheos parser; no card status bypass.

Next scoped acceptance: execute the actual legacy integration alias in this same JVM/Node sandbox using disposable native MongoDB, npm S3rver, and pinned real MiniLM384 through the existing embeddings HTTP protocol. Remove the law fixture hardcoded Mongo connection, use explicit test endpoint/model/dimension/digest settings, and make unavailable required services fail rather than silently skip. Preserve original nomic/768 defaults unless explicit configuration selects MiniLM. Supervisor must own and clean its data/processes, verify real Mongo and S3 operations plus actual embeddings, reject skips, and retain all attempted/failing gates. Existing Clio690 immutable proof remains separately sourced.

Actual Codex 3996289134 found a stronger direct-write defect: changed material content can reuse accepted identity and silently continue through the historical first-write-wins reference. Extend prospective Clio admission to every identity-bearing write, validating before duplicate filtering. Same semantic retries may change generated envelope IDs/timestamps; material fields must still conflict, optional request IDs must not collide, and canonical historical replay must remain unchanged. Add direct failure-first regressions and declare stronger Clio conflict law expectations explicitly while preserving legacy Mongo/reference fixture semantics. The lighter real integration now passes22tests108assertions with native Mongo ping, S3rver EDN roundtrip, and actual MiniLM384 embeddings; initial readiness MongoClient failed due closed topology after startup race, fixed with fresh bounded attempts.

Final source d06a08b88e940204ce4575fee8a34837eaf0a2a0 tree0eac2c8d734913ecb973b996478e006638da706b passed783unit tests/2217assertions, lint0/0, format, boundaries, interop, AOT, multiprocessClio/Lucene and actual shippedlauncher, unchanged-source against immutableClio690aad83ff54ef5225a1f1533b4a7bd0eaef3561. Direct material-retry P1 actualRED1test35assertions10fail to focusedGREEN17/103; strict prospective admission allseven types with historicalreplay unchanged. Actual lighter integration22/108 no skips passed on clean same source, matching all450file/symlink sourceSHA71d2824a82d960b9f1b8bb9deca959686aef702817d9563c2fc2761f25bbf5b7, realnativeMongo ping/S3rver EDN putget/MiniLM384 HTTP. Root supervisor/docs committed0e1e7d6518da41425ee410eb55c72976bfafde36. Exact gate and integration evidence in docs/notes/clio-review-recovery-2026-09-12.md. New kernel findings/rootpromotion/actualreview loop remain; no coverage transfer to future kernel.

Immutable upstream Clio identifier-integrity and singular locked-read successor is now published as090da40b20c7df1d29efbc4f194d0938f84478cc, exact tree2de3ad4974a68b915a4e4909c453558b072c40bb. Independently fetched and checked out that exact revision in this sandbox. Update the standalone fetch pin and repeat actual owned consumer gates plus real lighter integration against090; preserve prior690 receipts unchanged, with no result relabeling. Root promotion remains coordinated with upstream actual review clearance.

Review follow-up plan: verify actual Codex 3996355207 section-extraction identity using revision, blob and extractor version across independent command IDs; add concurrent and changed-content failure-first tests. Verify CodeRabbit 5186562035 clear-all retry safety with an explicit command ID while preserving old canonical replay and legacy provider contracts. Correct the recursive Markdown test dispatch key and clarify historical evidence through appended card comments. Independently fetched Clio 090da40b20c7df1d29efbc4f194d0938f84478cc already passed all nine consumer gates on clean a7fecdfacfdbdf927bf38ddf738df1df54d027c8: 783 unit tests / 2217 assertions, lint 0 errors / 0 warnings, format, boundary, interop, AOT, separate JVM process proof and the actual shipped launcher. Real native Mongo, S3rver and MiniLM HTTP integration passed 22 tests / 108 assertions with no skips. These results cover that immutable kernel only; its shared-read successor remains separately pending.

Readable correction to the earlier d06a08b88e940204ce4575fee8a34837eaf0a2a0 receipt: 783 unit tests / 2217 assertions passed, with lint 0 errors / 0 warnings, formatting, boundaries, interop, AOT, multiprocess Clio/Lucene and the actual shipped launcher. Strict prospective admission covered all seven record types; historical replay remained unchanged. The clean real service integration passed 22 tests / 108 assertions, with native Mongo ping, S3rver EDN create/put/get and MiniLM 384-dimensional HTTP embeddings. Exact earlier evidence files are docs/notes/evidence/clio-direct-review.json, docs/notes/evidence/clio-direct-review.txt and docs/notes/evidence/clio-local-services.json. The preceding 152b8f5c88fe597c723425788d7fab06b916d962 gate is historical and its exact files are docs/notes/evidence/clio-final-review.json and docs/notes/evidence/clio-final-review.txt. Prior canonical comments remain unchanged; this appended correction addresses CodeRabbit 3996354385 and 3996354391 without rewriting accepted history. Current follow-up pins independently verified Clio 6c5af6077d069620583b29a180eb925a0805e94b and adds extraction logical identity plus identified clear commands; fresh complete gates are next.

Exact current results against independently fetched Clio 6c5af6077d069620583b29a180eb925a0805e94b: complete c433d63f43cefd626c1d17152cc5cb4be3c6eeb5 passed 790 unit tests / 2273 assertions, lint 0 errors / 0 warnings, format, boundary, interop, AOT, separate JVM Clio/Lucene and actual launcher. Final validators-only normalization move 4c40cb93c689f328782671eb512b405be64aa5f7 separately passed 33 focused tests / 186 assertions, all static gates, fresh AOT and launcher. Real native Mongo, S3rver and MiniLM integration repeated on final source: 22 tests / 108 assertions, no skips, matching 456-file/symlink digest 6850f4305c5eafd0653ef84a88b60bfc9ff87583fdbd33ce3c02e035ddf0e0d5. Extraction logical identity, all accepted historical aliases and explicit clear command IDs now have actual failure-first regressions. The prior schema catalog fixture proves original history and schema bytes survive new code. Scoped peer review found no blocker. Evidence: docs/notes/evidence/clio-command-identity.json and docs/notes/evidence/clio-command-identity.txt. Full and final scoped source receipts stay separate; remote reviews and coordinated root promotion remain pending.

Recovered actual Codex finding 3996503279 from base 7c157fb3a824cf6ff23205486ccf8e285ea78fba. Real Git fixture and independent Clio handles reproduced 3 tests / 13 assertions / 5 failures: revision/extraction overcounts, discarded extraction indexed, and durable count retracted after index failure. Direct EDN record results now explicitly distinguish accepted and duplicate after locked append/fence; persisted result nil and legacy nil-return providers retain their contracts. Final unit 796 tests / 2316 assertions / 0 failures; configured lint 0 errors / 0 warnings, format, boundary and final JVM AOT pass. Native shipped CLI registration/ingest/Lucene/repeated no-op and separate-JVM append/reopen passed before the final core ex-message cleanup; final unit/AOT cover that cleanup. Clio runtime snapshot/reopen regressions separately improved 8 failures to 2 tests / 18 assertions passing. Dependency and standalone fetch pin are immutable ff48f090965be5c17cca1e3662d7402b383510c1. No interop baseline or warning suppression. Source digest, exact commands and actual logs in docs/notes/evidence/clio-ingest-outcomes.json and docs/notes/clio-ingest-outcomes-2026-09-12.md. Scoped peer review found no confirmed blocker; automatic index repair is an explicit separate follow-up. Clean committed-source real Mongo/S3rver/MiniLM integration, actual remote review convergence and root promotion remain pending.

FOLLOW-UP OBSERVATION 2026-09-12: Clean Epiphany 0e3950bf / tree20664b20 with immutable Clio ff48 passed the real service supervisor: Mongo ping, S3 EDN object roundtrip, pinned MiniLM embeddings, 22 tests, 108 assertions, 0 failures, no skips and no service-suite warnings. Initial Mongo startup failed before tests; recovery used a unique fully extracted and checksum-verified official Mongo 8.0.13 binary whose before/after SHA stayed identical. Self-review held publication because the old CLI dropped plain YAML labels from the generated board. Direct Rheos c94e789 (CLI SHA35eb33a238f4a869091302b1ec0c7aa7a46f7a2b0ab9f069899ee6539d5795d4) regenerated it: all116 Markdown inputs unchanged,116tasks12columns, every original label preserved. RED/GREEN, failed attempt, actual service output and immutable source receipts are in docs/notes/clio-ingest-outcomes-2026-09-12.md and linked evidence. Native CLI startup advisories remain disclosed separately; these service results do not erase them. Canonical acceptance and remote review remain pending.

---