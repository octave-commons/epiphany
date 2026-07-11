---
id: 01900d7c-7f3a-7e8b-9c4d-000000000005
title: "Epic 5: Redundancy and Tension Review"
status: accepted
type: epic
priority: high
phase: 1
design: docs/notes/design/phase-1-corpus-archaeology.md
size: 8
labels: [review, contradiction, duplication, consolidation]
---

# Epic 5: Redundancy and Tension Review

Surface note pairs or clusters that are duplicates, near-duplicates, supersessions, or possible contradictions—and make review safe.

## User outcome

“I can reduce note clutter without erasing the history of my thought, and I can identify where I have changed my mind or left incompatible assumptions unresolved.”

## Scope

Generate review candidates from hybrid retrieval and temporal proximity. Classify relationships into: `:duplicate`, `:near-duplicate`, `:complementary`, `:superseded`, `:possible-contradiction`, `:unclear`.

Detect deterministic contradiction patterns first: explicit negation, mutually exclusive values, incompatible dates/statuses, “always/never” versus scoped counterexamples, incompatible architecture decisions under the same scope.

Use LLMs for bounded comparison: extract claims and scope, identify the minimum conflicting proposition, quote source spans, produce confidence and alternative interpretations.

Make every action reversible: keep, link, mark superseded, merge into a synthesis note, archive, ignore, create research question.

## Acceptance criteria

- The system never deletes a note automatically.
- Every proposed relationship has at least two source spans and a score/rationale.
- Review actions create events, not destructive edits.
- A “merge” produces a synthesis artifact that links to its source notes.
- A “superseded” relation does not remove old notes from historical or lineage search.
- The user can tune candidate volume and confidence thresholds.
- Candidate classifiers can be evaluated against a human-labeled review set.

## Quality rule

“Low quality” must never mean “not useful.” Distinguish low informational density, obsolete claim, unfinished fragment, duplicate expression, private emotional/contextual trace, and historically important precursor.

## Next step

Design the claim-normalization schema and the contradiction-detection pipeline.
