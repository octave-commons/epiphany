---
title: Knowledge Management and Signals Intelligence Platform — Design Overview
slug: knowledge-platform-overview
created: 2026-07-11
source: docs/inbox/clojure Natural Language Processing.md
kind: design
research:
  - docs/notes/research/nlp-fundamentals-and-classifiers.md
  - docs/notes/research/clojure-nlp-stack.md
  - docs/notes/research/vector-search-knowledge-graphs.md
  - docs/notes/research/auto-research-agent-loops.md
  - docs/notes/research/nlp-and-code-intelligence-deep-research.md
  - docs/notes/research/vector-search-and-knowledge-graphs-deep-research.md
  - docs/notes/research/auto-research-agents-deep-research.md
  - docs/notes/research/agent-governance-and-ethics-deep-research.md
  - docs/notes/research/research-to-design-implications.md
decision:
  - docs/notes/decision/infrastructure-stack-options.md
  - docs/notes/decision/data-model-and-provenance.md
  - docs/notes/decision/multi-agent-governance.md
  - docs/notes/decision/external-source-acquisition-policy.md
  - docs/notes/decision/no-postgresql-stack-override.md
  - docs/adrs/ADR-000.md
  - docs/adrs/ADR-001.md
---

# Knowledge Management and Signals Intelligence Platform — Design Overview

## Objective

Build a local knowledge management and signals intelligence platform that turns a large and growing personal corpus of notes, Git history, source code, external research artifacts, datasets, and experiments into inspectable evidence and useful research workflows.

The immediate use case is to answer five questions about the personal corpus:

1. Where did this idea begin?
2. How did it change over time?
3. Which notes express the same concept redundantly?
4. Which notes contradict each other, and what is the evidence?
5. Which code modules belong conceptually together, regardless of where they landed in the filesystem?

## Constraints

- Four machines: only two are strong.
- Total 64 GB RAM across all four machines.
- One dedicated GPU, two weaker integrated GPUs, one NPU.
- 500 Mbps LAN.
- Plenty but finite disk.
- Strong preference for Clojure.
- Nothing but time, but build incrementally.

Required capabilities:

- Container orchestration
- Vector index
- Geospatial index
- Graph data store
- NoSQL datastore
- Event sourcing
- Web crawling
- Data visualization
- Physical simulations
- Job queues
- Intelligent caching
- Weather simulations
- Behavioral modeling and analysis
- Sentiment analysis

## Four-phase roadmap

| Phase | Focus | Exit test |
|---|---|---|
| Phase 1 | Corpus archaeology | Trace one important idea across two years of notes and commits with inspectable evidence |
| Phase 2 | Code comprehension | Produce an evidence-backed architectural investigation of a Clojure subsystem |
| Phase 3 | Research operations | Produce an inspectable research dossier for a real question from notes/code |
| Phase 4 | Simulation laboratory | Run a reproducible simulation research packet with baselines, sensitivity, and uncertainty analysis |

## Baseline stack

The stack is grounded in the deep research on hybrid retrieval, knowledge graphs, and event sourcing, but respects the platform owner’s requirement that **PostgreSQL not be used**. Instead of a relational event ledger, event sourcing is implemented as append-only documents in ArangoDB with NATS JetStream handling operational flow.

| Need | Recommended component | Role |
|---|---|---|
| Cluster | K3s | Lightweight Kubernetes across four machines |
| Canonical multi-model store | ArangoDB | Documents, property graph, full-text search, dense vectors, geospatial, aggregations; also houses the append-only event ledger |
| Event bus and job queue | NATS JetStream | Bounded work queues, durable consumers, retries, fan-out |
| Immutable artifact store | MinIO or filesystem-backed S3-compatible object storage | Raw corpus, Git snapshots, crawl outputs, ASTs, datasets, simulation output |
| Cache | Redis or KeyDB | Short-lived query/session/cache data, locks, deduplication |
| Metrics | Prometheus + Grafana | Service, node, job, inference, retrieval, and simulation metrics |
| Logs/traces | Loki + OpenTelemetry | Structured event correlation and temporal debugging |
| Parser foundation | Tree-sitter plus language-specific analysis tools | Incremental CST extraction across the source language set |
| Clojure semantic analysis | clj-kondo | Namespace/symbol/require/reference/lint semantic evidence |
| Semantic inference | GPU-hosted local embedding/rerank/LLM services | Embeddings, taxonomy candidates, extraction, contradiction triage |
| Physics and simulation | Clojure/ACO engine plus Python/Rust accelerators where justified | Graph dynamics, agents, weather/spatial experiments |
| Visualization | ClojureScript/SVG/WebGL/D3 or Observable Plot-style layers | Temporal maps, graph exploration, experiment dashboards |

> **Note on PostgreSQL.** Deep research and `docs/adrs/ADR-000.md` initially favored a best-of-breed stack including PostgreSQL for the event ledger. The platform owner rejects PostgreSQL because of past migration and operational burdens. This design therefore returns to the original inbox discussion’s ArangoDB-centric architecture. `ADR-000` should be amended or superseded by a new ADR that records this override.
>
> Tradeoff: ArangoDB reduces component count and avoids relational migrations, but its search relevance and deep graph traversal may not match standalone Elasticsearch/Neo4j. The decision is revisited if local benchmarks show a measured regression in retrieval or graph performance.

## Node responsibilities

| Machine | Workload ownership | Do not put here |
|---|---|---|
| Ultra 9, 4070 Ti, 32 GB | Embedding/inference server, reranking, LLM extraction, heavy AST batch, simulation batches | A second heavyweight database replica competing for RAM/VRAM |
| Ryzen 7, 16 GB | ArangoDB primary, NATS, K3s control plane, index and projection workers, API/UI services | Large model inference |
| Ryzen 3, 8 GB | MinIO/cold data, backup, metrics/log shipping, crawl fetch, reverse proxy | ArangoDB primary, graph analytics, model serving |
| Intel i5, 8 GB | Backup replica, logs/metrics storage, archive/reindex jobs, lightweight crawler/scheduler | Any latency-sensitive or memory-heavy service |

Weak nodes own non-competing, failure-tolerant responsibilities: cold storage, backup verification, crawl downloading, log retention, and ingress.

## Data model: immutable artifacts + observations

Canonical source artifacts are immutable and never mutated into “the current truth.” Instead, write separate observations and derived projections.

```clojure
{:artifact/id       "sha256:..."
 :artifact/kind     :markdown/file
 :artifact/path     "docs/research/agent-governance.md"
 :artifact/repo     "devel"
 :artifact/blob-ref "object://raw/git/..."
 :artifact/observed-at #inst "2026-07-10T..."
 :artifact/git      {:commit "abc123"
                     :author "Aaron"
                     :committed-at #inst "2025-04-17T..."}
 :artifact/schema-version 1}
```

Observations:

```clojure
{:observation/id "uuid"
 :observation/type :concept/extracted
 :subject "sha256:..."
 :model {:name "entity-extractor"
         :version "v7"}
 :claim {:concept/id "concept:agent-governance"
         :span {:start 428 :end 454}
         :confidence 0.91}
 :observed-at #inst "2026-07-10T..."}
```

Search indexes, graph edges, summaries, embeddings, label views, and dashboards are rebuildable projections.

The event ledger lives as append-only documents in ArangoDB; NATS JetStream handles operational flow.

## Clojure boundaries

Keep infrastructure behind narrow protocols and make dataflow/domain parts pure:

```clojure
(defprotocol ArtifactStore
  (put-blob! [store bytes metadata])
  (get-blob [store blob-id]))

(defprotocol EventLog
  (append! [log event])
  (events-after [log cursor]))

(defprotocol KnowledgeStore
  (upsert-projection! [store projection])
  (query-graph [store query])
  (search [store query]))

(defprotocol WorkQueue
  (publish! [queue subject message])
  (consume! [queue subject handler]))
```

Schemas are versioned EDN. Projectors accept event streams and emit document/edge updates. ArangoDB driver, NATS driver, object-store driver, Redis driver, and tree-sitter/clj-kondo adapters stay at the edge.

## Intelligent caching

Use a content-addressed, tiered cache:

- **L1:** in-process memoization for pure transforms.
- **L2:** Redis/KeyDB for TTL-bound distributed results: query results, rate limits, model-response dedupe, locks.
- **L3:** object store keyed by content hash for expensive durable artifacts: normalized documents, ASTs, embeddings, crawl payloads, simulation output.
- **Projection cache:** ArangoDB documents/graph/vector projections with explicit provenance and invalidation dependencies.

Cache keys include input hash, parser/model version, prompt/template version, configuration hash, and schema version.

## Visualization views

Three first-class views:

1. **Temporal idea map:** commits, note revisions, concept lineage, divergence/convergence.
2. **Semantic landscape:** clusters of notes/code concepts with confidence and evidence drill-down.
3. **System topology:** services, queues, projections, nodes, job throughput, cache hit rate, inference utilization, failure/retry flow.

Focused views:

- contradiction review board
- proposed merge/supersession board
- namespace/comprehension map
- crawler source/trust map
- experiment lineage DAG
- geospatial event map
- simulation state and parameter sweep explorer

## Multi-agent workflow

Agents are bounded by role, budget, trace, and governance:

- **Scout:** watches allowlisted sources and proposes artifacts for ingestion.
- **Reader:** extracts components and creates evidence-linked literature briefs.
- **Prior-art agent:** answers bounded “what existing work resembles this?” tasks.
- **Critic:** finds scope mismatch, missing baselines, threats to validity, and strongest counterarguments.
- **Designer:** composes candidate experiment specifications.
- **Reproduction agent:** attempts approved low-risk reruns or benchmark evaluations.
- **Librarian:** proposes taxonomy changes, deduplication, source-trust adjustments, and link repairs.
- **Guardrail agents:** enforce behavioral baselines, confidence thresholds, and safe-action pipelines.
- **Supervisor gate:** human approval and policy engine before expensive, externally visible, or high-impact actions.

Governance follows the NIST AI RMF cycle (Govern → Map → Measure → Manage). Each agent class has a moral operational design domain (ODD), a risk tier, a dedicated identity, and least-privilege access. High-volume, low-risk actions may run under human-on-the-loop thresholds; high-impact actions require human-in-the-loop approval.

## Cross-cutting concerns

- **Observability:** OpenTelemetry, Prometheus, Grafana, Loki; correlation IDs; structured logs.
- **Resilience:** projections are disposable and rebuildable; jobs are idempotent; failures enter quarantine/dead-letter state.
- **Governance:** source registry, trust tiers, rate/resource budgets, human review gates, provenance, agent identities, least-privilege access, and periodic red-teaming.
- **Ethics:** pluralistic stakeholder value elicitation (IEEE 7000); no claims about identifiable persons; no autonomous high-stakes decisions; no unsupported novelty claims.
- **Uncertainty:** calibrated intervals, scenario ranges, missing-data warnings, explicit assumptions, and IPCC-style confidence/likelihood language where justified.
