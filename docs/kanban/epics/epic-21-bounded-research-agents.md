---
id: 01900d7c-7f3a-7e8b-9c4d-000000000023
title: "Epic 21: Bounded Research Agent Workflows"
status: incoming
type: epic
priority: medium
phase: 3
design: docs/notes/design/phase-3-research-operations.md
size: 8
labels: [agents, workflows, research, governance]
---

# Epic 21: Bounded Research Agent Workflows

Introduce bounded research agents that can acquire evidence, draft analyses, propose designs, and run safe evaluations—but never collapse the human research process into an opaque autonomous loop.

## User outcome

“Agents continuously keep the research map current and prepare useful proposals; I intervene at decisions that require judgment, values, or a change in research direction.”

## Agent roles

- **Scout agent:** watches allowlisted sources and proposes artifacts for ingestion.
- **Reader agent:** extracts components and creates evidence-linked literature briefs.
- **Prior-art agent:** answers bounded “what existing work resembles this?” tasks.
- **Critic agent:** finds scope mismatch, missing baselines, threats to validity, and strongest counterarguments.
- **Designer agent:** composes candidate experiment specifications.
- **Reproduction agent:** attempts approved low-risk reruns or benchmark evaluations.
- **Librarian agent:** proposes taxonomy changes, deduplication, source-trust adjustments, and link repairs.
- **Supervisor gate:** human approval and policy engine before expensive or high-impact actions.

## Acceptance criteria

- Every agent action has an assigned task, tool permissions, time/token/resource budget, and trace.
- Agents write proposals and evidence packets; they do not directly promote claims, change trusted schemas, or publish results.
- Expensive downloads, model runs, external writes, and experiment execution require explicit approval policies.
- Agent performance is evaluated separately for retrieval, extraction, critique, design, and execution.
- The system records task outcomes, reviewer feedback, and failure categories for future agent improvement.
- Agents can request a human decision with a concise decision card: context, options, evidence, consequence, and reversibility.

## Next step

Design the agent task envelope, tool registry, and approval policy engine.
