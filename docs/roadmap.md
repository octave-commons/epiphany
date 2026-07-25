---
slug: epiphany-roadmap
uuid: 802cea43-4691-4e44-a9ca-5aefd16c78b9
kind: roadmap
status: active
title: "Epiphany Roadmap: from Phase-1 completion to all 30 epics"
description: "Tracks work from the current state (phase 0/1 engineering complete, US/epic parents verified) through phases 2–4, with working order, phase gates, and dependency edges. Supersedes the phase-1-only view in docs/kanban/BOARD-BREAKDOWN.md for phase ≥ 2 planning; the board contract (docs/kanban/AGENTS.md) remains authoritative for card mechanics."
created: "2026-07-25"
---

# Epiphany Roadmap

Current state → end state of all 30 epics, and the working order between.

## Where we are (2026-07-25)

**Done:** Phase 0 baseline and phase-1 engineering — 60+ cards across
epics 1–6 (archaeological ledger, markdown extraction, retrieval
substrate, temporal lineage, review, workbench) plus the ENG-017
assurance lane (contract-first verification: schema registry,
validating ports, Mongo/in-memory differential law suites, generative +
epistemic laws, CI evidence artifact, sabotage mutants). The CLI
vertical works end-to-end: `ep register → ep ingest → ep search/show/
diff/trace/inbox/export/status`, backed by MongoDB (durable
observations), Lucene (durable index), and Ollama (embeddings), with a
shared CLI/HTTP command seam. 743 unit tests green; integration suite
green against local services.

**In flight:** US-story and epic parents (breakdown lane) are being
verified against their acceptance criteria before closure
(ultra-code wave `us-parent-verification`).

**Not started:** Phases 2–4 (epics 7–30, all icebox).

## Phase gates (from the board contract)

Phase N+1 epics leave the icebox only when the phase-N **workbench
outcome is demonstrable** — not when cards are merely done. The gate
evidence for each phase below is the demo that must exist before
breakdown begins on the next phase.

## Working order

### Phase 1 closure (now)

1. Close verified US/epic parents after the verification wave
   (unmet criteria become new cards, parents stay open until met).
2. Phase-1 exit demo (gate evidence): against a real corpus,
   `ep ingest` a repo family, then from the workbench: search an idea,
   open its evidence, compare two expressions, trace its lineage,
   review a candidate, export the packet. Record the demo as a comment
   on epic-06.

### Phase 2 — Code archaeology (epics 7–13)

Goal: the same archaeology the system does for Markdown, done for
source code, and grounded against the phase-1 concepts.

| Order | Epic | Depends on | First breakdown target |
|---|---|---|---|
| 1 | **07 Polyglot Source Ledger** | phase-1 gate | language-detection + parser-strategy registry; code blobs into the same observation model |
| 2 | **08 Syntax Forest** | 07 | Tree-sitter grammar set + normalized node-stream schema |
| 3 | **09 Clojure Semantic Intelligence** | 08 | clj-kondo analysis export ingestion + semantic fact schema |
| 4 | **10 Program Relationship Graph** | 07–09 | relationship-type vocabulary + graph projection schema |
| 5 | **11 Concept-to-Code Grounding** | 10, phase-1 concepts | candidate scoring + review workflow for concept↔code links |
| 6 | **12 Architectural Boundary Inference** | 10, 11 | multi-view similarity + clustering, all tiers explicit |
| 7 | **13 Code Archaeology Workbench** | 11, 12 | code workbench views (capstone demo) |

Phase-2 exit demo: start from a phase-1 concept note, land on the
implementing namespaces, tests, and historical commits — or learn it
was never implemented.

### Phase 3 — Governed external research (epics 14–21)

Goal: arXiv/GitHub/etc. intake under governance, research knowledge
graph, gap detection, and bounded research agents that consume the
board contract.

| Order | Epic | Depends on | First breakdown target |
|---|---|---|---|
| 1 | **14 Governed External Source Registry** | phase-2 gate | source registry schema + policy DSL |
| 2 | **15 External Artifact Ingestion** | 14 | arXiv + GitHub adapters first, then Hugging Face |
| 3 | **16 Research Component Extraction** | 15 | research component schema + extraction pipeline |
| 4 | **17 Research Knowledge Graph** | 16 | relationship vocabulary + evidence ranking |
| 5 | **18 Prior-Art Gap & Contradiction** | 17 | gap/contradiction rules + risk-screen checklist |
| 6 | **19 Taxonomy & Research-Question Studio** | 17, 18 | taxonomy schema + question-generation pipeline |
| 7 | **20 Experiment Design & Reproducibility** | 19 | experiment spec schema + static design-check rules |
| 8 | **21 Bounded Research Agents** | 18, 20, phase-2 grounding | agent task envelope + tool registry + approval policy |

Phase-3 exit demo: a bounded agent answers "what is known and unknown
about X" with cited internal (phase 1–2) and external (phase 3)
evidence, and files its findings as reviewable board cards.

### Phase 4 — Simulation laboratory (epics 22–30)

Goal: a local-first simulation kernel with environmental and agent
models, uncertainty discipline, and bounded autonomous loops.

| Order | Epic | Depends on | First breakdown target |
|---|---|---|---|
| 1 | **22 Simulation Kernel** | phase-3 gate | simulation manifest schema + execution kernel interface |
| 2 | **23 Spatial-Temporal Data Fabric** | 22 | spatial-temporal entity schema + STAC-compatible catalog |
| 3 | **24 Weather & Environmental Modeling** | 22, 23 | environmental state contract + scenario perturbation |
| 4 | **25 Agent Behavioral Modeling** | 22 | agent-based model contract + starter templates |
| 5 | **26 Graph Dynamics & Semantic Physics** | 23, 24, 25 | ACO model spec + baseline comparison protocol |
| 6 | **27 Calibration, Uncertainty & Counterfactual** | 26 | uncertainty taxonomy + analysis result schema |
| 7 | **28 Distributed Experiment Scheduling** | 27 | second-machine scheduling (the clustering machine) |
| 8 | **29 Simulation Visual Analytics** | 26, 28 | visual analytics workbench views |
| 9 | **30 Bounded Autonomous Experiment Loops** | 27, 28, 29 | bounded loop policy + kill-switch semantics (capstone) |

Phase-4 exit demo: a calibrated weather/agent simulation whose runs
are reproducible, uncertainty-explicit, and stoppable — scheduled
across the two machines.

## Standing constraints (all phases)

- **The one domain rule** — observed → derived → provisional →
  accepted; promotion only through durable, evidence-preserving events.
  Applies to code edges, research claims, and simulation outputs alike.
- **ADR-000 data authority** — Git canonical, Mongo durable, indexes
  rebuildable. Every new store introduced by an epic joins this rule.
- **ADR-004 verification** — every new write op gets a registered
  schema, law-suite coverage (the harness now fails loudly without one),
  and generative laws where the record kind admits them.
- **5-point cap** — no card leaves breakdown over 5 points; epics above
  are broken into engineering slices at phase entry, not before.
- **One strong machine first** — clustering is deferred until epic 28.

## Near-term queue (after phase-1 closure)

1. Epic-07 breakdown: language-detection + parser-strategy registry,
   code-blob observation schema (schemas precede adapters, per the
   board's hard rule 4).
2. Epic-08 breakdown: Tree-sitter grammar set + normalized node stream.
3. Phase-2 gate demo card (epic-13 capstone demo, defined at 13
   breakdown time).

## Risks

- **Phase-2 scope pull** — polyglot breadth vs. Clojure depth. Epic 09
  (Clojure semantics) is where the system's own dogfood lives; resist
  flattening it into generic AST work.
- **Tree-sitter operational weight** — grammar binaries per language;
  prefer a small grammar set (clojure, python, javascript, markdown)
  over breadth.
- **Phase-3 governance cost** — the registry/policy DSL (epic 14) is
  the gate against ungoverned scraping; do not let 15 leapfrog it.
- **Phase-4 compute realism** — one machine until epic 28; simulation
  scale targets must respect that or 22–27 will be toy-grade by
  accident.
