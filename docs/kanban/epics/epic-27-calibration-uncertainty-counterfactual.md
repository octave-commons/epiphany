---
id: 01900d7c-7f3a-7e8b-9c4d-000000000030
title: "Epic 27: Calibration, Uncertainty, and Counterfactual Analysis"
status: icebox
type: epic
priority: low
phase: 4
design: docs/notes/design/phase-4-simulation-laboratory.md
size: 8
labels: [calibration, uncertainty, sensitivity, counterfactuals]
---

# Epic 27: Calibration, Uncertainty, and Counterfactual Analysis

Make models useful for research by testing their fit, identifying sensitivity, and separating robust results from artifacts of arbitrary assumptions.

## User outcome

“I can tell whether a result is stable, what assumptions drive it, what evidence supports calibration, and what I would need to observe to reduce uncertainty.”

## Scope

Implement parameter sweeps, Monte Carlo/seed ensembles, sensitivity analysis, scenario comparison, calibration against historical data where appropriate, holdout validation, backtesting, and counterfactual analysis with explicitly bounded causal assumptions.

Separate uncertainty categories: measurement/data, model-structure, parameter, scenario, computational/numerical, unknown/uncaptured.

Produce uncertainty-aware result structures with estimate, interval, sensitivity, calibration status, and limitations.

## Acceptance criteria

- A simulation cannot be presented as validated unless it passes a declared validation procedure.
- Result charts distinguish individual runs, ensembles, intervals, and observed reference values.
- Counterfactual questions require explicit intervention and causal-assumption declarations.
- Sensitivity reports identify which inputs dominate output variation.
- Calibration datasets are versioned and never mixed with evaluation datasets without disclosure.
- The system flags overfitting risks, missing data, insufficient repetitions, and unsupported causal interpretation.
- “Inconclusive” is a valid first-class outcome.

## Next step

Design the uncertainty taxonomy and the analysis result schema.
