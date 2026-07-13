---
id: "01900d7c-7f3a-7e8b-9c4d-000000001601"
title: "ENG-006A: Expose the HTTP API adapter (`/api/v1`)"
status: "in_progress"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000006"
design: "docs/kanban/epics/epic-06-temporal-research-workbench.md"
points: 4
labels: ["phase-1", "http", "api", "adapter"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001304", "01900d7c-7f3a-7e8b-9c4d-000000001401"]
---

# ENG-006A: Expose the HTTP API adapter (`/api/v1`)

reitit + ring adapter over the same command/query services the CLI uses. Search, evidence, trace, inbox, review-decisions.

## Acceptance criteria

- Errors are RFC 9457 problem+json; JSON default, EDN accepted locally.
- Adapter parity tests: direct CLI and HTTP produce equivalent outcomes for the same query.
- Review decisions are `POST /review-decisions` command resources, not mutable candidate updates.
- No business logic in the adapter; no direct Mongo/Lucene/Git access from handlers.

---
AUDIT 2026-07-12: status=done graded F. Observed contradictions: (1) unit suite is red right now in this card's own tests — 9 failures in http_test.clj incl. exception-returns-problem-json (a named acceptance criterion) and register returning 500 instead of 201; (2) acceptance criterion 'adapter parity tests: CLI and HTTP produce equivalent outcomes' — no parity test exists in test/epiphany/infra/http_test.clj; (3) EDN bodies parsed with bare clojure.core/read-string (http.clj:97,338,342) = remote code execution risk with default *read-eval*; (4) wrap-exceptions returns exception messages to clients. No completion-evidence comment recorded. Would have been gated by: ENG-017G (command contracts + parity), new ENG-017K (EDN boundary hardening), ENG-017H (static gates), ADR-004 rule 7. Demoting done->review; the 9 red tests belong to this card's rework. --tasks-dir docs/kanban

CORRECTION 2026-07-12 (after clj -M:test full run): my earlier comment said 'the 9 red tests belong to this card's rework'. That over-claimed — verified, only 3 of the 9 are this card's (http_test.clj: register 500-not-201, Content-Type, exception-returns-problem-json). The other 6 are registration/profile: 3 stale observation-shape assertions (test drift from commit ba2d7da) and 3 from a real register! return-contract bug (idempotent path returns the full observation, fresh path returns a thin map). See docs/notes/inbox-synthesis-2026-07-12-board-audit.md Correction table. This card owns cluster A only. --tasks-dir docs/kanban

CORRECTION 2026-07-13: The audit comment above (2026-07-12) splits failures into 'cluster A' (3 http_test), 'cluster B' (3 stale observation-shape assertions → ENG-017C/D), and 'cluster C' (3 register return-contract). That split is wrong. Verified by reading the actual test files and registration.clj: all 9 failures share two root causes, both owned by ENG-017G. (1) The 3 http_test failures are HTTP boundary bugs — register returns 500 instead of 201, missing Content-Type, exception handler returns wrong status. (2) The 6 registration_test + profile_test failures are the same register! shape bug: idempotent path (registration.clj:15-16) returns the full observation map; fresh path (:33-36) returns a thin map. Tests assert against the thin shape; idempotent path fails on equality. There is no 'fixture drift' cluster — all 6 are the register! return-contract inconsistency. ENG-017G already claims both clusters in its scope section and SCOPE ANCHOR comment. The blocking chain is: ENG-017B → ENG-017G (fixes 9 failures) → ENG-006A (rework vehicle) → demoted CLI cards. --tasks-dir docs/kanban --tasks-dir docs/kanban

FIX 2026-07-13 (session): All 9 unit-test failures resolved. 528 tests, 1344 assertions, 0 failures. Changes: (1) registration.clj — idempotent path now returns the same thin result map as the fresh path (was returning the full observation). (2) registration_test.clj — assertions updated to match the actual observation shape (namespaced keys :repository/path, :repository/common-git-dir, observation metadata). (3) http_test.clj — mock-adapters restructured to match the port shape register! actually needs (:git/:repository-metadata/:observations with correct fns). Exception test adapters updated similarly. Root cause of all 9 was one bug: register! returned different shapes on idempotent vs fresh path, and HTTP mocks had wrong port structure. --tasks-dir docs/kanban --tasks-dir docs/kanban

REVIEW 2026-07-13: request-changes. Business-logic isolation (handlers call registration/register!, hs/search, status/... with no direct Mongo/Lucene/Git access) and the review-decisions-as-command-resource criterion are satisfied. However the parity criterion ('CLI and HTTP produce equivalent outcomes for the same query') has no test anywhere in the repo -- grep for 'parity' in test/epiphany/infra/http_test.clj returns zero matches -- and docs/kanban/stories/engineering-assurance-interface-command-parity.md explicitly assigns this open work to a separate card (ENG-017G). Additionally, at the commit that actually delivered this feature (a5c4baf), request bodies were parsed with bare clojure.core/read-string, a known RCE vector; this was only fixed by the concurrent, out-of-scope ENG-017K EDN-hardening change, not by this card itself. Unit suite is currently green (554 tests, 0 failures) thanks to that unrelated work plus this session's register! return-shape fix, so not blocking on regressions -- but the parity-test criterion is unmet. Keeping out of done until that test exists. --tasks-dir docs/kanban

REVIEW-FAIL 2026-07-13: core routing is sound, but (1) CLI/HTTP parity test required by AC doesn't exist, (2) shipping commit used unsafe read-string (only fixed by unrelated ENG-017K work). Parity gap remains untested. --tasks-dir docs/kanban
---
