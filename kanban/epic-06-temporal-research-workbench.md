---
id: 01900d7c-7f3a-7e8b-9c4d-000000000006
title: "Epic 6: Temporal Research Workbench"
status: incoming
type: epic
priority: high
phase: 1
design: docs/notes/design/phase-1-corpus-archaeology.md
size: 8
labels: [ui, visualization, workbench, timeline]
---

# Epic 6: Temporal Research Workbench

Provide the first interface where archaeology is usable, reviewable, and genuinely interesting.

## User outcome

“I can follow an idea’s history as a map, pivot into original evidence, review candidate relationships, and ask grounded questions over my corpus.”

## Views

- **Timeline:** commits, revisions, section expressions, lineage candidates, accepted transitions.
- **Concept/idea map:** nodes clustered by hybrid retrieval; edges styled by relation type and confidence.
- **Evidence drawer:** exact source span, full section context, commit metadata, source diff.
- **Candidate review inbox:** duplicates, contradictions, supersessions, lineage suggestions.
- **Search workspace:** lexical/semantic/hybrid mode, filters, score explanation.
- **Corpus health panel:** unparsed revisions, extraction errors, index age, queue backlog, confidence distribution.

## Core loop

1. Search or select an idea.
2. Inspect evidence.
3. Traverse timeline/lineage.
4. Review suggested relationships.
5. Record a decision or research question.
6. Re-run projections and observe improved retrieval.

## Acceptance criteria

- From any graph edge, the user can open its source evidence in one interaction.
- From any section, the user can open its commit/revision timeline.
- The UI makes clear whether an edge is observed, inferred, or human-accepted.
- Search results and graph views share stable IDs and do not disagree about source lineage.
- The review queue supports keyboard-efficient triage and preserves rationale.
- The system can export a lineage packet: selected idea, timeline, sources, candidate/accepted edges, and review decisions.

## Next step

Design the first minimal workbench views (search + evidence drawer + timeline) before adding graph visualization.
