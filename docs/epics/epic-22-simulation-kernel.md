---
id: 01900d7c-7f3a-7e8b-9c4d-000000000025
title: "Epic 22: Simulation Kernel and Experiment Ledger"
status: incoming
type: epic
priority: low
phase: 4
design: docs/notes/design/phase-4-simulation-laboratory.md
size: 8
labels: [simulation, kernel, experiments, reproducibility]
---

# Epic 22: Simulation Kernel and Experiment Ledger

Establish one data-oriented, reproducible contract for all simulations, whether they are graph dynamics, agent-based scenarios, weather models, or external scientific tools.

## User outcome

“Every simulation is a first-class research artifact: I can inspect its inputs, assumptions, code, environment, outputs, uncertainty, and lineage—and rerun it later.”

## Scope

Create a versioned simulation manifest extending the Phase 3 experiment contract with kind, scenario, model, inputs, parameters, assumptions, interventions, outputs, metrics, validation plan, uncertainty plan, environment, resources, and provenance.

Support three execution interfaces:

- **Native Clojure simulation:** pure state-transition functions plus explicit effects at the outer boundary.
- **Containerized simulation:** external Python, Rust, Julia, Modelica, or domain-tool executables with pinned environment.
- **FMI/co-simulation adapter:** standardized exchange/execution of compatible external dynamic models.

## Acceptance criteria

- A simulation cannot execute without a pinned manifest, resource budget, and approval state.
- Every run records code/data/model revisions, container/image digest, seed, environment, hardware class, start/end time, and exit state.
- Outputs are immutable artifacts linked to their manifest.
- Any run can be replayed from retained inputs, subject to documented nondeterministic behavior.
- A failed or cancelled run remains visible and analyzable.
- The platform can diff two manifests and identify exactly what changed.
- Simulation state transitions, logs, and metric emissions correlate to a trace/run ID.

## Domain rule

A simulation is an argument under assumptions, not a forecast and not a discovered fact.

## Next step

Design the simulation manifest schema and the execution kernel interface.
