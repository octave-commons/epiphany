---
id: 01900d7c-7f3a-7e8b-9c4d-000000000034
title: "Phase 4 Cross-Cutting: Simulation Safety and Epistemic Governance"
status: incoming
type: cross-cutting
priority: low
phase: 4
design: docs/notes/design/phase-4-simulation-laboratory.md
size: 8
labels: [safety, governance, simulation, epistemics]
---

# Phase 4 Cross-Cutting: Simulation Safety and Epistemic Governance

Prevent a sophisticated simulation platform from manufacturing unjustified certainty.

## Required controls

- **Assumption visibility:** no result without assumptions attached.
- **Provenance:** every input, transformation, model, result, and chart carries source lineage.
- **Uncertainty:** calibrated intervals, scenario ranges, and missing-data warnings appear before conclusions.
- **Scope:** clearly distinguish exploratory models, validated models, and externally grounded analyses.
- **Human gatekeeping:** public/high-stakes scenarios require explicit review.
- **Privacy and ethics:** prohibit modeling identifiable individuals unless data rights, purpose, and review are explicit.
- **Adversarial robustness:** treat external data/code/models as untrusted until scanned, sandboxed, and provenance-checked.
- **Reproducibility:** retained manifests, images/containers, seeds, outputs, and failure records.
- **Cost accounting:** report compute, storage, and inference consumption per experiment.

## Next step

Define the simulation safety review checklist and the epistemic status taxonomy before any autonomous experiment loop is enabled.
