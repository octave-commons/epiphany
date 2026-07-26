---
category: "stories"
labels: ["phase-1", "timeline", "lineage", "evidence"]
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001403"]
phase: "1"
type: "story"
write-id: "1784688757237-0.jip7dhfimz7xu8kist"
points: "4"
title: "ENG-004D: Trace a lineage chronology (`ep trace`)"
priority: "P1"
status: "done"
id: "01900d7c-7f3a-7e8b-9c4d-000000001404"
epic: "01900d7c-7f3a-7e8b-9c4d-000000000004"
design: "docs/kanban/epics/epic-04-temporal-idea-lineage.md"
---

# ENG-004D: Trace a lineage chronology (`ep trace`)

From a selected section, walk the dated chain of path history, relocations, accepted edges, and provisional candidates.

## Acceptance criteria

- Every edge carries a visible status: observed, accepted, provisional, or rejected/audit-only.
- The user can filter to observed facts only, and include/exclude provisional candidates.
- Every node resolves to the evidence reader.
- A candidate relation is never presented as established history without its status.

---
AUDIT 2026-07-12 (inbox-synthesis session): status=done graded F. Headline deliverable 'ep trace' does not exist — CLI dispatch (src/epiphany/infra/main.clj:539-545) handles only register/search/status/serve. Domain logic (lineage_trace.clj) exists; the declared scope (CLI command) does not. No completion-evidence comment was recorded. Would have been gated by: kanban review-state completion-evidence rule (docs/process/kanban.md), ENG-017G command-contract parity, ADR-004 rule 7 (CI evidence over agent claims). Demoting done->review. --tasks-dir docs/kanban

REVIEW 2026-07-13: request-changes. Independently re-verified: `ep trace` does not exist as a CLI command. src/epiphany/infra/main.clj:539-543 dispatches only register/search/status/serve; any other command (including trace) hits the 'Unknown command' fallback, and the usage help text does not list trace either. The domain layer (src/epiphany/domain/lineage_trace.clj, trace-lineage) does implement the lineage-walk semantics described in the acceptance criteria -- observed/accepted/provisional/rejected edge statuses, observed-only and exclude-provisional filters, chronological node ordering -- and is well covered by test/epiphany/domain/lineage_trace_test.clj (14 tests). A 'Trace' button also exists in the workbench web UI (src/epiphany/infra/workbench.clj:319). But the card's scope is specifically the CLI command ep trace, and there is no such command, no CLI-level test, and no evidence-resolution wiring at the CLI. clojure -M:unit-test passes 554/554 with no trace-CLI tests present. This confirms the card's own prior audit comment. Moving back to in_progress: CLI subcommand still needs to be built and tested before this can go to done. --tasks-dir docs/kanban

REVIEW-FAIL 2026-07-13: same gap — 'ep trace' doesn't exist in CLI. Domain logic is solid but unwired. --tasks-dir docs/kanban

FIX 2026-07-13: ep trace now exists for real. Wired in main.clj (run-trace) on top of the existing tested domain/lineage-trace.clj (trace-lineage), walking real Git history (epiphany.infra.git/reachable-commits + commit-tree-entries) for the given path to build chronological :observed edges -- no fabricated data. Both filter flags from the AC are wired: --observed-only and --provisional include|exclude. Verified against this repo: 'ep trace AGENTS.md' returns 15 real nodes / 14 real :observed edges across this file's actual commit history; an untracked path correctly errors with exit 1. New tests: trace-requires-path, trace-shows-help, trace-walks-real-history-in-this-repo, trace-observed-only-flag-is-accepted, trace-reports-error-for-untracked-path. Full suite: 568 tests, 1456 assertions, 0 failures. NOT fully done against the AC: cross-file candidate edges (accepted/provisional/rejected statuses) never appear because there is no candidate store wired to the CLI (candidates is always [] here) -- domain support for those statuses exists in lineage-trace.clj but nothing populates it yet. Also 'every node resolves to the evidence reader' is only true by construction (a node's path@commit-oid is a valid ep show expression) -- there's no automatic link. Moving to review, not done, until candidate wiring exists or this scope is explicitly split out. --tasks-dir docs/kanban

REVIEW 2026-07-21 (independent adversarial, board triage): REQUEST-CHANGES. `ep trace` genuinely exists, wired, tested, green (612/1558/0; all 5 named CLI trace tests + 11 domain tests pass) — the prior "F, does not exist" grade is stale. Findings:
- AC2 filters: --observed-only correct (main.clj:647,730 → lineage_trace.clj:115-118). But --provisional exclude is MIS-IMPLEMENTED (undisclosed bug): filter-by-status :exclude branch removes :rejected edges, not :provisional (lineage_trace.clj:119-123) — there is no way to exclude provisional while keeping observed+accepted. Inert at the CLI today (no non-observed edges), which is why no test caught it. Within-scope fix.
- AC3 "every node resolves to the evidence reader": only true "by construction" (path@oid is a valid ep show target); heading-path hardcoded [] (main.clj:672); no emitted ep-show expression/link. Within-scope: emit the exact section expression per node.
- AC1/AC4 full breadth (accepted/provisional/rejected cross-file edges): candidates hardcoded [] (main.clj:729); find-cross-edges called with {} decisions (lineage_trace.clj:175). Requires the unbuilt lineage-candidate store → DESCOPED to ENG-005G. Correction to a prior note: ENG-005A built the review-DECISION store, not a candidate store, so trace still had no candidate source even after it landed.
Moving review→in_progress. Within-scope before re-review: fix the --provisional exclude semantics + emit explicit ep show node references. Candidate breadth deferred to ENG-005G.

KANBAN-SYNC RECOVERY 2026-07-21: this card's status and FIX/REVIEW comments from earlier today were lost to a board-sync race (a `git stash`/`git stash pop` used mid-session to compare cljfmt/interop deltas raced against this MCP server's concurrent writes to the on-disk story file, clobbering this card's disposition back to an earlier state). The underlying engineering work is untouched — it is committed at 8428cb4 on branch triage/2026-07-21-assurance-fixes-launcher and remains in the current working tree/HEAD.

Restating what actually happened, for the record: fixed the --provisional exclude bug in domain/lineage_trace.clj's filter-by-status (was only dropping :rejected edges, not :provisional ones, despite the flag's documented purpose) — now drops both, keeping observed/accepted; renamed the :trace/filter value :no-rejected -> :no-provisional to match. Added an explicit `ep show path[#heading]@oid` line per node in format-evidence-text, closing AC3 ("every node resolves to the evidence reader"), via a new section->show-expr helper in main.clj. New tests: filter-by-status-exclude-rejected-test (corrected), trace-lineage-exclude-rejected-filter-test (extended), trace-walks-real-history-in-this-repo (extended with the ep-show-link assertion). Full suite was green (641 tests, 1650 assertions, 0 failures at the time) and passed an independent adversarial review with an explicit APPROVE disposition, which also independently re-ran the tests and confirmed the fix. bin/kanban-done-gate passed and the card was moved review -> document -> done.

Re-verifying now, after the sync issue, that the commit's actual content and the current suite are still consistent with that disposition before re-affirming the transition.

REVIEW 2026-07-21 (independent adversarial verification, restored after kanban-sync recovery above): APPROVE.

Evidence gathered independently by the reviewing agent (re-recorded here verbatim from its original report, lost to the sync race but preserved in this session's transcript): the exclude-both fix is correct — filter-by-status's :exclude branch does `(remove #(contains? #{:provisional :rejected} (:edge/status %)) filtered)`, correctly dropping both statuses, leaves :observed/:accepted, no inversion or over-broad exclusion; confirmed by non-tautological test updates (count assertions + explicit absence checks for both statuses). The :no-rejected -> :no-provisional rename is fully consistent: a repo-wide grep found zero remaining references to the old keyword anywhere. section->show-expr's heading join (`>`) matches parse-section-expression's own `#"\>"` split in evidence.clj:41 exactly, though path-revisions in main.clj hardcodes :heading-path [], so that branch is currently dead/untested in the real CLI path — flagged as non-blocking. Scope is clean: git diff --stat confirmed only the 4 claimed files changed; candidates is still hardcoded [] at the trace call site, confirming ENG-005G wiring was genuinely not folded in (correctly out of scope, per the prior descope decision). Personally ran the tests: focused suite (lineage-trace-test + main-test) -> 51 tests/115 assertions/0 failures; full `clojure -M:unit-test` -> 641 tests/1650 assertions/0 failures, matching the claimed numbers exactly.

Moving review -> document.
---