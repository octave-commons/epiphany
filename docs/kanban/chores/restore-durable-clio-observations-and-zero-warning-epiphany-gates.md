---
category: "chores"
dependency: []
type: "chore"
write-id: "1789218200585-0.jqka5iwfi3gjotlu1m"
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

---