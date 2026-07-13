---
id: "01900d7c-7f3a-7e8b-9c4d-000000001502"
title: "ENG-005B: Serve the review inbox (`ep inbox`)"
status: "in_progress"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000005"
design: "docs/kanban/epics/epic-05-redundancy-tension-review.md"
points: 3
labels: ["phase-1", "review", "inbox", "cli"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001501"]
---

# ENG-005B: Serve the review inbox (`ep inbox`)

Filterable queue of unreviewed candidates with the evidence to judge them.

## Acceptance criteria

- Filters: relation type, confidence band, repository family, date range, generator version.
- Each item shows both exact source spans, surrounding context, scores, and why it was generated.
- Suppressed (do-not-suggest) candidates do not resurface by default.
- Keyboard-efficient triage: a decision takes one action from the list.

---
AUDIT 2026-07-12: status=done graded F. Headline deliverable 'ep inbox' does not exist — CLI dispatch (main.clj:539-545) handles only register/search/status/serve. domain/inbox.clj exists; none of the four acceptance criteria (filters, evidence display, suppression, keyboard triage) is observable from a CLI that lacks the command. The workbench inbox *page* is a different surface (ENG-006C) and is itself backed by placeholder candidate data. No completion evidence recorded. Would have been gated by: kanban completion-evidence rule, ENG-017G command contracts, ADR-004 rule 7. Demoting done->review. --tasks-dir docs/kanban

REVIEW 2026-07-13: request-changes. ep inbox does not exist -- main.clj:539-544's dispatch only handles register/search/status/serve, confirmed by grepping the whole src/ tree. The only inbox surface is the workbench web page (http.clj:300-305, workbench.clj:432-469), and that page itself is stubbed: the HTMX list handler returns placeholder data (workbench.clj:456) and the decide handler discards the actual decision, always returning an empty list (workbench.clj:461-469) -- so accept/reject don't record anything. The pure domain/inbox.clj logic is good (relation/confidence/generator/date filters, ranking, limit, exclude-already-decided) and well unit-tested (inbox_test.clj, 13 tests), but it's missing the 'repository family' filter named explicitly in the AC, and conflates 'already decided' with 'suppressed/do-not-suggest' rather than implementing that distinction from review.clj's visible-decisions. Full suite passes (554 tests/1421 assertions/0 failures) but that only exercises the isolated domain function, not the actually-shippable feature. Moving back to in_progress; domain logic is a solid foundation but the CLI command and a real, non-placeholder consuming surface still need to be built. --tasks-dir docs/kanban

REVIEW-FAIL 2026-07-13: (1) 'ep inbox' doesn't exist in CLI dispatch. (2) Web fallback UI is stubbed — placeholder data, decisions aren't persisted. --tasks-dir docs/kanban

FOLLOW-UP 2026-07-13: investigated whether this is fixable alongside ep show/diff/trace. It is not, honestly -- the actual blocker is one level deeper than 'CLI wiring is missing.' There is no durable, queryable store for lineage candidates or review decisions anywhere in the codebase (confirmed via grep across law/ports.clj, in_memory.clj, mongo.clj). ENG-005A ('Record review decisions as append-only events'), which this card depends on and which was marked done, turns out to have the same false-done pattern -- no port was ever built, just demoted separately with a comment on that card. I'm not fixing ep inbox now because doing so honestly requires building that storage/query layer first (ENG-005A's real scope), not something this card's own wiring gap. Leaving at in_progress; do not re-promote to done until ENG-005A has a real port and ep inbox can query real candidates/decisions through it. --tasks-dir docs/kanban
---
