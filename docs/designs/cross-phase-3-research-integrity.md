---
id: 01900d7c-7f3a-7e8b-9c4d-000000000024
title: "Phase 3 Cross-Cutting: Research Integrity and Governance"
status: incoming
type: cross-cutting
priority: medium
phase: 3
design: docs/notes/design/phase-3-research-operations.md
size: 8
labels: [governance, integrity, provenance, ethics]
---

# Phase 3 Cross-Cutting: Research Integrity and Governance

Make the platform suitable for honest, inspectable research rather than merely fast content synthesis.

## Required controls

- Provenance and source-tier visible in every answer, graph edge, and experiment plan.
- Copyright/license and terms metadata preserved for external artifacts.
- Explicit separation between observed data, author claim, model extraction, agent hypothesis, human-accepted interpretation, and experimental result.
- Dataset documentation, access restrictions, and known limitations surfaced before use.
- Reproducibility manifests and immutable result artifacts.
- Evaluation against human-curated research tasks.
- Clear uncertainty language and no unsupported novelty/reliability claims.
- Red-team review for source poisoning, prompt injection in crawled content, malicious repository content, and contaminated dataset/model cards.

## Next step

Define the research-integrity controls and review gates before any external source is connected or autonomous agent is deployed.
