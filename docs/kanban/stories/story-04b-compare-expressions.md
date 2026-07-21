---
category: "stories"
labels: ["phase-1", "comparison", "diff", "evidence"]
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001401"]
phase: "1"
type: "story"
write-id: "1784661367879-0.j2hbh2tgtgrb193bw7x"
points: "3"
title: "ENG-004B: Compare two historical expressions (`ep diff`)"
priority: "P1"
status: "in_progress"
id: "01900d7c-7f3a-7e8b-9c4d-000000001402"
epic: "01900d7c-7f3a-7e8b-9c4d-000000000004"
design: "docs/kanban/epics/epic-04-temporal-idea-lineage.md"
---

# ENG-004B: Compare two historical expressions (`ep diff`)

Side-by-side comparison of any two section or revision-at-path expressions.

## Acceptance criteria

- Both sides show exact source metadata; the diff is Markdown-appropriate (textual or structural).
- Continuity signals display alongside but are never conflated with the diff itself.
- The comparison is reproducible from recorded IDs and versions.
- A comparison can seed a candidate relation or review decision.

---
Implemented: compute-diff (line-by-line with lookahead matching), format-diff-text/format-diff-edn, compare-evidence (retrieves both sides, produces diff + continuity signals). 9 tests, 24 assertions. No ep show/ep diff CLI commands yet — needs ports wiring. --tasks-dir docs/kanban

AUDIT 2026-07-12: status=done graded D. Same shape as ENG-004A: completion comment discloses 'No ep show/ep diff CLI commands yet — needs ports wiring', card promoted to done regardless. Diff/continuity domain logic (9 tests) is real; 'ep diff' does not exist in CLI dispatch (main.clj:539-545). Criterion 'a comparison can seed a candidate relation or review decision' unverifiable without the surface. Would have been gated by: review->done acceptance-evidence check; ENG-017G command contracts. Demoting done->review. --tasks-dir docs/kanban

REVIEW 2026-07-13: request-changes. Independently re-verified: src/epiphany/infra/main.clj:539-543 dispatches only register/search/status/serve; there is no ep diff command, and unrecognized commands hit the 'Unknown command' fallback at line 544. The domain logic itself is real and solid: epiphany.domain.diff/compute-diff, format-diff-text/format-diff-edn, and compare-evidence (lines 45-200) correctly separate diff lines from continuity signals, backed by 9 passing tests / 24 assertions. However the acceptance criteria describe a user-facing comparison capability ('reproducible from recorded IDs', 'can seed a candidate relation or review decision'), none of which is reachable without a CLI surface -- zero test coverage exercises a diff command path in main_test.clj. Full suite passes (554 tests, 0 failures), but that only reflects the isolated domain tests, not the missing feature surface. Moving back to in_progress until ep diff CLI wiring lands; matches the same gap pattern already flagged on ENG-004A/ENG-004D. --tasks-dir docs/kanban

REVIEW-FAIL 2026-07-13: same gap as ENG-004A — domain logic is tested but 'ep diff' doesn't exist in the CLI dispatch table. Nothing wires it. --tasks-dir docs/kanban

FIX 2026-07-13: ep diff now exists for real. Wired in main.clj (run-diff) on top of the existing tested domain/diff.clj (compare-evidence), using the same real Git-backed evidence port as ep show. Verified against this repo: 'ep diff AGENTS.md@HEAD~3 AGENTS.md@HEAD' produces a real unified-style diff plus a separate, clearly-labeled Continuity/Summary block ('Continuity (not part of the diff): ...') so continuity signals are visible but never folded into the diff lines themselves, per AC. New test diff-compares-real-revisions-in-this-repo asserts both the diff markers and the continuity/summary block are present. Full suite: 568 tests, 1456 assertions, 0 failures. NOT fully done against the AC: 'a comparison can seed a candidate relation or review decision' is still unreachable -- there is no candidate/review-decision store to seed into (see ENG-005A finding below). Moving to review, not done. --tasks-dir docs/kanban

REVIEW 2026-07-21 (independent adversarial, board triage): REQUEST-CHANGES. AC1-3 MET and solid: both sides carry exact source metadata with full commit OIDs (main.clj:629-631, diff.clj:45-128); continuity is a distinct :diff/continuity key never folded into :diff/lines and rendered as a separately-labeled block (diff.clj:187-192, main.clj:633-635); reproducible from resolved OIDs via --format edn. Suite green (612/1558/0); diff-compares-real-revisions-in-this-repo passes.
- AC4 "seed a candidate relation or review decision": UNMET — run-diff only reads/prints; nothing wires it to the (now-existing) decision port or any candidate store. The FIX comment's deferral reason ("no store to seed into") has EXPIRED for decisions (ENG-005A landed), but no candidate store exists. → DESCOPED to new card ENG-005G (durable lineage-candidate store); this card's AC4 is amended to defer the seed-a-candidate capability there.
- Surfaced here, now fixed separately: the bin/ep launcher was a broken infinite self-exec (regression 0a99597) — the recorded acceptance evidence was non-reproducible through the shipped binary. Fixed under ENG-017M; `bin/ep diff …` now runs.
Moving review→in_progress for the AC4 amendment; with AC4 descoped and the launcher fixed, remaining scope is small and it should re-enter review quickly.
---