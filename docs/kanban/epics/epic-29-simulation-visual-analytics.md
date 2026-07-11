---
id: 01900d7c-7f3a-7e8b-9c4d-000000000032
title: "Epic 29: Simulation Visual Analytics Workbench"
status: icebox
type: epic
priority: low
phase: 4
design: docs/notes/design/phase-4-simulation-laboratory.md
size: 8
labels: [visualization, simulation, workbench, analytics]
---

# Epic 29: Simulation Visual Analytics Workbench

Make simulation output inspectable through maps, timelines, distributions, graph views, and comparison tools—not merely static charts or opaque “AI conclusions.”

## User outcome

“I can understand what happened in a simulation, compare scenarios, inspect uncertainty, trace data lineage, and identify the next question to investigate.”

## Core views

- **Scenario composer:** assumptions, inputs, interventions, resource estimates, approval state.
- **Spatial-temporal map:** layers for observed data, forecast data, synthetic conditions, agent state, and simulation outputs.
- **Timeline explorer:** events, state transitions, interventions, uncertainty intervals, and selected entity tracks.
- **Parameter-space explorer:** sweep matrices, parallel coordinates, response surfaces, and sensitivity rankings.
- **Distribution/ensemble view:** individual trajectories, percentile bands, histograms, failure/outlier runs.
- **Graph-dynamics view:** node/edge state, signals/particles, traversal paths, cluster evolution, cost fields.
- **Evidence panel:** source artifacts, assumptions, code/model version, calibration evidence, and known limitations.
- **Comparison workspace:** baseline vs. intervention, historical vs. synthetic, model A vs. model B.
- **Run ledger:** resource cost, queue time, failures, retries, reproducibility state, and exportable manifests.

## Acceptance criteria

- No visualization hides uncertainty by default when uncertainty data exists.
- Every visible result can be traced to an experiment manifest and input artifacts.
- Maps clearly distinguish observed, forecast, inferred, and simulated layers.
- Visualizations support time-window selection, scenario comparison, and evidence drill-down.
- Large datasets use progressive loading, aggregation, and bounded detail.
- Every visualization can export a data/provenance bundle suitable for a research notebook or paper figure workflow.
- The system marks exploratory simulations as exploratory in the UI.

## Next step

Design the simulation workbench layout and view interactions.
