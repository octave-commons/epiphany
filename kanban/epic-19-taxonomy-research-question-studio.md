---
id: 01900d7c-7f3a-7e8b-9c4d-000000000021
title: "Epic 19: Taxonomy and Research-Question Studio"
status: incoming
type: epic
priority: medium
phase: 3
design: docs/notes/design/phase-3-research-operations.md
size: 8
labels: [taxonomy, research-questions, llm, schema]
---

# Epic 19: Taxonomy and Research-Question Studio

Use LLMs to propose taxonomies, research questions, and classification schemas from grounded evidence, while keeping humans in control of the conceptual vocabulary.

## User outcome

“I can ask the system to organize a new research area, show competing taxonomies, identify ambiguities, and turn a real corpus gap into a crisp question.”

## Scope

Generate taxonomy candidates from selected artifact sets: topics/subtopics, tasks, methods, data types, evaluation metrics, limitations/failure modes, governance/risk categories.

Preserve multiple competing taxonomies rather than forcing one hierarchy.

Generate research-question candidates from explicit contradictions, missing evidence, underexplored combinations, local implementation capabilities, and feasible public datasets/compute budgets.

Question template:

```clojure
{:question/id ...
 :question/text ...
 :motivation [...]
 :claims-to-test [...]
 :scope {:population ... :task ... :conditions ...}
 :prior-art [...]
 :candidate-methods [...]
 :candidate-datasets [...]
 :candidate-metrics [...]
 :known-risks [...]
 :resource-estimate ...
 :status :proposed | :under-review | :approved | :rejected}
```

## Acceptance criteria

- Every taxonomy node and research-question candidate cites local/external evidence.
- The user can split, merge, rename, reject, or create taxonomy concepts.
- Candidate questions specify falsifiable claims or explicitly state why they are exploratory.
- The system generates at least one alternative framing and one strongest-obvious objection for each research question.
- Research questions are filtered through license, source-trust, compute, and ethics/governance policies.
- The system remembers accepted/rejected taxonomy decisions as review events.

## Next step

Design the taxonomy schema and the question-generation prompt pipeline.
