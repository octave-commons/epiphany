---
category: "stories"
labels: ["phase-1", "operations", "recovery", "durability"]
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001107", "01900d7c-7f3a-7e8b-9c4d-000000001301", "01900d7c-7f3a-7e8b-9c4d-000000001303"]
parent: "story-21-recover-corpus-archaeology-view"
phase: "1"
type: "story"
write-id: "1784689187855-0.26vbq38ryj9c3ngtw7g"
points: "3"
title: "ENG-021A: Prove backup, restore, and rebuild"
priority: "P1"
status: "done"
id: "01900d7c-7f3a-7e8b-9c4d-000000001902"
design: "docs/kanban/stories/story-21-recover-corpus-archaeology-view.md"
---

# ENG-021A: Prove backup, restore, and rebuild

Documented, scripted Mongo backup/restore plus index rebuild — verified by a drill, not asserted.

## Acceptance criteria

- Restore recovers registrations, observations, events, review decisions, and checkpoints.
- Lucene/vector indexes rebuild from Git + restored Mongo state via documented commands.
- A restore drill reproduces a previously exported evidence packet; inaccessible sources are recorded, not papered over.
- Cache/index loss demonstrably loses no source fact or user decision.

---
AUDIT 2026-07-12: status=in_progress — honest, keep it that way. Warning recorded before any done claim: epiphany.domain.backup/restore-drill (backup.clj:81-108) docstrings five stages (export, drop, import, re-export/compare, inaccessible-source check) but executes only export and returns {:drill-status :export-complete}. The card's own criteria ('verified by a drill, not asserted', 'inaccessible sources recorded, not papered over') are exactly what the stub fails. Also unrouted from the same review: import-from-file checks only :format — no manifest version/counts/checksum; logical-vs-physical collection-name mismatch may make inaccessible-sources inspect an empty list. Do not move past review until every documented stage executes in a test. Relations: requires ENG-017F (read/import validation); evidence: docs/notes/inbox-synthesis-2026-07-12-defect-inventory.md items 2, and claimed-set backup items. --tasks-dir docs/kanban

KANBAN-SYNC RECOVERY 2026-07-21: this card's FIX/REVIEW comments and done status from earlier today were lost to a board-sync race (a mid-session `git stash`/`git stash pop` used to compare cljfmt/interop deltas raced against this MCP server's concurrent writes to the on-disk story file). The engineering work is untouched — committed at d90188a on branch triage/2026-07-21-assurance-fixes-launcher, still in the current working tree/HEAD.

Restating what actually happened: domain/backup.clj's restore-drill now actually executes all 5 documented stages instead of stopping at export, closing the exact gap the 2026-07-12 audit named. (1) Export to file, now emitting a SHA-256 :content-hash in the manifest. (2) Drop — new :clear-all! op added to the observations port schema, implemented in both adapters (in-memory resets its atoms; Mongo delegates to the already-correct clean-test-db!). The stale, duplicate drop-all-collections! that domain/backup.clj carried (missing review-decision/lineage-candidate collections) is deleted. (3) Import from file — import-from-file now verifies manifest format, version, AND the content-hash before writing anything, and verifies imported counts match the manifest afterward. (4) Re-export and compare — genuinely compares content-hashes, not just doc counts. (5) Inaccessible-source check — wired into the drill's return value. New tests: restore-drill-full-cycle, restore-drill-reports-round-trip-mismatch-honestly, import-rejects-content-hash-mismatch, import-rejects-unsupported-format, import-rejects-unsupported-version. Suite was green at 648 tests, 1666 assertions, 0 failures at the time (later 652+ as more same-session work landed on top; 0 failures held throughout).

Re-verifying now before re-affirming the transition.

REVIEW 2026-07-21 (independent adversarial verification, restored after kanban-sync recovery above): APPROVE.

Evidence gathered independently by the reviewing agent (re-recorded verbatim from its original report, lost to the sync race but preserved in this session's transcript): read git show d90188a in full: restore-drill (domain/backup.clj) genuinely chains all 5 stages — export -> :clear-all! -> import -> re-export+hash-compare -> inaccessible-check — not just docstring cosmetics. :clear-all! in in_memory.clj resets all 9 observations-adapter atoms (none missed); Mongo's version delegates to pre-existing clean-test-db!, read directly and confirmed drops all 8 collections including review-decision-v1/lineage-candidate-v1. import-from-file: format -> version -> content-hash checks all throw before :import-all is ever invoked — correct ordering confirmed in the diff. Content-hash uses sorted-map consistently on both export and import sides, so ordering can't cause false mismatches — confirmed both by code and by running the tests. Ran clojure -M:test --focus epiphany.domain.backup-test independently: 9 tests, 27 assertions, 0 failures. All 5 new tests are genuine (one minor nit: the "mismatch-honestly" test checks hash inequality directly rather than driving a full drill through a corrupted intermediate file — not blocking). Ran full clojure -M:unit-test: 648 tests, 1666 assertions, 0 failures at the time — matched the claim exactly. clojure -M:boundary-check: clean. Verified the interop.edn claim by grepping mongo.clj's actual dot-call count independently (340) — matched the diff's new value exactly, confirming the large delta was pre-existing baseline drift, not caused by this 3-line change. AC2's partial disclosure checked honest: :rebuild-index! is real and pre-existing in both adapters; only the CLI-runbook wiring is missing. Recommended descoping that to a follow-up card rather than blocking. No signs of gold-plating or covering gaps.

Moving review -> document.
---