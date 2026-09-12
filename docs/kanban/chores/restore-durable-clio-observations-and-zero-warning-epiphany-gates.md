---
category: "chores"
dependency: []
type: "chore"
write-id: "1789208747602-0.kie7fsjtn50c1xxw37"
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

---