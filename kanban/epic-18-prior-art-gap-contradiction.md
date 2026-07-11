---
id: 01900d7c-7f3a-7e8b-9c4d-000000000020
title: "Epic 18: Prior-Art, Gap, and Contradiction Analysis"
status: incoming
type: epic
priority: medium
phase: 3
design: docs/notes/design/phase-3-research-operations.md
size: 8
labels: [prior-art, gap-analysis, contradiction, research]
---

# Epic 18: Prior-Art, Gap, and Contradiction Analysis

Identify what has likely already been tried, where local/external claims diverge, and where a question is genuinely unresolved enough to justify research.

## User outcome

“Before I build or write, I can see the adjacent literature, existing implementations, known failure modes, and the exact gap I might be able to investigate.”

## Scope

- **Prior-art search:** map local concepts/designs to external methods/tasks/implementations; identify direct matches, close analogues, and missing comparisons; highlight explicit citations and temporal precedence.
- **Gap analysis:** unsupported local claims, local ideas with no known external match, external open problems with relevant local assets, underexplored method/dataset/metric combinations, contradictory reported outcomes.
- **Contradiction analysis:** extract claim scope, reject false contradictions caused by different scope, flag potential conflicts for review.
- **Risk screen:** missing control/baseline, inappropriate metric, data leakage, underpowered sample, inaccessible data, incompatible license, compute cost beyond budget, unfalsifiable claims.

## Acceptance criteria

- Every gap/risk/contradiction candidate links to the evidence that generated it.
- The system can state why two apparently conflicting papers may not actually conflict.
- “Novelty” is never declared; use calibrated language such as “no close prior art found within configured sources and search coverage.”
- Prior-art searches preserve exact query, source coverage, dates, and retrieval configuration.
- The user can promote a candidate gap into a research question or dismiss it with a reason.
- The system records false positives to improve future screening.

## Critical rule

Absence of retrieved evidence is not evidence of novelty.

## Next step

Design the gap/contradiction detection rules and the risk-screen checklist.
