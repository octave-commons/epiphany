---
category: "stories"
labels: ["phase-1", "http", "api", "adapter"]
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001304", "01900d7c-7f3a-7e8b-9c4d-000000001401"]
phase: "1"
type: "story"
write-id: "1784687631042-0.qvgq44663nhiqkzgr12"
points: "4"
title: "ENG-006A: Expose the HTTP API adapter (`/api/v1`)"
priority: "P1"
status: "done"
id: "01900d7c-7f3a-7e8b-9c4d-000000001601"
epic: "01900d7c-7f3a-7e8b-9c4d-000000000006"
design: "docs/kanban/epics/epic-06-temporal-research-workbench.md"
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

FIX 2026-07-21: closes the remaining AC gap. The 2026-07-13 REVIEW-FAIL found two things: (1) missing CLI/HTTP parity tests, (2) unsafe read-string. Both are now resolved by ENG-017G's landed slice (done today): a safe `clojure.edn/read-string {:readers {}}` parser (http.clj:107,115 — no bare read-string anywhere), and real CLI/HTTP outcome-parity tests (test/epiphany/parity/cli_http_test.clj) for register/search.

While independently re-verifying this card's AC bullet "Review decisions are POST /review-decisions command resources, not mutable candidate updates," found and fixed a real, previously-undiscovered defect: `review-decisions-handler` was a pure stub. It validated the two required body fields, fabricated a random `{:id (random-uuid) :decision ... :created-at (now)}` response, and returned 201 — but never called `:record-review-decision!` on the observations port. The AC was met structurally (right HTTP verb, right resource shape — not a PATCH/PUT mutating a candidate) but not durably: nothing was ever recorded. This wasn't caught by prior reviews because their focus was the 9 red tests / parity / the read-string CVE, not whether this specific handler's body did real work — the same "green tests, decorative handler" pattern the 2026-07-12 audit found in the workbench (ENG-006C).

Fixed: the handler now validates the decision type against `review/review-decision-types` (400 if invalid), resolves the candidate's `:resource-id` by looking it up via `:find-lineage-candidate-by-id` (404 if the candidate doesn't exist — a decision can't be recorded against nothing), and durably records through `:record-review-decision!` — the same op `ep inbox decide` (this session's ENG-005B fix) uses, so an HTTP-recorded decision and a CLI-recorded one are indistinguishable in the store.

Updated `http_test.clj`'s `mock-adapters` to provide a real (if minimal) `find-lineage-candidate-by-id`/`record-review-decision!` pair and a real UUID candidate id — the old test's `"cand-1"` string candidate-id only ever worked against the stub, since nothing validated it. New tests cover the 400/404 paths the real implementation now has that the stub never could (invalid decision type, non-UUID candidate id, unknown candidate id), and assert the decision is actually recorded with the correct `:resource-id` resolved from the candidate.

Also fixed, found along the way: `test/epiphany/domain/backup_test.clj` (ENG-005A's ENG-021A sibling, already done) had two tests that `spit` directly into a test directory without ever creating it — every other test in that namespace happens to create the directory as a side effect of `export-to-file`'s `io/make-parents`, so these two only passed when kaocha's randomized test order happened to run after one of those; under an unlucky order they threw `FileNotFoundException`. This was making the FULL suite flaky (not this card's tests specifically). Added the missing `io/make-parents` calls; reran the full suite twice to confirm stability.

Evidence: clojure -M:unit-test — 678 tests, 1732 assertions, 0 failures (run twice for stability). clojure -M:boundary-check clean. Commit 55e1f0f.

Moving in_progress -> review.

REVIEW 2026-07-21 (independent adversarial verification): APPROVE

Verified independently, not from the fix's own claims:

1. Old handler confirmed stub via `git show 55e1f0f^:src/epiphany/infra/http.clj` (review-decisions-handler): only checked `str/blank?` on :decision/:candidate-id, then built `{:id (random-uuid) :decision ... :created-at (now)}` and returned 201 — zero reference to :observations or any adapter op. Matches the claim exactly.

2. New handler (http.clj:291-345 per diff) genuinely chains: decision-type membership check against `review/review-decision-types` → 400 (`bad-request-problem`); candidate-id UUID parse → 400 on failure; `:find-lineage-candidate-by-id` lookup → 404 (`problem-response 404`) if nil; only then `review/make-decision` + `review/decision->observation` + `:record-review-decision!` call, response built from the actual persisted `decision` map (`:review-decision/id`, `:review-decision/decided-at`), not fabricated values. Order is sound: 400s before the DB lookup, 404 before persistence, 201 only after a real record! call.

3. `decision->observation`'s `:resource-id` comes from `(:resource-id candidate)` — the looked-up candidate from `:find-lineage-candidate-by-id`, not the caller's input. Confirmed in review.clj:47-70 and the diff's `let` binding.

4. http_test.clj mock-adapters (test/epiphany/infra/http_test.clj:21-39) now implements `find-lineage-candidate-by-id` keyed on a real UUID (`mock-candidate-id`) returning a candidate with a distinct `mock-candidate-resource-id`, and `record-review-decision!` appends to an atom exposed as `:recorded-decisions`. `router-handles-review-decisions-post` asserts count=1, `:review-decision/decision` = `:accepted`, and `:resource-id` = `mock-candidate-resource-id` — a real behavioral assertion, not just status-code checking. New 400 (invalid type), 400 (non-UUID), 404 (unknown candidate) tests all exercise distinct branches. No cheating spotted — the mock behaves like a minimal real adapter would.

5. `clojure -M:test --focus epiphany.infra.http-test`: 33 tests, 52 assertions, 0 failures.

6. backup_test.clj: confirmed `io/make-parents` was absent before `spit` in both `import-rejects-unsupported-format`/`-version` in the pre-fix diff, and test-dir is otherwise only created as a side effect of other tests' `export-to-file` calls. Ran `--focus epiphany.domain.backup-test` 4x consecutively: 9 tests/27 assertions/0 failures every time — stable.

7. `clojure -M:unit-test` run twice: 678 tests, 1732 assertions, 0 failures both times.

8. `clojure -M:boundary-check`: clean. The new `epiphany.domain.review` require in http.clj is used only for `make-decision`/`decision->observation`/`review-decision-types` — same delegation pattern as `registration/register!`, `status/query-status` elsewhere in this file. No decision-worthy logic sits in the handler beyond validation + delegation.

9. Legitimacy: card history shows the 2026-07-13 REVIEW-FAIL only flagged missing parity tests and read-string, never this stub specifically — the 2026-07-12 audit's "decorative handler" observation was about ENG-006C's workbench, a different card. This is a genuinely new, independently-found defect, not padding. Searched for ENG-006C card (not found in this board's task index) — out of scope, correctly left untouched.

All 9 verification points check out. No discrepancies found between the claimed fix and the actual diff/behavior.
---