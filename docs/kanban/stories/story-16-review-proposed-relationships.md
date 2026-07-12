---
id: "01900d7c-7f3a-7e8b-9c4d-000000001016"
title: "US-016: Review proposed relationships"
status: "breakdown"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000005"
design: "docs/designs/phase-1-corpus-archaeology.md"
points: 8
labels: [review, candidates, inbox, relations, contradiction, decomposed]
category: "stories"
---

# US-016: Review proposed relationships

## Acceptance Criteria

- The inbox can filter candidates by relation type, confidence band, repository family, date range, and model/generator version.
- Each item shows both exact source spans, surrounding context, scores/signals, and why it was generated.
- The user can accept, reject, relabel, defer, annotate, or mark "do not suggest similar."
- A review action appends a durable event; it does not rewrite the original candidate or Git evidence.
- A rejected candidate remains available in audit mode.
- The default inbox avoids repeatedly surfacing candidates marked "do not suggest similar."
- The review action can optionally create a research question or a user-curated concept.

## Notes

**As a corpus owner,** I want a review inbox for candidate links so that I can turn useful proposals into trusted local knowledge without accepting model guesses blindly.

## Decomposed into

Product-outcome card; do not implement directly. The engineering slices:

- **ENG-005A** `story-05a-review-decision-events.md` — append-only decision events (3)
- **ENG-005B** `story-05b-review-inbox.md` — inbox + `ep inbox` (3)
- **ENG-005C** `story-05c-redundancy-contradiction-candidates.md` — deterministic redundancy/contradiction candidates (4)
