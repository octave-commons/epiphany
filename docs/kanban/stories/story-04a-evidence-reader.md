---
id: "01900d7c-7f3a-7e8b-9c4d-000000001401"
title: "ENG-004A: Open exact historical evidence (`ep show`)"
status: "review"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000004"
design: "docs/kanban/epics/epic-04-temporal-idea-lineage.md"
points: 4
labels: ["phase-1", "evidence", "provenance", "cli"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001202"]
---

# ENG-004A: Open exact historical evidence (`ep show`)

Given a section or revision expression, return the exact source span with its provenance.

## Acceptance criteria

- Displays source text from the exact blob (not the working tree) with repository, commit OID, author/commit times, exact path, heading path, byte/line span.
- Surrounding section context is available without changing the cited span; raw and rendered views both work.
- Links to the commit and parent/child revisions where available.
- An inaccessible Git object reports UNAVAILABLE — never a fabricated excerpt.

---
Implemented: parse-section-expression (path#heading@commit format), find-section-in-content (heading search by level), retrieve-evidence (full pipeline with UNAVAILABLE for missing Git objects), format-evidence-text/format-evidence-edn. 15 tests, 35 assertions, all green. Added read-blob to git.clj. No ep show CLI command yet — needs ports wiring. --tasks-dir docs/kanban

AUDIT 2026-07-12: status=done graded D. The completion comment honestly discloses 'No ep show CLI command yet — needs ports wiring' — yet the card was promoted to done anyway. Distinct failure shape from the 005x/006x cards: honest evidence, illegal transition. Domain pipeline (evidence retrieval, UNAVAILABLE semantics, 15 tests) is real; the declared user-facing deliverable is not. Would have been gated by: kanban rule that done = outcome accepted for the card's declared scope; a transition check requiring acceptance evidence at review->done. Demoting done->review; remaining work is CLI wiring + exercising criteria end-to-end. --tasks-dir docs/kanban

REVIEW 2026-07-13: request-changes. Independently confirmed: ep show does not exist. src/epiphany/infra/main.clj's dispatch (case command, lines 539-545) only wires register/search/status/serve; any other command, including show, falls through to 'Unknown command.' A grep across src/ and test/ for show/ep show returns zero hits. The domain-level pipeline described in the acceptance criteria is real and tested -- src/epiphany/domain/evidence.clj implements parse-section-expression, find-section-in-content, retrieve-evidence (with UNAVAILABLE semantics for inaccessible Git objects), and format-evidence-text/format-evidence-edn, backed by test/epiphany/domain/evidence_test.clj. clojure -M:unit-test passes (554 tests, 1421 assertions, 0 failures), but no test exercises a CLI show command because none exists. This matches and confirms the prior AUDIT comment's basis for demoting the card from done to review. Recommend keeping status at in_progress until CLI wiring (a run-show subcommand added to main.clj's dispatch, plus an integration/CLI test) lands. --tasks-dir docs/kanban

REVIEW-FAIL 2026-07-13: domain logic is solid and tested, but the CLI dispatch table only has register/search/status/serve. 'ep show' doesn't exist — nothing wires the domain function to a CLI subcommand. Recurring gap: domain logic exists but CLI integration is missing. --tasks-dir docs/kanban

FIX 2026-07-13: ep show now exists for real. Wired in src/epiphany/infra/main.clj (run-show), backed by the existing tested domain/evidence.clj pipeline, using real Git object access (epiphany.infra.git) against the repository at --repo (default .). Verified end-to-end against this repo's own history: 'ep show AGENTS.md@HEAD' returns real blob content with commit OID and line span; an unresolvable path correctly reports UNAVAILABLE with exit 1. Along the way found and fixed two real pre-existing bugs blocking this: (1) infra/git.clj read-blob called a nonexistent Repository.getObject method (JGit 7.3 has no such method) -- always threw; fixed to use .open + catch MissingObjectException, with new regression tests in git_commit_test.clj (read-blob-returns-real-content, read-blob-missing-oid-reports-failure-not-exception). (2) domain/evidence.clj incorrectly reported 'heading not found' failure even when no heading was requested at all -- fixed so full-content-no-heading is a clean success. New CLI tests: show-requires-expression, show-shows-help, show-retrieves-real-evidence-from-this-repo, show-reports-unavailable-for-missing-path. Full suite: 568 tests, 1456 assertions, 0 failures. NOT fully done against the AC: commit author/committer/time and parent/child revision links are not surfaced (evidence/retrieve-evidence never populates :evidence/commit-info or :evidence/parent-oids -- that's a real gap, not wired here). Moving to review, not done, until that's addressed or explicitly descoped. --tasks-dir docs/kanban
---
