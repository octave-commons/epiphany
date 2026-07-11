---
title: Research to Design Implications — Deep Research Synthesis
slug: research-to-design-implications
created: 2026-07-11
kind: research
sources:
  - docs/notes/research/nlp-and-code-intelligence-deep-research.md
  - docs/notes/research/vector-search-and-knowledge-graphs-deep-research.md
  - docs/notes/research/auto-research-agents-deep-research.md
  - docs/notes/research/agent-governance-and-ethics-deep-research.md
designs:
  - docs/notes/design/knowledge-platform-overview.md
  - docs/notes/design/phase-1-corpus-archaeology.md
  - docs/notes/design/phase-2-code-comprehension.md
  - docs/notes/design/phase-3-research-operations.md
  - docs/notes/design/phase-4-simulation-laboratory.md
---

# Research to Design Implications — Deep Research Synthesis

This note synthesizes the four deep-research notes in `docs/notes/research/` and maps their findings to concrete design changes. The goal is to bootstrap the project following the Research → Design → Decide → Task process.

## Executive summary

The deep research broadly **supports** the platform’s design direction: hybrid retrieval, immutable artifacts, provenance, graph traversal, and bounded autonomous agents are all well grounded in recent literature. However, several design claims need **qualification** and a few are **contradicted**. The most important cross-cutting changes are:

1. Align the platform architecture with ADR-000 (Elasticsearch + Neo4j + PostgreSQL) rather than the inbox note’s ArangoDB preference.
2. Make lexical/embedding baselines the default for text classification, reserving LLM-as-classifier for truly dynamic taxonomies.
3. Treat LLM-based extraction, lineage, and contradiction detection as candidate-generation stages with mandatory human review and explicit benchmarks.
4. Add a proactive governance layer (guardrail agents, moral ODD, conflict resolution, HOTL thresholds) on top of the existing human-review gates.
5. Use OSS Vizier or Optuna as the Phase 4 optimization backend, wrapped in static design checks and critic agents.

## Design contradictions and resolutions

| Design area | Original claim | Research finding | Resolution |
|---|---|---|---|
| Platform architecture | Knowledge-platform-overview leaned toward ArangoDB as a unified multi-model store. | Research and ADR-000 favor best-of-breed engines (ES + Neo4j + Postgres) for a search-heavy, graph-heavy, event-sourced platform. | Update the design overview to adopt ADR-000’s stack. Keep ArangoDB as a future consolidation experiment only. |
| Text classification | LLM-as-classifier was listed as a normal option for “few labels, complex policy, changing taxonomy.” | Fine-tuned encoders and even lexical baselines are often cheaper, faster, and similarly accurate for fixed-label tasks. LLMs are 1–2 orders of magnitude more expensive. | Add a tiered-classification policy: baseline first, then fine-tuned encoder, then LLM only with cost accounting. |
| Tree-sitter | Proposed for all Phase 2 languages. | Tree-sitter coverage is broad but uneven; Clojure/ClojureScript needs a separate semantic path (clj-kondo/rewrite-clj). | Add a parser-quality matrix and a Clojure-specific analysis path. |
| Auto-research loops | Long-running loops could improve spotting of subtle flaws. | InquiTree finds “cognitive tunneling” in long-horizon agent interactions; OpenAI/DeepSeek literature shows open-ended literature discovery remains <10% accurate. | Restrict autonomous loops to machine-checkable objectives; require human review for open-ended synthesis. |
| Scientific review | Critic agents can flag metric gaming, confounding, etc. | SoundnessBench shows LLM reviewers have optimism bias and are not reliable first-gate evaluators. | Use critic agents as filters, not final gates; validate critic agents against human-curated cases. |
| Agent governance | Human-in-the-loop gates for all high-stakes actions. | Literature warns pure HITL becomes a bottleneck; recommends human-on-the-loop with confidence thresholds and proactive guardrail agents. | Add HOTL thresholds, guardrail agents, behavioral baselines, and dynamic escalation. |

## Supported design claims

- Hybrid retrieval (BM25 + dense vector + metadata) outperforms single-signal retrieval; start with RRF and add calibrated linear combination once a benchmark exists.
- Tree-sitter is a suitable polyglot CST substrate for the syntax forest, with the Clojure caveat above.
- clj-kondo exports the namespace/var/reference/dependency facts needed for Phase 2 Clojure semantic analysis.
- Multi-view clustering (dependency + text + folder structure) is supported by SARIF as the strongest architecture-recovery approach.
- Provenance, candidate status, and human review are essential for any LLM- or extraction-derived graph edges.
- The platform’s rejection of “calling an LLM the scientist” is well justified by SoundnessBench and the Calibration Turn.
- Simulations should be framed as arguments under assumptions with explicit uncertainty, matching IPCC-style guidance.

## Cross-cutting design implications

### 1. Evaluation benchmarks must precede UI polish

Multiple research findings (classification benchmarks, AutoResearchBench, SWE-bench, SoundnessBench) show that unmeasured agent/NLP systems are easy to build and hard to trust. The designs should require:

- A Phase 1 note-pair benchmark (duplicate / contradiction / continuation / unrelated) before tuning the contradiction pipeline.
- A Phase 2 architecture-recovery benchmark (namespace boundaries, concept-to-code links, intentional bridges) before boundary inference.
- A Phase 3 critic-agent validation harness before trusting critic agents.
- A Phase 4 local benchmark comparing lexical-only, vector-only, and hybrid retrieval on the actual corpus.

### 2. Cost and latency must be first-class constraints

The research notes that LLMs are 1–2 orders of magnitude more expensive and slower than fine-tuned encoders for fixed-label tasks. The designs should add:

- A cost/latency budget per model call and per agent action.
- A content-addressed tiered cache (already in the overview) with explicit invalidation rules.
- An abstention path for low-confidence or high-cost queries.

### 3. Provenance and uncertainty propagation must be explicit

Research supports the platform’s emphasis on provenance, but the designs should make uncertainty propagation concrete:

- Every model output must carry model/version, prompt/template version, config hash, source evidence, confidence/calibration, and human-review status.
- Use IPCC-style confidence and likelihood language where quantification is justified.
- Distinguish observed, tool-derived, inferred, LLM-proposed, and human-accepted edges in the graph.

### 4. Governance must be proactive, not only reactive

The multi-agent governance decision record is well aligned on traceability and review gates, but literature (NIST AI RMF, AOM, meaningful human control) recommends adding:

- A moral operational design domain (moral ODD) for each agent class.
- Least-privilege agent identities and action logging.
- Guardrail agents and behavioral baselines.
- Confidence-based escalation from human-on-the-loop to human-in-the-loop.
- Conflict-resolution protocols for multi-agent coordination.
- Periodic red-teaming and adversarial testing.

### 5. Reproducibility contracts need a concrete backend

OSS Vizier or Optuna should back the Phase 4 parameter-sweep and optimization workflows. The designs should map their study/trial abstractions to the platform’s simulation manifest and experiment ledger.

## Recommended design changes

### A. Update `knowledge-platform-overview.md`

- Replace the ArangoDB-centric stack table with the ADR-000 stack (K3s, PostgreSQL, MinIO, Elasticsearch, Neo4j, NATS JetStream, Clojure, Tree-sitter, clj-kondo, local GPU inference).
- Explain why best-of-breed is better justified than unified multi-model for this workload, citing the vector/KG research.
- Note that ArangoDB remains a future consolidation experiment if operational complexity becomes a measured problem.
- Add a “Research grounding” section referencing the deep-research notes.

### B. Update `phase-1-corpus-archaeology.md`

- Add a tiered classification strategy for redundancy/tension/review: lexical baseline → embedding/fine-tuned → LLM with cost accounting.
- Specify hybrid-ranking approach: start with RRF, then evaluate linear combination once a 30–50 question benchmark exists.
- Add a note on chunking and long-document handling for embeddings and LLMs.
- Make the contradiction pipeline explicitly a candidate-generation stage with human review and evaluation benchmarks.
- Reference the NLP deep-research findings.

### C. Update `phase-2-code-comprehension.md`

- Add a parser-quality matrix for Tree-sitter with per-language maturity and error-telemetry plans.
- Specify that Clojure/ClojureScript use clj-kondo (and optionally rewrite-clj) for semantic analysis, not Tree-sitter alone.
- Anchor multi-view boundary inference in SARIF: dependency + text + folder structure as primary views; co-change and AST as secondary signals.
- Add evaluation benchmark requirements before boundary recommendations are promoted.
- Reference the code-intelligence deep-research findings.

### D. Update `phase-3-research-operations.md`

- Add a Research Question Certificate pattern (assumptions, falsifiable hypothesis, minimal test, failure update rule) to the Taxonomy and Research-Question Studio.
- Specify that critic agents must be validated against human-curated failure cases before being trusted.
- Add contamination screening for any benchmark used to validate agents.
- Clarify that LLM-generated components are candidate artifacts, not accepted facts, with explicit provenance and review status.
- Reference the auto-research deep-research findings.

### E. Update `phase-4-simulation-laboratory.md`

- Recommend OSS Vizier or Optuna as the parameter-sweep backend.
- Add IPCC-style uncertainty communication (confidence and likelihood qualifiers) to result structures.
- Require sensitivity analysis for all simulation experiments and retention of negative/null/inconclusive results.
- Add a simulation-safety critic-agent loop.
- Reference the simulation and governance deep-research findings.

### F. Update `docs/notes/decision/multi-agent-governance.md`

- Add NIST AI RMF (Govern → Map → Measure → Manage) as the top-level governance cycle.
- Map agent roles to risk tiers (EU AI Act / NIST style).
- Add moral ODD, guardrail agents, behavioral baselines, and HOTL thresholds.
- Add conflict-resolution protocols for multi-agent coordination.
- Schedule periodic red-teaming and adversarial testing.
- Reference the agent-governance deep-research findings.

## Open questions that should become design tasks

1. What is the minimal Phase 1 benchmark for note-pair classification?
2. Which embedding and reranking models will be used for hybrid retrieval, and how will they be evaluated?
3. What is the parser-quality matrix for Phase 2 languages, and what is the fallback for Clojure macros/dynamic code?
4. What is the ground-truth dataset for architecture boundaries and concept-to-code links?
5. Which optimization backend (Vizier vs. Optuna) will back Phase 4 parameter sweeps?
6. What is the moral ODD for each agent class, and how will it be enforced?
7. How will the platform validate critic agents and measure their false-positive/false-negative rates?
8. What is the calibrated uncertainty vocabulary for research and simulation outputs?

## Next step

Apply the recommended design changes to the design documents, then produce or update the corresponding ADRs and kanban task cards so the project can move from Design → Decide → Task.
