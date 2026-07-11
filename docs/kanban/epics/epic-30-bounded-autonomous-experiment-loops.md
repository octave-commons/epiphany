---
id: 01900d7c-7f3a-7e8b-9c4d-000000000033
title: "Epic 30: Bounded Autonomous Experiment Loops"
status: incoming
type: epic
priority: low
phase: 4
design: docs/notes/design/phase-4-simulation-laboratory.md
size: 8
labels: [agents, autonomy, experiments, governance]
---

# Epic 30: Bounded Autonomous Experiment Loops

Allow agents to propose, run, critique, and learn from low-risk simulation experiments within fixed budgets and explicit human-defined objectives.

## User outcome

“The platform can continuously test bounded hypotheses and surface surprising patterns, while I retain control over objectives, models, resources, and interpretation.”

## Autonomous loop

```text
Observe evidence/results
  -> propose bounded hypothesis or parameter change
  -> static design/risk check
  -> select a low-cost approved experiment
  -> execute under resource policy
  -> evaluate against predefined metrics
  -> retain result and critique
  -> request human direction or schedule next bounded iteration
```

## Allowed loops

- parameter tuning
- retrieval/graph algorithm comparison
- simulation calibration on approved historical data
- visualization anomaly detection
- benchmark replication
- low-cost ablation studies

## Require human approval for

- new model families
- new external datasets
- materially higher resource budgets
- altered objectives/metrics
- politically, socially, or safety-sensitive scenarios
- public claims or publication drafts

## Critic agents challenge

- metric gaming
- data leakage
- invalid comparison
- confounding
- unsupported causal claims
- brittle conclusions

## Acceptance criteria

- Every autonomous action is linked to a task, budget, objective, policy, and trace.
- Agents cannot redefine their own success metric or resource cap.
- A loop commits only results that meet predeclared validity and improvement conditions.
- Negative/failed results are retained and influence future proposal ranking.
- The agent provides a concise human decision card when a conceptual, ethical, or strategic choice is required.
- Autonomous loops are benchmarked against manual baselines and can be paused globally.
- The system detects repetitive or non-informative experiment cycles and halts them.

## Next step

Design the autonomous experiment loop state machine and the critic-agent protocol.
