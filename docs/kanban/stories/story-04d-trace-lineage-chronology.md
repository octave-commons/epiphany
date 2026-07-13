---
id: "01900d7c-7f3a-7e8b-9c4d-000000001404"
title: "ENG-004D: Trace a lineage chronology (`ep trace`)"
status: "review"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000004"
design: "docs/kanban/epics/epic-04-temporal-idea-lineage.md"
points: 4
labels: ["phase-1", "timeline", "lineage", "evidence"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001403"]
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
---
