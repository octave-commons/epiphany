---
id: "01900d7c-7f3a-7e8b-9c4d-000000001502"
title: "ENG-005B: Serve the review inbox (`ep inbox`)"
status: review
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
---
