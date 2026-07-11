---
title: Knowledge Platform Architecture Survey
slug: knowledge-platform-architecture-survey
created: 2026-07-11
source: docs/inbox/2026.07.11.14.35.24.md
kind: research
labels: ['architecture', 'epiphany', 'knowledge-platform', 'design', 'adr']
---

# Summary

Architecture survey for the knowledge-management and signals-intelligence platform: ingestion, search/vectors, knowledge graph, distributed compute, ADRs, identity, CLI/TUI, and Phase 1-4 epics.

# Say what I wanna do is discover all `**/docs/**.md`

extracting all of their ASTs, building ngram models from the trees, same for clojure, clojurescript, go, python, c, lua, ruby, bash, yaml, edn, json

files and create a multi layered index of labels, named entitites, just like the full works, build a knowledge graph, enable searches, user labeling, question anwering, just everything you just said, layered in the way you suggested tuning for the best model in a given circumstance, using LLMs to prototype the taxonomies, optimized for the fastest possible retrievals, shared/clustered databases, between 4 or more networked computers, one is "very high performance" intel ultra 9, 4070 ti, 32gb of ram, one is "Very good" ryzen 7, 16 gb of ram, and 2 beaters with like 8gb of ram an ryzen 3 and an intel i5

And on top of answering questions about the local corpus, incorperate opensource datasets to answer questions about physics, biology, sociology, mathmatics,
analyze geopolitical circumstances just like the knowledge management and signal inteligence platform, assuming like... I could also afford signifigant inference for gemma4:31b, and kimi k2.7 to power that layer.

Connecting them to every piece of language processing, and data analysis technology we have available before ever putting that context in front of the agent to run a tool calling loop.

using the full computational capabilities of the network.

How much would I have to build, and how much of it would just be standing up existing solutions? I'm imaging we use elastic search here? My guess at least, wouldn't be much point in optimizing for vector search when you have all of these other metrics to also deal with

Most of what you’re describing can be built by **standing up existing components for search, vectors, and orchestration**, but the hard part is the glue: corpus ingestion and normalization, AST-level modeling, knowledge graph design, retrieval logic, and evaluation across your heterogeneous data and hardware. Elasticsearch/OpenSearch can absolutely be your primary text + hybrid search backend, and you’d still want vector search—just integrated as one layer among several signals, not as the only retrieval mechanism.[^4_1][^4_2][^4_3]

## Big-picture architecture

You’re essentially talking about a local “knowledge management and signals intelligence platform” with a stack like:

- **Ingestion \& parsing layer:** walk repos, find `**/docs/**.md` and all the code / config formats, parse to ASTs and normalized documents.
- **Representation layer:** text index (BM25), vector index (embeddings), code-aware features (AST n-grams), plus a knowledge graph store.
- **LLM / taxonomy layer:** LLMs generate taxonomies, schemas, candidate labels, and rationales, not raw facts.
- **Retrieval \& QA layer:** hybrid keyword + vector search plus KG queries to assemble evidence; LLMs then answer questions grounded in this evidence.
- **Distributed compute \& orchestration:** assign parsing, embedding, indexing, and QA workloads across your four machines.

Most of the primitives (search, vectors, KG, orchestration) exist; what you’d build is the domain-specific “how all of this interacts” and the AST/code-centric stuff.

## What you can stand up vs. what you must build

### 1. Corpus discovery and parsing

**Stand up:**

- Use filesystem search tools plus language-specific parsers (e.g. tree-sitter bindings) to walk all repos and parse code/markup into ASTs and structured representations.
- Use document parsing libraries for markdown, JSON, YAML, etc., and text normalization utilities for standardized tokens and spans.

**Build:**

- Your **unified ingestion pipeline**:
    - “Find all docs and code, parse to AST, emit a normalized document record” as a Clojure-centric ETL graph.
    - A consistent schema that ties together: file path, repo, language, AST nodes, comments, docblocks, structured configs, timestamps, authors, and labels.
- AST-level **feature extraction**:
    - n-gram models over syntax trees (e.g. n-grams over node types and selected identifiers) and mappings from AST patterns to semantic labels.

No off-the-shelf system currently knows how to treat “AST n-gram signals from Clojure + Ruby + Bash + YAML” as first-class features in the same index—you’ll define that.

### 2. Search and vector storage

**Stand up:**

- **Elasticsearch or OpenSearch** for:
    - Full-text search (BM25, inverted index).
    - Dense vector search via `dense_vector`, `semantic_text`, HNSW indices, and hybrid BM25 + kNN queries.[^4_2][^4_3]
- Optionally, a **vector database** (Qdrant, Weaviate, pgvector, Pinecone) if you decide you want a specialized store separate from your search cluster. Modern guidance is: pick a vector store that fits your stack and scale—pgvector if you’re already on Postgres, Pinecone for managed scale, Qdrant for open-source with strong filtering, Weaviate for native hybrid search.[^4_1]

**Build:**

- Your **retrieval logic**:
    - How you combine BM25, filters (language, repo, date, label), AST features, metadata, and embeddings to rank candidates.
    - Rules like “for code questions, prioritize AST nodes and comments; for conceptual questions, prioritize markdown docs and long-form text.”
- The **indexing strategy**:
    - What is a “document”? A file, a function, a markdown section, a code snippet, a config block?
    - How you chunk and tag them for both text and vector search.

You’re right that “optimizing only for vector search” is a trap; but the real win is **hybrid search**—Elasticsearch supports dense-vector kNN combined with keyword scoring, so you can treat vector similarity as one signal among many in a single search backend rather than bolting on a separate system prematurely.[^4_3][^4_2][^4_1]

### 3. Knowledge graph layer

**Stand up:**

- A **graph database or triple store** (Neo4j, a property graph, or an RDF store) to hold entities, relations, and provenance.
- Off-the-shelf KG tooling that supports:
    - Nodes labeled by type (file, function, concept, person, organization, dataset, experiment).
    - Edges for “references,” “defines,” “uses,” “depends on,” “authored by,” “mentions,” etc.

**Build:**

- The **schema and mapping**:
    - How AST nodes, docs, configs, and external knowledge (physics, biology, geopolitics datasets) become entities and edges.
    - Provenance and versioning: which model/prompt produced which entity or relation; from which text span; at what time.
- **Graph-enriched retrieval**:
    - “Find all functions related to concept X that also use library Y and were edited after date D by person P” queries.
    - Selection of subgraphs that serve as context for LLM QA.

Existing KG tech will store the graph, but you’ll design the ontology and the rules for populating and querying it.

### 4. LLM and taxonomy layer

**Stand up:**

- Local LLM infrastructure to run Gemma 4 31B, Kimi K2.7, or similar models for:
    - Taxonomy prototyping, label suggestion, and schema refinement.
    - Question answering over retrieved evidence and KG subgraphs.
    - Entity and relation extraction (with strong validation and provenance).
- Use established patterns for fine-tuning or adapter-based training where you want task-specific improvements.[^4_4]

**Build:**

- Your **instruction templates and schemas**:
    - JSON-based label outputs, entity and relation schemas, QA response formats with citations.
    - Prompts that align with your KG ontology and index structure.
- A **tool-calling / orchestration layer**:
    - The LLM never sees raw filesystem; instead it is given high-level tools like “search_docs”, “search_code”, “query_graph”, “retrieve_external_dataset”.
    - You orchestrate which tools fire when, based on the question and the system’s current knowledge.

LLMs are doing more of the “semantic heavy lifting”—prototype taxonomies, structure unstructured text—but you’re building the **contracts** they must satisfy.

### 5. External scientific and geopolitical data

**Stand up:**

- Public **datasets and knowledge bases** in physics, biology, sociology, mathematics, and geopolitics:
    - CSV/Parquet databases, curated corpora, and existing semantic resources.
- Standard **analysis tools**:
    - Numerical libraries, simulation engines, statistical packages, and specialized knowledge bases.

**Build:**

- **Unification**:
    - Map those external datasets into your KG and index model so “local corpus facts” and “external domain knowledge” coexist.
- **Policy and trust layers**:
    - Which sources count as authoritative; how you distinguish local notes from published research; how you track the origin of each claim.

This is where your signals-intelligence analogy becomes real: the graph is not just a concept map; it’s a provenance-preserving ledger of where each statement came from.

### 6. Distributed compute on your four machines

**Stand up:**

- A **job/queue system** (e.g. Kafka, NATS, or a simpler queue) to assign units of work:
    - GPU-heavy nodes do embedding, LLM inference, and big batched parsing.
    - Mid-tier nodes handle ETL, index writes, and KG updates.
    - Beater nodes handle low-priority jobs, background normalization, and some search traffic.
- Monitoring and metrics infrastructure.

**Build:**

- The **workload partitioning strategy**:
    - How ingestion, re-indexing, and QA workloads flow through the cluster.
    - How you ensure idempotency and replay—important when you update models or taxonomies.
- **Capacity-aware scheduling**:
    - E.g. “never run large Gemma jobs on the Ryzen 3”, “reserve GPU node for embeddings and QA, not raw search.”

The hardware you describe is more than enough to run a serious local RAG/KG stack; the limiting factor will be system design, not horsepower.

## How much custom work vs. standing up existing solutions?

If we roughly categorize:

- **Mostly stand up / configure:**
    - Elasticsearch/OpenSearch cluster for text and vector search, including BM25 and kNN + hybrid search.[^4_2][^4_3]
    - Vector store (if you want a separate one) chosen to match stack and scale—pgvector for Postgres, Pinecone for managed scale, Qdrant for open-source filtering, Weaviate for native hybrid search.[^4_1]
    - Graph database for the KG.
    - LLM runtimes and inference servers for Gemma/Kimi.
    - Queues, job runners, and monitoring.
- **Mostly build:**
    - Language-agnostic ingestion that ties `**/docs/**.md`, code ASTs, configs, and external corpora into one unified schema.
    - AST-based n-gram modeling and code-aware feature design across Clojure, Clojurescript, Go, Python, C, Lua, Ruby, Bash, YAML, EDN, JSON.
    - Your ontology: entity types, relations, labels, taxonomies, and how they map to code/doc structures and external scientific/geopolitical data.
    - Retrieval policies: how to rank hybrid search results for different question types, and how to decide when to call which tools.
    - Evaluation harnesses: measuring retrieval quality, classification accuracy, KG correctness, and QA faithfulness across your layered system.
    - UX and workflow for “user labeling, search, QA, graph exploration” and for iteratively improving taxonomies and models.

In other words: **the infra is largely off-the-shelf; the intelligence of the system is your design.** Search, vectors, KG, LLMs, and queues are plumbing; the substantive work is deciding how ASTs, docs, datasets, and questions become entities, labels, and retrieval strategies that actually help you and other users reason about the corpus.

On your specific hunch: you don’t have to “optimize for vector search instead of other metrics”; you want **hybrid search** where vectors, text relevance, metadata filters, graph relations, and possibly AST signals all feed into ranking. Elasticsearch now has first-class dense vector search with kNN and HNSW, plus hybrid BM25 + vector scoring and, in newer releases, quantization for high-dimensional vectors—so using it as both your text and vector engine is a pragmatic starting point.  The truly hard optimization problem is in retrieval logic and data modeling, not in chasing one more point of QPS from your vector index.[^4_3][^4_2]
<span style="display:none">[^4_10][^4_11][^4_12][^4_13][^4_14][^4_15][^4_16][^4_5][^4_6][^4_7][^4_8][^4_9]</span>

<div align="center">⁂</div>

[^4_1]: https://gautamkhorana.com/blog/best-vector-database-for-rag-2026/

[^4_2]: https://www.elastic.co/docs/solutions/search/vector/dense-vector

[^4_3]: https://www.elastic.co/search-labs/blog/vector-search-improvements

[^4_4]: https://huggingface.co/docs/transformers/en/training

[^4_5]: https://www.elastic.co/docs/solutions/search/vector

[^4_6]: https://alphacorp.ai/blog/best-vector-databases-for-rag-2026-top-7-picks

[^4_7]: https://www.aiintelreport.com/enterprise-ai/best-vector-databases-rag-2026

[^4_8]: https://techsy.io/en/blog/best-vector-databases-2026

[^4_9]: https://futureagi.com/blog/best-vector-databases-for-rag-2026/

[^4_10]: https://markaicode.com/best/best-vector-database-for-rag/

[^4_11]: https://clawnewbie.com/reviews/best-rag-tools-vector-databases-2026

[^4_12]: https://subrupt.com/best/vector-database-for-rag

[^4_13]: https://iternal.ai/insights/best-vector-databases-2026

[^4_14]: https://aitoolguide.ai/blog/best-vector-databases-for-rag-2026/

[^4_15]: https://oneuptime.com/blog/post/2026-01-21-elasticsearch-vector-search/view

[^4_16]: https://www.pingcap.com/compare/best-vector-database/
---
# @Academic @GitHub @Hugging Face well, this problem I have with so many notes being accumulated, I know I've written enough that a system like this could classify all of it and help me solve the problem of "what to research" quickly, and on to what data sets, what sinks.

Alright, we're writing a green field system here. So we don't pick based on prior decisions. We pick based on the best options. So let's make all of our requirements, and constraints clear up front. I have 4 computers, 2 can do real work, 2 are probably best left as IO routers and cold storage.

so the system must be either scale horizontally, or the each of the machines own non competing responsibilities.

The system must be observable, and resilient
I kinda think minikube for this just cause? but they really shoved that down my throat at Raft.

I have plenty of disk space, but it is not limitless.
I have a reasonable amount of CPU, 1 dedicated graphics card, 2 middling integrated cards, and an NPU

A total of 64gb of ram between all 4 machines.
I have 500mbps bandwidth for networking

I have a strong preference for clojure.

I have nothing but time.

Those are the constraints.

Requirements:

- container orchestration
- vector index
- geo spatial index
- graph data store
- nosql datastore
- event sourcing

Given your hardware, Clojure preference, and greenfield freedom, I would not choose minikube as the foundation. **K3s is the better fit** for a 4-node homelab-style cluster because it is a lightweight, fully compliant Kubernetes distribution designed for edge, homelab, and constrained environments, with a smaller memory footprint and built-in components that reduce operational drag.  For the data plane, Elasticsearch is the strongest first pick because it already gives you full-text, vector, and geospatial indexing in one system, which means fewer moving parts while you are still building the higher-order knowledge and agent layers.[^8_1][^8_2][^8_3][^8_4][^8_5]

## Requirements mapped

Your required capabilities can be satisfied, but not all by a single datastore:


| Requirement | Best first choice | Why |
| :-- | :-- | :-- |
| Container orchestration | K3s | Lightweight Kubernetes, suited for homelab / edge / low-memory clusters. [^8_2] |
| Vector index | Elasticsearch | Native `dense_vector`, kNN, HNSW, compression/quantization support. [^8_1][^8_3][^8_6] |
| Geo spatial index | Elasticsearch | Native `geo_point`, `geo_shape`, geo queries, aggregations, and ES |
| Graph data store | Neo4j | Mature graph model, plus vector index support if you later want graph-local semantic search. [^8_7][^8_8] |
| NoSQL datastore | Elasticsearch or MongoDB, depending on workload | Elasticsearch works well for searchable document-oriented records; MongoDB is stronger if you need primary operational document storage and flexible CRUD. [^8_1][^8_9] |
| Event sourcing | EventStoreDB or PostgreSQL + append-only events | EventStoreDB is purpose-built for event sourcing; a Postgres-backed event log is simpler if you want fewer systems and tighter control from Clojure. [^8_10][^8_11][^8_12] |

## Recommended baseline stack

I would start with this **minimum serious architecture**:

- **K3s** for orchestration across all 4 machines.[^8_2]
- **Elasticsearch** as your primary search/document retrieval engine:
    - BM25/full text
    - dense vectors
    - geospatial
    - metadata filtering
    - aggregations
    - observability/search dashboards through Kibana if you choose to include it.[^8_3][^8_4][^8_1]
- **Neo4j** for the knowledge graph:
    - entity/relation modeling
    - graph traversals
    - optional vector search over graph nodes later.[^8_7][^8_8]
- **PostgreSQL or EventStoreDB** for event sourcing:
    - If you want conceptual purity around streams and projections, EventStoreDB is built for exactly that.[^8_13][^8_10]
    - If you want fewer operational systems and stronger Clojure ergonomics, Postgres with an append-only event table is a very defensible first version.[^8_14][^8_12]
- **Clojure services** for ingestion, parsing, orchestration, evaluation, and user workflows.
- **Object/file storage on your colder machines** for corpora snapshots, embeddings, model artifacts, logs, and backups.

That gives you separation of concerns:

- search in Elasticsearch,
- graph reasoning in Neo4j,
- event history in Postgres/EventStoreDB,
- orchestration in K3s.


## Why not minikube

Minikube is primarily for local single-node or developer-oriented Kubernetes workflows, whereas K3s is explicitly designed for lightweight real cluster operation with reduced operational overhead. K3s packages the control plane and common dependencies into a simpler footprint and supports external datastores when needed.[^8_2]

For your setup:

- 64 GB RAM total is enough, but not enough to be cavalier.
- Two “real work” nodes should carry stateful and compute-heavy workloads.
- Two weaker nodes should not be burdened with heavyweight control-plane plus search plus graph plus inference all at once.

K3s lets you keep Kubernetes without paying the full “clusterology tax.”[^8_2]

## Node roles

A clean split would be:


| Node class | Suggested role |
| :-- | :-- |
| Ultra 9 + 4070 Ti + 32 GB | Primary inference node, heavy ingestion jobs, embeddings, reranking, LLM serving, maybe one major stateful service if carefully tuned |
| Ryzen 7 + 16 GB | Primary search / orchestration / secondary compute node, scheduled batch jobs, ETL, background indexing |
| Ryzen 3 + 8 GB | Router, storage helper, log shipping, backup target, light queue / proxy / registry tasks |
| Intel i5 + 8 GB | Cold storage, replicas for lightweight services, monitoring endpoints, additional IO and backup roles |

In practice, you likely want your **stateful hot path** concentrated mostly on the two stronger machines, and your weaker machines handling:

- ingress / reverse proxy
- artifact storage
- backups
- metrics/log forwarding
- maybe read-only replicas or queue consumers.

That satisfies your constraint that machines either scale horizontally or own non-competing responsibilities.

## Observability and resilience

Your system must be observable and resilient, which means:

- **Metrics** for every service, queue, and indexer.
- **Structured logs** with correlation IDs from ingestion through search and QA.
- **Health checks** and restart policies at the orchestrator layer.
- **Snapshots/backups** for Elasticsearch, graph DB, and event store.
- **Replayability** from the event stream, so re-indexing or rebuilding projections is routine rather than catastrophic. Event sourcing is valuable here precisely because it gives you a canonical append-only history you can replay into new projections.[^8_12][^8_10]

Architecturally, resilience comes less from “Kubernetes exists” and more from:

- narrow service responsibilities,
- persistent logs/events,
- rebuildable projections,
- and not overloading the same node with all stateful dependencies.


## Best-option decisions

If we pick “best option” under your exact constraints, my recommendations are:

### Container orchestration

**K3s**, not minikube. It is designed for lightweight, low-overhead, real cluster use.[^8_2]

### Vector + geo + search

**Elasticsearch first.** It already supports vector search with `dense_vector` and kNN/HNSW, and geospatial indexing/querying with `geo_point` and `geo_shape`, including distance, polygon, and intersection-style queries.[^8_6][^8_4][^8_5][^8_3]

This is the biggest simplification you can buy yourself early.

### Graph

**Neo4j** for the graph layer. It is the most straightforward choice for knowledge-graph-heavy interactive querying, and it also has vector indexing support if you later want semantic graph retrieval.[^8_8][^8_7]

### NoSQL

If “NoSQL” means “primary operational JSON/document store,” use **MongoDB**. It has mature document semantics and geospatial indexing.[^8_9][^8_15]
If “NoSQL” means “searchable flexible documents” and you do not need Mongo-style application CRUD semantics, Elasticsearch may already cover that role well enough for the first phase.[^8_1]

### Event sourcing

For a greenfield Clojure-centric system, I would choose one of two paths:

- **Pragmatic path:** Postgres + append-only event table + Clojure projections. This is simpler to operate and still aligned with event-sourcing principles.[^8_14][^8_12]
- **Purist path:** EventStoreDB, because it is explicitly designed for event sourcing and stream-based architectures.[^8_10][^8_13]

Given your “I have nothing but time” constraint, EventStoreDB is appealing academically. Given your “I want to actually finish this” constraint, Postgres event sourcing may be the better first implementation.

## What I would build first

Order matters more than tool perfection.

1. **K3s cluster**
2. **Event log + ingestion pipeline**
3. **Elasticsearch cluster and searchable document model**
4. **AST extraction + chunking + labels/entities**
5. **Neo4j knowledge graph projection**
6. **Hybrid retrieval layer**
7. **LLM-driven taxonomy / QA / agent loops**
8. **Observability and automated replay/reindex workflows throughout**

That sequence lets you accumulate value immediately:

- first a stable cluster,
- then durable event history,
- then search,
- then semantics and graph,
- then agentic behavior.


## The simplest credible architecture

If you want the shortest path to a real system without overcommitting:

- K3s
- PostgreSQL for events and system-of-record metadata
- Elasticsearch for search/vector/geo
- Neo4j for graph
- Clojure everywhere else

That is already a serious platform, and each component has a clear reason to exist. It avoids the mistake of trying to make one datastore do everything, while also avoiding a zoo of trendy infrastructure.

Would you like me to turn this into a concrete phase-by-phase build plan with service boundaries, node assignments, and a first-pass deployment topology?
<span style="display:none">[^8_16][^8_17][^8_18][^8_19][^8_20][^8_21][^8_22][^8_23][^8_24][^8_25][^8_26][^8_27][^8_28][^8_29][^8_30]</span>

<div align="center">⁂</div>

[^8_1]: https://www.elastic.co/docs/solutions/search/vector

[^8_2]: https://docs.k3s.io/

[^8_3]: https://www.elastic.co/docs/solutions/search/vector/dense-vector

[^8_4]: https://www.elastic.co/docs/explore-analyze/geospatial-analysis

[^8_5]: https://www.elastic.co/docs/reference/query-languages/query-dsl/geo-queries

[^8_6]: https://www.elastic.co/docs/reference/elasticsearch/mapping-reference/dense-vector

[^8_7]: https://docs.spring.io/spring-data/neo4j/reference/repositories/vector-search.html

[^8_8]: https://neo4j.com/developer/genai-ecosystem/vector-search/

[^8_9]: https://www.mongodb.com/docs/manual/geospatial-queries/

[^8_10]: https://docs.kurrent.io/server/v23.10/

[^8_11]: https://github.com/eugene-khyst/eventstoredb-event-sourcing

[^8_12]: https://blog.andreyfadeev.com/p/event-sourcing-with-postgresql-and

[^8_13]: https://github.com/oskardudycz/eventstoredb-samples

[^8_14]: https://oskarth.com/basic-event-sourcing-in-clojure/

[^8_15]: https://www.mongodb.com/docs/drivers/node/v6.11/crud/query/geo/

[^8_16]: https://github.com/denisecodes/k3s-homelab

[^8_17]: https://github.com/active-group/active-events

[^8_18]: https://github.com/mattiasholmqvist/evently

[^8_19]: https://github.com/sleighzy/raspberry-pi-k3s-homelab/blob/main/k3s.md

[^8_20]: https://docs.spring.io/spring-data/neo4j/reference/data-commons/repositories/vector-search.html

[^8_21]: https://www.baeldung.com/elasticsearch-geo-spatial

[^8_22]: https://learn.microsoft.com/en-us/azure/cosmos-db/mongodb/vcore/vector-search

[^8_23]: https://www.elastic.co/search-labs/fr/blog/series/elasticsearch-geospatial-search

[^8_24]: https://docs.rs/rig-neo4j/latest/rig_neo4j/vector_index/index.html

[^8_25]: https://www.elastic.co/search-labs/blog/series/elasticsearch-geospatial-search

[^8_26]: https://github.com/codesulting/EventStoreDB

[^8_27]: https://github.com/TrogonStack/EventStore

[^8_28]: https://www.mongodb.com/docs/drivers/go/current/data-formats/geo/

[^8_29]: https://stackoverflow.com/tags/eventstoredb/info

[^8_30]: https://medium.com/@rividemeo/event-sourcing-in-python-with-eventstoredb-sql-server-3f23c7315486
---
# @Academic @GitHub @Hugging Face Nope, absolutely no PostgreSQL. you guys suck at managing that over time, you never write migrations right. That's why the requirement was for nosql. I have regretting allowing you to convince me to use it every single time you have done so, you only do it because it is the most common database.

Also, we have other requirements.

- web crawling
- Data visualization
- physical simulations
- job queus
- inteligent cacheing
- weather simulations
- behavioral modeling and analysis
- Sentiment analysis

This probably captures it.

My immediate use case would be to discover connections between widely distributed markdown notes, detect contradictions, to consolidate redundant concepts, clear low quality notes, to follow my train of thought over the last 2 years, using commit histories to pin down exact dates for when files appeared. Tracing the lineage of ideas as they developed, where did they start? How many times did an idea evolve, while remaining statistically legible to it's self?

For code, I hope I can identify conceptual boundaries. As in, organize namespaces for comprehension. In practice, code ends up rather near by to where you were when you realized you needed it, and work tends to get finished towards the end of ones peak mental faculties, just how time works. So the places where things land and settle, are usually never optimal, but being that we're guided the entire way by our static analysis tools, those metrics will never be the ones that convey the problem.

You’re right to veto PostgreSQL. I overfit to the familiar architecture instead of your actual requirement: **a durable, flexible, eventful document/graph system with minimal migration pressure**, not relational CRUD with a pile of migrations. Your immediate problem is fundamentally temporal knowledge archaeology—notes, commits, ideas, code, and semantic evolution—not transactional accounting.

The greenfield choice I would make now is: **K3s + ArangoDB as the central multi-model store + NATS JetStream for events/jobs + an object store for immutable source artifacts**, with Clojure services building projections, AST facts, embeddings, temporal lineage, and evaluations around that core. ArangoDB natively combines JSON documents, property graphs, integrated search, vector indexes, full-text search, and geospatial capabilities, so it directly meets four of your original requirements without forcing the system into four competing databases.[^9_1][^9_2][^9_3][^9_4][^9_5]

## Revised non-negotiables

Your architecture should optimize for these constraints:


| Constraint | Architectural consequence |
| :-- | :-- |
| Four machines, only two strong | Stateful hot workloads must be concentrated on the Ultra 9/4070 Ti machine and Ryzen 7 machine; weaker computers get IO, storage, routing, monitoring, and low-priority workers |
| 64 GB RAM total | Avoid a Kubernetes-plus-five-databases zoo; choose integrated primitives |
| One GPU, two weaker iGPUs, one NPU | GPU machine owns batch embeddings, local inference, reranking, simulation acceleration where applicable; do not scatter model serving |
| 500 Mbps LAN | Keep indexes local to their owners, batch network transfers, use event-driven updates rather than chatty services |
| Plenty but finite disk | Store immutable raw artifacts cheaply; derive indexes/projections so they can be dropped and rebuilt |
| Clojure preference | Define EDN contracts and pure projection functions; isolate each infrastructure driver behind a protocol |
| No PostgreSQL | Do not use relational migrations as the canonical evolution mechanism; use additive, versioned documents and event/projection schemas |
| “Nothing but time” | Build a research-grade platform incrementally, with every subsystem observable, replayable, and independently evaluable |

Your preference for a unified core store and separated concerns is already consistent with this design: do not make graph, documents, vectors, and geo fight across four separate persistence systems until scale truly forces that move.

## Central data plane

### ArangoDB, not Elasticsearch + Neo4j + Mongo

For this project, **ArangoDB is the better initial center of gravity** than the previous Elasticsearch/Neo4j/Mongo combination.

It is a multi-model database: JSON documents for note/code artifacts, a property graph for provenance and conceptual relationships, integrated search for lexical retrieval, vector indexing for semantic similarity, and geospatial support for external intelligence data.[^9_2][^9_3][^9_4][^9_5][^9_1]

That means a coherent query can ask:

> “Find notes from 2024 structurally similar to this Clojure namespace, semantically related to emergency-response simulation, connected through at least two idea-lineage edges, excluding concepts I have marked as obsolete.”

That is a document query, vector search, graph traversal, temporal filter, and user-label query—not five microservices passing IDs around.

ArangoDB supports geospatial indexing of GeoJSON and latitude/longitude data, which makes it a reasonable store for weather observations, disaster events, geopolitical boundaries, locations mentioned in reports, and spatially grounded simulations.  Its vector support is real but should be treated as one retrieval signal—not the only one; vector search retrieves approximate nearest neighbors using indexed embeddings, with a quality-versus-speed control such as `nProbe`.[^9_6][^9_7][^9_5]

### The data model

Your canonical model should be **immutable source artifacts plus append-only observations**.

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

Then do **not mutate that object into “the current truth.”** Instead, write separate observations and derived projections:

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

This is event sourcing without Postgres: source artifact events and derived observation events are append-only documents; search indexes, graph edges, summaries, embeddings, label views, and dashboards are **rebuildable projections**.

## Eventing and jobs

Use **NATS JetStream** as the cluster’s event backbone and job queue, with an append-only artifact/event collection in ArangoDB as the durable research ledger.

Why separate these two roles:

- **JetStream** handles operational flow: work distribution, retries, acknowledgements, backpressure, worker coordination, and fan-out.
- **ArangoDB** retains queryable durable domain history: what was ingested, which parser/model produced which result, which relation was accepted or rejected, and which projection version created it.
- **Object storage** retains raw blobs: Git objects, markdown originals, AST JSON/EDN, model artifacts, large datasets, simulation results, and crawl captures.

Do not use a job queue as your permanent truth store. Do not use the database as an unbounded low-latency work broker. Keep command/event flow and historical evidence distinct.

Apache Pulsar is another credible option, particularly because it supports tiered storage for offloading old backlog to long-term storage, but it is more operational machinery than your first version needs.  NATS JetStream is the pragmatic initial choice; if the platform later needs a massive replayable public-data firehose, evaluate Pulsar then.[^9_8]

## The full stack

| Need | Recommended component | Role |
| :-- | :-- | :-- |
| Cluster | K3s | Lightweight cluster scheduler across four machines. [^9_9] |
| Canonical multi-model store | ArangoDB | Documents, graph, lexical search, vectors, geo, aggregations. [^9_1][^9_4] |
| Event bus and job queue | NATS JetStream | Bounded work queues, durable consumers, retries, fan-out |
| Immutable artifact store | MinIO or filesystem-backed S3-compatible object storage | Raw corpus, Git snapshots, crawl outputs, ASTs, datasets, simulation output |
| Cache | Redis or KeyDB | Short-lived query/session/cache data, rate limits, distributed locks, work deduplication |
| Metrics | Prometheus + Grafana | Service, node, job, inference, retrieval, and simulation metrics |
| Logs/traces | Loki + OpenTelemetry | Structured event correlation and temporal debugging |
| Parser foundation | Tree-sitter plus language-specific analysis tools | Incremental AST extraction across the source language set |
| Semantic inference | GPU-hosted local embedding/rerank/LLM services | Embeddings, taxonomy candidates, extraction, contradiction triage |
| Physics and simulation | Your Clojure/ACO engine plus Python/Rust accelerators only where justified | Graph dynamics, agents, weather/spatial experiments |
| Visualization | ClojureScript/SVG/WebGL/D3 or Observable Plot-style layers | Temporal maps, graph exploration, experiment dashboards |

Tree-sitter has a Clojure/ClojureScript grammar, and existing tooling demonstrates its use for practical source-code analysis; its grammar alone handles syntactic primitives rather than high-level Clojure semantics, so pair it with `clj-kondo`/analyzer-derived facts for namespace resolution, symbol definitions, references, and lints.  That distinction matters: tree-sitter provides structural fidelity; semantic analysis tools provide language-aware meaning.[^9_10][^9_11][^9_12]

## Node responsibilities

Avoid pretending you have a symmetric four-node compute cluster. You have a **two-node active system with two support appliances**.


| Machine | Workload ownership | Do not put here |
| :-- | :-- | :-- |
| Ultra 9, 4070 Ti, 32 GB | Embedding and inference server, reranking, LLM extraction, heavy AST batch work, simulation batches, primary interactive research services | A second heavyweight database replica competing for RAM/VRAM |
| Ryzen 7, 16 GB | ArangoDB primary, NATS, K3s control plane, index and projection workers, API/UI services | Large model inference |
| Ryzen 3, 8 GB | MinIO/cold data, backup agent, Prometheus node exporter, crawl fetch workers, reverse-proxy/ingress | ArangoDB primary, graph analytics, model serving |
| Intel i5, 8 GB | Backup replica, logs/metrics storage, scheduled archive/reindex jobs, lightweight crawler/scheduler | Any latency-sensitive or memory-heavy service |

The weak nodes should own **non-competing, failure-tolerant responsibilities**: cold storage, backup verification, crawl downloading, log retention, and ingress. They are valuable because they isolate IO and ensure the strong nodes remain available for the work that requires their memory and compute.

## Immediate use case

Your first release should not be a universal signals platform. It should answer five questions about your own corpus reliably:

1. **Where did this idea begin?**
2. **How did it change over time?**
3. **Which notes express the same concept redundantly?**
4. **Which notes contradict each other, and what is the evidence?**
5. **Which code modules belong conceptually together, regardless of where they landed in the filesystem?**

That is already enough to make the system personally transformative.

### Idea lineage

Use Git as historical ground truth rather than file timestamps.

For every source artifact, ingest:

- repository and path
- blob hash
- first-seen commit
- every commit that changed it
- author and timestamp
- rename history where Git can identify it
- diff hunks
- parsed markdown headings / code AST nodes
- embeddings at file, section, and concept granularity

Then construct a temporal lineage graph:

```text
Commit --changed--> FileRevision
FileRevision --contains--> SectionRevision
SectionRevision --mentions--> Concept
ConceptRevision --evolves-from--> ConceptRevision
ConceptRevision --contradicts--> ConceptRevision
CodeSymbol --implements--> Concept
```

A note’s “birth” is not simply the oldest matching filename. It is the earliest reachable commit containing a semantically and structurally similar concept, with confidence and evidence preserved. That lets you answer: “this idea first emerged as a comment in a simulation module in March 2024, became a markdown concept in May, split into two competing framings in October, and was later embodied in the ACO graph design.”

### Redundancy and consolidation

Do not delete notes automatically. Categorize them:

- **Duplicate:** near-identical content, same claims, no unique provenance.
- **Near duplicate:** same conceptual role, different wording, perhaps one has examples or historical context.
- **Superseded:** an older claim replaced by a later, explicit version.
- **Contradictory:** claims cannot both hold under the same scope/time/model.
- **Complementary:** related but distinct facets that should be linked, not merged.
- **Low-signal:** insufficiently specific, unsupported, stale, fragmentary, or only useful as an historical trace.

The system should propose a consolidation plan with evidence, not silently “clean” your notes. The entire point of idea archaeology is that low-quality fragments can still preserve the path that produced a better thought.

### Contradiction detection

Contradiction is not simply opposite embeddings. Implement it as an evidence pipeline:

1. Extract candidate claims from note sections.
2. Normalize claims into a schema: subject, predicate, object, scope, time, modality, source.
3. Retrieve candidates with shared entities/topics and potentially incompatible predicates.
4. Let deterministic rules identify obvious forms: opposite polarity, mutually exclusive values, incompatible timelines.
5. Use an LLM only as a **triage/rationale generator**, returning structured evidence.
6. Store a proposed `:contradicts` edge with confidence, scope, and evidence spans.
7. Require your review before promoting it to an accepted relation.

This gives you a real epistemic graph instead of a hallucination graph.

## Code conceptual boundaries

Your intuition is correct: filesystem topology and commit locality encode the *history of work*, not necessarily the domain architecture. Static tools mostly validate local correctness; they do not answer whether namespaces form a comprehensible conceptual system.

Build several independent signals for code boundaries:


| Signal | What it reveals |
| :-- | :-- |
| Namespace/import graph | Explicit dependency and coupling direction |
| Symbol-reference graph | Actual use, not just declared dependencies |
| AST motif similarity | Repeated structural patterns, such as pipelines, state machines, adapters, reducers |
| Identifier/comment embeddings | Domain vocabulary and intent |
| Co-change graph | Files that evolve together across commits |
| Temporal sequence | Concepts implemented shortly after notes or decisions |
| Test affinity | Components verified together |
| Runtime traces | Components that actually collaborate |
| Complexity/lint facts | Candidates for decomposition, but not a definition of a boundary |

The key is **multi-view clustering**. Do not let any single metric decide “these files belong together.” A namespace cluster is meaningful when several signals agree: shared domain terms, reciprocal references, co-change, compatible AST patterns, and shared tests—while avoiding cycles or inappropriate dependency directions.

Your “where things land is where you were when you realized you needed them” observation is exactly why the system needs both a spatial/filetree view and a temporal/conceptual view. The mismatch between them is itself a valuable signal: it identifies code that is historically adjacent but architecturally misplaced.

## Additional requirements

### Web crawling

Treat web crawling as an evidence acquisition subsystem, not an agent free-for-all.

- Start with explicit allowlists: arXiv, GitHub, Hugging Face, official documentation, government/open-data feeds, chosen journals.
- Store raw retrieval timestamp, canonical URL, content hash, extraction method, license/terms metadata, and source trust tier.
- Respect robots.txt, API terms, rate limits, and copyright boundaries.
- Keep crawl artifacts immutable in object storage; derive text, entities, claims, and embeddings as projections.
- Make crawlers low-priority workers on the two weaker nodes, with a strict bandwidth budget.


### Intelligent caching

Use a **content-addressed, tiered cache**, not an opaque “Redis cache everywhere” pattern:

- L1: in-process memoization for pure transforms.
- L2: Redis/KeyDB for TTL-bound distributed results: query results, rate limits, model-response dedupe, locks.
- L3: object store keyed by content hash for expensive durable artifacts: normalized documents, ASTs, embeddings, crawl payloads, simulation output.
- Projection cache: ArangoDB documents with explicit provenance and invalidation dependencies.

Cache keys should include input hash, parser/model version, prompt/template version, configuration hash, and schema version. If any change, you get a new derived artifact instead of corruptly reusing an old answer.

### Visualization

You need three first-class views, not a generic dashboard:

- **Temporal idea map:** commits, note revisions, concept lineage, divergence/convergence.
- **Semantic landscape:** clusters of notes/code concepts, with confidence and evidence drill-down.
- **System topology:** services, queues, projections, nodes, job throughput, cache hit rate, inference utilization, and failure/retry flow.

Then add focused views:

- contradiction review board
- proposed merge/supersession board
- namespace/comprehension map
- crawler source/trust map
- experiment lineage DAG
- geospatial event map
- simulation state and parameter sweep explorer.


### Physical/weather simulation

Do not make weather simulation a first dependency of the KMS. Treat simulation as a plug-in research domain with a shared contract:

```clojure
{:experiment/id ...
 :model/id ...
 :inputs ...
 :parameters ...
 :seed ...
 :environment ...
 :outputs ...
 :metrics ...
 :provenance ...}
```

Then physical models, weather data assimilation, behavioral simulations, and your ACO/graph dynamics all become **experiments that emit artifacts and events**. That keeps the platform reusable without pretending all simulations have the same math.

### Behavioral and sentiment analysis

Use these as **probabilistic annotations**, never ground truth.

For each classification/extraction, retain:

- input artifact/version
- model and version
- label distribution rather than only the winning label
- confidence/calibration
- reasoning evidence span if available
- user correction / acceptance status.

Sentiment can help you trace emotional tone, uncertainty, urgency, or confidence across your own notes over time. It should not be allowed to make claims about mental state, intent, or character.

## Clojure boundaries

Keep infrastructure behind narrow protocols, and make the dataflow/domain parts pure:

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

Your schemas are versioned EDN. Your projectors accept event streams and emit document/edge updates. The Arango driver, NATS driver, object-store driver, Redis driver, and tree-sitter/clj-kondo adapters stay at the edge. That respects the data-oriented, protocol-bound Clojure approach you already prefer.[^9_13][^9_14]

## Build order

### Phase one: corpus archaeology

Build only:

- Git/markdown ingestion
- immutable artifact store
- commit/revision graph
- section-level extraction
- hybrid lexical/vector retrieval
- idea lineage candidates
- redundancy and contradiction review queue
- temporal visualizations

Success criterion: you can trace one important idea across two years of notes and commits with inspectable evidence.

### Phase two: code comprehension

Add:

- tree-sitter ASTs for all languages
- Clojure semantic facts from clj-kondo/analyzer tooling
- namespace/reference/co-change graphs
- conceptual clustering
- namespace-boundary recommendations
- code-to-note lineage links.

Success criterion: it can explain why two namespaces belong together, why a file seems misplaced, and what note/decision led to a code subsystem.

### Phase three: research platform

Add:

- allowlisted crawling
- source trust/provenance
- external datasets
- experiment DAGs
- LLM taxonomy proposal and review
- agent-generated but human-gated research tasks.

Success criterion: it can propose a research question from gaps or contradictions in your own corpus plus external literature, cite the evidence, and generate a runnable experiment plan.

### Phase four: simulations and command center

Add:

- physics/weather/behavioral simulation runtimes
- data assimilation pipelines
- geospatial dashboards
- distributed experiments
- continuous evaluation loops.

Success criterion: you can run reproducible simulations and have the knowledge system explain inputs, assumptions, models, outputs, uncertainty, and links to prior work.

The immediate project is not “AI command center.” It is a **versioned semantic time machine for your own thinking and codebase**. If that works, every later capability—crawling, simulation, research agents, geopolitics—has a durable epistemic foundation instead of becoming another pile of model outputs.
<span style="display:none">[^9_15][^9_16][^9_17][^9_18][^9_19][^9_20][^9_21][^9_22][^9_23][^9_24][^9_25][^9_26][^9_27][^9_28][^9_29][^9_30][^9_31][^9_32][^9_33]</span>

<div align="center">⁂</div>

[^9_1]: https://docs.arango.ai/

[^9_2]: https://docs.arangodb.com/3.10/graphs/

[^9_3]: https://github.com/arangodb/arangodb

[^9_4]: https://docs.arango.ai/arangodb/

[^9_5]: https://docs.arangodb.com/3.11/index-and-search/indexing/working-with-indexes/geo-spatial-indexes/

[^9_6]: https://docs.arangodb.com/3.11/index-and-search/arangosearch/geospatial-search/

[^9_7]: https://docs.arangodb.com/3.13/aql/functions/vector/

[^9_8]: https://pulsar.apache.org/docs/next/cookbooks-tiered-storage/

[^9_9]: https://docs.k3s.io/

[^9_10]: https://github.com/borkdude/analyze-reify

[^9_11]: https://github.com/sogaiu/tree-sitter-clojure

[^9_12]: https://github.com/sogaiu/tree-sitter-clojure/blob/master/doc/scope.md

[^9_13]: https://www.perplexity.ai/search/6e273836-cde5-48c9-a955-7b2d2fb83c41

[^9_14]: https://www.perplexity.ai/search/66a27c7c-3fc1-4433-8947-950d54d17be5

[^9_15]: https://docs.janusgraph.org/storage-backend/cassandra/

[^9_16]: https://cassandra.tools/ibm/janusgraph-utils/

[^9_17]: https://github.com/theHamsta/tree-sitter-clojure-1

[^9_18]: https://old-docs.janusgraph.org/0.2.0/index.html

[^9_19]: https://docs.janusgraph.org/v0.2/

[^9_20]: https://groups.google.com/g/janusgraph-users/c/gkpTEwV9xF4

[^9_21]: https://docs.janusgraph.org/v0.2/advanced-topics/hadoop/

[^9_22]: https://github.com/JanusGraph/janusgraph/releases/

[^9_23]: https://stackoverflow.com/questions/49461719/janusgraph-cassandra-es-cluster-as-backend-spark-cluster-as-analytics-top

[^9_24]: https://medium.com/@searchabhijeet/janusgraph-java-af894f373045

[^9_25]: https://github.com/sogaiu/tree-sitter-clojure/blob/master/doc/testing.md

[^9_26]: https://download.arangodb.com/arangodb32/doc/ArangoDB_Manual_3.2.3.pdf

[^9_27]: https://arango.ai/blog/vector-search-in-arangodb-practical-insights-and-hands-on-examples/

[^9_28]: https://github.com/jmdb-tutorial/datomic-event-sourcing

[^9_29]: https://docs.arangodb.com/3.12/about-arangodb/features/core/

[^9_30]: https://download.arangodb.com/arangodb2/doc/ArangoDB_Manual_2.8.9.pdf

[^9_31]: https://docs.arango.ai/arangodb/stable/graphs/

[^9_32]: https://docs.whgazetteer.org/content/v4/data-model/implementation.html

[^9_33]: https://arango.ai/resources/comparison-rag-with-vector-databases-vs-arangodb-graphrag-with-knowledge-graphs/
---
```
# <q>Build orderPhase one: corpus archaeologyBuild only:Git/markdown ingestionimmutable artifact storecommit/revision graphsection-level extractionhybrid lexical/vector retrievalidea lineage candidatesredundancy and contradiction review queuetemporal visualizationsSuccess criterion: you can trace one important idea across two years of notes and commits with inspectable evidence.</q> @Academic @GitHub @Hugging Face Let's focus on writing a few epics for phase 1 than
```

Yes. Phase 1 should be written as a small set of **vertical epics** that each produce user-visible archaeological capability, rather than as an infrastructure checklist.

The phase objective is:

> Given a current note, concept, or code-adjacent markdown file, reconstruct an evidence-backed timeline of where its ideas appeared, how they changed, what other notes overlap with it, and which claims might be redundant or in tension.

Git needs special treatment: it stores commit trees, not an intrinsic “rename” operation, so rename lineage must be recorded as a confidence-bearing inference made by comparing revisions, not treated as perfect historical fact.[^10_1][^10_2][^10_3]

## Phase-one definition

**In scope**

- Git repositories and Markdown files.
- Git commit/revision history.
- Markdown section parsing.
- Immutable source artifact capture.
- Lexical, structural, and semantic retrieval.
- Candidate-level lineage, redundancy, and contradiction analysis.
- Review queues and temporal visualization.
- Provenance, observability, replay, and deletion/rebuild safety.

**Explicitly out of scope**

- Full multi-language AST indexing.
- Automated note deletion.
- Autonomous external web crawling.
- General research agents.
- Weather, physics, geospatial intelligence, and behavior simulations.
- Fully automated claim truth adjudication.
- A universal graph ontology.

That keeps Phase 1 narrow enough to finish while establishing the custody chain every later subsystem will need.

## Epic 1: Archaeological Ledger

**Goal:** Ingest Git repositories and Markdown revisions into an immutable, replayable artifact ledger.

**User outcome:** “I can select a repository and see every Markdown artifact, its revisions, the commit that introduced each revision, and the exact source bytes that support it.”

### Scope

- Register a repository as a source.
- Discover tracked Markdown files, initially including `**/docs/**/*.md`, then configurable glob rules.
- Walk the reachable Git commit graph.
- Persist:
    - commits, parents, author/committer timestamps, message, tree hash;
    - file-at-commit revisions;
    - blob hash and path;
    - add/modify/delete status;
    - raw source blob in object storage;
    - ingestion run, tool version, configuration, and failures.
- Create initial file-lineage candidates from Git diff rename detection at multiple similarity thresholds.
- Never overwrite an existing source observation; reruns add an ingestion run and deduplicate by content hash/provenance.

Git history across moves should be modeled as a candidate edge with a similarity score and the detection configuration used. Git/GitHub detect renames by comparing content at diff time, not because commits intrinsically record a move, and `--follow` is a limited backward-history heuristic.[^10_4][^10_2][^10_1]

### Acceptance criteria

- Given a Git repository, ingestion is deterministic for a fixed commit set and configuration.
- Every current Markdown file is linked to a current Git blob and at least one commit.
- Every historical revision has an immutable blob reference, source commit, observed time, and parser/ingestor version.
- A user can inspect raw Markdown for any revision without relying on the working tree.
- Re-running ingestion does not duplicate semantically identical source artifacts.
- A failure in one repository or revision is recorded and does not prevent other repositories from being processed.
- An ingestion can be replayed into empty projections from retained source artifacts.


### Core events

```clojure
{:event/type :source/repository-registered
 :repository/id ...
 :repository/remote ...
 :repository/root ...}

{:event/type :git/commit-observed
 :commit/sha ...
 :commit/parents [...]
 :commit/authored-at ...
 :commit/committed-at ...
 :commit/message ...}

{:event/type :artifact/revision-observed
 :artifact/id ...
 :revision/blob-sha ...
 :revision/path ...
 :revision/commit-sha ...
 :revision/content-ref ...
 :revision/language :markdown}

{:event/type :artifact/lineage-candidate
 :from/revision-id ...
 :to/revision-id ...
 :relation :renamed-or-moved
 :similarity 0.87
 :detector {:name :git-diff :rename-threshold 0.70}}
```


### Non-goal

Do not attempt semantic idea lineage here. This epic establishes **revision lineage**: what Git can support with inspectable evidence.

***

## Epic 2: Markdown Evidence Extraction

**Goal:** Turn every Markdown revision into stable, addressable evidence units without losing the ability to trace them back to exact text.

**User outcome:** “I can search and inspect notes at the level of headings, paragraphs, lists, quotes, code blocks, and links—and every derived claim points to its source span.”

### Scope

- Parse Markdown into a normalized AST.
- Generate stable section IDs based on content/path/revision context.
- Extract:
    - document title;
    - heading hierarchy;
    - sections and paragraphs;
    - lists, blockquotes, tables, code fences;
    - links, tags, explicit wiki-style references if present;
    - front matter;
    - line/column and character offsets.
- Preserve parent/child containment:
    - repository → file lineage → revision → document → section → block → span.
- Emit canonical text for each retrieval unit.
- Define chunking as a pure, versioned function rather than an opaque embedding-preprocessing step.


### Acceptance criteria

- Every extracted unit has an exact revision ID and source offsets.
- Rendering a unit’s source span produces the corresponding source text from the immutable blob.
- Heading changes, section moves, and paragraph edits create new revision-level evidence rather than destroying the older form.
- Extraction is idempotent for the same input blob and extractor version.
- Extraction errors produce diagnostics and preserve the original artifact for retry.
- A user can navigate from a search result to its source revision, surrounding section, commit, and full file.


### Domain rule

A section is not “the concept.” It is an **evidence-bearing expression** of one or more concepts at a point in time. This prevents the graph from prematurely collapsing your thinking into false permanent entities.

***

## Epic 3: Retrieval Substrate

**Goal:** Build multi-signal retrieval over Markdown evidence units, with every result explainable.

**User outcome:** “I can find notes by phrase, topic, conceptual similarity, headings, tags, date, repository, and Git history—and see why each result ranked.”

### Scope

Implement three retrieval channels:

- **Lexical:** full-text index over title, headings, body, tags, commit messages, and selected metadata.
- **Structural:** heading/path/link/tag/repository/temporal filters and overlap relationships.
- **Semantic:** embeddings at section/block level, plus versioned model metadata.
- **Hybrid ranker:** combines signals rather than treating vector similarity as truth.

Every result should carry a score breakdown, such as:

```clojure
{:result/section-id ...
 :ranking {:lexical 0.71
           :semantic 0.84
           :structure 0.16
           :temporal 0.32
           :final 0.73}
 :evidence {:matched-terms ["event sourcing" "projection"]
            :semantic-neighbors [...]
            :shared-tags [:architecture :data]}}
```

ArangoDB’s integrated search, graph model, vector indexing, and geospatial support make it capable of serving as the initial unified store for this phase, but the application must retain retrieval scores and model/configuration provenance rather than relying on opaque database behavior.[^10_5][^10_6][^10_7][^10_8]

### Acceptance criteria

- Exact phrases and heading terms retrieve the expected sections.
- Semantic retrieval finds paraphrases that lexical retrieval misses.
- Filters can restrict search by repository, time interval, path, tag, and revision state.
- Every result exposes contributing signals and links to source evidence.
- Embedding model changes produce a new index/projection version; they do not silently overwrite historical results.
- A benchmark query set can report Recall@k, nDCG, latency, and source-coverage metrics.


### First benchmark

Create 30–50 questions from your own notes, such as:

- “Where did I first describe semantic gravity?”
- “Which notes argue for an event log as source of truth?”
- “Show me the early versions of the command-center idea.”
- “What did I mean by concept boundaries in namespaces?”
- “Find everything I wrote that might be a duplicate of this note.”

This becomes the first durable evaluation corpus, not merely a demo script.

***

## Epic 4: Temporal Idea Lineage

**Goal:** Infer candidate “same idea evolving over time” paths from revisions and extracted evidence, while preserving uncertainty.

**User outcome:** “Starting from a current idea, I can walk backward through its likely predecessors and forward through branches, refinements, and descendants.”

### Scope

- Build temporal graphs from:
    - Git revisions;
    - section containment;
    - lexical similarity;
    - semantic similarity;
    - shared named concepts/tags;
    - explicit links;
    - commit co-occurrence;
    - rename/move candidates.
- Produce **candidate** relations:
    - `:continues`
    - `:refines`
    - `:splits-into`
    - `:merges-from`
    - `:references`
    - `:possibly-derived-from`
    - `:supersedes`
- Use deterministic candidate generation first.
- Add LLM analysis only after retrieval chooses a small evidence set; require structured output and provenance.
- Support human review: accept, reject, relabel, or annotate candidate relations.


### Acceptance criteria

- The system can produce a ranked lineage path for a selected section.
- Each edge includes date ordering, evidence spans, contributing scores, and inference version.
- A user can inspect why a predecessor/descendant was suggested.
- Human accept/reject decisions become first-class events and influence later ranking.
- The graph never converts a candidate edge into a fact without explicit status.
- Lineage queries handle forks: one earlier idea can yield multiple descendants, and multiple earlier threads can converge.


### Important design constraint

Lineage is not necessarily textual similarity. A later note may reject, clarify, operationalize, or split an earlier idea. The epic should model “related historical transformation” rather than assume similarity means continuity.

***

## Epic 5: Redundancy and Tension Review

**Goal:** Surface note pairs or clusters that are duplicates, near-duplicates, supersessions, or possible contradictions—and make review safe.

**User outcome:** “I can reduce note clutter without erasing the history of my thought, and I can identify where I have changed my mind or left incompatible assumptions unresolved.”

### Scope

- Generate review candidates from hybrid retrieval and temporal proximity.
- Classify candidate relationships into:
    - `:duplicate`
    - `:near-duplicate`
    - `:complementary`
    - `:superseded`
    - `:possible-contradiction`
    - `:unclear`
- Detect deterministic contradiction patterns first:
    - explicit negation;
    - mutually exclusive enumerated values;
    - incompatible dates/statuses;
    - “always/never” versus scoped counterexamples;
    - incompatible architecture decisions under the same stated scope.
- Use LLMs for bounded comparison:
    - extract claims and scope;
    - identify the minimum conflicting proposition;
    - quote source spans;
    - produce confidence and alternative interpretations.
- Make every action reversible:
    - keep,
    - link,
    - mark superseded,
    - merge into a synthesis note,
    - archive,
    - ignore,
    - create research question.


### Acceptance criteria

- The system never deletes a note automatically.
- Every proposed relationship has at least two source spans and a score/rationale.
- Review actions create events, not destructive edits.
- A “merge” produces a synthesis artifact that links to its source notes.
- A “superseded” relation does not remove old notes from historical or lineage search.
- The user can tune candidate volume and confidence thresholds.
- Candidate classifiers can be evaluated against a human-labeled review set.


### Quality rule

“Low quality” must never mean “not useful.” Distinguish:

- low informational density,
- obsolete claim,
- unfinished fragment,
- duplicate expression,
- private emotional/contextual trace,
- and historically important precursor.

Those are different states with different retrieval and retention policies.

***

## Epic 6: Temporal Research Workbench

**Goal:** Provide the first interface where archaeology is usable, reviewable, and genuinely interesting.

**User outcome:** “I can follow an idea’s history as a map, pivot into original evidence, review candidate relationships, and ask grounded questions over my corpus.”

### Views

- **Timeline:** commits, revisions, section expressions, lineage candidates, accepted transitions.
- **Concept/idea map:** nodes clustered by hybrid retrieval; edges styled by relation type and confidence.
- **Evidence drawer:** exact source span, full section context, commit metadata, source diff.
- **Candidate review inbox:** duplicates, contradictions, supersessions, lineage suggestions.
- **Search workspace:** lexical/semantic/hybrid mode, filters, score explanation.
- **Corpus health panel:** unparsed revisions, extraction errors, index age, queue backlog, confidence distribution.

Do not start with a “beautiful graph visualization” that has no workflow. The core loop is:

1. Search or select an idea.
2. Inspect evidence.
3. Traverse timeline/lineage.
4. Review suggested relationships.
5. Record a decision or research question.
6. Re-run projections and observe improved retrieval.

### Acceptance criteria

- From any graph edge, the user can open its source evidence in one interaction.
- From any section, the user can open its commit/revision timeline.
- The UI makes clear whether an edge is observed, inferred, or human-accepted.
- Search results and graph views share stable IDs and do not disagree about source lineage.
- The review queue supports keyboard-efficient triage and preserves rationale.
- The system can export a lineage packet: selected idea, timeline, sources, candidate/accepted edges, and review decisions.


## Cross-cutting epic: Operability

This is not optional infrastructure work. It belongs across all six epics.

**Goal:** Every ingestion, extraction, embedding, projection, and review decision is observable, attributable, and replayable.

OpenTelemetry is a vendor-neutral framework for collecting and exporting traces, metrics, and logs; its logs model includes resource information so telemetry can be correlated to the service/process that produced it.[^10_9][^10_10]

### Required telemetry

- Per-run correlation ID.
- Repository, commit, blob, revision, section, and job IDs in structured logs.
- Queue depth, retry count, dead-letter count, and job latency.
- Parsing success/error rates by repository and extractor version.
- Embedding throughput, cache hit rates, GPU utilization, and index lag.
- Search latency and retrieval metrics by query class.
- Candidate edge volume, acceptance/rejection rate, and reviewer disagreement.
- Projection build time and replay time.


### Required resilience properties

- Every derived projection is disposable and rebuildable from raw blobs + events.
- Each job is idempotent using content hash plus processor/configuration version.
- Failed tasks enter a visible quarantine/dead-letter state rather than disappearing.
- Backups include raw artifacts, event records, graph/document collections, and configuration/version manifests.
- Human review is preserved as source data, not merely UI state.


## Delivery sequence

A sensible sequence is:

1. **Epic 1: Archaeological Ledger**
2. **Epic 2: Markdown Evidence Extraction**
3. **Epic 3: Retrieval Substrate**
4. **Epic 6: Minimal Workbench search + evidence view**
5. **Epic 4: Temporal Idea Lineage**
6. **Epic 5: Redundancy and Tension Review**
7. **Expand the workbench with timeline/map/review workflows**

This creates a usable tool before the inference-heavy work begins. You should be able to search and inspect evidence before trusting the system to tell you that two ideas are related or contradictory.

## Phase-one exit test

Phase 1 is complete when you can take one present-day note—say, your command-center / knowledge-graph system idea—and produce an inspectable report that shows:

- its earliest recovered evidence in your Git/Markdown history;
- major section-level expressions and revision dates;
- candidate and accepted lineage transitions;
- branches where the idea split or acquired a distinct implementation;
- related notes ranked by lexical, semantic, and structural evidence;
- redundant/superseded notes separated from historically meaningful predecessors;
- at least one reviewed possible contradiction, with its scope and evidence;
- links to all source blobs and commits;
- enough telemetry to explain how that result was built.

That makes the system a reliable instrument for thinking before it becomes an autonomous research machine.
<span style="display:none">[^10_11][^10_12][^10_13][^10_14][^10_15][^10_16][^10_17][^10_18][^10_19]</span>

<div align="center">⁂</div>

[^10_1]: https://stackoverflow.com/questions/33865637/clean-git-history-of-deleted-files-keeping-renamed-files-history

[^10_2]: https://stackoverflow.com/questions/67738831/disable-rename-detection-during-the-git-commit-to-preserve-history

[^10_3]: https://git-scm.com/docs/git-log

[^10_4]: https://github.blog/changelog/2022-06-06-view-commit-history-across-file-renames-and-moves/

[^10_5]: https://docs.arango.ai/

[^10_6]: https://docs.arangodb.com/3.13/aql/functions/vector/

[^10_7]: https://docs.arangodb.com/3.10/graphs/

[^10_8]: https://docs.arango.ai/arangodb/

[^10_9]: https://opentelemetry.io/docs/specs/otel/logs/

[^10_10]: https://opentelemetry.io/docs/

[^10_11]: https://stackoverflow.com/questions/55339525/how-to-log-the-entire-git-history-of-a-file-that-is-renamed-moved-using-its-old

[^10_12]: https://stackoverflow.com/questions/5743739/how-to-really-show-logs-of-renamed-files-with-git

[^10_13]: https://tomodahinata.com/en/blog/opentelemetry-observability-production-tracing-metrics-logs

[^10_14]: https://docs.redhat.com/en/documentation/red_hat_build_of_opentelemetry/3.9/html/sending_traces_logs_and_metrics_to_the_collector/otel-sending-traces-logs-and-metrics-to-otel-collector

[^10_15]: https://chelseatroy.com/2020/05/09/question-how-does-git-detect-renames/

[^10_16]: https://download.arangodb.com/arangodb33/doc/ArangoDB_Manual_3.3.22.pdf

[^10_17]: https://stackoverflow.com/questions/51793811/renaming-file-name-to-older-name-loses-history-in-git

[^10_18]: https://stackoverflow.com/questions/64454637/git-log-follow-not-working-to-show-history-beyond-renames

[^10_19]: https://www.elysiate.com/blog/observability-opentelemetry-complete-implementation-guide
---
# @Academic @GitHub @Hugging Face now draft the epics for phase 2

Phase 2 is **code comprehension and architectural archaeology**. Its purpose is not to “lint every language” or automatically reorganize code; it is to reconstruct the conceptual boundaries that source layout, incremental commits, and local static-analysis metrics fail to express.

The phase objective is:

> Given a symbol, namespace/module, or subsystem, show what it does, what it depends on, what concepts it implements, which files historically evolved with it, where its real boundaries are, and which organizational changes are worth human review.

Tree-sitter is suitable as the common syntax substrate because it builds concrete syntax trees and can update them incrementally, but it is deliberately syntax-oriented. Clojure semantic facts—namespace resolution, definitions, usages, lint findings—should come from `clj-kondo` analysis exports rather than being reconstructed from tree shape alone.[^11_1][^11_2][^11_3][^11_4]

## Phase-two definition

**In scope**

- Clojure and ClojureScript first; then Go, Python, Bash, YAML, EDN, JSON, Lua, Ruby, and C through a common parsing contract.
- Immutable source revisions from Phase 1.
- Language-aware ASTs and semantic facts.
- Symbol/reference/dependency/co-change/runtime-evidence graphs.
- Concept-to-code links.
- Multi-view clusters and boundary recommendations.
- Code maps and comprehension workflows.
- Human-reviewed architecture refactoring plans.

**Explicitly out of scope**

- Automatic large-scale file moves or namespace rewrites.
- Replacing language-native compilers, linters, type checkers, or build tools.
- Treating AST similarity as proof of equivalent behavior.
- Whole-program semantic analysis for every language from day one.
- General-purpose vulnerability scanning.
- Agent-authored refactors without a human-approved plan.
- Making code organization conform to a generic style ideology.


## Epic 7: Polyglot Source Ledger

**Goal:** Extend the Phase 1 artifact ledger from Markdown revisions to source and configuration revisions, with language identity, parser provenance, and a stable source-unit model.

**User outcome:** “I can see every source file, configuration file, and historical revision in the corpus, and I know exactly which parser and extraction version produced every derived fact.”

### Scope

- Discover configured source types:
    - Clojure: `.clj`, `.cljc`, `.cljs`
    - Go: `.go`
    - Python: `.py`
    - C: `.c`, `.h`
    - Lua: `.lua`
    - Ruby: `.rb`
    - Bash: `.sh`, executable scripts
    - YAML: `.yaml`, `.yml`
    - EDN: `.edn`
    - JSON: `.json`
- Assign a language and parser strategy per revision.
- Store original bytes in immutable object storage, as Phase 1 does for Markdown.
- Represent file lineage independently of path and preserve candidate rename/move edges.
- Establish a common source-unit contract:
    - file;
    - module/namespace;
    - declaration;
    - callable;
    - type/schema;
    - import/require;
    - invocation/reference;
    - literal/configuration key;
    - comment/docstring;
    - parse diagnostic.
- Version every parser, grammar, extractor, and normalization rule.


### Acceptance criteria

- A source revision can be retrieved exactly by repository, commit, path, and blob hash.
- Every revision has a detected language plus parser/extractor provenance.
- Unsupported or malformed files are retained as artifacts and produce diagnostics rather than disappearing.
- The system records whether each semantic fact came from syntax-only extraction, a language-native analyzer, or an inferred model.
- The same source blob and parser configuration always produce the same normalized extraction.
- Replaying the source ledger into an empty index reproduces the same file/revision records.


### Domain rule

A file is a historical container, not the principal unit of understanding. The system should preserve file topology but project meaningful units—namespaces, declarations, schemas, functions, comments, blocks, and configuration fragments—into the graph.

## Epic 8: Syntax Forest and Structural Features

**Goal:** Parse source files into lossless-ish concrete syntax representations and derive language-neutral structural features for retrieval, comparison, and visualization.

**User outcome:** “I can inspect a function or config block as a tree, search for recurring structural patterns, and compare implementation shapes across the corpus without pretending different languages are identical.”

### Scope

- Use Tree-sitter as the default CST/AST parser substrate where grammars are suitable.
- Store:
    - raw tree;
    - normalized node stream;
    - node types;
    - parent/child/sibling relationships;
    - source spans;
    - comments and docstrings;
    - parser errors/missing nodes.
- Produce versioned structural representations:
    - preorder node-type sequences;
    - rooted subtree hashes;
    - parent-child edge n-grams;
    - selected AST paths;
    - declaration shape;
    - control-flow and data-literal summaries where parser support permits.
- Define an **AST n-gram vocabulary per language family**, rather than one universal bag of node names.
- Index structural features at declaration/function and module level.
- Build a visual tree inspector with source synchronization.

Tree-sitter is a parser generator and incremental parsing library that builds concrete syntax trees, which is useful for retaining enough syntax fidelity to inspect source structure and update analyses efficiently as code changes.[^11_5][^11_1]

### Acceptance criteria

- Every extracted node can resolve back to an exact source span.
- A malformed file still yields partial parse structure and useful diagnostics where the grammar allows it.
- Structural fingerprint generation is deterministic and versioned.
- A user can query for recurring patterns within a language, for example “all Clojure `defmethod` forms with similar dispatch patterns” or “all YAML deployment objects with similar key structure.”
- Structural similarity searches show the matching subtrees, not merely a score.
- A language-specific adapter can add richer features without changing the common source-unit contract.


### Non-goal

Do not assume AST similarity implies domain similarity. A map transformation and an ECS reducer can look structurally alike while serving unrelated concepts; structural signals are one input to later multi-view clustering.

## Epic 9: Clojure Semantic Intelligence

**Goal:** Make Clojure and ClojureScript the first deeply understood language layer using `clj-kondo`, compiler-aware metadata where appropriate, and Clojure-specific domain modeling.

**User outcome:** “I can ask what a namespace provides, what symbols it consumes, where a var is defined and used, how macros affect the analysis, and which namespaces form a coherent subsystem.”

### Scope

- Run `clj-kondo` project-wide and ingest its analysis export/cache-derived facts.
- Capture:
    - namespace declarations;
    - `:require`, `:use`, `:import`, aliases, refer clauses;
    - var definitions;
    - var usages;
    - keywords;
    - protocol definitions and implementations;
    - multimethods and methods;
    - macros and macro usages;
    - test declarations;
    - linter findings;
    - source locations.
- Ingest project configuration from `.clj-kondo/config.edn`.
- Treat macro-heavy or dynamically resolved behavior as explicitly incomplete rather than falsely resolved.
- Build Clojure-specific relationship types:
    - `:namespace/requires`
    - `:var/defines`
    - `:var/references`
    - `:protocol/implemented-by`
    - `:multimethod/implemented-by`
    - `:macro/expands-into` where evidence exists
    - `:test/verifies`
    - `:config/affects-analysis`.
- Link Clojure docstrings, comments, namespace names, and keyword vocabularies to Phase 1 concepts.

`clj-kondo` supports dependency analysis and cached namespace analysis, while its own design guidance distinguishes low-latency linting from project-wide analysis exports. That makes it appropriate to ingest as semantic evidence rather than misuse as an all-purpose architecture oracle.[^11_2][^11_3][^11_6][^11_4]

### Acceptance criteria

- Every Clojure semantic edge identifies its analyzer version and source location.
- Users can navigate definition → references → containing namespace → dependents.
- Namespace dependency views distinguish explicit requires from actual resolved symbol usage.
- Protocol, multimethod, macro, and test relationships are visible as different edge types.
- Dynamic/unresolved references are visibly marked as unknown or partial, not silently omitted.
- `clj-kondo` lint diagnostics are searchable and traceable to historical revisions.
- The system can compare namespace dependency structure across Git revisions.


### Research question enabled

> “Did this namespace become conceptually incoherent because its responsibilities drifted, because it acquired too many dependency directions, or because the original domain boundary was never expressed in code?”

## Epic 10: Program Relationship Graph

**Goal:** Build a versioned, multi-layer graph of structural, semantic, temporal, and conceptual relationships across source artifacts.

**User outcome:** “I can traverse from a concept to notes, then to code symbols, dependent namespaces, tests, co-changing files, and historical implementation decisions.”

### Relationship layers

| Layer | Example edges | Evidence source |
| :-- | :-- | :-- |
| Containment | Repository → revision → namespace → var | Parser/analyzer |
| Syntax | Declaration → AST subtree / structural fingerprint | Tree-sitter |
| Dependencies | Namespace → requires → namespace | `clj-kondo`, language analyzers |
| References | Symbol → calls/references → symbol | Semantic analysis |
| Configuration | Service → reads → EDN/YAML/JSON key | Parser + semantic adapter |
| Verification | Test → verifies → function/namespace | Test/analyzer conventions |
| Temporal | Revision → changed-with → revision | Git history |
| Co-change | File/symbol → co-changes-with → file/symbol | Commit projections |
| Conceptual | Note concept → described-by/implemented-by → code unit | Hybrid retrieval + review |
| Runtime | Service/function → observed-to-interact-with → service/function | Future traces; optional Phase 2 ingest |

### Scope

- Implement relationship provenance as a first-class requirement.
- Preserve source revision and temporal validity for edges.
- Separate:
    - observed relationship;
    - tool-derived relationship;
    - probabilistic inference;
    - user-accepted architectural relation.
- Build graph projections optimized for:
    - local traversal;
    - dependency impact;
    - historical evolution;
    - concept-to-code retrieval;
    - cluster computation.


### Acceptance criteria

- Every edge has type, evidence source, source revision, confidence/status, and extraction version.
- Users can filter graph traversal by relation type and evidence status.
- A query can answer: “Which notes, symbols, tests, config entries, and historical commits are connected to this concept?”
- Historical queries return relationships valid at a selected commit or time interval.
- Rebuilding graph projections from the artifact/event ledger preserves IDs and does not erase human review decisions.
- Graph queries are bounded by hop, edge-type, time, and result budgets to prevent runaway traversals.


### Domain rule

A graph edge is not automatically an architectural claim. Most edges are evidence. Architecture is the reviewed interpretation of many edges.

## Epic 11: Concept-to-Code Grounding

**Goal:** Link the human concepts expressed in Markdown to the code and configuration structures that implement, mention, test, or contradict them.

**User outcome:** “I can start with an idea in my notes and find its implementation, tests, relevant configs, and historical transitions—or learn that it was never implemented.”

### Scope

- Derive candidate links between Phase 1 concept/section units and Phase 2 code units using:
    - lexical overlap;
    - embeddings;
    - docstrings/comments;
    - namespace/module naming;
    - keyword/configuration vocabulary;
    - Git temporal proximity;
    - commit-message overlap;
    - explicit links;
    - user labels.
- Classify candidate relationship types:
    - `:implements`
    - `:describes`
    - `:tests`
    - `:configures`
    - `:depends-on-concept`
    - `:obsolete-implementation-of`
    - `:contradicts-design`
    - `:possibly-related`.
- Add a human review workflow for high-value links.
- Support reverse queries:
    - code → relevant design notes;
    - note → implementation candidates;
    - concept → tests/config/services.


### Acceptance criteria

- Every proposed code-concept relation contains evidence from both sides.
- A user can distinguish “the code uses the same vocabulary” from “this implements the design.”
- Human decisions on relationships are retained as events and incorporated into later candidate ranking.
- Temporal ordering is visible: a note can precede, follow, or co-evolve with an implementation.
- The system can identify “orphan concepts” with no accepted implementation and “orphan implementations” with no explanatory notes.
- The system never presents a semantic retrieval hit as confirmed implementation without status/provenance.


### Example question

> “Which parts of the OpenPlanner/Graph-Weaver code were actually motivated by the semantic-gravity and ACO notes, and which parts only became adjacent because they evolved in the same repository?”

That directly extends your existing Graph-Weaver / ACO work into a traceable research object rather than a loose mental model.[^11_7]

## Epic 12: Architectural Boundary Inference

**Goal:** Identify candidate subsystem and namespace boundaries using multiple independent views of the codebase, then turn them into human-reviewable architectural hypotheses.

**User outcome:** “I can see why a group of files belongs together conceptually, why a namespace is likely misplaced, and what a low-risk reorganization would look like.”

### Scope

Build a multi-view similarity model. Do not cluster on imports alone.

Signals include:

- directed namespace/import dependency;
- resolved symbol reference;
- shared protocol/multimethod participation;
- shared domain vocabulary;
- docstring/comment embeddings;
- AST structural patterns;
- shared config keys;
- shared tests;
- co-change history;
- runtime traces when available;
- temporal co-emergence;
- user labels and accepted concept-to-code links;
- complexity/lint facts as weak diagnostic signals.

Generate candidate boundary relations:

- `:belongs-to-subsystem`
- `:bridge-module`
- `:adapter`
- `:boundary-violation`
- `:cyclic-coupling`
- `:misplaced-by-concept`
- `:overloaded-namespace`
- `:candidate-extraction`
- `:candidate-merge`.


### Acceptance criteria

- Every suggested cluster includes a signal breakdown rather than just a generated name.
- A user can view how cluster membership changes if one signal family is removed.
- The system identifies bridge modules separately from strongly cohesive clusters; a bridge is not automatically “bad.”
- Recommendations include a confidence level, expected effect, affected symbols/tests, and a proposed safe investigation—not an autonomous move.
- Users can pin accepted boundaries and label intentional exceptions.
- The system evaluates recommendations against historical refactors where possible: did files later move together, split apart, or continue co-changing?
- Boundary recommendations can be exported as an ADR/research memo draft with linked evidence.


### Critical principle

The filesystem is one coordinate system. Dependency topology, semantic vocabulary, tests, runtime behavior, and time are other coordinate systems. A useful boundary recommendation appears where those views converge—or where their mismatch reveals hidden architectural debt.

## Epic 13: Code Archaeology Workbench

**Goal:** Extend the Phase 1 research workbench with source and architectural-comprehension workflows.

**User outcome:** “I can explore the codebase as a living historical system rather than a directory tree, and move from a question to evidence to a reviewable design hypothesis.”

### Core views

- **Namespace map:** directed dependency graph, layering, cycles, inbound/outbound pressure.
- **Symbol explorer:** definition, references, callers/callees where supported, historical changes, tests, docs.
- **Concept-to-code view:** Markdown concepts and implementation candidates with evidence/status.
- **Co-change timeline:** files, namespaces, and symbols that changed together across commits.
- **Structural motif explorer:** AST pattern search and matching subtrees.
- **Boundary map:** candidate clusters, bridges, exceptions, and signal breakdown.
- **Historical architecture slider:** select a commit/time range and compare dependency/cluster structure.
- **Refactor review packet:** proposed investigation, impacted units, evidence, tests, and rollback strategy.


### Acceptance criteria

- From a note, the user can reach a related code symbol and inspect the evidence in no more than two pivots.
- From a namespace, the user can identify its core concepts, strongest dependencies, co-change peers, tests, and architectural candidates.
- Every recommendation separates observed facts from inferred interpretation.
- Graph visualizations support filtering, search, time range, relation-type selection, and evidence drill-down.
- Views remain usable at scale through progressive disclosure; do not render the whole graph by default.
- A user can export a bounded investigation packet rather than screenshotting an unbounded graph.


## Cross-cutting epic: Analyzer quality and evaluation

**Goal:** Ensure Phase 2 is a research instrument rather than an attractive graph of plausible nonsense.

### Evaluation datasets

Create a manually curated “architecture archaeology” benchmark from your own corpus:

- 20 known namespace/module boundaries.
- 20 known concept-to-code links.
- 10 intentional bridges/adapters.
- 10 historical moves/renames.
- 10 examples of co-change that does **not** mean conceptual cohesion.
- 10 intentionally dynamic Clojure patterns where static resolution is incomplete.
- 10 known stale/orphan notes or implementations.


### Required metrics

- Parser coverage and error rate by language.
- Semantic-resolution coverage and unknown-rate by language/tool.
- Precision/recall for reviewed concept-to-code links.
- Precision/recall for accepted boundary recommendations.
- Cluster stability across commits and indexing/model versions.
- False-positive rate for “misplaced module” suggestions.
- Search Recall@k / nDCG for code comprehension queries.
- Time-to-evidence for a user investigation.
- Human-review acceptance, rejection, and “insufficient evidence” rates.

`clj-kondo` itself emphasizes project-wide analysis through its analysis export rather than trying to turn every editor-oriented linter run into a global architecture analysis. Follow that separation: build offline projections and research views from exported facts, while retaining fast local analysis paths for interactive use.[^11_3][^11_4]

## Delivery sequence

1. **Epic 7: Polyglot Source Ledger**
2. **Epic 8: Syntax Forest and Structural Features**
3. **Epic 9: Clojure Semantic Intelligence**
4. **Epic 10: Program Relationship Graph**
5. **Epic 13: Minimal Code Archaeology Workbench**
6. **Epic 11: Concept-to-Code Grounding**
7. **Epic 12: Architectural Boundary Inference**
8. **Analyzer quality/evaluation hardening throughout**

That order starts with Clojure as the deep implementation path while establishing a clean parser contract for the other languages. It avoids the common trap of spending months building a broad but shallow “polyglot code intelligence” layer before the system can answer useful questions about the language you care about most.

## Phase-two exit test

Phase 2 is complete when you can choose one of your Clojure subsystems—such as the OpenPlanner ingestion/data layer or Graph-Weaver—and produce an evidence-backed architectural investigation that includes:

- its namespaces, symbols, dependencies, protocols, multimethods, tests, and relevant configuration;
- the Git timeline that shows how the subsystem formed;
- links from design notes and concepts to implementation units, with accepted versus candidate status;
- co-change, structural, semantic, and dependency signals;
- an explanation of which namespaces form a cohesive subsystem, which are bridges, and which may be historically misplaced;
- at least one human-reviewed boundary/refactor hypothesis with affected units, tests, evidence, and a reversible rollout plan;
- a clear accounting of unknowns caused by macro expansion, dynamic resolution, generated code, or incomplete analysis.

The success condition is not “the platform reorganized your code.” It is that it can make your implicit architecture visible enough that you can reorganize it deliberately.
<span style="display:none">[^11_10][^11_11][^11_12][^11_13][^11_14][^11_15][^11_16][^11_8][^11_9]</span>

<div align="center">⁂</div>

[^11_1]: https://github.com/tree-sitter/tree-sitter

[^11_2]: https://github.com/clj-kondo/clj-kondo

[^11_3]: https://github.com/clj-kondo/clj-kondo/blob/master/analysis/README.md

[^11_4]: https://github.com/clj-kondo/clj-kondo/blob/master/doc/dev.md

[^11_5]: https://github.com/AmirHesam46/Tree-sitter-Code-Parsing-and-Analysis

[^11_6]: https://github.com/clj-kondo/clj-kondo/blob/master/doc/hooks.md

[^11_7]: https://www.perplexity.ai/search/0c80ab22-a903-436b-94bf-4b026a660698

[^11_8]: https://github.com/bollian/tree-sitter-openscad

[^11_9]: https://github.com/clj-kondo/clj-kondo/blob/master/src/clj_kondo/impl/cache.clj

[^11_10]: https://flexdoc.flexiana.com/lib/clj-kondo

[^11_11]: https://arxiv.org/pdf/1504.03477.pdf

[^11_12]: https://raw.githubusercontent.com/clj-kondo/clj-kondo/master/CHANGELOG.md

[^11_13]: https://dr.lib.iastate.edu/server/api/core/bitstreams/6a3c7af4-ce49-4622-a6b9-f52712e68d46/content

[^11_14]: https://pure.rug.nl/ws/portalfiles/portal/1286147518/1-s2.0-S0950584925000618-main.pdf

[^11_15]: https://en.wikipedia.org/wiki/Tree-sitter_(parser_generator)

[^11_16]: https://www.deusinmachina.net/p/tree-sitter-revolutionizing-parsing
---
# ADR 000: Foundational Technology Stack

- **Status:** Accepted
- **Date:** 2026-07-10
- **Decision owners:** Platform owner
- **Scope:** Phases 1–4 of the local knowledge archaeology, code intelligence, research, and simulation platform


## Context

The platform is a greenfield, self-hosted research system intended to turn a large and growing personal corpus of notes, Git history, source code, external research artifacts, datasets, and experiments into inspectable evidence and useful research workflows.

The system is expected to support four cumulative capabilities:

1. **Corpus archaeology:** immutable Git/Markdown ingestion, revision and idea lineage, hybrid retrieval, contradiction/redundancy review, and temporal visualization.
2. **Code archaeology:** polyglot parsing, Clojure-first semantic analysis, program relationship graphs, concept-to-code grounding, and architecture-boundary review.
3. **Research operations:** governed external acquisition from academic/open-source sources, prior-art discovery, research-question generation, datasets/models, and reproducible experiment design.
4. **Computational laboratory:** graph dynamics, agent-based models, spatial-temporal and environmental scenarios, simulations, uncertainty analysis, and bounded automated experiment loops.

The physical cluster is heterogeneous:


| Node | Approximate capability | Intended primary responsibility |
| :-- | :-- | :-- |
| Intel Ultra 9, RTX 4070 Ti, 32 GB RAM | High-performance CPU/GPU node | Inference, embeddings, reranking, expensive extraction, high-value simulation jobs |
| Ryzen 7, 16 GB RAM | General-purpose worker | Search/database projections, ETL, orchestration, CPU-bound analysis |
| Ryzen 3, 8 GB RAM | Low-resource node | Storage, routing, queued background work, telemetry |
| Intel i5, 8 GB RAM | Low-resource node | Storage, backup, telemetry, low-priority IO work |

The cluster has approximately 64 GB total RAM, ample but finite disk, a 500 Mbps LAN, one dedicated GPU, two integrated GPUs, and an NPU of uncertain practical utility.

The owner strongly prefers Clojure, has time to build deliberately, and wants a design that favors inspectability, deterministic replay, and durable evidence over fashionable complexity.

## Decision drivers

The architecture must satisfy these properties:

- **Evidence before synthesis:** every result should trace to source artifacts, parser/analyzer output, human decision, or explicitly labeled model inference.
- **Event-sourced and rebuildable:** raw artifacts and durable events are authoritative; search indices, graph projections, embeddings, and dashboards are replaceable projections.
- **Clojure-first application logic:** ingestion, orchestration, domain models, workflows, and research logic should be idiomatic Clojure where viable.
- **Hybrid retrieval:** lexical, vector, structural, temporal, graph, and user-label signals should be combinable; vector similarity is never the sole source of relevance.
- **Incremental complexity:** deploy a thin, useful Phase 1 before introducing broad polyglot analysis, external research agents, or simulation.
- **Heterogeneous hardware awareness:** avoid treating weak nodes as small versions of the GPU workstation; give them stable, non-competing IO, storage, backup, and observability roles.
- **Operational visibility:** all ingestion, extraction, jobs, model calls, index updates, and experiments need logs, metrics, traces, replay IDs, resource measurements, and failure states.
- **Resilience through recoverability:** data must be reproducible from raw blobs plus events, not dependent on any one index or graph database.
- **Local-first and cost-conscious:** use open-source/self-hosted components unless a future managed service materially improves a defined requirement.


## Decision

The platform will use the following baseline stack.


| Concern | Chosen technology | Role |
| :-- | :-- | :-- |
| Deployment/orchestration | K3s | Lightweight Kubernetes cluster across four machines |
| Core application language | Clojure/JVM | Ingestion, event handling, workflows, graph/retrieval logic, APIs |
| Event transport and durable work queues | NATS + JetStream | Commands/events, replayable worker queues, backpressure, durable consumers |
| System of record | PostgreSQL | Event ledger, user decisions, task state, provenance, configuration, relational projections |
| Immutable artifact store | S3-compatible object storage, initially MinIO | Raw Git blobs, Markdown/code revisions, PDFs, datasets, model artifacts, simulation outputs |
| Search/vector/geospatial index | Elasticsearch | Full-text/BM25, metadata filters, dense-vector kNN, hybrid retrieval, geospatial queries |
| Knowledge graph | Neo4j | Evidence graph, lineage, dependency, concept-to-code, research, and experiment traversals |
| Clojure analysis | clj-kondo | Namespace/symbol/require/reference/lint semantic evidence for Clojure/ClojureScript |
| Polyglot syntax parsing | Tree-sitter | Versioned concrete-syntax trees and structural features for supported languages |
| Model/inference runtime | Local GPU-serving layer, selected per model | Embeddings, reranking, extraction, taxonomy proposals, constrained QA and agents |
| Observability | OpenTelemetry + Prometheus + Grafana + Loki/Tempo-compatible backends | Correlated logs, metrics, traces, dashboards, alerting |
| Infrastructure definition | GitOps-style manifests plus declarative configuration | Versioned deployment, repeatable cluster state, auditable changes |
| Experiment runtime | Containers first; FMI adapters where justified | Reproducible simulations and external scientific workloads |
| Spatial-temporal catalog | STAC-compatible metadata model | Provenance-aware cataloging/search of spatial-temporal assets |

K3s is selected over minikube as the cluster substrate. K3s is a fully compliant lightweight Kubernetes distribution, distributed as a compact binary/container image and designed for edge, homelab, development, air-gapped, and resource-constrained environments; it packages core components such as container runtime, networking, DNS, ingress, load-balancing, and local persistent-volume support.[^14_1]

NATS JetStream is selected as the event transport and durable work-queue layer. JetStream extends NATS with persisted streams, replay, consumers, acknowledgements, file/memory storage, replication options, and at-least-once delivery semantics; it also supports deduplicated/idempotent publishing through message IDs.[^14_2][^14_3][^14_4][^14_5]

## Architecture principles

### Raw artifacts are authoritative

The system of record is not Elasticsearch, Neo4j, embeddings, or any LLM-produced label. The authoritative record is:

1. immutable raw artifact content;
2. source identity and revision metadata;
3. append-only domain events;
4. user decisions and annotations;
5. versioned extractor/parser/model configurations.

Every other representation is a projection that can be discarded and rebuilt.

```text
Git/API/source files
        |
        v
Immutable object artifacts + Postgres event ledger
        |
        +-------------------------+
        |                         |
        v                         v
Elasticsearch projections     Neo4j projections
        |                         |
        +-----------+-------------+
                    |
                    v
Hybrid retrieval, graph traversal, review UX, QA, simulation inputs
```


### Derived facts carry provenance

Every derived entity, relationship, label, embedding, ranking, and simulation result must include:

```clojure
{:provenance/source-artifacts [...]
 :provenance/source-spans [...]
 :provenance/observed-at ...
 :provenance/processor {:name ...
                         :version ...
                         :config-hash ...}
 :provenance/status :observed | :derived | :candidate | :human-accepted
 :provenance/confidence ...
 :provenance/run-id ...}
```

A graph edge is evidence or an inference with status; it is not automatically a fact. A vector hit is a retrieval signal; it is not automatically a semantic relationship. An LLM output is a candidate interpretation; it is not automatically an accepted claim.

### Event sourcing is domain-level

Event sourcing will be applied to the platform’s durable domain changes:

- source registered;
- commit/revision observed;
- artifact stored;
- section/symbol extracted;
- embedding/index projection completed;
- candidate lineage/contradiction/link generated;
- user accepted/rejected/relabelled a candidate;
- source policy changed;
- experiment approved/executed/completed;
- taxonomy/concept decision recorded.

NATS JetStream is not the sole long-term event ledger. It provides durable asynchronous transport, replayable work streams, and worker coordination. PostgreSQL stores the canonical business/event records and user decisions. This separation avoids overloading a message broker with all query and audit responsibilities while retaining the ability to replay projections from a durable event log.[^14_3][^14_5]

### Projections are versioned

Every projection records the exact source and processing identity:

```clojure
{:projection/type :search-section
 :projection/source-revision ...
 :projection/processor-version ...
 :projection/model-version ...
 :projection/schema-version ...
 :projection/created-at ...}
```

Changing an embedding model, AST normalizer, ontology, ranking strategy, or LLM prompt creates a new projection version. It must not silently overwrite the prior interpretation.

## Component rationale

## K3s for orchestration

K3s is chosen because the project requires real multi-node deployment, service discovery, restart behavior, workload placement, persistent volumes, secrets, and observability integrations—but the cluster is a four-node homelab with finite RAM and only two strong workers.

Minikube is not selected as the foundation because it is optimized mainly for local Kubernetes development rather than being the long-lived operating substrate for this heterogeneous cluster. K3s provides the Kubernetes API and a batteries-included minimal deployment model without requiring a full enterprise Kubernetes control-plane footprint.[^14_1]

**Deployment policy**

- Use K3s with a deliberately small number of stateful services.
- Treat the Ultra 9 and Ryzen 7 nodes as primary compute/stateful candidates.
- Use the Ryzen 3 and i5 nodes for ingress, object-storage roles, backups, telemetry, registry/cache, and low-priority workers.
- Do not distribute a stateful database across all four nodes merely because Kubernetes permits it.
- Start with simple replicated storage/backup procedures before adopting a complex distributed storage fabric.


## Clojure/JVM for platform services

Clojure is the primary language for:

- artifact ingestion pipelines;
- event schema and event handlers;
- source/parser coordination;
- retrieval orchestration and ranking;
- graph projection workers;
- user annotation/review workflow;
- research-question and experiment specifications;
- simulation kernels where pure functional state transitions are advantageous;
- API and command-line interfaces.

Clojure’s immutable-data orientation aligns with artifact provenance, event sourcing, replayable projections, and explicit transformation pipelines. Java interoperation permits use of mature JVM libraries and service clients without abandoning the preferred language.

Python, Go, Rust, or shell tools may be used at well-defined boundaries when a language-specific ecosystem is materially superior—for example, a scientific package, model-serving adapter, or parser—but they will interact through versioned artifacts, container boundaries, and explicit contracts rather than becoming the ungoverned center of the platform.

## PostgreSQL as canonical metadata and event ledger

PostgreSQL is selected for durable, transactional platform state:

- append-only domain events;
- source registry and acquisition policies;
- artifact metadata and content references;
- users, labels, annotations, and review decisions;
- task state and idempotency keys;
- schema/configuration/model versions;
- experiment manifests and approvals;
- relational read models where graph/search is unnecessary.

PostgreSQL is preferred over EventStoreDB for the initial system because it reduces the number of stateful services, works well with Clojure/JDBC tooling, supports ordinary relational querying and constraints, and still permits a disciplined append-only event-table model.

EventStoreDB remains a future option if event-stream semantics, subscriptions, and operational requirements become complex enough to justify a dedicated event database. It is not required for the initial architecture.

**Canonical event-store policy**

- Events are append-only.
- Aggregate/domain stream IDs, event type, sequence, event time, causation ID, correlation ID, actor, payload schema version, and idempotency key are persisted.
- Mutations create compensating or superseding events; they do not rewrite history.
- Projections record checkpoints and can rebuild from events plus immutable artifacts.
- Database backups are tested through restore-and-replay exercises.


## MinIO for immutable objects

An S3-compatible object store, initially MinIO, will hold immutable or large binary/content artifacts:

- raw Git blobs;
- Markdown/code/file revisions;
- rendered extraction artifacts;
- external PDFs and permitted source snapshots;
- model cards/dataset cards;
- dataset manifests, samples, and approved data materializations;
- embedding batches;
- parser/analyzer outputs;
- simulation inputs, checkpoints, results, figures, and logs;
- database/search snapshots and backup bundles.

This prevents PostgreSQL from becoming a large binary archive and keeps every derived layer anchored to a durable content-addressed source.

**Object-storage policy**

- Object keys include artifact kind, content hash, and/or immutable revision identity.
- Metadata links object content to Postgres artifact records.
- Content is deduplicated by hash where appropriate.
- Retention rules differentiate raw source artifacts, rebuildable derived data, large datasets, and disposable caches.
- Backups prioritize raw artifacts and event records before searchable/graph projections.


## Elasticsearch for hybrid retrieval and geospatial queries

Elasticsearch is selected as the initial search platform because the system requires all of the following in the same retrieval flow:

- full-text lexical ranking;
- phrase/title/heading search;
- filters over repository, path, language, time, labels, trust tier, and revision;
- dense-vector kNN retrieval;
- hybrid lexical/vector ranking;
- aggregations and faceting;
- geospatial points/shapes and spatial filtering;
- indexing at section, block, symbol, repository, artifact, and dataset granularity.

This avoids prematurely maintaining a separate vector-only database and separate geospatial system. Vector retrieval is important for paraphrase and concept-level recall, but lexical, temporal, structural, graph, and user-reviewed signals remain equally important.

**Search policy**

- Start with lexical retrieval as the baseline.
- Add embeddings at section/block, symbol/module, and research-component levels.
- Retain raw component scores: lexical, vector, structural, temporal, graph, user-label, and final rank.
- Evaluate hybrid ranking on curated personal queries before tuning by intuition.
- Version embeddings, chunking, ranking functions, and index mappings.
- Never use a vector score alone to create a permanent graph relationship.


## Neo4j for the knowledge graph

Neo4j is selected as the dedicated graph engine because graph traversal is central to the product’s distinct value:

- commit/revision/file lineage;
- document section and idea lineage;
- symbol/reference/dependency relationships;
- concept-to-code grounding;
- research artifact/citation/method/dataset relationships;
- experiment/scenario/result provenance;
- user labels and accepted/rejected candidate relations;
- bounded multi-hop evidence discovery.

A separate graph database is justified because graph traversal and relationship-first investigation are not incidental features. They are the core interface for questions such as:

- “What is the historical lineage of this idea?”
- “Which notes, symbols, tests, commits, and experiments support this concept?”
- “Which Clojure namespaces are coherent, which are bridges, and which appear misplaced?”
- “Which research claims and datasets underlie this experiment result?”

Neo4j is not authoritative for raw source or event history. It is a versioned projection and traversal engine.

**Graph policy**

- All nodes/edges retain source/revision/provenance/status.
- Distinguish observed, tool-derived, inferred, and human-accepted edges.
- Time-bound graph queries must support “as of commit/time” investigation.
- Graph traversal has explicit hop, relationship, time, and result budgets.
- Human-reviewed architecture/taxonomy decisions are preserved as events and reprojected into the graph.


## NATS JetStream for work coordination

NATS JetStream will coordinate asynchronous pipelines:

```text
artifact observed
  -> parse requested
  -> extraction completed
  -> embedding requested
  -> search projection updated
  -> graph projection updated
  -> candidate review queued
```

It is suited to workloads such as:

- repository scanning;
- parsing/extraction;
- embedding and reranking batches;
- index writes;
- graph updates;
- external-source acquisition;
- model inference;
- simulation task distribution;
- scheduled reindex/replay;
- notifications and telemetry-adjacent workflow events.

JetStream’s persisted streams, replay, consumers, acknowledgements, and duplicate-message handling support worker restart, retries, backpressure, and idempotent processing.[^14_4][^14_5][^14_3]

**Messaging policy**

- Consumers are idempotent using event/artifact IDs and processor/configuration versions.
- Dead-letter/quarantine streams retain failures with diagnostics.
- Message payloads contain references to canonical artifacts/events rather than large content bodies.
- Retention is tuned by stream purpose; JetStream is not a substitute for raw artifact storage or Postgres history.
- Consumer lag, retry rates, dead letters, and processing latency are first-class metrics.


## Tree-sitter plus language-native analyzers

Tree-sitter is the common parser substrate for polyglot syntax extraction and structural features. It provides concrete syntax trees, source-position mapping, partial/error-tolerant parsing behavior, and incremental parsing properties useful for revision-oriented source analysis.[^14_6]

Tree-sitter alone is not treated as semantic analysis. Each language has different name-resolution, macro, dynamic-dispatch, build, type, and runtime behavior.

**Clojure-first analysis**

For Clojure/ClojureScript, `clj-kondo` is the semantic-analysis adapter. Its analysis/cache output can expose namespace declarations, requires, aliases, var definitions, usages, keywords, protocols, multimethods, macros, tests, source locations, and lint findings.[^14_7][^14_8][^14_9]

The platform will explicitly mark semantic uncertainty:

- statically resolved;
- parser-derived only;
- partially resolved;
- dynamically unresolved;
- runtime-observed;
- human-confirmed.

This is especially important for macros, dynamic loading, generated code, reflection, data-driven dispatch, and non-Clojure languages with runtime metaprogramming.

## Local models and LLM usage

Local and affordable hosted inference models may be used for:

- embeddings;
- reranking;
- structured entity/relation extraction;
- taxonomy proposal;
- evidence synthesis;
- candidate duplicate/contradiction analysis;
- research-question drafting;
- experiment-plan critique;
- bounded agent workflows.

Models are not authoritative stores of knowledge. They run **after** retrieval, filtering, graph traversal, and evidence assembly—not before.

**LLM policy**

- The model receives bounded evidence packs, not unrestricted raw corpus access.
- Tool calls are typed, auditable, rate/resource-limited, and least-privilege.
- Structured outputs conform to versioned schemas.
- Every output records model, prompt/template, parameters, tool results, source evidence, and run ID.
- Claims without evidence are labeled as hypotheses or omitted.
- Expensive inference is scheduled primarily on the RTX 4070 Ti node.
- The NPU is optional acceleration only after a proven, maintainable runtime path exists.


## Observability stack

The platform will use OpenTelemetry instrumentation as the common observability API for traces, metrics, and structured logs, with Prometheus-compatible metric storage, Grafana dashboards, Loki-compatible log storage, and Tempo/Jaeger-compatible tracing.

Observability must cover the whole evidence pipeline:

```text
repository ingest
  -> artifact storage
  -> parser/analyzer
  -> embedding/model call
  -> Elasticsearch index
  -> Neo4j projection
  -> retrieval/QA
  -> review decision
  -> experiment execution
```

Every workflow receives a correlation ID. Logs include artifact/revision/job IDs. Metrics include queue depth, worker throughput, failures, index lag, GPU/CPU/RAM/disk use, retrieval latency, candidate acceptance rate, and experiment cost.

The logs component of OpenTelemetry associates records with resource information, enabling correlation between logs and originating services/processes.[^14_10]

## External research and dataset stack

External acquisition begins only in Phase 3, after the local artifact, provenance, retrieval, and review foundations exist.

**Primary initial connectors**

- arXiv API/OAI-PMH for scholarly metadata and controlled artifact acquisition;
- GitHub REST/webhook APIs for repositories, releases, issues/PRs where permitted;
- Hugging Face Hub/Datasets APIs for models, dataset cards, metadata, revisions, and bounded samples;
- official documentation sources;
- allowlisted spatial/environmental/open-data sources in Phase 4.

arXiv provides a programmatic API and an OAI-PMH interface whose metadata is updated daily; this favors metadata harvesting and controlled acquisition over web scraping.  GitHub documents distinct search rate limits and recommends authentication, serial/queued requests, conditional requests, backoff, and webhooks instead of polling.  Hugging Face provides Hub APIs, model/dataset cards, revisioned repositories, and Datasets tooling that supports inspection, streaming, and processing; initial ingestion should therefore be metadata-first and sample-first.[^14_11][^14_12][^14_13][^14_14][^14_15][^14_16][^14_17][^14_18][^14_19][^14_20]

**Source-governance policy**

- Every source has a registry entry, trust tier, access method, rate/resource budget, retention rule, and license/terms metadata.
- No broad crawling.
- Large model/dataset downloads require an explicit quota/approval event.
- Remote artifacts are pinned by immutable version identifiers when available.
- External claims, observed metadata, LLM extractions, and human interpretations remain separate graph/evidence types.
- Absence of retrieved prior art never becomes a claim of novelty.


## Simulation and spatial-temporal stack

Simulation begins in Phase 4 only after experiment manifests, artifact provenance, and research governance exist.

**Execution model**

- Native Clojure for pure graph dynamics and state-transition simulations.
- Containers for Python/Rust/Julia/Modelica or other specialist workloads.
- FMI adapters when coupling independently developed dynamic models becomes justified.

FMI is an open model-exchange/co-simulation standard. Its co-simulation interface supports coordinated execution of independently implemented simulation models/tools.[^14_21][^14_22][^14_23]

**Spatial-temporal model**

- Elasticsearch handles location/time filtering for retrieval.
- STAC-compatible metadata models catalog spatial-temporal assets, their geometry/time extent, provider, license, and content references.
- Object storage holds large rasters/asset files.
- Postgres records canonical dataset/transform manifests.
- Neo4j links data, scenarios, models, assumptions, and results.

STAC is designed to catalog and search spatial-temporal assets through catalogs, collections, items, and API query mechanisms.[^14_24][^14_25][^14_26]

**Simulation policy**

- Every run has a pinned manifest, code/model/data revision, seed, environment image, resource budget, and explicit approval.
- All results expose assumptions, uncertainty, calibration status, and limitations.
- Simulation outputs are arguments under assumptions, never automatic predictions or facts.
- Autonomous loops may tune bounded parameters or run low-risk ablations; they cannot redefine objectives, expand data access, raise budget, or publish conclusions without approval.


## Deployment topology

### Initial node placement

| Workload | Primary placement | Secondary/failover placement | Rationale |
| :-- | :-- | :-- | :-- |
| LLM inference, embeddings, reranking | Ultra 9 + RTX 4070 Ti | Ryzen 7 CPU fallback only | GPU is the scarce acceleration resource |
| Parsing, `clj-kondo`, AST jobs | Ryzen 7 and Ultra 9 | Low-resource nodes for small/background tasks | Parallelizable CPU work |
| PostgreSQL | Ryzen 7 or Ultra 9, depending on measured contention | Backups on cold node | Stable RAM/disk latency matters more than distribution |
| Elasticsearch | Ryzen 7 initially; move/replicate only after measurement | Ultra 9 if required | Search needs RAM and fast disk; avoid weak nodes |
| Neo4j | Ryzen 7 or Ultra 9 | Snapshot/restore strategy, not premature clustering | Graph workload is memory sensitive |
| MinIO/object data | i5/Ryzen 3 with replicated backup strategy | Other cold node/strong-node copy | Fits IO/cold-storage role |
| NATS JetStream | Strong node primary, low-resource node replica if measured stable | Second strong node | Messaging is lightweight but persistent state needs care |
| OpenTelemetry collector, Prometheus/Grafana/Loki | Ryzen 3/i5 where feasible | Strong node fallback | Prevent monitoring from competing with inference |
| Ingress/reverse proxy/DNS | Low-resource nodes | Both low-resource nodes | Stable non-competing edge responsibility |
| Batch acquisition/archive | Low-resource nodes | Ryzen 7 | IO-bound and low priority |
| Simulations | Ultra 9 for GPU/high-value jobs; Ryzen 7 for CPU sweeps | Low-resource nodes only for explicitly small jobs | Resource-aware scheduling |

This placement is a starting hypothesis, not a permanent topology. Metrics will drive changes. The system must not deploy heavy JVM databases, large Elasticsearch heaps, or GPU-adjacent jobs indiscriminately to 8 GB nodes.

## Consequences

### Positive consequences

- The platform can start with a credible Phase 1 without requiring every future subsystem.
- Raw artifacts plus events provide a durable audit/replay base.
- Elasticsearch supports the first practical hybrid lexical/vector/geospatial retrieval path.
- Neo4j supports relationship-first investigation without forcing relational or search indexes to emulate graph traversals.
- Clojure remains the central expression of the domain, while language-specific tooling is used where it is strongest.
- K3s provides a real cluster environment without the resource and operational cost of a full enterprise Kubernetes installation.[^14_1]
- JetStream supports durable asynchronous workflows, replay, acknowledgements, and idempotent worker design.[^14_5][^14_3]
- The architecture supports progressive sophistication: document archaeology first, then code, research, and simulation.
- Failures in a parser, model, index, or worker do not invalidate raw source evidence.


### Negative consequences

- The system operates several stateful components: PostgreSQL, MinIO, Elasticsearch, Neo4j, and JetStream.
- Kubernetes adds operational complexity even in lightweight form.
- Elasticsearch and Neo4j require careful memory limits, disk monitoring, backups, and measured tuning.
- Dual search/graph projections require schema/version discipline.
- Event sourcing increases design rigor: event schemas, migrations, idempotency, and projection replay become normal engineering work.
- LLM-based extraction and inference require evaluation datasets and review workflows; without them, the system can produce impressive but unreliable structure.
- A unified stack does not eliminate the need for language-specific analyzers or domain-specific scientific software.
- The 500 Mbps network makes large dataset replication and distributed object storage a deliberate operational concern.


## Alternatives considered

### Minikube for orchestration

**Rejected.** It is useful for local Kubernetes development but is not the chosen long-running multi-node substrate. K3s better matches the lightweight, heterogeneous, self-hosted cluster requirement.[^14_1]

### Full upstream Kubernetes or a large enterprise distribution

**Deferred/rejected for current scale.** The cluster does not justify the control-plane, storage, and operational burden. K3s preserves Kubernetes compatibility while reducing footprint and setup friction.[^14_1]

### A separate vector database as the initial vector layer

**Rejected for Phase 1.** A dedicated vector database may later be justified for scale or specialized workloads, but Elasticsearch already provides lexical search, metadata filtering, vector retrieval, hybrid rank composition, aggregations, and geospatial features. A separate vector system would multiply operational complexity before there is evidence it is needed.

### ArangoDB as a unified graph/document/search/vector store

**Deferred.** ArangoDB is attractive because a unified multi-model database can reduce component count. It is not selected initially because the platform’s search relevance and graph traversal are both primary product capabilities, and Elasticsearch plus Neo4j give each concern a mature, independently tunable engine. A future consolidation experiment may compare operational simplicity, query performance, and Clojure integration.

### MongoDB as the primary NoSQL store

**Rejected initially.** The platform’s authoritative data is better modeled as relational events/metadata plus immutable objects. Elasticsearch covers flexible searchable documents, while PostgreSQL covers transactional state. MongoDB adds a stateful system without resolving a first-phase need.

### EventStoreDB as the canonical event store

**Deferred.** EventStoreDB is purpose-built for event streams, but PostgreSQL lowers initial stateful-service count and supports both event ledger and ordinary application metadata. Revisit if projections/subscriptions require dedicated stream semantics beyond a disciplined Postgres event model.

### Apache Kafka as the event backbone

**Rejected initially.** Kafka is powerful but operationally heavier than needed for a four-node personal cluster. NATS JetStream provides durable streams, replay, consumers, acknowledgements, and idempotency support with a lighter operational profile for this workload.[^14_3][^14_5]

### Object storage as the only system of record

**Rejected.** Blob/object storage is essential for immutable source material but insufficient for transactional event sequencing, user decisions, task state, constraints, and queryable provenance metadata. PostgreSQL complements it.

### Tree-sitter as the sole code-intelligence engine

**Rejected.** Tree-sitter provides syntax structure, not complete language semantics. Clojure/ClojureScript require `clj-kondo` semantic analysis; future languages need native analyzers where available.[^14_8][^14_6][^14_7]

### Vector-only retrieval

**Rejected.** A personal corpus requires exact phrase matching, path/title/date filtering, source type, temporal reasoning, AST structure, graph relations, and user labels. Vector similarity augments retrieval but cannot replace these signals.

### LLM-first architecture

**Rejected.** LLMs are constrained consumers and producers of evidence-linked candidates. They do not own artifacts, truth, indexing, event history, user decisions, or system policy.

## Implementation sequence

### Foundation

1. Build K3s cluster and node labels/taints/resource quotas.
2. Establish MinIO/object-storage layout and tested backup target.
3. Deploy PostgreSQL with backup/restore verification.
4. Deploy NATS JetStream with durable streams and dead-letter conventions.
5. Deploy baseline observability: OpenTelemetry Collector, Prometheus, Grafana, logs/traces backend.
6. Define event envelopes, artifact IDs, provenance schema, and processor-version conventions.

### Phase 1

1. Git/Markdown discovery and immutable artifact ingestion.
2. Postgres event ledger and worker orchestration.
3. Markdown parsing, section extraction, and source-span provenance.
4. Elasticsearch lexical retrieval.
5. Embeddings and hybrid retrieval.
6. Neo4j revision/section/idea-lineage projection.
7. Review queue and temporal workbench.

### Phase 2

1. Source ingestion for Clojure/ClojureScript and configured polyglot formats.
2. Tree-sitter syntax extraction.
3. `clj-kondo` semantic projection.
4. Program relationship graph and code workbench.
5. Concept-to-code grounding and reviewed boundary hypotheses.

### Phase 3

1. Governed source registry.
2. arXiv/GitHub/Hugging Face adapters.
3. External artifact snapshots and component extraction.
4. Research graph, prior-art workflows, and taxonomy studio.
5. Reproducible experiment manifests and bounded research agents.

### Phase 4

1. Simulation manifest/kernel and resource scheduler.
2. Graph-dynamics experiments and baseline evaluation.
3. Spatial-temporal asset catalog and environmental scenarios.
4. Agent-based simulations, uncertainty/calibration workflows.
5. Bounded autonomous experiment loops.

## Validation and review

This ADR is considered successful when the following remain true in actual operation:

- A raw Git or external artifact can be traced from object storage through events, extraction, search index, graph edges, user decisions, and any answer that cites it.
- Elasticsearch, Neo4j, embeddings, and all derived projections can be rebuilt without source loss.
- A failed worker/job is visible, retriable, and attributable.
- Search results can explain their ranking signals.
- Candidate lineage, contradiction, code-boundary, and research relations retain inspectable evidence and status.
- A model-produced statement can be separated from the source evidence it used.
- A simulation result can be replayed from a pinned manifest and is presented with assumptions and uncertainty.
- The two weak nodes primarily handle stable IO/storage/observability duties rather than becoming bottlenecks for memory-heavy databases or inference.
- The platform remains operable by one person with documentation, backups, dashboards, and a manageable number of stateful services.


## Revisit triggers

Revisit this ADR when any of the following occurs:

- Elasticsearch heap/index requirements materially exceed the strong-node capacity.
- Neo4j graph size or query latency becomes unacceptable.
- JetStream retention/replay semantics prove insufficient for workflow needs, or Postgres event projections become operationally cumbersome.
- A single multi-model database demonstrably reduces operational complexity without losing required search/graph quality.
- A dedicated vector database yields measured, material benefits over Elasticsearch for actual corpus queries.
- Dataset/simulation workloads require distributed storage or a dedicated compute scheduler beyond K3s jobs.
- Multiple users, remote access, or security requirements change the threat model.
- Hosted inference becomes materially cheaper, faster, or more reliable than local GPU inference for a defined workload.
- The practical NPU ecosystem becomes robust enough to justify scheduling it as a first-class inference resource.
<span style="display:none">[^14_27][^14_28][^14_29][^14_30][^14_31][^14_32][^14_33][^14_34][^14_35][^14_36]</span>

<div align="center">⁂</div>

[^14_1]: https://docs.k3s.io/

[^14_2]: https://docs.nats.io/nats-concepts/jetstream

[^14_3]: https://beta-docs.nats.io/reference/jetstream/

[^14_4]: https://docs.nats.io/nats-concepts/jetstream/consumers

[^14_5]: https://nats-io.github.io/nats.net/documentation/jetstream/publish.html

[^14_6]: https://github.com/tree-sitter/tree-sitter

[^14_7]: https://github.com/clj-kondo/clj-kondo

[^14_8]: https://github.com/clj-kondo/clj-kondo/blob/master/analysis/README.md

[^14_9]: https://github.com/clj-kondo/clj-kondo/blob/master/doc/hooks.md

[^14_10]: https://opentelemetry.io/docs/specs/otel/logs/

[^14_11]: https://info.arxiv.org/help/api/user-manual.html

[^14_12]: https://info.arxiv.org/help/oa/index.html

[^14_13]: https://info.arxiv.org/help/bulk_data.html

[^14_14]: https://docs.github.com/en/rest/rate-limit/rate-limit

[^14_15]: https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api

[^14_16]: https://docs.github.com/en/rest/using-the-rest-api/best-practices-for-using-the-rest-api

[^14_17]: https://huggingface.co/docs/datasets/en/index

[^14_18]: https://huggingface.co/docs/hub/datasets

[^14_19]: https://huggingface.co/docs/hub/en/api

[^14_20]: https://huggingface.co/docs/hub/en/index

[^14_21]: https://openmodelica.org/doc/OpenModelicaUsersGuide/latest/fmitlm.html

[^14_22]: https://github.com/modelica/fmi-standard

[^14_23]: https://github.com/modelica/fmi-standard/blob/main/docs/4___co-simulation.adoc

[^14_24]: https://docs.ogc.org/cs/25-005/25-005.html

[^14_25]: https://cmr.earthdata.nasa.gov/stac/docs/index.html

[^14_26]: https://github.com/radiantearth/stac-api-spec

[^14_27]: https://docs.nats.io/using-nats/developer/develop_jetstream/consumers

[^14_28]: https://docs.nats.io/nats-concepts/jetstream/js_walkthrough

[^14_29]: https://github.com/nats-io/nats.js/blob/main/jetstream/README.md

[^14_30]: https://pkg.go.dev/github.com/nats-io/nats.go/jetstream

[^14_31]: https://github.com/nats-io/nats.docs/blob/master/nats-concepts/jetstream/streams.md

[^14_32]: https://oneuptime.com/blog/post/2026-01-26-nats-jetstream-persistence/view

[^14_33]: https://www.youtube.com/watch?v=s6THUHpvtY4

[^14_34]: https://docs.nats.io/using-nats/developer/develop_jetstream/model_deep_dive

[^14_35]: https://docs.nats.io/reference/faq

[^14_36]: https://download.arangodb.com/arangodb2/doc/ArangoDB_Manual_2.8.9.pdf
---
# @Academic @GitHub @Hugging Face I told you I do not want postgres

You’re right—I got that wrong. **PostgreSQL is removed from ADR 000.** Your stated constraint is NoSQL-first, MongoDB-centered, with a preference for one durable central datastore rather than a relational event ledger plus a separate document database.

## Correction

The ADR should say:

> MongoDB is the canonical operational datastore and durable event ledger. It stores source metadata, append-only domain events, user decisions, task state, projections where appropriate, and document-oriented entities. Raw immutable payloads remain in object storage; Elasticsearch and Neo4j remain purpose-built derived projections.

Your existing direction already fits this: MongoDB as durable persistence, Redis as a hot LRU cache, and LMDB as a medium-term/local document cache where it materially reduces repeated IO or Mongo reads.

## Revised core stack

| Concern | Technology | Decision |
| :-- | :-- | :-- |
| Container orchestration | K3s | Keep |
| Application/domain services | JVM Clojure | Keep |
| Canonical NoSQL store | MongoDB | **Replace PostgreSQL** |
| Event sourcing | MongoDB append-only event collections | **Replace Postgres event ledger** |
| Immutable payloads | MinIO / S3-compatible object store | Keep |
| Hot cache | Redis | Add as optional early operational cache |
| Medium-term local cache | LMDB | Add where repeated document access justifies it |
| Full-text/vector/geo retrieval | Elasticsearch | Keep as a derived retrieval projection |
| Graph traversal | Neo4j | Keep as a derived graph projection |
| Durable asynchronous work | NATS JetStream | Keep |
| Clojure semantics | clj-kondo | Keep |
| Syntax parsing | Tree-sitter | Keep |
| Telemetry | OpenTelemetry + Prometheus/Grafana/Loki/Tempo | Keep |

## MongoDB’s role

MongoDB owns the platform’s **durable operational truth**, excluding raw binary content:

- source registry and ingestion policies;
- repository and external-source metadata;
- artifact metadata and immutable content references;
- append-only events;
- user accounts, labels, review decisions, and annotations;
- task records, idempotency keys, retries, and workflow state;
- taxonomy definitions and ontology decisions;
- experiment/simulation manifests, approval decisions, and run ledgers;
- document-oriented materialized views that do not need Elasticsearch ranking or Neo4j traversal.

Each Mongo document should have a clear ownership boundary. This matches your earlier preference for “one writer owns one document” and avoids turning routine writes into multi-document transactional coordination.[^15_1]

## Event sourcing in MongoDB

MongoDB is entirely suitable for the platform’s event ledger. The key is not a special database feature; it is an append-only discipline and projection model.

```clojure
{:_id ...
 :stream/id "artifact:github:open-hax/openplanner:path:README.md"
 :stream/sequence 42
 :event/id ...
 :event/type :artifact/revision-observed
 :event/occurred-at ...
 :event/recorded-at ...
 :event/version 1
 :event/payload {...}
 :causation/id ...
 :correlation/id ...
 :actor {:type :system :id :git-ingestor}
 :idempotency/key ...
 :provenance {...}}
```

Recommended collections:

```text
events                  ; append-only canonical domain events
event-streams           ; stream heads / expected-sequence coordination
artifacts               ; logical artifact metadata
artifact-revisions      ; immutable revision metadata and blob pointers
ingestion-runs          ; operational run state and diagnostics
jobs                    ; durable task/worker state
reviews                 ; candidate decisions and annotations
taxonomies              ; user-managed classification models
experiments             ; approved experiment/simulation specifications
experiment-runs         ; immutable execution records and output references
projection-checkpoints  ; per-projection replay positions
```

The `events` collection is append-only; projectors consume ordered stream events and write derived documents or request updates to Elasticsearch/Neo4j. MongoDB transactions should be used sparingly, primarily for atomicity between an event append and a stream-head/idempotency update—not as the default business-logic model. Your preference is specifically to avoid the migration and long-term maintenance friction you associate with Postgres, and Mongo’s document model gives you more flexibility for evolving event payloads and research-oriented entities.[^15_2]

## Revised source-of-truth hierarchy

```text
Tier 0 — Immutable bytes
MinIO/S3 object store
  Git blobs, Markdown/code revisions, PDFs, raw API responses,
  datasets, models, simulation inputs/outputs

Tier 1 — Canonical operational metadata and events
MongoDB
  Artifact records, append-only events, user reviews, configuration,
  source policy, workflow state, experiment manifests

Tier 2 — Derived retrieval and relationship views
Elasticsearch
  BM25, vector search, hybrid rank components, geo, faceting

Neo4j
  Lineage, dependencies, concept/code/research/experiment graph edges

Tier 3 — Caches
Redis
  Hot LRU query/document/task-result cache

LMDB
  Local medium-term cache for downloaded/extracted source material,
  embeddings, or high-churn read paths where benchmarked as useful
```

MongoDB is the durable central place; Elasticsearch and Neo4j are rebuildable capabilities around it, not competing primary stores. That preserves the “single database to keep track of” principle without forcing MongoDB to be a high-performance graph traverser or search relevance engine.

## ADR replacement text

Replace the prior ADR section:

> **System of record | PostgreSQL | Event ledger, user decisions, task state, provenance, configuration, relational projections**

with:

> **Canonical NoSQL datastore and event ledger | MongoDB | Append-only events, artifact metadata, source policies, user decisions, workflow/task state, provenance, taxonomy, and experiment manifests**

Replace every statement that says “PostgreSQL stores canonical business/event records” with:

> **MongoDB stores canonical document-oriented business state and append-only domain events. NATS JetStream provides durable transport and replayable worker coordination, while MongoDB preserves durable event history, source metadata, user decisions, and projection checkpoints.**

Replace the former Postgres rationale with:

> **MongoDB is selected because the platform’s authoritative entities are naturally document-shaped and evolve rapidly: artifacts, revisions, event payloads, labels, provenance records, review decisions, research components, and experiment manifests. This avoids a relational migration-centered workflow and keeps the platform’s durable operational state in one NoSQL-oriented datastore. Elasticsearch and Neo4j are retained as rebuildable, specialized read/projection systems.**

## What remains deliberately separate

Choosing MongoDB as the central database does **not** mean MongoDB must do every job:

- **MinIO** holds raw and large immutable artifacts.
- **NATS JetStream** distributes and replays work messages; it is not the long-term canonical event database. JetStream provides persistence, replay, and at-least-once delivery for messaging workflows.[^15_3][^15_4]
- **Elasticsearch** owns high-quality hybrid text/vector/geo retrieval.
- **Neo4j** owns long/higher-order graph traversal and graph exploration.
- **Redis/LMDB** reduce latency and repeated IO; neither is durable authority.

That is a central-Mongo architecture with specialized projections, rather than a “Mongo plus Postgres plus five competing sources of truth” architecture.

<div align="center">⁂</div>

[^15_1]: https://www.perplexity.ai/search/3d34e377-5a13-455c-99f1-cbd96eaea449

[^15_2]: https://www.perplexity.ai/search/a864a6ec-54a8-4c37-bf56-59c9289bdcc4

[^15_3]: https://docs.nats.io/nats-concepts/jetstream

[^15_4]: https://beta-docs.nats.io/reference/jetstream/
---
# @Academic @GitHub @Hugging Face you have to rewrite it, you put that shit in there way too many times. In fact, you made too many decisions in a single record. Let's go through it step by step from the start. Starting fresh. First, list the architectural decisions we have to make

You’re right. I conflated **decision inventory**, **architecture**, and **implementation plan** into one oversized ADR. Starting fresh: this is only the list of decisions we need to make—**not decisions made**.

Two constraints are already fixed: **no PostgreSQL**, and **Clojure is the preferred core implementation language**. The system also has four heterogeneous machines, 64 GB aggregate RAM, 500 Mbps networking, and needs orchestration, vector, geo, graph, NoSQL, and event-sourcing capabilities.

## Decision model

Each ADR should answer exactly one question:

- **Context:** What problem requires a choice?
- **Decision:** What single choice did we make?
- **Consequences:** What becomes easier, harder, or excluded?
- **Alternatives:** What did we reject or defer?
- **Evidence:** Benchmarks, constraints, and operational observations.

A decision that cannot be stated in one sentence probably contains several ADRs.

## A. System boundaries

These decisions define what the system *is* before choosing technologies.

1. **What is the platform’s authoritative source of truth?**
    - Raw files, immutable artifacts, event records, user annotations, or some combination?
    - What is canonical versus rebuildable?
2. **What is an artifact?**
    - A file, Git blob, source revision, Markdown section, AST node, dataset asset, paper, model card, simulation output, or all of these as separate types?
3. **What is the durable identity model?**
    - How do repository, file, revision, section, symbol, concept, dataset, model, experiment, and user label IDs work?
    - Which IDs survive rename, move, reparse, rechunk, re-embedding, and reindexing?
4. **What does “event sourcing” mean here?**
    - Is it only an append-only audit log?
    - Is it the canonical mutation model for every domain object?
    - Which workflows require ordered streams, replay, idempotency, and compensating events?
5. **Which facts are observed versus inferred?**
    - Git commit observed.
    - Parser output derived.
    - Embedding similarity inferred.
    - LLM relationship candidate inferred.
    - Human-reviewed link accepted.
    - This needs a universal evidence/status vocabulary.
6. **What is the minimum viable Phase 1 boundary?**
    - The previously stated success condition is tracing one important idea across two years of notes and commits with inspectable evidence.
    - Which capabilities are strictly necessary to achieve that, and which are deferred?

## B. Deployment model

These decisions define how the four computers work together.

7. **Do we use container orchestration at all in Phase 1?**
    - Kubernetes/K3s, Docker Compose plus systemd, Nomad, or a custom process manager.
    - “Container orchestration” is a requirement, but the exact operational scope remains a decision.
8. **If Kubernetes, which distribution and operating model?**
    - K3s, another lightweight distribution, or a development-first setup.
    - This should be separate from the prior decision: “do we need Kubernetes now?”
9. **What are the permanent node roles?**
    - High-performance node: GPU inference, embeddings, batch processing?
    - Strong CPU node: databases, indexing, projections?
    - Two weaker nodes: ingress, storage, backup, telemetry, routing, low-priority IO?
    - The decision is whether nodes are pooled horizontally or assigned non-competing roles.
10. **What workloads may run on weak nodes?**

- Explicit memory/CPU ceilings.
- Whether stateful databases are prohibited there.
- Whether they may host replicas, only backup targets, or only stateless infrastructure.

11. **What is the storage topology?**

- Local disk ownership, replicated local disks, network shares, object store replication, backup-only nodes.
- Disk is plentiful but finite, so retention and tiering are architectural rather than cleanup details.

12. **What is the network/security boundary?**

- LAN-only versus remote access.
- TLS, service identity, secrets management, user authentication, and segmentation.
- This cannot be postponed indefinitely if agent tools, external acquisition, or personal corpus access are involved.


## C. Persistence model

These are intentionally separate: “MongoDB is central” does not resolve all of them.

13. **What is the canonical NoSQL datastore?**

- MongoDB is the leading candidate because you prefer NoSQL, avoid PostgreSQL, and generally favor one central durable database.
- This ADR decides the central operational datastore—not search, graph traversal, caching, or blobs.

14. **What belongs in MongoDB?**

- Events, artifact metadata, user labels, workflow state, source policies, experiment manifests, projection checkpoints, graph-adjacent documents?
- This is a data-ownership decision, not merely a schema choice.

15. **How is the event ledger represented in MongoDB?**

- One global append-only collection, per-aggregate streams, partitioned collections, event envelopes, stream heads, sequencing, idempotency, retention, and snapshot policy.

16. **What consistency guarantees are needed?**

- When do we require single-document atomicity?
- When are multi-document transactions justified?
- When must an event append and a state update be atomically coupled?

17. **What is the immutable artifact-store design?**

- Object store, content-addressed filesystem, Git-native object reuse, or a combination.
- What raw content must be retained forever, what can be recreated, and what must be purgeable?

18. **Do we need a cache hierarchy?**

- Redis for hot shared cache?
- LMDB for node-local medium-term cache?
- No cache until profiling proves one is needed?
- You have already considered Redis as hot LRU and LMDB as a medium-term layer over durable Mongo persistence, but that is still a decision to validate against actual access patterns.

19. **What is the retention and lifecycle policy?**

- Raw artifacts, embeddings, parser output, search indices, model weights, external datasets, simulation output, telemetry, and backups each need different retention classes.

20. **What is the backup and restore model?**

- Backup frequency, immutable/offline copies, restore testing, and acceptable data-loss window.
- This must be decided before corpus ingestion makes the system valuable.


## D. Retrieval and indexing

21. **What is the retrieval architecture?**

- One engine for lexical/vector/geo?
- Separate engines with a broker/ranker?
- The decision is about query ownership and ranking composition.

22. **What provides lexical/full-text search?**

- Elasticsearch/OpenSearch, MongoDB Search, another engine, or a staged approach.

23. **What provides vector search?**

- MongoDB vector search, Elasticsearch/OpenSearch vector search, a dedicated vector store, or a custom graph index.
- You have evaluated custom semantic graph indexing alongside MongoDB vector stores, so this needs an explicit benchmark ADR rather than an assumption.

24. **What provides geospatial indexing?**

- MongoDB, Elasticsearch/OpenSearch, a spatial extension/service, or a dual-projection model.
- This should be chosen based on actual required operations: radius, polygon containment, temporal joins, map aggregation, raster assets, etc.

25. **What is the hybrid ranker?**

- Which signals exist: BM25, vector similarity, recency, path/title, graph proximity, AST features, labels, source trust, temporal continuity?
- How are signals normalized, weighted, logged, evaluated, and versioned?

26. **What is the unit of indexing?**

- Whole file, Markdown section, paragraph, code symbol, AST subtree, commit, research claim, dataset row group, graph neighborhood?
- This determines storage growth, ranking behavior, and citation quality.

27. **What is the embedding strategy?**

- Which model(s), dimensions, chunking method, normalization, batch size, GPU scheduling, update/re-embedding policy, and evaluation benchmark?

28. **What query evaluation protocol do we use?**

- Human-labeled query set, Recall@k, nDCG, latency budget, evidence coverage, and regression tests.
- Without this, ranking changes become taste-driven.


## E. Graph model

29. **Do we need a dedicated graph database in Phase 1?**

- Or can graph relationships live in MongoDB plus indexed projections until traversal requirements prove otherwise?
- This is separate from “which graph DB?”

30. **If yes, what graph runtime/store do we use?**

- Neo4j, ArangoDB, MongoDB graph-like documents plus application traversal, Datalog-backed relations, or another option.

31. **What relationships are first-class in Phase 1?**

- Git parentage, file/revision lineage, section containment, explicit links, similarity candidates, human-accepted idea lineage.
- Do not add code dependencies, research citations, simulation results, and agent behavior until their phases.

32. **How do we model uncertain edges?**

- Confidence, evidence spans, generator version, time validity, review state, expiration/re-ranking, and user override.

33. **What is the graph query contract?**

- Traversal depth, time slicing, source-type filters, confidence thresholds, response-size limits, and explanation requirements.

34. **What is the relationship between the graph and retrieval?**

- Does graph expansion happen before search, after search, during reranking, or only in user-facing exploration?


## F. Ingestion and processing

35. **What is the ingestion execution model?**

- Direct synchronous pipeline, durable job queue, event-driven workers, scheduled batch jobs, or hybrid.

36. **What message/work-queue system is needed?**

- NATS JetStream, another broker, database-backed work queues, or initially no separate broker.
- This is not automatically decided by choosing event sourcing.

37. **What is the idempotency and replay model?**

- How is “this Git blob has been parsed with extractor version X” represented?
- What causes a reparse, re-embed, reindex, or graph rebuild?

38. **What is the parser architecture?**

- Markdown parser choice.
- Tree-sitter as common syntax layer.
- Language-native analyzers, beginning with `clj-kondo` for Clojure/ClojureScript.
- Shared normalized extraction contract.

39. **What is the language support order?**

- Clojure/ClojureScript first is the obvious candidate because of your stack, but it should be explicit.
- Which formats belong in Phase 1 versus Phase 2?

40. **How do we represent parse failure and ambiguity?**

- Partial parse, unsupported grammar, malformed source, dynamic references, extraction confidence, retry policy, review queue.

41. **What LLM tasks are permitted?**

- Taxonomy proposals, entity extraction, relationship candidates, contradiction review, QA synthesis, code summaries, experiment critique.
- Each task should become its own later ADR because reliability and evidence requirements differ.


## G. Application and interfaces

42. **What is the application/service boundary model?**

- Modular monolith first, several deployable Clojure services, or a small core plus workers.
- This should be decided before generating microservices from the component list.

43. **What is the public/internal API style?**

- HTTP/JSON, GraphQL, EDN-over-HTTP, gRPC, message contracts, or a mixture.
- Clojure preference may favor EDN internally, but browser/client interoperability needs its own decision.

44. **What is the user review workflow?**

- How do you accept/reject/annotate idea lineage, duplicates, contradictions, code links, and research claims?
- Human review is part of the data model, not a UI garnish.

45. **What is the first user interface?**

- CLI, local web app, notebook-style workbench, graph explorer, editor integration, or API-only.
- Phase 1’s success criterion requires evidence inspection, so this cannot be entirely deferred.

46. **What is the answer-generation contract?**

- Source citations required?
- Separate answer, evidence, uncertainty, and tool trace?
- What happens when evidence is insufficient?


## H. Observability and operations

47. **What is the observability baseline?**

- Metrics, logs, traces, profiling, audit logs, job state, storage health, search quality, model cost, and experiment cost.

48. **Which telemetry stack is justified initially?**

- OpenTelemetry, Prometheus, Grafana, Loki, Tempo, alternatives, or minimal node/service metrics first.
- “Observable” is a requirement; the specific toolchain is a decision.

49. **What are the service-level objectives?**

- Ingestion freshness, query latency, index lag, job retry time, data-loss tolerance, restore target, and acceptable downtime.

50. **What is failure handling?**

- Dead-letter queue policy, retries, quarantine, operator alerts, manual repair workflow, and replay safety.

51. **What is capacity management?**

- Memory limits, disk quotas, model-cache policies, index lifecycle, batch scheduling, GPU reservation, and network throttles.


## I. External research and simulation

These are real decisions, but they belong later because Phase 1 does not need them.

52. **What external sources are allowlisted?**

- arXiv, GitHub, Hugging Face, official docs, datasets, geospatial sources, and later others.

53. **What is the external-source governance policy?**

- Licensing, rate limits, terms, provenance, content retention, trust tiers, prompt-injection handling, and download quotas.

54. **What is the research claim model?**

- How do papers, authors’ claims, methods, datasets, metrics, results, limitations, and citations become evidence-linked entities?

55. **What is the experiment/simulation contract?**

- Manifest, code/data/model versions, seeds, environment, metrics, budgets, approval, result retention, and reproducibility.

56. **What kinds of automated agents are allowed?**

- Retrieval-only, extraction, taxonomy, research scout, experiment designer, simulation scheduler.
- What they may read, write, execute, or spend is a security and governance decision.


## Decision order

The first decisions should be made in this order:

1. **Define the Phase 1 system boundary**
2. **Define authoritative data, artifact identity, and evidence status**
3. **Choose the canonical datastore role and MongoDB ownership model**
4. **Choose raw artifact storage and backup/restore model**
5. **Choose deployment model and node responsibilities**
6. **Choose ingestion/event/work execution model**
7. **Choose lexical/vector/geo retrieval architecture**
8. **Define indexing units, embedding policy, and retrieval evaluation**
9. **Decide whether a dedicated graph store is needed in Phase 1**
10. **Define the first review and evidence-inspection interface**
11. **Choose observability baseline**
12. **Only then decide Phase 2–4 technology details**

The correct first ADR is therefore not “Tech Stack.” It is likely:

> **ADR 000 — Define the authoritative data model and rebuildability boundary for Phase 1**

That one decision determines what may safely become a projection, cache, index, graph edge, or LLM-derived hypothesis.

<div align="center">⁂</div>
---
# @Academic @GitHub @Hugging Face Ok, let's discuss the open decisions with that scope.

For **Phase 1 only**, we should narrow the decision set to what is necessary to trace one idea across two years of Git and Markdown with inspectable evidence. That means: no code intelligence, external research acquisition, geospatial data, simulation, or autonomous agents yet.

The open decisions below are ordered by dependency. We should settle the first four before writing an ADR or choosing Kubernetes/search/graph products.

## Phase 1 boundary

The Phase 1 success criterion is:

> Select an important present-day idea and reconstruct its evidence-backed history across Git commits and Markdown sections over roughly two years.

Required capabilities are:

- Git + Markdown ingestion
- immutable artifact preservation
- commit and revision history
- section-level Markdown extraction
- lexical plus semantic retrieval
- candidate idea-lineage links
- redundancy/contradiction review queue
- temporal evidence views.

Everything else is deferred unless it directly makes that loop work.

## Decisions to make

| Order | Open decision | Why it matters now | Output |
| :-- | :-- | :-- | :-- |
| 1 | Authoritative data and rebuildability boundary | Determines what can safely be reindexed, regenerated, or discarded | ADR 000 |
| 2 | Artifact and identity model | Determines how files, blobs, sections, and ideas survive time and path changes | ADR 001 |
| 3 | Event ledger scope in MongoDB | Determines how ingestion, review decisions, and replay are represented | ADR 002 |
| 4 | Immutable artifact storage | Determines how original Git/Markdown evidence is retained and restored | ADR 003 |
| 5 | Execution and deployment model | Determines whether Phase 1 needs K3s, a queue, workers, and node roles | ADR 004 |
| 6 | Markdown extraction contract | Determines what a section is and how it points to exact source spans | ADR 005 |
| 7 | Retrieval architecture | Determines lexical/vector search ownership and hybrid ranking | ADR 006 |
| 8 | Graph minimum | Determines whether Phase 1 needs a dedicated graph database | ADR 007 |
| 9 | Review and provenance model | Determines how lineage, redundancy, and contradiction candidates become trusted | ADR 008 |
| 10 | First interface | Determines how you inspect evidence and complete the success test | ADR 009 |
| 11 | Observability and backup baseline | Determines whether the platform is safely operable from the beginning | ADR 010 |

## 1. Authoritative data boundary

This is the first decision because it establishes what can never be lost versus what can always be regenerated.

### The choices

| Model | Canonical data | Rebuildable data | Main risk |
| :-- | :-- | :-- | :-- |
| Document-first | Mongo documents representing current notes/files | Search and graph | Historical evidence can become secondary or mutable |
| Artifact-first | Raw Git blobs and source snapshots | Everything else | Harder to represent user decisions and operational state |
| Event-first | Immutable event ledger | All state projections, including artifact metadata | Higher early design discipline |
| Hybrid artifact + event | Immutable source artifacts plus append-only domain events | Search, graph, embeddings, candidate inferences, UI views | Requires clear boundary rules |

For this project, the strongest candidate is **hybrid artifact + event**:

- Raw Git blob bytes and preserved source snapshots are the evidence base.
- MongoDB records artifact identity/metadata plus append-only events.
- Search results, embeddings, graph edges, lineage candidates, and summaries are all derived projections.
- Human review decisions are durable events, because they change the platform’s interpretation without rewriting the source evidence.

That aligns with your MongoDB-centered, event-log-plus-projection approach and keeps the data “touchable”: source and ledger records remain legible even if infrastructure changes around them.[^17_1]

### Questions to settle

- Is **Git itself** always authoritative for repository history, with our object store being an acquisition cache/archive?
- Or, once ingested, is our stored Git blob snapshot authoritative even if the working repo later disappears or rewrites history?
- Are user labels, annotations, and accepted lineage links immutable events only, or do they also get a mutable convenience document?
- Can any source artifact ever be deleted, or only redacted/unlinked through a later event?
- Is the system required to answer “what did we know at time X?” from ledger/event history alone?


### Recommended position

Use this rule:

> **Source bytes and append-only interpretation events are durable authority. Every searchable, graph, semantic, visual, or model-derived representation is a versioned projection.**

That is narrowly sufficient for Phase 1 and does not prematurely dictate a graph DB, search engine, or orchestration system.

## 2. Artifact and identity model

The system cannot trace an idea reliably if identity is only a file path. A path changes; a filename is reused; a section moves; headings are renamed; two similar notes may diverge.

We need separate identities for:

```text
Repository
  -> Git commit
    -> tree entry/path-at-commit
      -> blob/content revision
        -> parsed document revision
          -> section revision
            -> source spans / blocks
```

The important distinction is:

- **Artifact identity:** “this is the same logical document or note lineage.”
- **Revision identity:** “this exact content existed in this exact blob/commit.”
- **Extraction identity:** “this parser version produced this section/block representation.”
- **Concept/idea identity:** “these separate expressions may concern the same idea”—always uncertain at first.


### Questions to settle

- Is a logical file identity generated on first observation, then continued through path changes using candidate lineage?
- Or do we avoid a permanent logical-file identity until a user accepts a rename/move relation?
- Are Markdown headings the identity of sections, or are sections revision-scoped only?
- What counts as a source span: byte offsets, character offsets, line/column, or all three?
- Do we preserve only Git-tracked Markdown or also untracked/local vault material?
- Do we model notebook-like notes outside Git as first-class sources in Phase 1, or explicitly defer them?


### Recommended position

Do **not** make an “idea” or “section” permanently identical merely because a parser sees similar text.

Use four levels:

```clojure
{:artifact/id ...}           ; logical source object, cautiously maintained
{:revision/id ...}           ; immutable blob at a commit/path
{:extraction/id ...}         ; parser/version output
{:section/id ...}            ; section expression within one extraction
{:lineage-candidate/id ...}  ; claim that two artifacts/revisions/sections connect
```

That design lets the platform be uncertain where it should be uncertain. A probable rename, section continuation, or idea lineage is a relationship with evidence—not an identity mutation.

## 3. MongoDB event-ledger scope

MongoDB is already a fixed constraint for central persistence; the decision is not “MongoDB or Postgres.” It is **how much of Phase 1 uses append-only events, and what remains ordinary document state**.

### Three reasonable scopes

| Scope | Event-sourced objects | Ordinary documents | Tradeoff |
| :-- | :-- | :-- | :-- |
| Minimal | Ingestion/audit events only | Most metadata and review state | Fastest start, weaker historical reconstruction |
| Core domain | Artifacts, revisions, reviews, labels, ingestion, projections | Caches and derived read views | Best Phase 1 fit |
| Universal | Every state mutation | Almost nothing | Maximum replayability, maximum ceremony |

### Recommended position

Choose **core-domain event sourcing**.

Append events for:

- source registered or source configuration changed
- commit/revision/blob observed
- artifact captured
- parse/extraction completed or failed
- embedding/index/graph projection requested or completed
- lineage/redundancy/contradiction candidate created
- candidate accepted, rejected, relabelled, or annotated
- projection checkpoint advanced
- manual correction or source redaction requested.

Keep mutable convenience documents for:

- current source configuration
- latest ingestion status
- job lease/heartbeat
- materialized current artifact summary
- current candidate-review queue state
- cache metadata.

This preserves the “event log + projection” model you already use conceptually: events show what happened; projections make current work practical. MongoDB Change Streams can later bridge persisted event insertion to worker dispatch, but that is an execution-model decision, not something ADR 002 needs to settle.[^17_2][^17_1]

## 4. Immutable artifact storage

MongoDB should not carry every raw blob, Markdown version, large file, or future PDF/dataset payload inside operational documents. We need to decide where immutable source bytes live and how they are addressed.

### Choices

| Approach | Benefit | Cost |
| :-- | :-- | :-- |
| Git repositories only | Lowest initial complexity | Loses resilience to deleted/remapped repos; weak cross-source abstraction |
| MongoDB GridFS | One database operationally | Mongo becomes the large-file storage and backup burden |
| Content-addressed filesystem | Simple and local | More custom replication, access, and metadata work |
| S3-compatible object storage | Clear blob boundary, snapshots, scalable artifact class | One additional stateful service |
| Hybrid: Git + object snapshots | Git remains native, object store preserves observed bytes | Requires policy for capture timing and duplication |

### Questions to settle

- Must every Git blob be copied immediately, or can Git object databases remain the initial byte source?
- Do we snapshot only Markdown blobs in Phase 1, or the full Git object graph?
- Is the artifact store content-addressed by SHA-256, Git blob SHA, or both?
- What source data can be reconstructed from Git and therefore need not be backed up separately?
- What is the restore target: one machine, a new cluster, or a new directory plus Mongo restore?
- Do you want object storage now, or should a content-addressed local filesystem be enough until external papers/datasets arrive in Phase 3?


### Recommended position

For Phase 1:

> Preserve the exact bytes of every ingested Markdown blob, plus ingestion manifests and hashes. Keep Git commit metadata in MongoDB. Do not yet require full-repository object mirroring unless a repository is unstable, private, or likely to disappear.

That gives you inspectable evidence without immediately turning Phase 1 into a general Git-hosting/archive system.

## 5. Execution model

This is the first infrastructure decision, but it comes *after* the data model because the data model should survive any scheduler or deployment change.

### Options

| Model | Best for | Weakness |
| :-- | :-- | :-- |
| Single JVM modular monolith | Fastest Phase 1 delivery | Weak horizontal separation |
| Modular monolith + local worker processes | Clear stages without cluster overhead | Manual supervision/deployment |
| Containerized services with Compose/systemd | Practical multi-machine operation | Less standardized scheduling |
| K3s from day one | Learning Kubernetes and future cluster work | Significant early operational surface |
| Custom actor/process manager | Deep Clojure/EDN alignment | Becomes its own large project |

Your cluster has two nodes that can do substantial work and two better suited to IO, storage, routing, backups, and telemetry. That makes **non-competing roles** more sensible than pretending all four nodes are equal compute workers.

### Questions to settle

- Is the goal of Phase 1 to validate corpus archaeology or to establish the eventual cluster operating environment?
- Do you need automatic cross-node placement before you have a workload benchmark?
- Is Kubernetes a learning/infrastructure goal in its own right, or only a means to run the application?
- Can Phase 1 run on one strong node with a second worker and cold-storage/backup nodes?
- Do you want to build around your EDN-based process/actor work now, or keep the archaeology platform independent of it until core ingestion exists?[^17_3][^17_4]


### Recommended position

Start Phase 1 as a **modular Clojure monolith plus independently deployable workers**, containerized but not forced into K3s on day one.

That means:

```text
API / review application
Git + Markdown ingestion worker
Markdown extraction worker
Embedding/index worker
Projection worker
MongoDB
artifact storage
search engine, if chosen
```

Each process communicates through durable records/events and can later be scheduled under K3s. This lets you prove the data and retrieval model before paying Kubernetes’ coordination tax.

That recommendation does **not** reject K3s. It says “K3s adoption” should become a separate decision once Phase 1 has a known workload profile and a deployment shape worth orchestrating.

## 6. Markdown extraction contract

A “section” needs a concrete definition before we can index it, embed it, compare it, or use it as evidence.

### Questions to settle

- Are sections defined by heading hierarchy only?
- How do preamble text, title blocks, lists, tables, code fences, quotations, and front matter behave?
- Does a section include all nested subsections, or are parent/child sections separately indexed?
- Do we index paragraphs/blocks separately in addition to sections?
- Are tags, links, TODOs, dates, people, project names, and code identifiers extracted in Phase 1?
- What must an evidence citation contain to let you inspect the exact original text?


### Recommended position

Use a three-level extraction model:

```text
Document revision
  -> section expression (heading-delimited, hierarchy-aware)
    -> block expression (paragraph/list/table/quote/code fence)
```

Index sections as the default retrieval unit. Retain blocks for citations, contrast/duplicate review, and precise evidence. This is enough to trace ideas without prematurely modeling every sentence as a graph entity.

## 7. Retrieval architecture

The fundamental product question is:

> How does the platform retrieve the evidence set from which lineage and review decisions can be made?

You correctly identified that vector search is only one signal. The Phase 1 ranker needs at least:

- lexical matching;
- semantic similarity;
- time/date;
- repository/path;
- headings/tags;
- Git/revision relationship;
- explicit links;
- eventually, human labels and accepted relationships.


### Questions to settle

- Is Elasticsearch/OpenSearch necessary now, or is MongoDB Search adequate for the first corpus size?
- Is vector search colocated with lexical search or kept separate?
- Do we use one embedding model for all Markdown, or distinct models for notes, code blocks, and commit messages?
- Is retrieval at section level only, or section plus block?
- How does the system combine lexical and vector scores?
- What does a search result have to expose so you can trust it?


### Recommended position

Do not pick a stack by feature checklist alone. Write a benchmark corpus first:

- 30–50 questions you know your notes should answer;
- a small set of known related note/section pairs;
- a small set of known false friends;
- 10–20 cases where exact wording matters;
- 10–20 cases where paraphrase matters;
- at least a few time- and path-constrained queries.

Then compare:

1. MongoDB Search plus MongoDB vector search;
2. Elasticsearch/OpenSearch hybrid search;
3. any custom semantic-graph retrieval you want to test.

You have already explored MongoDB vector search and custom semantic graph indexing, so a measured Phase 1 evaluation is a better basis than importing Elasticsearch because it is conventional.

## 8. Graph minimum

The Phase 1 graph is smaller than “knowledge graph platform.”

It needs only:

```text
commit -> parent commit
commit -> revision observed
revision -> blob
revision -> document extraction
document -> section
section -> explicit link / tag
revision or section -> candidate lineage relation
candidate -> human decision
```


### Questions to settle

- Is MongoDB sufficient for these bounded relationships and evidence packets?
- Do you need arbitrary multi-hop traversal and interactive graph exploration on day one?
- Does the graph store need to exist before you have enough observed links to make it useful?
- Should lineage candidates be documents in MongoDB first, with graph projection later?
- What queries cannot be served acceptably by Mongo documents plus search results?


### Recommended position

**Defer a dedicated graph database until after basic ingestion, extraction, and retrieval exist.**

Phase 1 can represent revision/section/candidate relations in MongoDB documents, build focused lineage packets in application code, and measure what traversal queries become painful. This respects your preference for a central datastore and avoids declaring Neo4j a mandatory component before the corpus demonstrates the need.

A graph projection becomes justified when you need frequent, interactive, multi-hop questions such as:

- “show all evolving descendants of this idea through accepted and candidate links,”
- “find all note clusters reachable through two concept bridges but not through Git ancestry,”
- “compare topology at two historical dates.”


## 9. Review and provenance

No automatic lineage, redundancy, or contradiction result should become authoritative just because an embedding or LLM says so.

### Questions to settle

- What candidate relation types exist in Phase 1?
- What evidence threshold is required before showing a candidate?
- Which review actions are available?
- Are user decisions global, per-project, or private annotations?
- How do rejected relationships affect future ranking?
- How do you prevent the review queue from becoming an infinite pile?


### Recommended Phase 1 relation types

```clojure
:continues
:refines
:references
:possibly-derived-from
:near-duplicate
:possible-contradiction
:possibly-supersedes
:unrelated
```


### Recommended review actions

```clojure
:accept
:reject
:relabel
:defer
:ignore-similar-future-candidates
:add-note
:create-research-question
```

Each decision should append an event and preserve source spans, candidate-generation configuration, score components, and rationale. This is exactly where an event-ledger approach pays off: you can later revise an inference model without losing your historical judgment.[^17_1]

## 10. First interface

The Phase 1 UI does not need to be a polished universal workbench. It needs to support the proof of value.

Minimum screens or CLI views:

1. **Search** — query, filters, lexical/semantic evidence.
2. **Evidence reader** — exact source span, surrounding section, raw revision, commit metadata.
3. **Timeline** — chronological note/revision expressions and candidate links.
4. **Lineage packet** — selected idea, evidence chain, confidence/status, human decisions.
5. **Review inbox** — candidates with accept/reject/relabel actions.
6. **Operations panel** — ingestion errors, queue/index state, replay status.

The first user interface is an architectural decision because it determines what evidence and provenance the backend must expose.

## Proposed discussion sequence

I suggest we discuss these one at a time, in this order:

1. **Authoritative data and rebuildability boundary**
2. **Artifact and identity model**
3. **Mongo event-ledger scope**
4. **Raw artifact storage**
5. **Execution model**
6. **Extraction contract**
7. **Retrieval benchmark and retrieval architecture**
8. **Graph deferral/adoption criteria**
9. **Review/provenance contract**
10. **First interface and operations baseline**

Let’s start with the first one: **when a repository is ingested, do you want the platform to preserve its own immutable copy of the Markdown blob bytes as evidence immediately, or treat the Git repository itself as canonical until a later archival step?**

<div align="center">⁂</div>

[^17_1]: https://www.perplexity.ai/search/b4c9045c-cfa8-4d39-b0ac-c40ef4f8091a

[^17_2]: https://www.perplexity.ai/search/036ac07c-8e6f-456e-8d89-3c241660cfd5

[^17_3]: https://www.perplexity.ai/search/eed52251-a7f7-405c-a6a2-1b35308a6465

[^17_4]: https://www.perplexity.ai/search/9609f5c5-5446-49f3-9c92-e11a410efd58
---
# @Academic @GitHub @Hugging Face I'm not sure... if we store a copy, search will be faster, especially on my larger gits. But that's a lot of extra space. I think we treat the git repo as canonical during the indexing, and we store copies of the blobs in a cache appropriate to the operation being performed.

Yes—that is a sound Phase 1 position: **Git is canonical during indexing; the platform stores metadata and derived projections, while blob bytes are cached according to the operation rather than permanently duplicated by default.**

Git already provides content-addressed, immutable blob objects: identical contents can resolve to the same object ID, and `git cat-file` can retrieve object contents or metadata directly; batch modes exist specifically for efficient scripted object access.[^18_1][^18_2][^18_3]

## Proposed decision shape

### Canonical source

For Git-backed corpus material:

```text
Canonical source of bytes:
  local Git repository object database

Canonical source of Git history:
  Git commit/tree/blob graph

Platform-owned durable records:
  MongoDB metadata, ingestion events, extraction records,
  review decisions, and projection checkpoints

Derived/rebuildable material:
  extracted sections, embeddings, lexical/vector indices,
  candidate lineage links, temporal views, cache entries
```

The platform stores a **Git object reference**, not another durable copy of every Markdown blob:

```clojure
{:source/kind :git
 :repository/id ...
 :commit/oid ...
 :tree/oid ...
 :path "docs/architecture.md"
 :blob/oid "..."
 :blob/algorithm :sha1 ; or :sha256
 :blob/size-bytes ...
 :observed-at ...}
```

A source span is then reproducible as:

```clojure
{:revision/blob-oid "..."
 :span {:byte-start ...
        :byte-end ...
        :line-start ...
        :line-end ...}}
```


## Cache by operation

You do not need one generic “blob cache.” Different operations want different forms and different retention.


| Operation | Cached representation | Suggested location | Eviction rule |
| :-- | :-- | :-- | :-- |
| Git walk / metadata scan | Commit, tree, path, blob-ID metadata | MongoDB | Durable; small and needed for history |
| Parse Markdown | Raw blob bytes and parser output | Node-local disk/LMDB | LRU or TTL; keyed by blob OID + parser version |
| Section extraction | Normalized section/block records | MongoDB | Durable if cheap; regenerate if schema changes |
| Embedding | Canonical section text / tokenized payload | Node-local cache, optionally MongoDB pointer | TTL/LRU; retain only when re-embedding cost warrants it |
| Lexical/vector retrieval | Search index structures | Search engine | Rebuildable projection |
| Evidence reader | Raw blob/section text | Memory/Redis or node-local cache | Short TTL |
| Duplicate/contradiction comparison | Selected source spans and normalized text | Memory / local cache | Job-scoped or short TTL |
| Backup/archive | Git mirror or repository backup | Cold storage | Explicit backup policy, not cache eviction |

This preserves disk for what actually improves repeated work: parsing, embedding, evidence display, and active review. It also avoids storing the same Markdown corpus separately in Git, MongoDB, an object store, a lexical index, a vector index, and graph records.

## Important distinction

“Git is canonical” only works while the repository remains available and its relevant objects remain reachable.

Git’s object store is content-addressed, so a blob ID identifies the exact content; but Git garbage collection can eventually prune unreachable objects, and a force-push or deletion can make historical content unavailable from a normal clone.[^18_2][^18_3]

So this decision needs a **repository preservation policy**, not a permanent blob-copy policy.

### Recommended preservation tiers

| Source class | Policy | Reason |
| :-- | :-- | :-- |
| Active local repositories | Index directly from local `.git`; no permanent per-blob duplication | Fastest, no needless duplication |
| Important/private repositories | Maintain a local bare mirror or periodic bundle backup | Preserves the full Git graph and original blobs |
| Remote/third-party repositories | Mirror only if the source is necessary to an accepted research record | Avoid unbounded storage/crawling |
| Deleted/unavailable repository | Promote required Git objects or a bundle into archival storage | Preserve evidence only when normal source disappears |
| User-pinned evidence | Freeze the precise blob or Git bundle referenced by a research/lineage decision | Human judgment determines retention value |

A **Git bundle or bare mirror** is better than copying individual Markdown blobs for most archival cases, because it preserves commits, trees, paths, and blobs as one coherent historical object graph.

## What MongoDB stores

MongoDB should retain enough metadata to answer whether a cache item or Git read is valid:

```clojure
{:artifact-revision/id ...
 :repository/id ...
 :commit/oid ...
 :path-at-commit "docs/foo.md"
 :blob/oid ...
 :blob/size-bytes ...
 :language :markdown
 :git/object-format :sha1
 :observed-at ...
 :source-availability :available
 :cache-hints {:parse-key ...
               :extraction-key ...
               :embedding-key ...}}
```

For a parsed Markdown section:

```clojure
{:section-expression/id ...
 :artifact-revision/id ...
 :extractor {:name :markdown
             :version "..."
             :config-hash "..."}
 :heading-path ["Architecture" "Events"]
 :span {:byte-start ...
        :byte-end ...
        :line-start ...
        :line-end ...}
 :content-hash ...
 :text-cache/status :evictable}
```

The section text itself does not have to be permanently stored in MongoDB if it can be reconstructed from the blob OID and span. But a short canonical text field may still be worth retaining for a small Markdown corpus if it materially simplifies indexing and evidence display. That is a later **extraction-storage** decision, not an artifact-archiving decision.

## Indexing flow

```text
1. Walk Git commits/trees
2. Record commit, path, blob OID, and metadata in MongoDB
3. Ask local cache for (blob OID, operation/version)
4. On cache miss: read blob from Git using batched object access
5. Parse/extract sections
6. Cache the operation-specific representation
7. Write versioned extraction metadata/projection
8. Send sections to lexical/vector indexing
9. Record projection checkpoints and failures
```

Use `git cat-file --batch` or repository-library equivalents to avoid one Git process per blob; Git’s `cat-file` documentation explicitly provides batch output modes for processing objects from standard input efficiently.[^18_1]

## Consequences

**You gain:**

- No default corpus-wide duplication of Git blobs.
- Git remains the natural forensic object model for commits, trees, paths, and content.
- Faster repeated parsing/embedding/search-adjacent operations through targeted caches.
- The ability to scale storage policy by source importance.
- A clean reason to add object storage later: preservation, external artifacts, datasets, and experiment outputs—not redundant copies of every active Git blob.

**You accept:**

- The platform must track whether each Git object is still retrievable.
- A cache miss may require disk access to a repository object database.
- Repository backup/mirroring becomes part of evidence durability.
- A result referring to an unarchived blob cannot be guaranteed forever if its source repo is deleted, rewritten, or pruned.


## ADR candidate

This can become:

> **ADR 000 — Treat Git repositories as canonical source storage during Phase 1; use operation-specific, evictable blob caches and promote only selected sources/evidence to archival preservation.**

The next open question is narrower:

> **What cache tiers do you actually want in Phase 1: node-local filesystem only, LMDB, Redis, or a combination—and which operations justify each one?**
<span style="display:none">[^18_10][^18_11][^18_12][^18_13][^18_14][^18_15][^18_4][^18_5][^18_6][^18_7][^18_8][^18_9]</span>

<div align="center">⁂</div>

[^18_1]: https://git-scm.com/docs/git-cat-file

[^18_2]: https://github.blog/open-source/git/gits-database-internals-i-packed-object-store/

[^18_3]: https://git-scm.com/book/en/v2/Git-Internals-Git-Objects

[^18_4]: https://dev.to/tim_derzhavets/demystifying-gits-object-database-a-hands-on-exploration-of-blobs-trees-and-commits-ij3

[^18_5]: https://www.cs.princeton.edu/courses/archive/fall24/cos316/lectures/L05-git-naming.pdf

[^18_6]: https://www.grizzlypeaksoftware.com/library/git-internals-objects-refs-and-the-dag-n5uojqz2

[^18_7]: https://gist.github.com/lmmx/c6ed259a32957069b4a39bf9e40becae

[^18_8]: https://www.gadaalabs.com/courses/git-fundamentals/09-git-internals

[^18_9]: https://blogs.kenokivabe.com/article/how-git-internally-stores-objects-blobs-trees-commits

[^18_10]: https://www.youtube.com/watch?v=1eHwkyOmb-4

[^18_11]: https://www.kernel.org/pub/software/scm/git/docs/gittutorial-2.html

[^18_12]: https://git-git-71.mintlify.app/internals/object-model

[^18_13]: https://dev.to/__whyd_rf/a-deep-dive-into-git-internals-blobs-trees-and-commits-1doc

[^18_14]: https://shafiul.github.io/gitbook/1_the_git_object_model.html

[^18_15]: https://jvns.ca/blog/2023/09/14/in-a-git-repository--where-do-your-files-live-/
---
# @Academic @GitHub @Hugging Face LMDB is very easy to justify for cacheing, it's simple, no service dependency, I never regret using it early on. Locking into redis for cacheing early with out a plan tends to create overly complicated systems for minimal gain.

This system, though, being distributed unlike those prior situations, might have more to gain from a redis style networked fast cache. I wanted to say that operations with remote origins might prefer redis, but that'd depend on how sensitive it was to latency.

Exactly. **LMDB should be the default Phase 1 cache primitive; Redis should be an explicitly justified shared-cache capability, not infrastructure we deploy because “distributed system.”** LMDB is embedded and memory-mapped, so it gives each worker fast local key-value caching without introducing a separate network service or operational dependency.[^19_1][^19_2]

The deciding factor is not primarily whether content originated remotely. It is:

1. **Is the value reused across nodes?**
2. **Is recomputing or refetching it expensive?**
3. **Does sharing it avoid more network/remote-origin latency than the Redis round trip adds?**
4. **Can it tolerate cache staleness or invalidation delay?**
5. **Is it small enough and hot enough to justify consuming shared RAM?**

## Cache policy direction

```text
Default:
  node-local LMDB cache

Optional:
  process-memory cache for tiny, very hot, short-lived values

Deferred:
  Redis/shared network cache, only for measured cross-node reuse
```

That respects the architecture you have actually found useful: begin with an embedded, boring cache that has no service dependency, then add a shared cache only where the cluster has a demonstrated coordination or reuse problem.

## What LMDB should cache

LMDB is a good fit for cache entries that are **local to a worker’s work pattern** and can be recreated deterministically.


| Operation | LMDB cache key | Cached value | Why local is enough |
| :-- | :-- | :-- | :-- |
| Git blob read | `git/blob/{repo}/{oid}` | Raw blob bytes or compressed bytes | The parser/embedding worker that needs it usually benefits most |
| Markdown parse | `parse/{blob-oid}/{parser-version}` | AST / normalized parse output | Reused by the same worker during reindexing |
| Section extraction | `sections/{blob-oid}/{extractor-version}` | Section/block offsets and normalized text | Avoids repeated parsing |
| Embedding input | `embed-input/{section-hash}/{chunker-version}` | Canonical normalized text | Avoids repeated normalization/token preparation |
| Embedding vector | `embedding/{model}/{section-hash}` | Vector plus model metadata | Reuse during index rebuilds or candidate comparison |
| Git diff features | `diff/{parent-oid}/{child-oid}/{config}` | Rename/similarity/diff data | Mostly batch-worker-local |
| Candidate analysis | `compare/{left}/{right}/{model-version}` | Pairwise comparison features | Prevents duplicate work in one review batch |

The important rule is that every cache key includes the **content identity and the operation version**. A cache hit is valid only if its blob/content hash and parser, extractor, chunking, or model version match.

```clojure
{:cache/key [:embedding
             :model/bge-m3-v1
             :chunker/markdown-section-v2
             :section/content-sha256]
 :cache/value ...
 :cache/rebuildable? true}
```


## When Redis earns its place

Redis becomes worth deploying when the value is both **network-shareable** and **demonstrably reused across machines** before it expires.

Good candidates:

- A remote-origin fetch result needed simultaneously by several workers.
- Shared rate-limit, retry, backoff, or lease coordination for external APIs.
- Short-lived deduplication locks preventing multiple nodes from downloading or embedding the same large artifact.
- Active user/session context if the interface runs on more than one application node.
- Shared job-result/status cache for a UI polling distributed workers.
- A high-volume remote metadata cache where local LMDB misses repeatedly cause external API calls.

Redis supports memory-limit eviction policies such as LRU, LFU, TTL-based eviction, random eviction, and no-eviction; that flexibility is useful once you have a specific shared-cache workload and an explicit loss/eviction policy.[^19_3][^19_4]

## What should not go in Redis

Avoid using Redis early for:

- The canonical event ledger.
- Durable artifact bytes.
- Full Git-history cache.
- Search/index source of truth.
- A generic “put everything here just in case” document cache.
- Long-lived embedding corpus storage.
- Anything that needs complex invalidation without a measurable latency benefit.

That is how a fast cache becomes an accidental second database.

## Remote-origin rule

For remote origins, use **local LMDB first** when one node owns or performs the acquisition. Promote the result to a shared Redis cache only when one of these is true:

```text
A. Two or more nodes repeatedly request the same remote object
B. The remote source has strict rate limits or expensive latency
C. Coordinated freshness matters across nodes
D. A shared lock prevents duplicate expensive work
E. The object has a short useful lifetime and fits a defined RAM budget
```

Otherwise, routing a worker through Redis can add another network hop without reducing total latency. Redis also supports server-assisted client-side caching, which can reduce repeated remote Redis reads while coordinating invalidation, but that is a later optimization—not a Phase 1 dependency.[^19_5]

## Practical Phase 1 stance

I would record this as a preliminary architecture position, not a final full ADR:

> **Phase 1 uses per-node LMDB caches for rebuildable, operation-specific artifacts. Redis is not deployed by default. A shared Redis cache may be introduced for a measured cross-node reuse, coordination, or remote-rate-limit problem, with one documented keyspace, TTL, size budget, and invalidation/eviction policy per use case.**

That gives you a simple default and leaves a clean door for distributed caching when the cluster proves it needs it.
<span style="display:none">[^19_10][^19_11][^19_12][^19_13][^19_14][^19_15][^19_6][^19_7][^19_8][^19_9]</span>

<div align="center">⁂</div>

[^19_1]: https://lmdb.readthedocs.io/en/latest/

[^19_2]: https://rdf4j.org/documentation/programming/lmdb-store/

[^19_3]: https://redis.io/docs/latest/operate/rs/databases/memory-performance/eviction-policy/

[^19_4]: https://redis.io/docs/latest/develop/reference/eviction/

[^19_5]: https://redis.io/docs/latest/develop/reference/client-side-caching/

[^19_6]: https://en.wikipedia.org/wiki/Lightning_Memory-Mapped_Database

[^19_7]: https://docs.otc.t-systems.com/distributed-cache-service/umn/faqs/redis_usage/what_is_the_default_data_eviction_policy.html

[^19_8]: http://www.lmdb.tech/media/20150921-SDC-LMDB.pdf

[^19_9]: https://docs.aws.amazon.com/whitepapers/latest/database-caching-strategies-using-redis/evictions.html

[^19_10]: https://redis.io/blog/redis-assisted-client-side-caching-in-python/

[^19_11]: https://redis.io/blog/cache-eviction-strategies/

[^19_12]: https://deepwiki.com/LMDB/lmdb

[^19_13]: https://redis.io/blog/lfu-vs-lru-how-to-choose-the-right-cache-eviction-policy/

[^19_14]: https://pub.dev/documentation/dart_lmdb2/latest/lmdb/

[^19_15]: https://oneuptime.com/blog/post/2026-01-21-redis-reduce-latency/view
---
# @Academic @GitHub @Hugging Face What questions remain open?

## Decisions to make

| Order | Open decision | Why it matters now | Output |
| :-- | :-- | :-- | :-- |
| 1 | Authoritative data and rebuildability boundary | Determines what can safely be reindexed, regenerated, or discarded | ADR 000 |
| 2 | Artifact and identity model | Determines how files, blobs, sections, and ideas survive time and path changes | ADR 001 |
| 3 | Event ledger scope in MongoDB | Determines how ingestion, review decisions, and replay are represented | ADR 002 |
| 4 | Immutable artifact storage | Determines how original Git/Markdown evidence is retained and restored | ADR 003 |
| 5 | Execution and deployment model | Determines whether Phase 1 needs K3s, a queue, workers, and node roles | ADR 004 |
| 6 | Markdown extraction contract | Determines what a section is and how it points to exact source spans | ADR 005 |
| 7 | Retrieval architecture | Determines lexical/vector search ownership and hybrid ranking | ADR 006 |
| 8 | Graph minimum | Determines whether Phase 1 needs a dedicated graph database | ADR 007 |
| 9 | Review and provenance model | Determines how lineage, redundancy, and contradiction candidates become trusted | ADR 008 |
| 10 | First interface | Determines how you inspect evidence and complete the success test | ADR 009 |
| 11 | Observability and backup baseline | Determines whether the platform is safely operable from the beginning | ADR 010 |

We have resolved part of decisions **1 and 4**, and we have a strong direction for cache policy. The remaining work is to turn those directions into precise contracts: what is retained, what is only referenced, when a Git source is promoted to an archive, and how a cache proves its entry is valid.

## Current state

| Order | Decision | Current direction | Still open? |
| :-- | :-- | :-- | :-- |
| 1 | Authoritative data / rebuildability | Git repositories are canonical for Git-backed source bytes during indexing; derived views are rebuildable | **Partially** |
| 2 | Artifact and identity model | Not decided | **Yes** |
| 3 | MongoDB event-ledger scope | MongoDB is central; event-log-plus-projection is the likely pattern | **Partially** |
| 4 | Immutable artifact storage | Do not permanently copy every Git blob; use operation-specific caches, archive selectively | **Partially** |
| 5 | Execution/deployment | Four nodes have unequal responsibilities; no orchestration decision yet | **Yes** |
| 6 | Markdown extraction contract | Section-level extraction is required | **Yes** |
| 7 | Retrieval architecture | Hybrid lexical/vector retrieval is required; engine/ranking architecture undecided | **Yes** |
| 8 | Graph minimum | Revision/idea relations are required; dedicated graph database undecided | **Yes** |
| 9 | Review/provenance model | Inspectable evidence and review queues are required | **Yes** |
| 10 | First interface | Evidence inspection, timeline, and review must exist | **Yes** |
| 11 | Observability/backup baseline | Required, but tools/SLOs/restore model undecided | **Yes** |

The system remains MongoDB-centered and NoSQL-first; it should not reintroduce PostgreSQL or make caches into alternate systems of record.

## 1. Authority boundary

### Decided

For Git-originated material:

- Git is the canonical source of commit/tree/blob history during indexing.
- The platform records Git identities and derived metadata rather than duplicating every blob as permanent platform storage.
- Search indices, embeddings, section derivations, lineage candidates, and visualizations are rebuildable.
- Cache contents are not evidence authority.


### Still open

1. **What is the durable authority for non-Git material?**
    - Local notes outside a repository.
    - Files imported from a directory.
    - Future PDFs, datasets, API responses, and scraped/collected external artifacts.
    - This needs one common rule before Phase 3, but can be deferred from the first Git-only implementation.
2. **What makes an observed Git source “durably preserved”?**
    - Is a local clone sufficient?
    - Does an important repository require a periodic bare mirror?
    - Do you archive Git bundles for sources that inform accepted lineage/research decisions?
    - What happens when a source repository is removed, force-pushed, or garbage-collected?
3. **Which user-created facts are authoritative?**
    - Labels.
    - Accepted/rejected candidate edges.
    - Annotations.
    - Manually declared “this note supersedes that note.”
    - These likely belong in MongoDB’s durable record, but the representation remains open.
4. **What is the temporal question the system must answer?**
    - “What did this file contain at commit X?”
    - “What did the system infer at time X?”
    - “What did I believe/accept at time X?”
    - These are distinct historical views and imply different event/projection rules.

## 2. Artifact and identity model

This is the next most important unresolved decision.

### Questions

1. **What is the canonical identity of a repository?**
    - Local absolute path?
    - Git remote URL?
    - Normalized remote identity plus local clone instances?
    - How are forks, mirrors, and offline-only repositories represented?
2. **What identifies a document?**
    - `repository + path`?
    - A platform-generated logical artifact ID assigned at first observation?
    - A Git blob is content identity, but it does not tell you whether two paths are the same evolving note.
3. **How do we handle moves and renames?**
    - Keep paths as revision facts and make rename/move a candidate relation?
    - Automatically establish a logical-document lineage when Git detects it?
    - Require human acceptance before treating two path histories as one note?
4. **What identifies a Markdown section?**
    - Heading path, such as `["Architecture" "Retrieval"]`?
    - Content hash?
    - Revision-scoped ordinal/offset?
    - A persistent logical section identity inferred across revisions?
5. **Which identities are immutable versus inferred?**
    - Immutable: repository observation, commit OID, tree OID, path-at-commit, blob OID, byte span.
    - Inferred: same file across renamed paths, same section across edits, same idea across notes.
    - This distinction is essential to prevent the system from silently converting similarity into fact.
6. **Which source-coordinate format is canonical?**
    - Byte offsets are stable for exact blob lookup.
    - Line/column is better for inspection.
    - Character offsets may matter for Unicode-facing UI.
    - The likely answer is: store byte offsets as the canonical anchor and derive line/column for display.

## 3. MongoDB event ledger

MongoDB is the central durable datastore direction, while Git remains canonical for Git content. The unanswered question is how event-oriented the platform should be in Phase 1.

### Questions

1. **What is an event versus ordinary state?**
    - Is `:artifact/revision-observed` an append-only event?
    - Is `:review/decision-recorded` an append-only event?
    - Is `:ingestion-run-started` an event, mutable job state, or both?
2. **How broad is event sourcing?**
    - Only provenance and user review?
    - All core domain changes?
    - Every operational update?
3. **What is the event-envelope contract?**
    - Required IDs.
    - Event type.
    - Event schema version.
    - Occurred time versus recorded time.
    - Causation and correlation IDs.
    - Actor/process identity.
    - Idempotency key.
    - Payload and provenance shape.
4. **How are stream/ordering rules defined?**
    - Per repository?
    - Per logical artifact?
    - Per ingestion run?
    - One globally ordered collection is easy to append to but may not represent domain ordering meaningfully.
5. **How are projections checkpointed?**
    - Per projector and source stream?
    - Per event sequence?
    - Per ingestion run?
    - What happens after a projector schema or ranking model changes?
6. **Will MongoDB Change Streams dispatch work?**
    - They are a plausible seam between persisted ledger events and asynchronous processing, but that does not decide whether a separate queue/broker is needed.[^20_1]

## 4. Git retention and caching

The broad direction is settled:

> Read canonical bytes from Git during indexing. Cache representations appropriate to the operation. Do not automatically retain a permanent duplicate of every Git blob.

### Cache policy questions still open

1. **What is the Phase 1 local-cache implementation?**
    - LMDB only.
    - LMDB plus a tiny in-process cache.
    - Filesystem staging plus LMDB metadata.
    - Your preference strongly supports LMDB as the default embedded cache because it avoids a service dependency.
2. **What cache key format is mandatory?**
    - A good baseline is:

```text
source-content identity
+ operation kind
+ implementation/configuration version
```

    - Example:

```text
git-blob-oid + markdown-parser-version + extraction-config-hash
```

3. **What cache values may be stored?**
    - Raw blob bytes?
    - Parsed AST?
    - Normalized Markdown?
    - Section/block extraction?
    - Embedding vectors?
    - Pairwise comparison features?
    - The answer may differ by operation.
4. **What invalidates or expires entries?**
    - Content hash mismatch.
    - Extractor/model/configuration version mismatch.
    - LRU pressure.
    - TTL.
    - Explicit cache-generation invalidation.
    - Since source blobs are immutable, correctness invalidation is mostly version-based rather than time-based.
5. **What are the cache size budgets per node?**
    - The GPU node may benefit from embedding-input/vector cache.
    - The indexing node may benefit from parse/extraction cache.
    - Weak nodes should not be given cache responsibilities that crowd out backup, telemetry, or routing.
6. **What promotes source material to archive?**
    - User pins it as evidence.
    - A repository becomes unavailable.
    - It participates in an accepted lineage relationship.
    - It underlies a later research result.
    - The repository is private or known fragile.
7. **When does Redis become justified?**
    - Only when cross-node reuse, shared short-lived coordination, or remote-source latency/rate limits prove that local LMDB caches are insufficient.
    - It should enter with a single defined keyspace, ownership rule, TTL, memory budget, and eviction policy—not as a general cache.

## 5. Execution and deployment

### Questions

1. **Does Phase 1 use K3s immediately?**
    - Or do you start with containers plus systemd/Compose and move to K3s after the ingestion/indexing pipeline is known?
2. **What is the smallest deployable shape?**
    - One modular Clojure application plus workers.
    - Separate ingest, extraction, embedding, indexing, and review services.
    - A single process first, split only when actual load requires it.
3. **How are the four nodes assigned?**
    - Which node is primary for MongoDB?
    - Which node runs embeddings/inference?
    - Which machines are backup/object-storage/telemetry/ingress?
    - Which workloads are prohibited on 8 GB machines?
4. **How are jobs coordinated?**
    - MongoDB collection polling/change streams.
    - NATS JetStream.
    - Another job system.
    - A Clojure/EDN actor/process layer.
    - This must be decided before distributed indexing is built.
5. **What is the failure model?**
    - Can a parse job retry safely?
    - How do leases expire?
    - Where do poison jobs go?
    - How do you prevent two nodes from embedding the same section simultaneously?

## 6. Markdown extraction

### Questions

1. **What Markdown dialects are supported?**
    - CommonMark only?
    - GitHub Flavored Markdown?
    - Front matter?
    - Wiki links?
    - MDX excluded?
2. **What is a section?**
    - Heading-delimited region.
    - A heading plus all content until the next heading of equal-or-higher level.
    - Nested sections indexed separately or rolled into parent context?
3. **What is a block?**
    - Paragraph, list, quote, code fence, table, heading, front matter field?
    - Which blocks can independently serve as retrieval evidence?
4. **What text is embedded/indexed?**
    - Heading + section body.
    - Parent-heading path + block body.
    - Commit message plus changed sections.
    - Code fences separately or in parent note context.
5. **What structured facts are extracted in Phase 1?**
    - Tags, links, dates, people, project names, task markers, citations, explicit claims?
    - Keep this minimal enough that the first extraction remains deterministic and inspectable.
6. **How are malformed Markdown and parser changes handled?**
    - Partial parse output.
    - Diagnostics.
    - Versioned extractor outputs.
    - Re-extraction queue.

## 7. Retrieval architecture

### Questions

1. **Is MongoDB Search sufficient for Phase 1 lexical and vector retrieval?**
    - Or does corpus scale / query requirements justify Elasticsearch/OpenSearch immediately?
    - This should be benchmarked, not settled by convention. You have prior experience using MongoDB for vector-oriented systems and prefer it as central persistence.
2. **What is the first lexical baseline?**
    - BM25 over title, heading, section text, path, tags, and commit messages?
    - Which fields are boosted?
3. **What embedding model is used?**
    - General retrieval model.
    - Model dimension and quantization.
    - Local GPU versus remote inference.
    - Re-embedding/version strategy.
4. **What is indexed?**
    - Full note.
    - Section.
    - Block.
    - Commit message.
    - Diff.
    - Every additional unit changes index size, evaluation, and explanation behavior.
5. **What is the hybrid ranking function?**
    - Lexical + vector only at first?
    - Add path, recency, explicit links, accepted labels, and Git proximity later?
    - How are score components exposed to the user?
6. **What is the retrieval benchmark?**
    - Which questions and known relationships become ground truth?
    - Which metrics matter: Recall@k, nDCG, latency, evidence coverage, false-association rate?

## 8. Graph minimum

### Questions

1. **Can MongoDB documents represent the Phase 1 relation set initially?**
    - Commit parentage.
    - revision containment.
    - section containment.
    - explicit links.
    - candidate lineage.
    - review decisions.
2. **What query would force a dedicated graph database?**
    - Define this before adopting Neo4j or another graph engine.
    - For example: frequent interactive multi-hop traversal with time slicing and mixed relationship filters.
3. **Which relationships are observed versus inferred?**
    - Git parent edge: observed.
    - Section contains block: observed.
    - Same document after rename: candidate.
    - Same idea across years: candidate or accepted interpretation.
4. **How do relationships expire or change?**
    - A candidate may be superseded by stronger evidence.
    - A rejected relation should remain historically visible but reduce future suggestion probability.
5. **Is the graph a Phase 1 storage concern or a Phase 1 visualization/query concern?**
    - Those are different. You can produce an evidence lineage graph in application code before operating a dedicated graph database.

## 9. Review and provenance

### Questions

1. **What candidate types belong in the first queue?**
    - Idea continuation.
    - Refinement.
    - Near duplicate.
    - Supersession.
    - Contradiction.
    - Explicit reference recovery.
2. **What minimum evidence is required?**
    - At least two exact source spans?
    - Score breakdown?
    - Why this candidate was generated?
    - Context before/after each span?
3. **What review states exist?**
    - Proposed.
    - Under review.
    - Accepted.
    - Rejected.
    - Deferred.
    - Ignored.
    - Superseded.
4. **What do accepted/rejected decisions do?**
    - Create durable relations.
    - Train or reweight future ranking.
    - Filter repeated bad suggestions.
    - Change only local/project-specific views?
5. **How are contradictions defined?**
    - Direct negation?
    - Differing recommendations under same scope?
    - Claims that vary because time/context changed?
    - This needs careful scope extraction to avoid a noisy “contradiction” queue.

## 10. First interface

### Questions

1. **CLI first, local web app first, or both?**
2. **What exact workflow proves Phase 1 works?**
    - Search present-day idea.
    - Open source section.
    - Traverse candidate timeline.
    - Inspect commit/revision/diff.
    - Accept or reject relationships.
    - Export evidence packet.
3. **What is the primary navigation object?**
    - Query.
    - Note.
    - Section.
    - Timeline.
    - Candidate review queue.
    - Concept/idea.
4. **What must be visible in every result?**
    - Source path and commit.
    - Date.
    - Exact span.
    - Score explanation.
    - Status: observed/candidate/accepted.
    - Parser/model/index version.
5. **What is the first export format?**
    - Markdown evidence report.
    - EDN/JSON packet.
    - Static HTML timeline.
    - A shareable research memo.

## 11. Observability and backup

### Questions

1. **What does “observable enough for Phase 1” mean?**
    - Structured logs only?
    - Metrics plus logs?
    - Distributed traces from day one?
    - Job dashboard and replay status?
2. **What must be measured?**
    - Repositories scanned.
    - Commits/blobs parsed.
    - Cache hit rates.
    - Parse failures.
    - Embedding throughput.
    - Index lag.
    - Search latency.
    - Candidate volume and review acceptance rate.
    - Disk growth by cache/index/artifact category.
3. **What backup is needed before indexing valuable repositories?**
    - MongoDB snapshots.
    - Git mirrors/bundles for selected repositories.
    - LMDB cache excluded or included?
    - Search index rebuild versus snapshot.
    - Where does the offline/cold copy live?
4. **What restore test is required?**
    - Restore MongoDB metadata/events.
    - Reconnect repository sources or restore mirrors.
    - Rebuild extraction/search projections.
    - Reproduce one complete lineage report.
5. **What is the recovery objective?**
    - How much work can be lost?
    - How long can the system be unavailable?
    - Is the corpus/index acceptable to rebuild overnight, or must it recover within hours?

## Best next discussion

The next unresolved decision with the most downstream impact is **ADR 001: Artifact and identity model**.

The first concrete question is:

> When a Markdown file moves or is renamed, should the platform automatically treat its history as one logical document whenever Git detects a rename, or should it store only a `:same-document-candidate` relation until you accept it?

<div align="center">⁂</div>

[^20_1]: https://www.perplexity.ai/search/036ac07c-8e6f-456e-8d89-3c241660cfd5
---
# @Academic @GitHub @Hugging Face We defer 1. I have been creating local only gits where those scattered documents tend to appear. Let's talk about 2. more.

Agreed: defer the non-Git authority question. For now, **Phase 1’s source universe is Git repositories, including local-only repositories**; their commit/tree/blob history is sufficient to define observed identity.

## Artifact identity

The important move is to separate **things Git proves** from **continuities the platform infers**.

Git natively gives us immutable commits, trees, paths-in-trees, and blob objects. It does *not* store a permanent “this file was renamed” record; rename/copy status is calculated later from content similarity, with Git’s default rename threshold at 50 percent and configurable thresholds such as exact-only `-M100%`.[^21_1][^21_2]

So we should not make “a document” a primitive Git fact.

### Four identity layers

```text
1. Repository
   A particular Git object universe, observed through a local clone/mirror.

2. Revision-at-path
   “At commit C, path P resolved to blob B.”
   Fully observed and immutable.

3. Extraction expression
   “Parser/extractor version E found this section/block/span in blob B.”
   Deterministic but versioned/rebuildable.

4. Continuity claim
   “This revision/path/section continues that earlier revision/path/section.”
   Inferred, scored, reviewable, and never silently upgraded to fact.
```

That lets you inspect the historical record without forcing a false answer to “is this really the same document?” when a note is renamed, split, merged, or extensively rewritten.

## Repository identity

A local-only Git repository cannot rely on remote URL as identity. The platform should assign a stable internal ID when it first registers a repository.

```clojure
{:repository/id #uuid "..."
 :repository/origin :local-only
 :repository/first-seen-path "/home/me/projects/foo"
 :repository/git-dir-fingerprint "..."
 :repository/object-format :sha1
 :repository/first-observed-commit "..."
 :repository/registered-at ...}
```


### Open details

1. **Move detection for the repository itself:** if `/home/me/notes` moves to `/mnt/archive/notes`, how do we know it is the same repo?
2. **Clone identity:** if two local clones point at the same history, do we treat them as two observations of one repository or two repository instances?
3. **Fork identity:** if a repo begins as a clone but later diverges, do we use a shared “repository family” relation?
4. **History rewrites:** does a newly observed ref set become another observation, while old commits remain retained in platform metadata?

### Proposed initial rule

```text
repository identity = generated platform UUID
repository location = mutable observation
Git commit/blob IDs = immutable evidence identities
```

Do not make filesystem path or remote URL the primary key.

For Phase 1, we can defer repository families/fork logic and simply preserve each registered local Git repository as a separate repository entity.

## Revision-at-path identity

This is the durable, factual unit for Git-backed content:

```clojure
{:revision-at-path/id ...
 :repository/id ...
 :commit/oid "a1b2..."
 :path "docs/retrieval.md"
 :blob/oid "c3d4..."
 :mode "100644"
 :parent-commit/oids ["..."]
 :observed-at ...}
```

It means only:

> At this commit, this path pointed to this blob.

It does **not** mean:

- this is the same logical file as the path in the previous commit;
- this blob has the same semantic content as another blob;
- the note is a continuation of any earlier note;
- a parser’s current section model is the only correct interpretation.


### Why this is enough initially

It allows you to reconstruct:

```text
Current note -> exact blob -> commit -> parent commits
```

even if name/path history becomes ambiguous. You can always examine the exact source bytes, commit metadata, and diff.

## Document identity

This is the key question: **do we create a durable logical-document ID early, or only create continuity claims between revision-at-path facts?**

There are two viable models.


| Model | Meaning of “document” | Strength | Cost |
| :-- | :-- | :-- | :-- |
| Path-lineage document | Platform maintains a stable logical document ID across revisions | Easy UI/history | Risk of treating heuristic rename detection as truth |
| Revision-first graph | No permanent document identity; relation edges connect revision-at-path nodes | Faithful to evidence and splits/merges | More graph-like UI/query logic |
| Hybrid | A logical document is a user-approved projection over continuity claims | Honest by default; simple once reviewed | Needs review/projection semantics |

### Recommended model: hybrid

Use **revision-first facts** and create a `:document/lineage` only as a projection over accepted continuity edges.

```clojure
{:continuity-claim/id ...
 :relation/type :path-rename-candidate
 :from/revision-at-path-id ...
 :to/revision-at-path-id ...
 :evidence {:git-similarity 0.93
            :detector :git-diff
            :threshold 0.90}
 :status :proposed}
```

After you accept it:

```clojure
{:continuity-decision/id ...
 :claim/id ...
 :decision :accepted
 :decided-at ...
 :basis {:user/id ...}}
```

The “logical document” view is then generated from accepted edges:

```text
logical document = connected component of accepted
                   :continues-document edges
```

No mutable `document-id` has to be written into historical Git facts. You gain a useful document timeline without making Git’s rename heuristic into ontology.

## Rename detection policy

Git’s rename detection is an input signal, not the truth. It is computed from changes between snapshots and depends on the selected similarity threshold; Git documents 50 percent as the default and supports exact-only matching with `-M100%`.[^21_2][^21_1]

### Good initial policy

| Case | System action |
| :-- | :-- |
| Same path in parent and child commit | Emit an observed `:path-continuation` fact |
| Same blob at different path | Emit a high-confidence rename/move candidate |
| Git reports rename at >= 90% similarity | Emit `:path-rename-candidate`; auto-link only for navigation, not semantics |
| Git reports rename at 50–89% | Emit candidate only if relevant to active indexing/review scope |
| Delete + add with low similarity | No document-continuity assertion |
| Split one file into several | Emit one-to-many candidate relations only when evidence supports them |
| Merge several files into one | Emit many-to-one candidate relations only when evidence supports them |
| Copy while original remains | Emit `:copy-candidate`, never a primary document continuation automatically |

The threshold should be a versioned configuration, e.g.:

```clojure
{:rename-detector :git-diff
 :rename-threshold 0.90
 :copy-threshold 0.95
 :config-version "rename-v1"}
```

That makes later recalculation possible without rewriting history.

## Section identity

A section is more unstable than a file: headings change, content moves under different headings, two headings merge, or a paragraph moves into another note.

For Phase 1, make each extracted section **revision-scoped**:

```clojure
{:section-expression/id ...
 :revision-at-path/id ...
 :extraction/version "markdown-v1"
 :heading-path ["Corpus archaeology" "Identity"]
 :ordinal 7
 :span {:byte-start ...
        :byte-end ...
        :line-start ...
        :line-end ...}
 :content-hash "..."}
```

This is an observed/extracted fact: parser version X found a heading-delimited section at an exact source span in blob B.

### What we should not do yet

Do **not** assign a permanent `section-id` across revisions at ingestion time.

Instead, build candidate edges later:

```clojure
{:continuity-claim/id ...
 :relation/type :section-continuation-candidate
 :from/section-expression-id ...
 :to/section-expression-id ...
 :evidence {:same-document-lineage? true
            :heading-similarity ...
            :content-overlap ...
            :embedding-similarity ...
            :git-diff-hunks [...]}
 :status :proposed}
```

This is necessary because “same heading” is not always “same thought,” and “different heading” does not always mean a new thought.

## Idea identity

An **idea** is even more clearly a hypothesis rather than an observed object.

For Phase 1, avoid a global canonical “idea” object at ingestion. Store:

- section expressions;
- observed Git/path links;
- explicit Markdown links/tags;
- generated candidate relationships;
- human accepted/rejected decisions.

When a review process establishes a meaningful cluster, create an explicit user-owned concept record:

```clojure
{:concept/id ...
 :concept/name "Evidence-backed corpus archaeology"
 :created-from {:section-expression-ids [...]}
 :status :user-curated}
```

The concept is not “discovered truth.” It is a durable interpretive lens connected to evidence.

## Identity rules

This is the core proposed contract:


| Object | Identity basis | Status |
| :-- | :-- | :-- |
| Repository | Platform UUID | Durable administrative identity |
| Repository location | Filesystem observation | Mutable |
| Commit | Git OID | Observed immutable fact |
| Tree | Git OID | Observed immutable fact |
| Blob | Git OID | Observed immutable fact |
| Revision-at-path | Repository + commit OID + path | Observed immutable fact |
| Extracted section | Revision-at-path + extractor/config version + span | Derived, reproducible |
| File/document continuity | Relation among revisions | Inferred/reviewable |
| Section continuity | Relation among section expressions | Inferred/reviewable |
| Idea/concept | User-created or review-approved cluster | Interpretive, versioned |
| Search result/ranking | Query + index/model/version | Rebuildable projection |

This gives you a model that can accommodate:

- a note changing names,
- a note moving between repos,
- a note being split or merged,
- copied templates,
- rewritten concepts,
- later LLM proposals,
- and humans disagreeing with automatic grouping.


## Open questions within ADR 001

We have narrowed ADR 001 to six decisions:

1. **Repository relocation:** What observation data should identify a moved local-only repository as the same registered repository?
2. **Clone/fork handling:** In Phase 1, do we treat each local clone as independently registered, even if commit histories overlap?
3. **Same-path behavior:** Is same path across parent/child commits an observed path continuation, even if content is totally rewritten?
4. **Exact blob move:** Should an identical blob at a different path automatically become an accepted document-continuity edge, or only a high-confidence candidate?
5. **Git rename threshold:** What threshold is eligible for automatically generated navigation edges: 100%, 95%, 90%, or none?
6. **Logical document view:** Is it built only from accepted continuity edges, or may it include clearly marked provisional candidate edges?

The decision that changes the most downstream behavior is \#3:

> If `docs/foo.md` exists at the same path in a child commit but has been completely rewritten, do we treat that as the same document’s revision history, or as a new document expression at a reused path?
<span style="display:none">[^21_10][^21_11][^21_12][^21_13][^21_14][^21_15][^21_3][^21_4][^21_5][^21_6][^21_7][^21_8][^21_9]</span>

<div align="center">⁂</div>

[^21_1]: https://git-scm.com/docs/git-diff/2.12.5

[^21_2]: https://git-scm.com/docs/git-log

[^21_3]: https://libgit2.org/docs/reference/v0.24.1/diff/git_diff_find_options.html

[^21_4]: https://stackoverflow.com/questions/34401477/git-thinking-added-removed-files-are-renamed

[^21_5]: https://stackoverflow.com/questions/65914714/show-moved-renamed-files-of-git-repo

[^21_6]: https://stackoverflow.com/questions/7759193/git-diff-renamed-file

[^21_7]: https://github.com/jesseduffield/lazygit/issues/2904

[^21_8]: https://stackoverflow.com/questions/43906993/how-does-git-log-follow-filename-work/43960010

[^21_9]: https://stackoverflow.com/questions/7938582/how-does-git-detect-similar-files-for-its-rename-detection

[^21_10]: https://stackoverflow.com/questions/64454637/git-log-follow-not-working-to-show-history-beyond-renames

[^21_11]: https://stackoverflow.com/questions/12251186/for-git-log-the-difference-between-options-of-follow-and-find-renames

[^21_12]: https://stackoverflow.com/questions/67738831/disable-rename-detection-during-the-git-commit-to-preserve-history

[^21_13]: https://github.com/git/git/commit/5404c116aa921d7e2f9408e103c80b7801735d08

[^21_14]: https://stackoverflow.com/questions/5743739/how-to-really-show-logs-of-renamed-files-with-git

[^21_15]: https://stackoverflow.com/questions/38870925/is-there-a-trick-to-git-log-follow-a-directory-which-has-been-renamed/38871146
---
# @Academic @GitHub @Hugging Face 1. I don't know the answer to this one. The best I can come up with, is our system adds a meta data edn file to the .git/ so when the folder is moved, we can check for the presence of .git/meta.edn.

2. I don't think we treat them separate. I think we index the shared history, and the divergent histories, really they are still just 1 tree who's representation has been split.
3. I'm not sure. I think a file name that persists over the course of a repos life, but was getting rewritten constantly, still deserves that persistent identity, because... usually that type of thing is happening to a file that is being used for the same purpose over time, and it's the location that is more important to it's identity than the contents? That might not always be true...
4. I think it's a high confidence signal, using the same template over and over would have this effect, that doesn't communicate continuity of identity or purpose automatically.
5. should probably be 100% to be automatic with out some other correlating data.
6.It can include clearly marked provisional candidates

That gives us a coherent identity policy. The unifying idea is: **location and history establish a default continuity; content similarity and human review qualify or challenge it.** That matches how long-lived working notes tend to behave better than a purely content-addressed document model.

## ADR 001 decisions

| Question | Decision | Status |
| :-- | :-- | :-- |
| Repository relocation | Store platform repository identity in Git-local metadata | Decided, with placement detail open |
| Clone/fork handling | Treat overlapping histories as one repository tree/family, not unrelated repos | Decided conceptually |
| Same path across commits | Same path carries default persistent document identity despite rewrites | Decided |
| Same blob at new path | High-confidence continuity signal, not automatic identity | Decided |
| Automatic rename rule | Exact 100% content move can be automatic only with corroborating evidence | Decided direction |
| Logical-document view | Show accepted continuity plus clearly marked provisional candidates | Decided |

## Repository relocation

Your `meta.edn` idea is good because a filesystem path is an observation, not identity. When a local-only repository moves, the platform can rediscover its stable repository ID from metadata stored alongside the Git object database.

I would use a **namespaced subdirectory**, not a lone `.git/meta.edn`:

```text
.git/
  corpus-archaeology/
    repository.edn
```

For linked worktrees, `.git` may be a file that points elsewhere, and Git distinguishes the worktree’s Git directory from the common Git directory. Put the canonical repository metadata in the resolved **common Git directory**, not blindly beside the worktree checkout. Git documents that worktree-specific configuration and common configuration can live in separate Git directories.[^22_1][^22_2]

```clojure
{:schema/version 1
 :repository/id #uuid "..."
 :repository/created-at #inst "..."
 :repository/object-format :sha1
 :repository/first-observed-head "..."
 :platform {:name :corpus-archaeology
            :version "..."}}
```


### Remaining relocation details

- **Opt-in mutation:** Should the indexer create this metadata automatically on registering any local repo, or only after you explicitly mark a repo as platform-managed?
- **Read-only repositories:** If it cannot write into `.git`, it needs a fallback external registry mapping repository fingerprints to platform IDs.
- **Git metadata preservation:** The metadata is administrative identity, not part of Git’s commit graph; moving/copying a repository preserves it, but cloning from a remote ordinarily does not.

I recommend: **write it automatically for local repositories you register as managed sources; otherwise use a MongoDB-side external registration record.** Do not write arbitrary metadata into every third-party repository just because it was indexed.

## One history, split representations

Your answer to clone/fork handling is stronger than “same repository ID”:

> A clone, mirror, or fork is a representation of one or more shared commit histories; divergence creates new branches in a shared history space rather than an unrelated tree.

So we need three concepts:

```text
Repository family
  The broader historical object universe.

Repository instance
  A concrete local Git directory / clone / worktree observed by the system.

History segment
  A connected commit graph available through one or more instances.
```

```clojure
{:repository-family/id #uuid "..."
 :repository-instance/id #uuid "..."
 :repository-instance/family-id #uuid "..."
 :repository-instance/git-common-dir "/..."
 :repository-instance/repo-meta-id #uuid "..."}
```

A local clone that shares ancestry with an existing source should normally join the same **repository family**, while retaining its own instance record for local refs, availability, remotes, and observed path. This avoids duplicate indexing of shared commits/blobs while preserving divergent branches.

### Open rule

The remaining question is **how conservative family joining should be**:

- **Exact shared commit rule:** Join families only when a newly registered repository shares at least one commit OID with an existing family.
- **Remote-identity assist:** Also use normalized remote identity as a candidate hint, never as decisive proof.
- **Manual merge:** Let the user declare that two unrelated Git histories belong to the same conceptual project family.

I recommend exact shared commit overlap as the automatic rule. Remote URL can support a suggested merge; it should not prove identity.

## Same path means continuity

Your reasoning is correct for a personal corpus. A file path that persists for years often names a durable *role*: a roadmap, index, design document, research log, project glossary, or planning surface. It can be completely rewritten while remaining “the same place where this kind of thought lives.”

So the default rule should be:

> Within a repository family, the same normalized path across parent-child commits establishes an **observed path continuity** and therefore carries a stable logical-document identity by default.

```clojure
{:document/id #uuid "..."
 :document/family-id #uuid "..."
 :document/anchor {:kind :repository-path
                   :repository-family/id #uuid "..."
                   :initial-path "docs/architecture.md"}
 :document/identity-policy :path-persistent}
```

Each exact observation remains independently factual:

```clojure
{:revision-at-path/id ...
 :document/id ...
 :commit/oid "..."
 :path "docs/architecture.md"
 :blob/oid "..."
 :relation-to-parent :same-path}
```

This does **not** claim semantic sameness. It claims document-role continuity through a stable location.

### Escape hatch

A same path can eventually be reused for an entirely different purpose. Rather than denying same-path continuity, model the exceptional case as a **document epoch boundary**:

```clojure
{:document-epoch/id ...
 :document/id ...
 :starts-at {:commit/oid "..."}
 :reason :path-repurposed
 :status :proposed}
```

Then the UI can say:

```text
docs/research.md
  Epoch 1 — 2023–2024: personal research notes
  Epoch 2 — 2025–present: generated experiment registry
```

An epoch boundary is a reviewable interpretation that overrides the normal default without destroying the ordinary useful case.

## Exact blob at a new path

Agreed: an exact blob match is strong evidence that the same bytes moved, but templates, copies, and boilerplate mean it does not by itself prove that the *document role* continued.

Git supports exact-only rename matching with `-M100%`; normal rename detection is similarity-based and defaults to 50 percent, so an exact content move is a notably stronger but still incomplete signal.[^22_3][^22_4]

The relation should be:

```clojure
{:continuity-claim/id ...
 :relation/type :exact-content-relocation
 :from/revision-at-path-id ...
 :to/revision-at-path-id ...
 :evidence {:blob/oid "same-oid"
            :similarity 1.0
            :detector :git-diff
            :threshold 1.0}
 :status :proposed
 :confidence :high}
```

It should never silently become `:same-document` just because the blob OID matches.

## Automatic rename rule

You set the right baseline: **100% similarity is eligible for automatic handling, but only with another correlating signal.**

The rule must distinguish a **navigation shortcut** from a **document identity mutation**.

### Correlating evidence options

| Corroboration | Why it helps |
| :-- | :-- |
| Source path disappears in parent-to-child diff while target path appears | Distinguishes move from a copy retained at both locations |
| Same Git commit contains delete + add | Anchors the observation in one transition |
| Same filename or basename | Helpful but weak |
| Same parent directory lineage | Helpful for reorganizations |
| Commit message mentions rename/move | Useful, but not authoritative |
| No previous target-path history | Reduces accidental template collision |
| Temporal adjacency | Ensures the relation describes a single historical transition |
| Same explicit metadata/front matter identifier | Very strong where available |
| Human acceptance | Converts a candidate into interpretive authority |

### Proposed automatic policy

```text
Automatic observed relation:
  exact blob match
  + source path deleted in the same commit transition
  + destination path added in that transition
  + source blob no longer exists at its former path in child

Result:
  :exact-relocation-observed

Not automatic:
  merge into same logical document identity
  semantic continuation
  section/idea continuity
```

This gives the interface permission to show a solid “moved from” path event while preserving the distinction between file relocation and a durable document-role judgment.

If the source remains at its old path, emit `:exact-copy-observed`, not relocation.

## Provisional logical-document view

Yes: the document view should include provisional candidates, but the display must make their status impossible to miss.

```text
Document: docs/retrieval.md
Identity basis: persistent path

Observed history
  ✓ Same-path continuation: 2024-02-10 → 2024-07-08
  ✓ Exact relocation: docs/search.md → docs/retrieval.md

Provisional continuity
  ? 93% similarity: docs/indexing.md → docs/retrieval.md
  ? Shared heading lineage: “Hybrid retrieval”

Rejected connections
  × Template copy: templates/retrieval.md
```

The default timeline should show:

- **Observed** edges as solid.
- **Accepted interpretation** edges as solid but visually distinct.
- **Provisional** candidates as dashed.
- **Rejected** candidates only on request or in audit mode.

This satisfies the evidence-first requirement: a person can see not only what the system thinks, but what kind of claim it is making.

## Revised identity contract

```text
Repository family:
  Shared commit-history universe.

Repository instance:
  A specific local Git directory, mirror, clone, or worktree.

Document:
  A persistent repository-family + path role by default.

Document epoch:
  A reviewable boundary when a stable path is repurposed.

Revision-at-path:
  Immutable fact: commit C maps path P to blob B.

Section expression:
  Versioned extraction from one revision-at-path.

Exact relocation:
  Observed file transition based on exact blob identity plus
  same-commit deletion/addition evidence.

Continuity candidate:
  A scored, reviewable claim joining documents, epochs, sections,
  or concepts.

Concept:
  A user-curated or accepted interpretive cluster over source expressions.
```


## Remaining questions for ADR 001

We have reduced the open part of identity design to four practical decisions:

1. **Repository metadata write policy:** Auto-create `.git/corpus-archaeology/repository.edn` for every registered local repository, or require explicit “managed source” registration?
2. **Repository-family rule:** Automatically join two local repositories only on shared commit OIDs, or also automatically on matching remote identity?
3. **Path normalization:** How do we normalize case, Unicode, separator style, symlinks, and case-only renames before declaring two paths “the same path”?
4. **Path-repurpose detection:** What evidence should propose a document-epoch boundary—complete content discontinuity, changed front matter/type, changed title/heading, a long time gap, or an explicit user action?

The next one with the greatest practical impact is **repository metadata write policy**: do you want registration to mutate the `.git` directory automatically, or should a repo only acquire platform metadata after you deliberately designate it as a managed corpus source?
<span style="display:none">[^22_10][^22_11][^22_12][^22_13][^22_14][^22_15][^22_16][^22_17][^22_5][^22_6][^22_7][^22_8][^22_9]</span>

<div align="center">⁂</div>

[^22_1]: https://manpages.ubuntu.com/manpages/focal/man1/git-worktree.1.html

[^22_2]: https://fossies.org/windows/misc/git-2.55.0.windows.2.zip/git-2.55.0.windows.2/Documentation/config/extensions.adoc

[^22_3]: https://git-scm.com/docs/git-log

[^22_4]: https://git-scm.com/docs/git-diff/2.12.5

[^22_5]: https://git-scm.com/docs/git-config

[^22_6]: https://github.com/git/git/blob/master/Documentation/technical/repository-version.txt

[^22_7]: https://public-inbox.org/git/0260ff6cac0c76c6d66187d77defef1edd4c6fb5.1644269583.git.gitgitgadget@gmail.com/

[^22_8]: https://github.com/git/git/blob/de658515ae1166577441da09fe7624769e263a3e/Documentation/config.txt

[^22_9]: https://git-scm.com/docs/git

[^22_10]: https://github.com/libgit2/libgit2/issues/6044

[^22_11]: https://code.googlesource.com/git/+/refs/tags/v2.7.0-rc1/Documentation/config.txt

[^22_12]: https://github.com/git/git/blob/master/worktree.c

[^22_13]: https://www.reddit.com/r/git/comments/uhic5h/trouble_with_extensionsworktreeconfig_and_corebare/

[^22_14]: https://github.com/libgit2/libgit2/pull/6202

[^22_15]: https://github.com/techgaun/git-internals/blob/master/README.md

[^22_16]: https://stackoverflow.com/questions/72760015/git-fatal-unknown-repository-extensions-found-worktreeconfig

[^22_17]: https://www.atlassian.com/git/tutorials/refs-and-the-reflog
---
# @Academic @GitHub @Hugging Face 1. auto-create 2. shared commit OIDs 3. I don't think we are normalizing the paths? This seems like over thinking. I mean, I use the .ημ/ folder, tools register that in unicode numbers instead of literally as the characters "eta" and "mu", but that is them normalizing or choosing to not handle that. Whatever string we get back from the file system, is the name of that file/directory

4. I think all of those things get weighted depending on their scale, and in a situation where you need a hard boundary, there is a threshold, otherwise, the boundary is more of a gradient. Especially in code files. They can become discontinuous with themselves very gradually, a hard boundary would miss this.

That resolves the remaining core choices for ADR 001. The resulting model is deliberately evidence-first: preserve the exact names and history Git/filesystem present; infer continuity and discontinuity as graded, inspectable relationships rather than forcing premature identity breaks.

## ADR 001 outcomes

| Question | Decision |
| :-- | :-- |
| Repository metadata | Automatically create Git-local platform metadata when a repository is registered |
| Repository family | Join repository instances automatically only when they share one or more commit OIDs |
| Path treatment | Preserve and compare the exact path string returned by the filesystem/Git; do not Unicode-normalize or rewrite names |
| Path repurposing | Model discontinuity as a weighted gradient; propose hard document-epoch boundaries only above a configurable threshold |
| Same path, new content | Default to persistent path/document-role continuity |
| Exact blob at new path | Strong relocation/copy signal, but not automatic document-purpose identity |
| Provisional view | Include candidates, clearly labeled separately from observed and accepted edges |

## Git-local registration

The indexer automatically creates metadata when registering a local repository:

```text
<resolved-common-git-dir>/
  corpus-archaeology/
    repository.edn
```

```clojure
{:schema/version 1
 :repository/id #uuid "..."
 :repository/family-id #uuid "..."
 :created-at #inst "..."
 :object-format :sha1
 :first-observed {:instance-path "/absolute/path/to/repo"
                  :head "commit-oid"}
 :managed-by {:system :corpus-archaeology}}
```

The metadata gives a moved local-only repository a durable platform identity, while its filesystem location remains an observation that can change. For worktrees, the system should resolve Git’s common directory before writing, since a checkout’s `.git` can be a pointer file rather than the repository’s actual shared Git directory.[^23_1][^23_2]

Two caveats belong in the implementation contract:

- If the repository is read-only or metadata creation fails, registration still succeeds with an external MongoDB registration record and a visible `:identity/persistence :external-fallback` status.
- The system should never interpret a copied `repository.edn` alone as proof that two object stores are the same; it is a continuity hint checked against Git object evidence.


## Shared history rule

A newly registered repository instance joins an existing repository family **only** if it shares at least one commit OID with that family.

```text
same commit OID
  -> observed overlap in commit history
  -> same repository family automatically

same remote URL, same folder name, similar content
  -> possible hint only
  -> never automatic family identity
```

This fits local-only Git well. It avoids treating unrelated repositories as one because a remote name, directory name, or template happens to match; it also avoids duplicate indexing for clones, mirrors, and histories that later diverge.

The family is not necessarily one current branch or one checkout. It is the connected commit-history space. Each checkout remains a separate **repository instance**, because it has its own local path, refs, availability, and potentially unique unpushed commits.

## Exact paths, no normalization

Agreed: the system should not rewrite `.ημ/` to an ASCII approximation, apply Unicode normalization, case-fold names, resolve symlinks into another spelling, or decide that two filesystem strings “must really mean” the same path.

The primary path record is simply what Git/tree traversal or the filesystem yielded:

```clojure
{:path/raw ".ημ/architecture/identity.md"
 :path/encoding :utf-8
 :path/comparison :exact}
```

Path equality for identity purposes is therefore exact equality of the recorded string. That means:

```text
.ημ/spec.md       != .ημ/Spec.md
.ημ/spec.md       != .ημ/specs.md
.ημ/spec.md       != .ημ/spec.md with differently encoded code points
```

That is the right default for corpus archaeology: preserving the observed spelling is more important than “helpfully” collapsing potentially meaningful differences.

There is a small separation to retain:

- **Identity layer:** exact path strings only.
- **Filesystem-access layer:** the OS/Git implementation may have practical rules for opening files, case behavior, Unicode conversion, or symlink traversal.
- **Search layer:** later, optional aliases may support recall—for example, an ASCII query finding `.ημ/`—but an alias is a retrieval aid, never a replacement identity.


## Continuity as a gradient

This is an important improvement over a binary “same document/new document” model.

A persistent path establishes a default **document-role continuity**, but the platform calculates a separate **continuity field** across revisions. Over time, a code or note file can drift gradually until it has little semantic relationship to its earliest revision; forcing only one hard repurposing cutoff would hide that history.

```clojure
{:revision-transition/id ...
 :document/id ...
 :from/revision-at-path-id ...
 :to/revision-at-path-id ...

 :continuity/signals
 {:same-path 1.0
  :git-content-similarity 0.61
  :heading-structure-similarity 0.43
  :front-matter-schema-similarity 0.85
  :symbol-overlap 0.72
  :import/dependency-overlap 0.66
  :time-gap-days 14}

 :continuity/score 0.68
 :continuity/class :moderate
 :epoch-boundary/status :none}
```

The score does not become identity. It is a transparent model output explaining how much continuity a given transition appears to retain.

### Two uses of the gradient

| Use | Behavior |
| :-- | :-- |
| Navigation/history | Keep persistent-path history intact, but show continuity strength along transitions |
| Epoch proposal | Emit a candidate boundary when discontinuity crosses a configured threshold or pattern |
| Retrieval/ranking | Prefer nearby high-continuity ancestors for “how did this evolve?” questions |
| Timeline visualization | Fade, widen, or color transitions according to continuity class |
| Review | Let you declare a hard epoch, reject one, or leave an ambiguous gradual transition unresolved |

A hard epoch is thus **not** “the system believes the old document ceased to exist.” It is a reviewable interpretation that says a particular interval is best understood as a meaningful role/purpose boundary.

## Boundary candidates

Instead of one simplistic trigger, the platform computes a weighted boundary score from signals appropriate to artifact type.

```clojure
{:epoch-boundary-candidate/id ...
 :document/id ...
 :between {:from/revision-at-path-id ...
           :to/revision-at-path-id ...}

 :artifact/kind :markdown

 :signals
 {:content-discontinuity 0.87
  :heading-structure-change 0.72
  :front-matter-type-change 1.0
  :title-change 0.65
  :time-gap-days 190}

 :boundary/score 0.81
 :boundary/threshold 0.90
 :status :proposed-low-priority}
```

For code, the signal set will eventually differ:

```clojure
{:artifact/kind :clojure

 :signals
 {:ast-structure-change ...
  :public-symbol-overlap ...
  :namespace-change ...
  :dependency-overlap ...
  :test-overlap ...
  :comment/docstring-overlap ...
  :path-persistence 1.0}}
```

That is properly deferred from Markdown Phase 1, but the identity model already has room for it. A code file can gradually become discontinuous; the model must represent a long decline in continuity rather than pretend every transition is a crisp rename or replacement.

## Continuity classes

Rather than only exposing a raw score, use a small configurable classification:

```text
0.90–1.00   strong continuity
0.70–0.89   substantial continuity
0.40–0.69   partial / transforming continuity
0.15–0.39   weak continuity
0.00–0.14   apparent discontinuity
```

These are presentation and queueing bands—not ontological facts. A repository-specific configuration can tune them later, and the exact score must remain stored with the extractor/model configuration that produced it.

## What remains in ADR 001

The identity policy is now mostly settled. Four implementation-level questions remain, none requiring another conceptual redesign:

1. **Metadata versioning:** What minimum fields and schema-migration policy does `repository.edn` need?
2. **Repository-family discovery timing:** Do we discover shared commit OIDs only at registration, or also periodically as local repositories acquire/fetch new history?
3. **Continuity signals for Markdown Phase 1:** Which deterministic signals are in the first model—text similarity, heading structure, front matter, explicit links, time gap—and which are deferred?
4. **Boundary thresholds:** Do we begin with a single global default, or maintain separate defaults for Markdown and code from the outset?

The first one to settle should be deliberately small: **the exact v1 schema for `.git/corpus-archaeology/repository.edn` and the external fallback record.**

<div align="center">⁂</div>

[^23_1]: https://manpages.ubuntu.com/manpages/focal/man1/git-worktree.1.html

[^23_2]: https://fossies.org/windows/misc/git-2.55.0.windows.2.zip/git-2.55.0.windows.2/Documentation/config/extensions.adoc
---
# @Academic @GitHub @Hugging Face 1. [:resource-id :cursor-commit]? I don't think it needs much, the cursor may not be necessary. Just an identity is needed for the purpose we talked about already, maintaining a repositories identity in our system even if it is moved. Most anything else can be gained from the git database it's self. Unless you have a compelling reason to track other metadata here, instead of in the mongo collection.

2. I don't think we need to do it more than ones? two repo's either have a shared history, or they don't. Time won't realistically change this. I suppose if a repo required a history rewrite because of a leaked secret, maybe? So I suposed if we detect a rewritten history, we could check.
3. similarity, frontmatter, links, time gaps, named entities.
4. Should be separate defaults from the outset.

Yes. This keeps `.git` metadata intentionally tiny, preserves exact path identity, and treats continuity/epoch boundaries as a typed gradient rather than a brittle binary classification.

## Metadata v1

`repository.edn` should contain only the durable local identity needed to recognize the repository after its directory moves:

```clojure
{:resource-id #uuid "7a6b0d26-..."
 :cursor-commit nil}
```

But I would make `:cursor-commit` **not part of the initial contract**. It is processing/projection state, not repository identity, and it will vary by operation: commit scan, Markdown extraction, embedding, indexing, lineage analysis, and so on.

So the actual minimal v1 file is:

```clojure
{:resource-id #uuid "7a6b0d26-..."}
```

That is enough to satisfy the intended purpose:

```text
repository moves on disk
  -> indexer finds resolved .git directory
  -> reads corpus-archaeology/repository.edn
  -> recognizes the existing platform resource ID
  -> records the new location as a new observation
```

Everything else belongs in MongoDB:

- first/last observed filesystem location
- Git common directory and worktree locations
- current refs and HEAD
- object format
- commits seen
- ingestion job status
- cursor/checkpoint per projector
- cache placement
- user/source configuration
- repository-family membership
- history-rewrite observations.

This is a good separation: **Git-local metadata survives a move; MongoDB records the platform’s evolving knowledge about that repository.**

### File location

```text
<resolved Git common directory>/
  corpus-archaeology/
    repository.edn
```

For a standard repository that becomes:

```text
my-notes/
  .git/
    corpus-archaeology/
      repository.edn
```

For linked worktrees, resolve the common Git directory first; the working tree’s `.git` can be a pointer file rather than a directory.[^24_1][^24_2]

### Small schema rule

The file should be valid EDN and satisfy one narrow schema:

```clojure
[:map
 [:resource-id uuid?]]
```

No system configuration, cache hints, credentials, paths, arbitrary notes, or runtime state. That keeps it portable, legible, and hard to corrupt semantically.

## Cursor policy

You are right that a single repository-level cursor is probably the wrong abstraction.

A cursor is not “where the repository is.” It is:

> Where a particular projection has successfully processed a particular observed history.

For example:

```clojure
{:projection/id :git-observation-v1
 :resource-id ...
 :cursor {:last-processed-commit "..."}

 :status :healthy}
```

```clojure
{:projection/id :markdown-extraction-v1
 :resource-id ...
 :cursor {:last-processed-revision-at-path-id ...}

 :status :behind}
```

```clojure
{:projection/id :embedding-bge-m3-v1
 :resource-id ...
 :cursor {:last-processed-section-expression-id ...}

 :status :healthy}
```

Those records should live in MongoDB alongside the projections they describe. Putting a generic `:cursor-commit` in `.git` would imply one linear processing position even though the repository has branches, merges, rewrites, multiple processing stages, and independently replayable projections.

## Family discovery timing

We can simplify this too:

> Compare shared commit OIDs once when a repository instance is first registered. Do not repeatedly recompute family identity during ordinary indexing.

This is reasonable because ordinary commits add descendants to an existing commit graph; they do not cause two previously unrelated histories to acquire a shared preexisting commit. Git treats histories with no common ancestor as unrelated by default, which reflects the underlying commit-parent graph relationship.[^24_3][^24_4]

### Exception: detected rewrite or replacement

The exception you identified is correct: if the system detects a potential history rewrite—such as familiar refs suddenly resolving to unrelated commit histories, or an instance’s known tips disappearing and being replaced—then it should schedule a **family-reassessment check**.

That check is not a normal polling loop. It is a consequence of an unusual observation:

```clojure
{:event/type :repository/history-rewrite-suspected
 :resource-id ...
 :old-observed-tip "..."
 :new-observed-tip "..."
 :reason :no-common-ancestor-at-known-ref}
```

Then:

```text
1. Preserve prior observations.
2. Compare the newly reachable commit set against known families.
3. If overlap exists, keep the repository instance in its family.
4. If no overlap exists, represent a history-replacement/rewrite event.
5. Do not silently merge or split families.
```

For a leaked secret, a filtering tool can rewrite Git history, changing commit IDs even if much of the apparent content remains. That is precisely a case where the original shared-commit test may no longer prove continuity, so reassessment and user review are appropriate.[^24_5]

## Exact path policy

The identity-level rule is settled:

> The exact string returned by the source traversal is the canonical path spelling.

No normalization means we do **not** transform:

```text
.ημ/              -> .eta-mu/
MyFile.md         -> myfile.md
café.md           -> café.md
a\b.md            -> a/b.md
```

The system records the string exactly as supplied by Git tree traversal for Git facts, and by filesystem enumeration only where dealing with uncommitted/local material in the future.

For Git-backed Phase 1 content, prefer the Git tree entry path over the operating system’s filesystem enumeration whenever possible. This gives the platform a path in the same coordinate system as the commit/tree/blob evidence, without relying on local filesystem casing or Unicode behavior.

```clojure
{:path/raw ".ημ/identity.md"
 :path/source :git-tree-entry
 :path/comparison :exact}
```

A later search alias may let an ASCII query find `.ημ`, but that is an optional retrieval feature and never alters the identity record.

## Continuity signals

For Markdown Phase 1, use exactly the five signal classes you named:


| Signal | Measures | Initial use |
| :-- | :-- | :-- |
| Similarity | Textual/structural continuity between adjacent revisions or candidate sections | Main continuity feature |
| Front matter | Schema/type, stable IDs, project/category fields, title changes | Strong structural clue when present |
| Links | Explicit outbound/inbound links, target overlap, moved references | Strong authored evidence |
| Time gaps | Time between commits/revisions | Weak contextual evidence, never decisive alone |
| Named entities | People, projects, systems, places, technical terms | Topic/purpose continuity and change |

Do not add embeddings, LLM judgments, or global taxonomy labels to the **first deterministic continuity model**. They can later become additional, versioned candidate-generation signals, but your initial model should be auditable from source text and simple extraction output.

### Signal directions

Not every signal means the same thing:

```text
High text similarity          -> supports continuity
Stable front matter identity  -> strongly supports continuity
Changed front matter type     -> supports epoch boundary
Stable/overlapping links      -> supports continuity
Abrupt link-graph replacement -> supports epoch boundary
Short time gap                -> weakly supports continuity
Long time gap                 -> weakly supports boundary review
Entity overlap                -> supports continuity
Entity replacement            -> supports boundary review
```

The system should retain **raw measurements**, not only the final score:

```clojure
{:continuity/model :markdown-continuity-v1
 :from/revision-at-path-id ...
 :to/revision-at-path-id ...

 :signals
 {:text/similarity 0.43
  :frontmatter/stability 0.10
  :links/jaccard 0.04
  :time/gap-days 231
  :entities/overlap 0.13}

 :continuity/score 0.28
 :continuity/class :weak
 :computed-at ...}
```

That makes later reweighting possible without pretending earlier judgments were made by a different model.

## Separate defaults from the outset

Agreed. The system should have an explicit artifact-kind policy map from version one, even though Phase 1 initially executes only Markdown policy.

```clojure
{:continuity/policies
 {:markdown
  {:model :markdown-continuity-v1
   :signals [:text/similarity
             :frontmatter/stability
             :links/overlap
             :time/gap
             :entities/overlap]
   :classes {:strong [0.90 1.00]
             :substantial [0.70 0.90]
             :partial [0.40 0.70]
             :weak [0.15 0.40]
             :apparent-discontinuity [0.00 0.15]}
   :epoch-boundary-threshold 0.90}

  :code
  {:model :code-continuity-v1
   :signals [:ast/shape-overlap
             :symbols/public-overlap
             :dependencies/overlap
             :tests/overlap
             :comments/overlap
             :time/gap]
   :epoch-boundary-threshold 0.95}}}
```

The exact initial weights and thresholds remain empirical. The important decision is that Markdown and code do not share an accidental universal definition of “continuity.”

For Markdown, a sustained topic/structure replacement may be meaningful. For code, large changes can be routine while namespace role, public API, imports, and tests retain long-lived continuity.

## ADR 001 is now ready

The actual decision is concise:

> **Each registered local Git repository receives an automatically created, Git-local EDN file containing only a stable platform resource ID. Repository instances join a shared repository family only when they share commit OIDs at registration; reassessment occurs only after a suspected history rewrite. Git/tree paths are stored and compared exactly as observed. Persistent paths supply default document-role continuity, while Markdown and code apply separate weighted continuity models and propose hard epoch boundaries only above artifact-specific thresholds.**

All remaining details—exact MongoDB collections, event envelopes, projection checkpoints, section extraction schema, and cache key/value implementation—belong to later ADRs rather than this identity decision.
<span style="display:none">[^24_10][^24_11][^24_12][^24_13][^24_14][^24_15][^24_16][^24_17][^24_6][^24_7][^24_8][^24_9]</span>

<div align="center">⁂</div>

[^24_1]: https://manpages.ubuntu.com/manpages/focal/man1/git-worktree.1.html

[^24_2]: https://fossies.org/windows/misc/git-2.55.0.windows.2.zip/git-2.55.0.windows.2/Documentation/config/extensions.adoc

[^24_3]: https://git-scm.com/docs/git-merge

[^24_4]: https://www.git-tower.com/learn/git/faq/fatal-refusing-to-merge-unrelated-histories

[^24_5]: https://github.com/newren/git-filter-repo/blob/main/Documentation/git-filter-repo.txt

[^24_6]: https://stackoverflow.com/questions/37937984/git-refusing-to-merge-unrelated-histories-on-rebase

[^24_7]: https://www.reddit.com/r/git/comments/1fuixp4/merging_a_completely_historically_unrelated/

[^24_8]: https://medium.com/@kanishkanuwanperera/understanding-fatal-refusing-to-merge-unrelated-histories-in-git-and-how-to-fix-it-002b57d334b7

[^24_9]: https://oneuptime.com/blog/post/2026-01-24-git-refusing-merge-unrelated-histories/view

[^24_10]: https://dev.to/jkreeftmeijer/combine-git-repositories-with-unrelated-histories-4beo

[^24_11]: https://github.com/orgs/community/discussions/22075

[^24_12]: https://spences10.medium.com/git-allow-unrelated-histories-a39a3814b981

[^24_13]: https://stackoverflow.com/questions/45272492/git-is-refusing-to-merge-unrelated-histories-what-are-unrelated-histories

[^24_14]: https://medium.com/@jkc5186/fatal-refusing-to-merge-unrelated-histories-git-github-aef8a171a53d

[^24_15]: https://stackoverflow.com/questions/61734449/what-happens-in-git-when-you-merge-unrelated-histories/61735274

[^24_16]: https://repo.or.cz/git.git/blob/d5f4b8260f623d6fdef36d5eaa8a0c2350390472:/Documentation/git-filter-branch.txt

[^24_17]: https://stackoverflow.com/questions/44028493/how-to-push-history-edited-with-git-replace-to-remote?lq=1
---
# ADR 001: Git-Backed Resource Identity and Continuity

- **Status:** Accepted
- **Date:** 2026-07-11
- **Deciders:** Corpus Archaeology maintainers
- **Scope:** Phase 1 Git and Markdown corpus ingestion
- **Related:** ADR 000 — Authoritative Data and Rebuildability Boundary *(to be completed)*


## Context

The platform must trace a present-day idea across years of local Git-backed notes and commits while preserving inspectable evidence. Local repositories may be moved, cloned, mirrored, forked, rewritten, and reorganized; files may persist at one path while their content changes completely, or move/copy paths while retaining identical content. Phase 1 treats Git repositories as canonical for Git-originated source history and uses derived indices and caches as rebuildable projections.

Git provides immutable commit, tree, and blob object identities, but rename/copy labels are inferred from comparisons rather than durably stored as Git facts. Git’s `-M100%` setting limits rename detection to exact content matches; ordinary rename detection uses a similarity threshold, with 50 percent as the documented default.[^25_1][^25_2]

The system therefore must distinguish:

- immutable Git observations;
- platform resource identity across a local directory move;
- persistent document-role identity through a stable path;
- inferred continuity and discontinuity relations;
- human-accepted interpretation.


## Decision

### 1. Register each repository with a minimal Git-local identity record

When a local Git repository is registered, the system automatically writes a metadata file in its resolved common Git directory:

```text
<common-git-dir>/corpus-archaeology/repository.edn
```

For a standard working repository:

```text
my-repository/
  .git/
    corpus-archaeology/
      repository.edn
```

The v1 file contains exactly one required durable field:

```clojure
{:resource-id #uuid "7a6b0d26-1000-4000-8000-000000000001"}
```

The `:resource-id` identifies the platform resource across filesystem relocation. The file contains no operational cursor, paths, current refs, cache state, credentials, configuration, or user annotations.

If a repository is moved, the indexer resolves the Git directory, reads `repository.edn`, and associates the new filesystem location with the existing resource. Worktree-aware implementations must write to the resolved common Git directory, because a worktree’s `.git` may be a pointer rather than the shared Git directory.[^25_3]

If the metadata cannot be written—for example, the repository is read-only—the system registers an external MongoDB fallback record and marks the resource identity persistence mode as external. Registration must not fail solely because the Git directory cannot be modified.

### 2. Store all evolving platform knowledge in MongoDB

MongoDB stores the platform’s observations and projections, including:

- registered repository locations and availability;
- common Git directory locations;
- repository instances and family membership;
- observed commits, trees, paths, blobs, and refs;
- ingestion state;
- per-projection cursors/checkpoints;
- extraction and index status;
- event ledger records;
- review decisions;
- cache metadata;
- rewrite/replacement observations.

A repository-level `:cursor-commit` is explicitly rejected. A cursor belongs to a particular projection, not to the repository as a whole.

Example projection checkpoint:

```clojure
{:projection/id :markdown-extraction-v1
 :resource-id #uuid "7a6b0d26-1000-4000-8000-000000000001"
 :checkpoint {:last-processed-revision-at-path-id #uuid "..."}
 :status :healthy
 :updated-at #inst "2026-07-11T11:00:00Z"}
```


### 3. Model repository instances and repository families separately

A **repository instance** is one observed local Git object database or working directory. It has a platform resource ID and mutable location observations.

A **repository family** is the connected historical commit space shared by one or more repository instances.

A newly registered repository instance joins an existing family automatically only when it shares at least one commit OID with that family:

```text
shared commit OID
  -> observed shared commit history
  -> automatic family membership
```

Remote URLs, directory names, matching files, and similar content may be recorded as hints, but do not automatically establish family identity.

This prevents unrelated repositories from being merged because they share a remote name or template while avoiding duplicate treatment of clones, mirrors, and divergent local representations of shared history.

Family membership is checked at registration only. It is not continuously recomputed during ordinary indexing.

### 4. Reassess family relationships only after suspected history replacement

If the system observes a likely history rewrite or replacement—such as a previously observed ref resolving to an unrelated new tip—it emits a rewrite-suspected event and schedules family reassessment.

```clojure
{:event/type :repository/history-rewrite-suspected
 :resource-id #uuid "..."
 :old-observed-tip "..."
 :new-observed-tip "..."
 :reason :no-common-ancestor-at-known-ref}
```

The reassessment must:

1. Preserve all prior observations.
2. Compare newly reachable commits against known repository families.
3. Preserve family membership if overlap remains.
4. Record a history-replacement/rewrite observation if overlap no longer exists.
5. Never silently merge, split, or discard repository families.

History-rewriting tools can change commit IDs, including when removing leaked material, so commit overlap may no longer establish continuity after such an event.[^25_4]

### 5. Preserve paths exactly as observed

For Git-backed Phase 1 source material, paths are read from Git tree entries and retained exactly as provided.

```clojure
{:path/raw ".ημ/architecture/identity.md"
 :path/source :git-tree-entry
 :path/comparison :exact}
```

The identity layer must not:

- transliterate Unicode path names;
- Unicode-normalize strings;
- case-fold paths;
- rewrite path separators;
- resolve symlinks into alternate spellings;
- substitute ASCII aliases for observed paths.

Thus, these are distinct identity values:

```text
.ημ/spec.md
.ημ/Spec.md
.ημ/specs.md
```

The filesystem-access and future search-alias layers may require their own compatibility behavior, but neither may replace the canonical observed path string.

### 6. Treat persistent paths as default document-role continuity

Within a repository family, the same exact path in a direct parent-child commit transition establishes an observed **path continuity**.

This means a path has a stable platform-level document role by default even when its content changes substantially:

```clojure
{:document/id #uuid "..."
 :repository-family/id #uuid "..."
 :document/anchor {:kind :persistent-path
                   :initial-path "docs/architecture.md"}
 :identity-policy :path-persistent}
```

Each revision remains a separate immutable observation:

```clojure
{:revision-at-path/id #uuid "..."
 :document/id #uuid "..."
 :repository-family/id #uuid "..."
 :commit/oid "..."
 :path/raw "docs/architecture.md"
 :blob/oid "..."
 :relation-to-parent :same-path}
```

Path continuity is a claim about durable document role or location. It is **not** a claim of semantic equivalence, unchanged purpose, or persistent idea identity.

### 7. Model path repurposing as a continuity gradient

A file at a persistent path may gradually become discontinuous with its prior purpose, especially for code. Therefore, the platform does not assume an all-or-nothing epoch break.

For each relevant revision transition, it stores raw signals and a versioned, artifact-specific continuity score:

```clojure
{:revision-transition/id #uuid "..."
 :document/id #uuid "..."
 :from/revision-at-path-id #uuid "..."
 :to/revision-at-path-id #uuid "..."

 :continuity/model :markdown-continuity-v1
 :continuity/signals
 {:text/similarity 0.43
  :frontmatter/stability 0.10
  :links/overlap 0.04
  :time/gap-days 231
  :entities/overlap 0.13}

 :continuity/score 0.28
 :continuity/class :weak}
```

A document epoch boundary is a reviewable candidate, not an automatic identity mutation:

```clojure
{:epoch-boundary-candidate/id #uuid "..."
 :document/id #uuid "..."
 :between {:from/revision-at-path-id #uuid "..."
           :to/revision-at-path-id #uuid "..."}
 :boundary/score 0.91
 :boundary/threshold 0.90
 :status :proposed}
```

Accepted epoch boundaries alter the logical document view while preserving all original path and revision observations.

### 8. Use distinct continuity policies by artifact kind

Markdown and code do not share a universal definition of continuity. The policy map must support separate defaults from the outset.

Markdown v1 uses these signal classes:

- text similarity;
- front matter stability/change;
- explicit-link overlap/change;
- time gaps;
- named-entity overlap/change.

```clojure
{:continuity/policy :markdown-continuity-v1
 :signals [:text/similarity
           :frontmatter/stability
           :links/overlap
           :time/gap
           :entities/overlap]
 :epoch-boundary-threshold 0.90}
```

Code has a separate, initially deferred policy based on signals suitable for program structure:

```clojure
{:continuity/policy :code-continuity-v1
 :signals [:ast/shape-overlap
           :symbols/public-overlap
           :dependencies/overlap
           :tests/overlap
           :comments/overlap
           :time/gap]
 :epoch-boundary-threshold 0.95}
```

Scores, weights, thresholds, configuration, and model versions are derived metadata. They may be recomputed without altering source facts or prior review decisions.

### 9. Treat exact content relocation as strong evidence, not automatic document-purpose identity

When the same blob OID occurs at a different path, the platform records an exact-content relationship. It does not automatically declare document-role continuity because templates and copies can yield the same bytes without sharing ongoing purpose.

When a parent-to-child transition contains:

- source path removed;
- destination path added;
- identical blob OID;
- source absent from its prior path in the child;

the system records:

```clojure
{:relation/type :exact-relocation-observed
 :from/revision-at-path-id #uuid "..."
 :to/revision-at-path-id #uuid "..."
 :evidence {:blob/oid "..."
            :similarity 1.0
            :detector :git-diff
            :threshold 1.0}
 :status :observed}
```

If the original path remains, the relation is:

```clojure
{:relation/type :exact-copy-observed
 :status :observed}
```

Git’s exact rename mode is a useful signal here, but it remains a diff-derived classification rather than a permanent Git identity fact.[^25_2][^25_1]

### 10. Include provisional continuity in logical views

Logical document/history views may include:

- observed path continuities and exact relocations;
- accepted continuity and epoch decisions;
- provisional candidates.

The presentation must clearly distinguish them:

```text
Solid: observed Git/path fact
Solid, distinct style: accepted human interpretation
Dashed: provisional inferred candidate
Hidden by default/audit mode: rejected candidate
```

A candidate must always retain its evidence, generator/model/configuration version, score components, and review state.

## Consequences

### Positive

- A local-only repository retains a stable platform identity after being moved.
- The `.git` mutation is small, explicit, and narrowly scoped.
- Git remains authoritative for Git content and history.
- MongoDB remains the central durable platform store for observations, events, projections, and review state.
- Clones/mirrors/forks with shared history can share indexed commit/blob work without being collapsed into one local instance.
- Long-lived paths retain useful document-role histories even through extensive rewrites.
- Gradual semantic drift is visible rather than forced into arbitrary binary boundaries.
- Exact moves/copies become high-quality evidence without being mistaken for purpose identity.
- Unicode filenames such as `.ημ/` remain exactly represented rather than “corrected” by the platform.
- Future parser, index, embedding, and continuity-model changes can rebuild derived projections without changing observed Git facts.


### Negative

- One resource has two state locations by design: a minimal Git-local identity file and MongoDB operational metadata.
- A copied repository carries the same `:resource-id`; the system must validate Git evidence before treating it as the same instance/family.
- Exact-path equality intentionally does not compensate for filesystem case/Unicode quirks.
- The system must build and version continuity scoring rather than rely solely on Git rename detection.
- UI/query code must communicate observed, inferred, and accepted relations clearly.
- Path-persistent document identity can require later epoch boundaries when a path is repurposed.


## Non-goals

This ADR does not decide:

- MongoDB collection schemas, indexes, or event-envelope design;
- cache implementation or cache key format;
- container orchestration, job queues, worker placement, or K3s adoption;
- Markdown parser, section/block extraction schema, or AST strategy;
- lexical/vector search engine selection;
- dedicated graph datastore selection;
- embedding models or LLM-driven candidate generation;
- external non-Git material identity;
- full backup/archive policy;
- user authentication and multi-user authorization.


## Implementation notes

1. On registration, resolve the common Git directory before reading or writing metadata.
2. Create `<common-git-dir>/corpus-archaeology/` if absent.
3. Create `repository.edn` atomically with a generated UUID if absent.
4. Validate the file as EDN containing a UUID `:resource-id`.
5. Record registration, filesystem location, Git HEAD/ref observations, and family-match result in MongoDB.
6. Compare the repository’s reachable commit set with known families only during registration or an explicit rewrite-reassessment job.
7. Store all Git source paths from Git tree entries exactly as returned.
8. Emit immutable revision-at-path observations before any extraction, indexing, continuity, or review work.
9. Compute Markdown continuity only from the v1 signal set and persist raw signals with the resulting score.
10. Keep automatic exact-relocation relations separate from document-role continuity and concept/idea relations.
<span style="display:none">[^25_10][^25_11][^25_12][^25_13][^25_14][^25_15][^25_16][^25_5][^25_6][^25_7][^25_8][^25_9]</span>

<div align="center">⁂</div>

[^25_1]: https://git-scm.com/docs/git-diff/2.12.5

[^25_2]: https://www.kernel.org/pub/software/scm/git/docs/git-diff.html

[^25_3]: https://chromium.googlesource.com/external/gitster/git-htmldocs/+/188ae81bab23d645a02ed4f442ddafe9099d6e97/git-worktree.adoc

[^25_4]: https://github.com/newren/git-filter-repo/blob/main/Documentation/git-filter-repo.txt

[^25_5]: https://git-scm.com/docs/git-cat-file

[^25_6]: https://stackoverflow.com/questions/7759193/git-diff-renamed-file

[^25_7]: https://www.reddit.com/r/git/comments/1oi0tw/is_there_any_way_have_git_diff_show_that_a_file/

[^25_8]: https://stackoverflow.com/questions/67738831/disable-rename-detection-during-the-git-commit-to-preserve-history

[^25_9]: https://chelseatroy.com/2020/05/09/question-how-does-git-detect-renames/

[^25_10]: https://github.com/git/git/blob/master/diffcore-rename.c

[^25_11]: https://stackoverflow.com/questions/5730460/how-to-do-a-git-diff-on-moved-renamed-file/8632921

[^25_12]: https://github.com/gdb/git/blob/master/diffcore-rename.c

[^25_13]: https://github.com/git/git/commit/5404c116aa921d7e2f9408e103c80b7801735d08

[^25_14]: https://www.gitkraken.com/learn/git/git-worktree

[^25_15]: https://groups.google.com/g/git-for-windows/c/lSVIDCQpCG0

[^25_16]: https://github.com/jesseduffield/lazygit/issues/2904
---
# @Academic @GitHub @Hugging Face We've established the data architecture, the tech stack. Now for the phase 1 designs. User stories.

Phase 1 is a **corpus archaeology loop**, not a general AI research platform: register local Git repos, observe their Markdown history, retrieve evidence, trace an idea across time, and let you review the system’s proposed continuities. Each story below has observable acceptance criteria, which makes the work testable rather than merely aspirational.[^26_1][^26_2]

## Phase 1 outcome

> I can select a current idea in my notes, find its earlier expressions across local Git history, inspect the exact source and commits behind every connection, and accept or reject the system’s proposed lineage.

Git-oriented evidence views should preserve raw file content, revisions, and line-level historical context, analogous to the way Git tooling exposes raw file content and per-line history.[^26_3]

## Product principles

These are constraints on every story, not features to add later:

- **Git facts are distinct from inferred claims.**
- **Every claim shown to the user has inspectable evidence.**
- **No LLM, embedding, similarity model, or heuristic silently creates identity.**
- **The initial corpus is local Git repositories and Markdown.**
- **Git is canonical during indexing; caches and projections are rebuildable.**
- **Paths are recorded exactly as observed, including Unicode names such as `.ημ/`.**
- **Clojure/EDN remain the system’s primary internal expression language.**
- **A result must disclose whether it is observed, inferred, accepted, or rejected.**


## Personas

| Persona | Phase 1 role | Primary need |
| :-- | :-- | :-- |
| Corpus owner | You, working across years of notes and local repos | Recover history of an idea without rereading everything |
| Reviewer | You in a deliberate validation mode | Accept, reject, or annotate suggested relationships |
| Operator | You maintaining the cluster/services | Know whether ingestion, extraction, indices, and backups are healthy |
| Future agent | Not an autonomous Phase 1 user | Consume evidence packets without being allowed to invent provenance |

## Epic: Register sources

### US-001 — Register a local repository

**As a corpus owner,** I want to register a local Git repository as a source so that its Markdown history becomes available to the system.

**Acceptance criteria**

- Given a path to a valid local Git worktree or bare repository, registration succeeds.
- The system resolves the common Git directory before writing metadata.
- The system creates `.git/corpus-archaeology/repository.edn` with only a generated `:resource-id`.
- The system records the initial local location as an observation in MongoDB.
- The UI/CLI reports the resource ID, current HEAD, default branch/ref if available, and registration time.
- Registration does not scan, parse, or embed all history synchronously; it creates an ingest request/job.
- If the Git directory is not writable, registration succeeds with an external MongoDB identity fallback and clearly reports that fallback state.
- Invalid or non-Git paths fail with a specific error and do not create partial source records.


### US-002 — Rediscover a moved repository

**As a corpus owner,** I want a registered local repository to retain its platform identity after I move its directory.

**Acceptance criteria**

- Given a repository moved to a new filesystem location with its `.git/corpus-archaeology/repository.edn` retained, registering/scanning the new path resolves the existing `:resource-id`.
- The system records the new path as a new location observation rather than creating a duplicate source.
- The old location is retained as historical observation and marked unavailable only after a failed accessibility check.
- The system never uses the absolute filesystem path as the repository’s primary identity.
- If a copied repository carries the same `:resource-id` but does not share expected Git object/history evidence, the system flags an identity conflict for review rather than merging silently.


### US-003 — Recognize shared history

**As a corpus owner,** I want clones, mirrors, and diverged local representations with shared commits to share historical indexing work.

**Acceptance criteria**

- When a newly registered repository shares one or more commit OIDs with an existing repository family, it joins that family automatically.
- The new path remains a separate repository instance with independent availability and ref observations.
- Commits and blobs already observed in the family are not re-recorded as new facts merely because another instance exposes them.
- Matching remote URLs, matching folder names, or similar content alone never auto-merge repository families.
- The registration result explains whether the instance created a new family or joined an existing family and cites the overlapping commit evidence.


## Epic: Observe Git history

### US-004 — Discover Git-backed Markdown revisions

**As a corpus owner,** I want the system to discover every eligible Markdown revision in selected Git history so that historical notes are not limited to the current checkout.

**Acceptance criteria**

- The ingestion policy can target one or more exact path patterns, initially including `**/docs/**.md` and configurable Markdown paths.
- For each observed commit/tree entry, the system records repository family, repository instance, commit OID, parent OIDs, exact path string, blob OID, file mode, and observed time.
- The system reads Git object content through Git object access, not by checking out every historical revision.
- The system treats Git commits, trees, blob IDs, and path-at-commit relationships as observed facts.
- Re-running the same ingestion range is idempotent.
- A malformed Markdown file does not halt discovery of other files; it produces a structured extraction diagnostic.
- The system can report counts for commits scanned, Markdown paths found, unique blobs found, duplicate blobs reused, and failures.


### US-005 — Resume and replay projections

**As an operator,** I want ingestion and derived projections to resume safely after interruption or be replayed deliberately.

**Acceptance criteria**

- Every projection has its own durable MongoDB checkpoint.
- A failed worker can resume without duplicating immutable Git observations.
- An operator can replay one projection—for example extraction or embedding—without replaying source discovery.
- Replaying a projection produces new versioned derived records rather than overwriting prior evidence.
- The operator can inspect the source version, extractor/model version, checkpoint, status, last success time, and latest error for each projection.
- A projection can be marked stale after an extractor, parser, chunker, or index schema changes.


### US-006 — Detect potential history replacement

**As a corpus owner,** I want the system to preserve historical observations and call attention to a likely rewrite instead of silently treating a rewritten repository as ordinary new commits.

**Acceptance criteria**

- If a known ref changes to a newly observed tip without expected ancestry, the system emits a `:repository/history-rewrite-suspected` event.
- The system preserves previously observed commit/path/blob records.
- It schedules repository-family reassessment only for this exception, not during ordinary indexing.
- The UI/CLI identifies old tip, new tip, affected repository instance, detection reason, and time.
- No repository-family merge, split, deletion, or reassignment occurs automatically as a consequence.


## Epic: Extract Markdown evidence

### US-007 — Extract revision-scoped sections

**As a corpus owner,** I want Markdown revisions split into inspectable sections so that retrieval and evidence point to meaningful source regions rather than whole files.

**Acceptance criteria**

- Given a Markdown blob, the system creates a versioned extraction record.
- The extractor represents a document revision, heading-delimited sections, and blocks within sections.
- A section records exact heading path, ordinal, byte span, line span, content hash, extractor version, and source revision-at-path ID.
- Nested headings preserve hierarchy.
- Front matter is represented separately from the body and remains linked to the same revision.
- Paragraphs, lists, tables, quotes, and fenced code blocks are represented as typed blocks or diagnostics where unsupported.
- The exact raw source slice can be recovered from the Git blob plus recorded byte span.
- The system never assumes a section in one revision is the same persistent section as one in another revision.


### US-008 — Extract deterministic continuity features

**As a corpus owner,** I want the system to calculate inspectable Markdown continuity features so that I can understand why it thinks a document changed gradually or discontinuously.

**Acceptance criteria**

- For relevant adjacent revision transitions, the system stores raw measurements for:
    - text similarity;
    - front matter stability/change;
    - explicit-link overlap/change;
    - time gap;
    - named-entity overlap/change.
- The resulting Markdown continuity score identifies the versioned policy/configuration used.
- The score is stored separately from observed Git facts.
- The UI can display every raw signal alongside the score.
- Recomputing the policy creates a new derived result; it does not rewrite prior results or review decisions.
- Markdown and code have separate policy namespaces from the outset, even though code extraction is deferred.


### US-009 — Propose path-repurpose boundaries

**As a corpus owner,** I want the system to flag likely purpose changes in a long-lived path without erasing its ordinary history.

**Acceptance criteria**

- The system creates a proposed epoch boundary only when the artifact-specific boundary model exceeds its configured threshold.
- A long time gap alone cannot create a boundary.
- A low continuity score alone may be displayed as gradual drift without creating a hard-boundary proposal.
- Each proposal includes the transition, raw signal values, score, threshold, and model version.
- Accepting a boundary creates a durable interpretive decision while retaining all historical same-path observations.
- Rejecting a boundary prevents it from being presented as unresolved in the default interface while preserving its audit record.


## Epic: Search the corpus

### US-010 — Search current and historical notes

**As a corpus owner,** I want to search my Markdown corpus by words, phrases, and meaning so that I can find relevant evidence even when the wording changed.

**Acceptance criteria**

- Search supports lexical retrieval across title/heading text, section text, path, front matter fields, tags, links, and commit messages where available.
- Search supports semantic/vector retrieval over the selected Phase 1 indexing unit.
- The default search result is a section expression, not an opaque whole-file match.
- Results can be filtered by repository family, repository instance, exact path prefix, date range, branch/ref where available, and observed/inferred status.
- Each result exposes source path, commit, date, heading path, exact excerpt, retrieval mode, and score components.
- The user can switch among lexical-only, semantic-only, and hybrid modes for diagnosis.
- Query logs capture sufficient metadata to evaluate retrieval quality without storing sensitive user text beyond the chosen local policy.

Commit search and code/document search conventionally benefit from scoped qualifiers such as repository and date filters; Phase 1 should provide equivalent local scoping rather than a single undifferentiated corpus search.[^26_4][^26_5]

### US-011 — Inspect search evidence

**As a corpus owner,** I want every search hit to open at the exact historical source span so that I can decide whether the result actually supports my question.

**Acceptance criteria**

- Opening a result shows the source text from the exact blob/revision, not only a current working-tree file.
- The evidence reader displays repository instance/family, commit OID, author and commit times, exact path, heading path, and byte/line span.
- It provides surrounding section context without changing the cited source span.
- It can display raw Markdown and a rendered Markdown view.
- It links to the commit and direct parent/child revisions where available.
- If the Git object is currently inaccessible, the UI identifies the missing source state rather than fabricating an excerpt.
- The user can copy/export a stable evidence reference packet.


### US-012 — Compare historical expressions

**As a corpus owner,** I want to compare two historical sections or revisions side by side so that I can see whether an apparent continuity, contradiction, or rewrite is real.

**Acceptance criteria**

- The user can select any two section expressions or revision-at-path observations.
- The view shows exact source metadata for both sides.
- It shows a textual diff or structured comparison appropriate to Markdown.
- It displays continuity signals without conflating them with the comparison itself.
- The user can create a candidate relation or a review decision from the comparison.
- The comparison remains reproducible from recorded source IDs and extractor/model versions.


## Epic: Build idea lineage

### US-013 — Generate candidate lineage links

**As a corpus owner,** I want the system to propose plausible historical relationships between note sections so that I can rapidly discover what an idea became.

**Acceptance criteria**

- The system can generate candidates such as:
    - `:continues`;
    - `:refines`;
    - `:references`;
    - `:possibly-derived-from`;
    - `:near-duplicate`;
    - `:possibly-supersedes`;
    - `:possible-contradiction`.
- Every candidate identifies source and target section/revision expressions.
- Every candidate includes evidence spans, retrieval/similarity features, timestamps, generator version, and confidence/score.
- Candidate generation never mutates document, section, or concept identity.
- The initial candidate generator may use deterministic retrieval and continuity signals; LLM-generated relations are deferred unless separately designed and evaluated.
- Duplicate candidates for the same relation pair/configuration are idempotently merged or versioned rather than endlessly re-created.


### US-014 — Trace an idea through time

**As a corpus owner,** I want to start from a current section and traverse related earlier material as a dated evidence chain.

**Acceptance criteria**

- Starting from a selected section, the system returns a chronology containing observed path history, exact relocation/copy observations, accepted relation edges, and provisional candidate edges.
- Every edge has a visual status: observed, accepted, provisional, or rejected/audit-only.
- The timeline can include gradual continuity strength between successive revisions of a persistent path.
- The user can filter the view to observed facts only.
- The user can choose whether provisional candidates are included.
- Every node can open the evidence reader.
- The system never presents a candidate relation as established history without its status and evidence.


### US-015 — Identify research gaps

**As a corpus owner,** I want the system to show where a line of thought became unresolved, contradictory, or weakly supported so that I can decide what to research next.

**Acceptance criteria**

- A lineage/timeline view can identify:
    - unresolved contradiction candidates;
    - recurring open questions or TODO markers;
    - abrupt low-continuity transitions;
    - isolated later claims without earlier evidence;
    - repeated near-duplicate notes that may need synthesis.
- Each suggested gap links to exact evidence, not only an LLM-generated statement.
- The system can create a user-owned research-question record from selected evidence.
- The question record does not claim the gap is objectively unresolved; it records the user’s interpretation and linked evidence.
- No external-data acquisition occurs in Phase 1.


## Epic: Review interpretations

### US-016 — Review proposed relationships

**As a corpus owner,** I want a review inbox for candidate links so that I can turn useful proposals into trusted local knowledge without accepting model guesses blindly.

**Acceptance criteria**

- The inbox can filter candidates by relation type, confidence band, repository family, date range, and model/generator version.
- Each item shows both exact source spans, surrounding context, scores/signals, and why it was generated.
- The user can accept, reject, relabel, defer, annotate, or mark “do not suggest similar.”
- A review action appends a durable event; it does not rewrite the original candidate or Git evidence.
- A rejected candidate remains available in audit mode.
- The default inbox avoids repeatedly surfacing candidates marked “do not suggest similar.”
- The review action can optionally create a research question or a user-curated concept.


### US-017 — Curate a concept from evidence

**As a corpus owner,** I want to name and curate a concept after reviewing evidence so that an important idea can become a stable navigation object without pretending it was discovered automatically.

**Acceptance criteria**

- A concept is created explicitly by the user from one or more selected section expressions.
- The concept stores name, optional description, creation event, and evidence links.
- The concept may contain accepted and provisional evidence relations, but their status remains visible.
- Removing a section from a concept does not delete the source, candidate, or historical review records.
- A concept can be used as a search/timeline entry point.
- The system distinguishes a user-curated concept from an inferred cluster.


## Epic: Evidence packets and interface

### US-018 — Export an evidence packet

**As a corpus owner,** I want to export a compact evidence-backed research packet so that I can use the result in planning, writing, or later agent-assisted work.

**Acceptance criteria**

- The user can export selected results, timeline nodes, and review decisions as Markdown and EDN or JSON.
- Each export identifies repository resource ID, commit OID, exact path, source spans, timestamps, relation status, and extraction/index/model versions.
- The packet separates:
    - observed source facts;
    - inferred candidates;
    - accepted user interpretations;
    - unanswered questions.
- The export contains enough identifiers to reproduce evidence lookup locally.
- Exporting never includes a claim without an attached evidence reference or an explicit “interpretation/no direct source” label.


### US-019 — Use a minimal local workbench

**As a corpus owner,** I want one local interface that unifies search, evidence, timeline, and review so that corpus archaeology is a practical loop rather than a collection of admin tools.

**Acceptance criteria**

- The first interface may be a local web application backed by CLI/EDN-compatible operations.
- It has at least:
    - search;
    - evidence reader;
    - comparison view;
    - timeline/lineage view;
    - review inbox;
    - ingestion/projection status.
- Every screen shows clear source/projection status where relevant.
- The UI supports Unicode paths without transliteration.
- The UI has no dependency on external SaaS access for the local corpus workflow.
- The UI works when only one strong machine is online, subject to the availability of its local source repositories and indices.


## Epic: Operate safely

### US-020 — Observe ingestion and indexing

**As an operator,** I want to see health and throughput for each processing stage so that a large corpus does not become silently incomplete.

**Acceptance criteria**

- The system records structured events/logs for registration, discovery, extraction, cache use, indexing, candidate generation, and review actions.
- A status view shows repositories scanned, commits seen, blobs read, Markdown revisions extracted, sections indexed, failures, retries, and projection lag.
- Cache metrics distinguish LMDB hit/miss/eviction behavior from durable source availability.
- The user can inspect a failure with repository resource ID, commit/blob/path context, operation version, and error details.
- A failed derived operation can be retried or replayed without mutating Git observations.
- Weak nodes are not treated as mandatory holders of irreplaceable hot state.


### US-021 — Recover the corpus archaeology view

**As an operator,** I want to restore core metadata and rebuild projections so that a machine failure does not permanently destroy evidence navigation.

**Acceptance criteria**

- MongoDB backup/restore procedures recover resource registrations, observed Git metadata, events, review decisions, and projection state.
- Git repositories remain the source of canonical Git blobs during ordinary reindexing.
- LMDB caches are treated as disposable; their loss does not lose canonical source facts or user review decisions.
- Search/vector indices can be rebuilt from Git plus MongoDB metadata/events.
- A restore drill can reproduce a selected evidence packet from restored data and available repository sources.
- The system records which sources were inaccessible during restore/rebuild.


## Delivery slices

| Slice | Stories | Demonstrable outcome |
| :-- | :-- | :-- |
| **Slice A: Source facts** | US-001 to US-006 | Register repositories, preserve moved identity, traverse Git/Markdown history safely |
| **Slice B: Evidence units** | US-007 to US-009 | Extract exact revision-scoped sections and continuity/epoch proposals |
| **Slice C: Retrieval** | US-010 to US-012 | Search and inspect/compare historical evidence |
| **Slice D: Archaeology loop** | US-013 to US-017 | Trace one idea, review candidates, create research questions and concepts |
| **Slice E: Usable system** | US-018 to US-021 | Export evidence, operate locally, observe and recover the system |

## Phase 1 release gate

Phase 1 is complete when all of the following are demonstrated against real local repositories:

1. Register at least two local Git repositories, including one moved after registration.
2. Ingest at least two years of Markdown/commit history from a meaningful corpus.
3. Search for a present-day idea and retrieve at least one older relevant expression through hybrid retrieval.
4. Open every result at an exact historical source span and commit.
5. Produce a dated lineage view containing observed and explicitly marked provisional/accepted relationships.
6. Accept or reject at least several candidate relationships and show their audit trail.
7. Create one evidence-backed research question from the resulting history.
8. Export one reproducible evidence packet.
9. Restore/rebuild the derived views without relying on LMDB or search-index persistence.
10. Confirm that no inferred relation was silently represented as a Git fact.
<span style="display:none">[^26_10][^26_11][^26_12][^26_13][^26_14][^26_15][^26_6][^26_7][^26_8][^26_9]</span>

<div align="center">⁂</div>

[^26_1]: https://www.atlassian.com/agile/project-management/user-stories

[^26_2]: https://www.atlassian.com/work-management/project-management/acceptance-criteria

[^26_3]: https://docs.github.com/en/repositories/working-with-files/using-files/viewing-and-understanding-files

[^26_4]: https://docs.github.com/en/search-github/searching-on-github/searching-commits

[^26_5]: https://docs.github.com/en/search-github/searching-on-github

[^26_6]: https://community.atlassian.com/forums/App-Central-articles/Acceptance-criteria-in-Jira-how-to-write-store-and-validate-them/ba-p/3165137

[^26_7]: https://github.com/github/docs/blob/main/content/search-github/github-code-search/using-github-code-search.md

[^26_8]: https://github.com/github/docs/blob/main/content/search-github/searching-on-github/searching-code.md

[^26_9]: https://docs.github.com/en/account-and-profile/how-tos/contribution-settings/viewing-commit-details-from-your-timeline

[^26_10]: https://github.com/github/docs/blob/main/content/search-github/github-code-search/understanding-github-code-search-syntax.md

[^26_11]: https://www.altexsoft.com/blog/acceptance-criteria-purposes-formats-and-best-practices/

[^26_12]: https://community.atlassian.com/forums/Jira-questions/How-to-Add-Acceptance-Criteria-to-User-Story/qaq-p/1053304

[^26_13]: https://marketplace.atlassian.com/apps/1223139/acceptance-criteria-for-jira-free

[^26_14]: https://github.com/github/docs/blob/main/content/rest/search/search.md

[^26_15]: https://cli.github.com/manual/gh_search_commits
---
# @Academic @GitHub @Hugging Face Let's talk about the shape of that CLI and rest API.

The CLI and REST API should be **two adapters over one command/query algebra**, not two independently designed interfaces. The CLI is the primary Phase 1 work surface; REST exists for the local web workbench, automation, and future distributed workers. Clojure’s `tools.cli` supports GNU-style option parsing and nested argument groups, making it a reasonable thin parser rather than a domain layer.[^27_1][^27_2]

## Design stance

Use a **resource-oriented read API** and a **command-oriented write API**:

```text
Queries:
  Read facts and projections
  No side effects
  GET over REST
  stdout from CLI

Commands:
  Request durable work or record a user decision
  Append event / create job / change review state
  POST over REST
  subcommands from CLI
```

Do not make the HTTP surface mimic MongoDB collections, raw event types, or internal worker messages. The public contract should express user intent: “register this repository,” “search this corpus,” “review this candidate,” “replay this projection.”

Internally, commands and responses are EDN-shaped maps validated by Malli. Externally:

- CLI defaults to human-readable text, with `--format edn|json|table`.
- REST accepts and returns JSON by default.
- REST may support `application/edn` for local/native clients.
- Server errors use RFC 9457 Problem Details in JSON, augmented with stable local error codes and correlation IDs. RFC 9457 defines a machine-readable error payload precisely to avoid inventing a new error response format for every API.[^27_3]


## One core algebra

The domain layer should expose functions shaped like:

```clojure
;; Commands
(register-resource! system command)
(request-ingest! system command)
(request-replay! system command)
(record-review! system command)
(create-concept! system command)
(create-research-question! system command)

;; Queries
(get-resource system query)
(list-resources system query)
(get-projection-status system query)
(search-corpus system query)
(get-evidence system query)
(compare-expressions system query)
(get-lineage system query)
(list-review-candidates system query)
(get-concept system query)
(export-evidence-packet system query)
```

The CLI translates flags and arguments into these maps. REST translates HTTP method/path/body into the same maps. Neither adapter owns business behavior.

```text
CLI invocation
  -> parse / validate
  -> command or query map
  -> application service
  -> MongoDB, Git, cache, index, jobs
  -> response map
  -> CLI renderer

HTTP request
  -> decode / validate
  -> command or query map
  -> application service
  -> MongoDB, Git, cache, index, jobs
  -> response map
  -> HTTP renderer
```


## Naming

I would use a single executable named after the project once you name it. Until then, use `ca` only as a placeholder:

```text
ca resource register
ca ingest request
ca search
ca evidence show
ca lineage show
ca review list
ca review decide
ca projection status
```

Avoid making the CLI mirror storage or implementation terms:

```text
Bad:
  ca mongo insert
  ca vector upsert
  ca graph edge-add
  ca event append

Good:
  ca resource register
  ca ingest request
  ca review decide
  ca lineage show
```

The CLI should express *what you mean*, while the system determines which projections, events, and stores are involved.

## Global CLI contract

Every command supports a small common option set:

```text
--format text|table|edn|json     Output representation
--quiet                          Emit only primary result
--verbose                        Include diagnostics and correlation ID
--request-id UUID                Supply an idempotency/correlation ID
--config PATH                    Override local client configuration
--api URL                        Use REST endpoint rather than embedded/local mode
--no-wait                        Submit asynchronous command and return job ID
--wait                           Wait for terminal job state where meaningful
--help                           Command-specific help
```

The default should be **human-readable text**, not raw JSON. Scripts and Clojure tooling can request EDN:

```bash
ca search "event sourcing identity" --format edn
ca resource list --format json
ca projection status --format table
```

A CLI command should write normal output to stdout and diagnostics/errors to stderr. Exit codes should be stable:


| Code | Meaning |
| --: | :-- |
| 0 | Successful query or accepted command |
| 1 | Unhandled/internal failure |
| 2 | Invalid syntax or invalid command input |
| 3 | Requested resource not found |
| 4 | Conflict or review-state conflict |
| 5 | Source unavailable or Git object inaccessible |
| 6 | Accepted asynchronously; work not complete |
| 7 | Permission/authentication failure, if introduced later |

## CLI command tree

### Resource registration

```bash
ca resource register /home/me/projects/notes
ca resource register /home/me/projects/notes --name personal-notes
ca resource list
ca resource show res_01J...
ca resource locations res_01J...
ca resource scan res_01J...
```

`register` is deliberately separate from ingestion. It creates/reads `.git/corpus-archaeology/repository.edn`, records the repository instance, determines family membership through shared commit OIDs, and then returns a resource record.

Example:

```bash
$ ca resource register ~/notes

Registered resource: res_01J7...
Repository family:   fam_01J7... (new)
Location:            /home/user/notes
Git HEAD:            2a9c8f1
Identity record:     .git/corpus-archaeology/repository.edn
Ingestion:           not requested
```


### Ingestion and projections

```bash
ca ingest request res_01J...
ca ingest request res_01J... --path-glob '**/docs/**.md'
ca ingest status job_01J...
ca ingest failures res_01J...

ca projection list
ca projection status --resource res_01J...
ca projection replay markdown-extraction-v1 --resource res_01J...
ca projection replay embedding-v1 --resource res_01J... --no-wait
```

The CLI says **request** or **replay**, never “run a worker.” A command accepts work; the eventual executor is not part of the user-facing contract.

### Search and evidence

```bash
ca search "identity across git history"
ca search '"event sourcing"' --mode lexical
ca search "same document after rewrite" --mode hybrid
ca search "continuity" --resource res_01J... --from 2024-01-01
ca search "continuity" --path '.ημ/**' --format table

ca evidence show sec_01J...
ca evidence show sec_01J... --context 2
ca evidence raw sec_01J...
ca evidence export sec_01J... --format markdown
```

The default `search` result should contain enough information to decide what to open:

```text
1. score 0.841  2025-03-22  .ημ/architecture/identity.md
   Architecture > Repository continuity
   “A resource ID follows the repository even when its location changes…”
   status: observed  retrieval: hybrid

2. score 0.803  2024-08-14  docs/corpus.md
   Archaeology > Identity
   “Paths persist as role anchors across long periods…”
   status: observed  retrieval: lexical+semantic
```


### Comparison and lineage

```bash
ca compare sec_01J... sec_01K...
ca compare rev_01J... rev_01K... --format edn

ca lineage show sec_01J...
ca lineage show sec_01J... --include provisional
ca lineage show sec_01J... --observed-only
ca lineage explain edge_01J...
```

A lineage command should never hide epistemic status:

```text
Observed:
  2025-06-02  docs/identity.md @ 31f95c
  -> same-path continuation

Accepted:
  2024-02-19  docs/notes-identity.md @ f8ab64
  -> refines (accepted 2026-07-11)

Provisional:
  2023-04-07  .ημ/archive/continuity.md @ 7ea0f1
  -> possibly-derived-from (0.82)
```


### Candidate review

```bash
ca review list
ca review list --type possibly-derived-from --status proposed
ca review show cand_01J...
ca review decide cand_01J... accept
ca review decide cand_01J... reject --note "Same vocabulary; unrelated purpose"
ca review decide cand_01J... relabel --as refines
ca review decide cand_01J... defer
ca review decide cand_01J... suppress-similar
```

The decision command must require an explicit decision verb. Do not provide an ambiguous `review update` that may conceal a change from proposed to accepted.

```clojure
{:candidate/id "cand_01J..."
 :decision :accept
 :note "The later section directly elaborates the earlier proposal."
 :request/id #uuid "..."}
```


### Concepts and research questions

```bash
ca concept create "Git-backed corpus identity" sec_01J... sec_01K...
ca concept list
ca concept show con_01J...

ca question create \
  "When did path continuity become an explicit architecture decision?" \
  --evidence sec_01J... \
  --evidence sec_01K...

ca question list
ca question show q_01J...
```

These commands must create **user-curated records**, not pretend the platform has established an objective concept or research gap.

### Operations and export

```bash
ca status
ca status --resource res_01J...
ca doctor
ca events tail --resource res_01J...
ca failures list
ca backup verify
ca export packet sec_01J... --lineage --format markdown
```

`doctor` should report source availability, MongoDB reachability, index health, cache status, projection lag, and the local API endpoint—but it should avoid treating a cold/disposable LMDB cache as a data-integrity failure.

## REST API shape

Use a versioned base path:

```text
/api/v1
```

The REST surface follows resources for reads and **action endpoints for durable commands**. That is more honest than attempting to force every queued/replay/review operation into generic CRUD.

OpenAPI describes operations as a path plus HTTP method, with request parameters, body, and expected responses; keeping stable, resource-oriented paths makes the API documentable and testable.[^27_4][^27_5]

### Resource endpoints

```text
POST   /api/v1/resources
GET    /api/v1/resources
GET    /api/v1/resources/{resource-id}
GET    /api/v1/resources/{resource-id}/locations
GET    /api/v1/resources/{resource-id}/history-status

POST   /api/v1/resources/{resource-id}/ingest-requests
GET    /api/v1/resources/{resource-id}/projections
GET    /api/v1/resources/{resource-id}/failures
```

**Register a resource**

```http
POST /api/v1/resources
Content-Type: application/json
Idempotency-Key: 70a89a1f-...

{
  "location": "/home/user/notes",
  "display_name": "personal-notes"
}
```

```json
{
  "resource": {
    "id": "res_01J7...",
    "family_id": "fam_01J7...",
    "location": "/home/user/notes",
    "identity_persistence": "git-local",
    "head": "2a9c8f1..."
  },
  "registration": {
    "family_match": "new-family",
    "shared_commit_evidence": []
  }
}
```

If the registration is accepted but needs asynchronous Git scanning, return `201 Created` for the resource and provide a separate ingest-request resource only when explicitly requested.

### Job/request endpoints

Use durable request objects rather than transient task endpoints:

```text
POST   /api/v1/ingest-requests
GET    /api/v1/ingest-requests/{request-id}

POST   /api/v1/projection-replays
GET    /api/v1/projection-replays/{request-id}

GET    /api/v1/projections
GET    /api/v1/projections/{projection-id}
```

Example:

```http
POST /api/v1/projection-replays
Idempotency-Key: 19d0...

{
  "resource_id": "res_01J7...",
  "projection": "markdown-extraction-v1",
  "scope": {
    "mode": "all-observed-history"
  }
}
```

```json
{
  "request": {
    "id": "replay_01J...",
    "status": "accepted",
    "resource_id": "res_01J7...",
    "projection": "markdown-extraction-v1"
  }
}
```

Return:

- `202 Accepted` when a command has been durably accepted but work is pending;
- `201 Created` when it creates an immediately available resource;
- `200 OK` for a completed synchronous query/result;
- `409 Conflict` for invalid state transitions, such as reviewing an already-finalized candidate without an explicit supersession action.


### Search endpoints

```text
POST   /api/v1/searches
GET    /api/v1/searches/{search-id}
GET    /api/v1/evidence/{expression-id}
POST   /api/v1/comparisons
GET    /api/v1/comparisons/{comparison-id}
```

Use `POST /searches`, not an enormous `GET /search?...`, because hybrid queries need structured scopes, modes, explainability flags, and cursor pagination.

```http
POST /api/v1/searches
Content-Type: application/json

{
  "query": "repository identity after moving local notes",
  "mode": "hybrid",
  "scope": {
    "resource_ids": ["res_01J7..."],
    "path_prefixes": [".ημ/", "docs/"],
    "observed_between": {
      "from": "2023-01-01",
      "to": "2026-07-11"
    }
  },
  "result_kind": "section-expression",
  "include": {
    "explain": true,
    "provisional_relations": false
  },
  "page": {
    "size": 25
  }
}
```

```json
{
  "search": {
    "id": "search_01J...",
    "mode": "hybrid",
    "index_versions": {
      "lexical": "lexical-v1",
      "embedding": "embedding-v1"
    }
  },
  "results": [
    {
      "expression_id": "sec_01J...",
      "score": 0.841,
      "source": {
        "resource_id": "res_01J7...",
        "commit_oid": "31f95c...",
        "path": ".ημ/architecture/identity.md",
        "heading_path": ["Architecture", "Repository continuity"],
        "span": {"line_start": 12, "line_end": 29}
      },
      "excerpt": "…",
      "status": "observed",
      "scores": {
        "lexical": 0.71,
        "semantic": 0.89,
        "hybrid": 0.841
      }
    }
  ],
  "page": {
    "next_cursor": null
  }
}
```


### Lineage endpoints

```text
GET    /api/v1/expressions/{expression-id}/lineage
GET    /api/v1/relations/{relation-id}
POST   /api/v1/lineage-requests
```

Use `GET` for a bounded lineage projection:

```text
GET /api/v1/expressions/sec_01J.../lineage?include=observed,accepted,provisional&depth=4
```

Use a `POST /lineage-requests` only when the query is computationally expensive, requires custom scoring, or constructs a durable exportable lineage analysis.

### Review endpoints

```text
GET    /api/v1/review-candidates
GET    /api/v1/review-candidates/{candidate-id}
POST   /api/v1/review-decisions
GET    /api/v1/review-decisions/{decision-id}
```

Do not expose:

```text
PATCH /review-candidates/{id} {"status":"accepted"}
```

That would obscure the fact that a review decision is an append-only interpretive event. Use an explicit decision resource:

```http
POST /api/v1/review-decisions
Idempotency-Key: 01e7...

{
  "candidate_id": "cand_01J...",
  "decision": "accept",
  "relation_type": "refines",
  "note": "The later note develops the earlier proposal.",
  "evidence_expression_ids": ["sec_01J...", "sec_01K..."]
}
```

```json
{
  "decision": {
    "id": "dec_01J...",
    "candidate_id": "cand_01J...",
    "decision": "accept",
    "relation_type": "refines",
    "status": "recorded",
    "recorded_at": "2026-07-11T11:30:00Z"
  }
}
```


### Concepts and questions

```text
POST   /api/v1/concepts
GET    /api/v1/concepts
GET    /api/v1/concepts/{concept-id}

POST   /api/v1/research-questions
GET    /api/v1/research-questions
GET    /api/v1/research-questions/{question-id}
```

These should carry explicit authorship/status:

```json
{
  "name": "Git-backed corpus identity",
  "created_by": "user",
  "evidence_expression_ids": ["sec_01J...", "sec_01K..."]
}
```

Not:

```json
{
  "entity_type": "concept",
  "confidence": 0.99
}
```

The platform should not smuggle an inferred cluster into a user-owned concept through API vocabulary.

## Shared response envelope

Do **not** wrap every response in `{"data": ...}` by habit. Resource reads can return the resource directly. Lists and complex analyses need explicit named fields.

Use three patterns:

```json
// Singular resource
{
  "id": "res_01J...",
  "kind": "repository-resource"
}
```

```json
// Collection
{
  "items": [],
  "page": {
    "next_cursor": null
  }
}
```

```json
// Analysis/result packet
{
  "search": {},
  "results": [],
  "page": {}
}
```

Every externally addressable record includes:

```json
{
  "id": "prefix_...",
  "kind": "section-expression",
  "status": "observed",
  "created_at": "..."
}
```

Use human-recognizable prefixes:

```text
res_     repository resource
fam_     repository family
inst_    repository instance
rev_     revision at path
sec_     section expression
blk_     block expression
cand_    inferred candidate
dec_     review decision
con_     curated concept
q_       research question
job_     accepted work request
exp_     evidence export
```

The UUID/object ID can remain internal, but prefixed public IDs make a CLI transcript intelligible and make mistakes less likely.

## Error contract

REST errors use:

```http
Content-Type: application/problem+json
```

```json
{
  "type": "https://local.corpus/errors/source-unavailable",
  "title": "Git source object is unavailable",
  "status": 503,
  "detail": "Blob 31f95c… is not reachable from the registered source.",
  "instance": "/api/v1/evidence/sec_01J...",
  "code": "source-unavailable",
  "request_id": "req_01J...",
  "resource_id": "res_01J...",
  "commit_oid": "31f95c...",
  "path": ".ημ/architecture/identity.md",
  "retryable": false
}
```

RFC 9457’s fields—such as `type`, `title`, `status`, `detail`, and `instance`—give clients a consistent base; local fields should be additive and domain-specific.[^27_6][^27_3]

## First contract boundaries

For Phase 1, I would formalize these first—not all endpoints at once:

1. `POST /resources` and `ca resource register`
2. `POST /ingest-requests` and `ca ingest request`
3. `GET /resources/{id}/projections` and `ca projection status`
4. `POST /searches` and `ca search`
5. `GET /evidence/{id}` and `ca evidence show`
6. `GET /expressions/{id}/lineage` and `ca lineage show`
7. `GET /review-candidates`, `POST /review-decisions`, and `ca review ...`

That is the complete Phase 1 archaeology loop, with no premature API surface for external research, LLM agents, graph administration, or infrastructure internals.

The first design choice to settle is whether the CLI should **always call the local REST API**—giving one production code path—or whether it can call the Clojure application service directly in an embedded/offline mode, with REST as a separate adapter.
<span style="display:none">[^27_10][^27_11][^27_12][^27_13][^27_14][^27_15][^27_7][^27_8][^27_9]</span>

<div align="center">⁂</div>

[^27_1]: https://github.com/clojure/tools.cli/tree/a741b23f230123179fc518af772f1c057058f7d2

[^27_2]: https://github.com/clojure/tools.cli

[^27_3]: https://www.rfc-editor.org/info/rfc9457/

[^27_4]: https://learn.openapis.org/specification/paths.html

[^27_5]: https://spec.openapis.org/oas/v3.2.0.html

[^27_6]: https://dl.acm.org/doi/book/10.17487/RFC9457

[^27_7]: https://clojure.github.io/tools.cli/

[^27_8]: https://medium.com/@thomsmed/handling-http-api-errors-with-problem-details-398a9967aee4

[^27_9]: https://github.com/clojure/tools.cli/blob/master/CHANGELOG.md

[^27_10]: https://github.com/clojure/tools.tools

[^27_11]: https://medium.com/@shuklanitesh805/the-last-api-error-format-your-apis-will-ever-need-a1d57c0e6853

[^27_12]: https://medium.com/@mhd.umair/understanding-rfc-9457-problem-details-for-http-apis-6bdb675e685f

[^27_13]: https://swagger.io/docs/specification/v3_0/paths-and-operations/

[^27_14]: https://tex2e.github.io/rfc-translater/html/rfc9457.html

[^27_15]: https://swagger.io/blog/problem-details-rfc9457-api-error-handling/
---
# @Academic @GitHub @Hugging Face What do you think?

I would choose a **dual-mode CLI with a direct application-service default**, plus an explicit REST mode—not “CLI always calls HTTP.”

That gives you a first-class Clojure operator tool that works during bootstrap, repair, offline work, and testing, while still forcing the REST adapter to use the same command/query contracts. One-off administrative processes should share the application’s code and configuration, but they do not inherently need to traverse an HTTP server to be valid operations.[^28_1]

## Recommendation

```text
Default:
  CLI -> application service directly

Explicit remote mode:
  CLI -> REST API -> application service

Never:
  CLI -> MongoDB/Git/search engine directly
```

The invariant is:

> **The application service is the only domain boundary the CLI and REST adapters may invoke.**

So the choice is not “two code paths with duplicated behavior.” It is:

```text
Direct CLI:
  command parser
    -> validated EDN command/query
      -> application service
        -> ports/adapters
          -> Git, MongoDB, LMDB, search, job dispatch

REST CLI:
  command parser
    -> same validated EDN command/query
      -> HTTP JSON/EDN adapter
        -> REST API
          -> same application service
            -> same ports/adapters
```

The difference is transport only.

## Why direct by default

Phase 1 needs reliable operational commands before the API is necessarily healthy.

You will need commands such as:

```bash
ca doctor
ca resource register ~/notes
ca ingest request res_...
ca projection replay markdown-extraction-v1 --resource res_...
ca failures list
ca repair reconcile-resource res_...
```

If the CLI *always* calls the local REST service, then:

- you cannot use it comfortably to diagnose an API startup/binding/configuration failure;
- a broken HTTP layer blocks direct repair or inspection;
- local tests must boot an HTTP server for every CLI behavior test;
- bootstrap becomes circular: “start enough infrastructure to use the tool that tells you why infrastructure did not start.”

A direct one-off process using the same code, configuration, schemas, and persistence adapters is a normal operational pattern; the key requirement is code/config parity, not HTTP indirection.[^28_2][^28_1]

## Why keep REST mode

REST mode is still essential once the system is distributed.

Use it when:

- the CLI runs from a laptop or a different cluster node;
- the command concerns a resource whose Git checkout/cache is available only on a particular service node;
- the local web workbench calls the application;
- an automation process should operate against the central deployment;
- you want the request to inherit server-side authentication, authorization, audit identity, and rate controls;
- you need a single central command ingress for a multi-user future.

Example:

```bash
ca --api http://archivist.internal:8080 \
  search "path continuity across notes"

ca --api http://archivist.internal:8080 \
  review decide cand_... accept \
  --note "Direct elaboration of the 2024 design note"
```


## Do not make it automatic fallback

I would **not** quietly fall back from direct mode to REST mode, or vice versa.

That causes dangerous ambiguity:

```text
Did ca resource register mutate the local repo?
Or did it ask a remote server to register a path that exists only locally?
Did ca search query a local incomplete projection?
Or a centralized indexed corpus?
```

Require the mode to be explicit in user intent:

```bash
# Direct application-service mode; default
ca resource register ~/notes

# Remote HTTP mode
ca --api https://node-a.internal/api/v1 resource register /mnt/corpus/notes

# Optional explicit alias for clarity
ca remote search "continuity"
ca local doctor
```

The output should always report the execution target:

```text
Target: direct application service
Profile: local
Database: mongodb://localhost/...
```

or:

```text
Target: REST API
Endpoint: https://archivist.internal/api/v1
Server request: req_01J...
```


## The real distinction: locality of Git

This is particularly important because Git source access is local.

A command like:

```bash
ca resource register ~/projects/notes
```

has a filesystem-bearing argument. In remote REST mode, that path names a path on the **server node**, not on the CLI machine.

That must be explicit:

```bash
# Register a path on this machine through direct mode
ca resource register ~/projects/notes

# Register a path on node-a, where node-a owns that checkout
ca --api https://node-a.internal/api/v1 \
  resource register /srv/corpus/notes
```

Later, if you want a remote client to register a repository from its own machine, that is not the same endpoint. It is an **import/agent protocol**:

```text
remote client
  -> identifies/copies/exposes source repository
  -> source-owning ingest agent reads Git
  -> central service receives observations
```

Do not let an innocent REST `location` field pretend the central service can read arbitrary client filesystem paths.

## Command classification

Use the execution mode based on the command’s nature.


| Command type | Direct default | REST supported | Notes |
| :-- | --: | --: | :-- |
| Local repository registration | Yes | Only for server-local paths | Filesystem locality must be explicit |
| Git discovery/extraction | Yes | Yes, as a request to owning node | Never assume shared filesystem |
| Search | Yes | Yes | Results differ if direct node has different index scope; expose scope |
| Evidence display | Yes | Yes | REST source may need object access or archived source |
| Review decision | Yes | Yes | Both append the same durable decision event |
| Projection status | Yes | Yes | REST is often more useful for cluster aggregate |
| Projection replay | Yes | Yes | REST creates central durable work request |
| Doctor/repair | Yes | Limited | Direct mode is essential when HTTP is unhealthy |
| Backup verification | Yes | Yes | Operational context determines location |
| User-facing web UI | No | Yes | Browser consumes REST only |

## Client configuration

Use explicit named profiles rather than hidden environment-based behavior:

```edn
;; ~/.config/corpus-archaeology/config.edn
{:profiles
 {:local
  {:mode :direct
   :mongo-uri "mongodb://localhost:27017/corpus_archaeology"
   :git-access :local}

  :cluster
  {:mode :http
   :api-url "https://archivist.internal/api/v1"}}}
```

Then:

```bash
ca --profile local resource register ~/notes
ca --profile cluster search "event sourcing"
ca --profile cluster review decide cand_... accept
```

Default profile can be `:local`, but each command prints the resolved profile and target under `--verbose`.

Do not put long-lived secrets in the EDN profile. Reference environment variables or a later secrets mechanism.

## Contract discipline

Use one Malli schema per command/query, with the same schema validated before direct service invocation and at the REST boundary.

```clojure
(def RegisterResource
  [:map
   [:location [:and string? [:fn #(absolute-path? %)]]]
   [:display-name {:optional true} string?]
   [:request-id {:optional true} uuid?]])

(def SearchCorpus
  [:map
   [:query string?]
   [:mode [:enum :lexical :semantic :hybrid]]
   [:scope {:optional true} SearchScope]
   [:page {:optional true} PageRequest]])
```

This means:

- direct mode validates exactly what remote mode validates;
- generated documentation can describe both CLI flags and REST payloads;
- the core can be tested without networking;
- REST gets a formal language-agnostic contract through OpenAPI when you are ready to publish it. OpenAPI is explicitly designed as a language-agnostic description of HTTP APIs.[^28_3]


## Narrow exception: event tailing

There is one area where the modes should intentionally differ.

```bash
ca events tail
```

In direct mode, this may subscribe to local event/projection instrumentation. In remote mode, it should use a server stream—SSE or WebSocket—not periodic REST polling.

But this is an observability feature, not part of the core user-story loop. Defer it until basic command/query behavior is solid.

## First implementation

Build these layers in order:

```text
1. Domain command/query schemas
2. Application service functions
3. Direct CLI adapter
4. HTTP REST adapter
5. HTTP CLI profile/client
6. Local web workbench
```

That sequence gives you a usable system immediately and avoids making web transport a prerequisite for corpus archaeology.

The concrete decision I would record is:

> **ADR 002 — The CLI is a dual-mode adapter. It invokes application services directly by default and invokes the REST API only under an explicit HTTP profile or `--api` option. Both modes use the same versioned Malli command/query contracts; neither mode may access infrastructure stores directly outside application ports.**
<span style="display:none">[^28_10][^28_11][^28_12][^28_13][^28_14][^28_15][^28_4][^28_5][^28_6][^28_7][^28_8][^28_9]</span>

<div align="center">⁂</div>

[^28_1]: https://12factor.net/admin-processes

[^28_2]: https://www.dreamfactory.com/resources/whitepapers/implementing-the-twelve-factor-app-methodology

[^28_3]: https://spec.openapis.org/oas/v3.2.0.html

[^28_4]: https://github.com/clojure/tools.cli

[^28_5]: https://clojure.github.io/tools.cli/

[^28_6]: https://medium.com/@tech_18484/introduction-701b7a8f4730

[^28_7]: https://notes.kodekloud.com/docs/12-Factor-App/Twelve-Factor-App-methodology/Admin-Processes

[^28_8]: https://github.com/clojure/tools.cli/tree/a741b23f230123179fc518af772f1c057058f7d2

[^28_9]: https://medium.com/@naval2408/12-factor-app-principle-12-admin-processes-fcbe85048ea7

[^28_10]: https://qiita.com/totakke/items/18bc513e4c7086e27286

[^28_11]: https://github.com/konradzagozda/twelve-factor-app-demo

[^28_12]: https://github.com/ONSdigital/dp/blob/main/training/architecture/12_FACTOR_APP_PRINCIPLES.md

[^28_13]: https://dev.to/cadienvan/admin-processes-the-twelve-factor-app-methodology-1jdh

[^28_14]: https://learn.openapis.org/introduction.html

[^28_15]: https://www.libelle.com/blog/the-12-factor-app-part3/
---
# ADR 002: Dual-Mode CLI and REST Adapter Boundary

- **Status:** Accepted
- **Date:** 2026-07-11
- **Deciders:** Corpus Archaeology maintainers
- **Scope:** Phase 1 command-line interface, local REST API, and application-service boundary
- **Related:** ADR 001 — Git-Backed Resource Identity and Continuity


## Context

Phase 1 needs a CLI for source registration, ingestion, search, evidence inspection, lineage review, projection replay, diagnostics, and repair. It also needs a REST API for the local web workbench, automation, future remote clients, and distributed deployment.

The corpus is Git-backed and source access is local to the machine holding a repository instance. A command such as:

```bash
ca resource register ~/notes
```

is meaningful only relative to the machine interpreting that filesystem path. A remote server cannot safely be assumed to have access to the caller’s home directory.

The CLI must also remain useful when the HTTP server is unavailable, during bootstrap, diagnostics, recovery, repair, and tests. At the same time, direct CLI behavior and REST behavior must not diverge into two independent implementations.

Clojure `tools.cli` provides command-line parsing, including nested argument groups, but is used only as a boundary parser; it does not define the domain command model.[^29_1][^29_2]

OpenAPI provides a language-neutral format for describing HTTP APIs, suitable for documenting and testing the remote adapter once its Phase 1 contract stabilizes.[^29_3][^29_4]

## Decision

### 1. Use one command/query application boundary

The system exposes a versioned internal command/query algebra. The CLI and REST API are adapters over this same application-service boundary.

```text
CLI parser
  -> validated command/query map
  -> application service
  -> infrastructure ports
  -> Git, MongoDB, LMDB, search index, job dispatch

HTTP request
  -> decoded and validated command/query map
  -> application service
  -> infrastructure ports
  -> Git, MongoDB, LMDB, search index, job dispatch
```

Neither adapter may implement domain behavior independently.

Neither adapter may access MongoDB, Git, LMDB, search indices, queues, or event collections outside approved application ports.

The internal maps are EDN-shaped and validated with versioned Malli schemas. The same command/query schema validates:

- direct CLI input after flag parsing;
- REST input after JSON or EDN decoding;
- application-service invocation;
- asynchronous work request payloads where applicable.

Example:

```clojure
(def RegisterResource
  [:map
   [:location string?]
   [:display-name {:optional true} string?]
   [:request-id {:optional true} uuid?]])

(def SearchCorpus
  [:map
   [:query string?]
   [:mode [:enum :lexical :semantic :hybrid]]
   [:scope {:optional true} map?]
   [:page {:optional true} map?]])
```

The application service owns validation beyond shape, authorization when introduced, idempotency, event creation, projections, and response semantics.

### 2. Make direct application-service mode the CLI default

By default, the CLI invokes the application service directly in the current process:

```bash
ca resource register ~/notes
ca ingest request res_01J...
ca search "repository identity"
ca evidence show sec_01J...
ca review decide cand_01J... accept
ca doctor
```

Direct mode uses the configured local adapters for MongoDB, Git access, caches, search, and work dispatch. It is not a shortcut around the application boundary.

The CLI must identify its target under `--verbose`:

```text
Target: direct application service
Profile: local
```

Direct mode is required for:

- first-time bootstrap;
- local repository registration;
- local Git inspection;
- diagnostics and repair when HTTP is unavailable;
- test execution without starting an HTTP server;
- one-off maintenance against the same application configuration.


### 3. Support explicit HTTP mode for remote operation

The CLI supports a REST mode only when selected explicitly through a named profile or `--api` option:

```bash
ca --profile cluster search "repository identity"
ca --api https://archivist.internal/api/v1 \
  review decide cand_01J... accept

ca --api https://archivist.internal/api/v1 \
  projection replay markdown-extraction-v1 \
  --resource res_01J...
```

HTTP mode serializes the same command/query contract to the REST adapter. It must not use a distinct domain protocol.

The CLI reports the target under `--verbose`:

```text
Target: REST API
Endpoint: https://archivist.internal/api/v1
Server request: req_01J...
```

The CLI must never silently fall back between direct and HTTP mode.

### 4. Treat filesystem paths as target-local

A filesystem path supplied to a command is interpreted by the selected target:

```bash
# Direct mode: path exists on the CLI machine.
ca resource register ~/projects/notes

# HTTP mode: path must exist on the API server/source-owning node.
ca --api https://archivist.internal/api/v1 \
  resource register /srv/corpus/notes
```

The REST API must reject ambiguous client-local path semantics. A remote client cannot register its own local checkout by posting its path to a central server unless a later, separate source-import or source-agent protocol is introduced.

That later protocol is out of scope for Phase 1.

### 5. Separate queries from commands

The application contract distinguishes read-only queries from durable commands.


| Category | Semantics | CLI behavior | REST behavior |
| :-- | :-- | :-- | :-- |
| Query | Reads observations or projections; no durable mutation | Print result to stdout | Usually `GET`; `POST` only for complex analysis/search bodies |
| Command | Creates durable user intent, event, request, or review decision | Print created record/request | `POST`; return created/accepted resource |
| Work request | Durable command requesting asynchronous projection work | Return request ID; `--wait` optional | `202 Accepted` while pending |
| Review decision | Durable, append-only interpretive action | Requires explicit decision verb | `POST` a decision resource |

Examples:

```clojure
;; Queries
{:query/type :resource/get
 :resource-id "res_01J..."}

{:query/type :corpus/search
 :query "identity across history"
 :mode :hybrid}

;; Commands
{:command/type :resource/register
 :location "/home/user/notes"}

{:command/type :projection/replay-request
 :resource-id "res_01J..."
 :projection :markdown-extraction-v1}

{:command/type :review/record-decision
 :candidate-id "cand_01J..."
 :decision :accept}
```

The system must not represent a durable review decision as a generic mutable update such as:

```text
PATCH /review-candidates/{id} {"status":"accepted"}
```

A decision is an append-only interpretation event and must be modeled explicitly.

### 6. Use explicit CLI profiles

CLI execution configuration is selected through an explicit profile:

```edn
{:profiles
 {:local
  {:mode :direct
   :mongo-uri #env "CORPUS_MONGO_URI"
   :git-access :local}

  :cluster
  {:mode :http
   :api-url "https://archivist.internal/api/v1"}}}
```

Usage:

```bash
ca --profile local resource register ~/notes
ca --profile cluster search "continuity"
```

The default profile is `:local` unless explicitly configured otherwise.

Profiles may reference environment variables or a later secrets mechanism. They must not contain long-lived credentials directly in committed EDN configuration.

### 7. Define CLI output behavior

The CLI supports:

```text
--format text|table|edn|json
--quiet
--verbose
--request-id UUID
--profile NAME
--api URL
--wait
--no-wait
```

Default output is human-readable text or tables. Automation and Clojure-native tooling use EDN; interoperability clients may use JSON.

Rules:

- normal result output goes to stdout;
- diagnostics, warnings, and errors go to stderr;
- `--verbose` includes resolved profile, target mode, correlation/request ID, and relevant projection/index versions;
- each public resource has a recognizable identifier prefix;
- CLI output never conceals whether data is observed, provisional, accepted, rejected, stale, or unavailable.

Stable exit codes:


| Exit code | Meaning |
| --: | :-- |
| 0 | Success |
| 1 | Internal/unexpected failure |
| 2 | Invalid command syntax or validation failure |
| 3 | Requested resource not found |
| 4 | State conflict, including invalid review transition |
| 5 | Git source or object unavailable |
| 6 | Durable asynchronous request accepted but not complete |
| 7 | Authentication or authorization failure, when enabled |

### 8. Version the REST API under `/api/v1`

The local/remote HTTP adapter begins at:

```text
/api/v1
```

Initial Phase 1 resources:

```text
POST   /api/v1/resources
GET    /api/v1/resources
GET    /api/v1/resources/{resource-id}
GET    /api/v1/resources/{resource-id}/locations

POST   /api/v1/ingest-requests
GET    /api/v1/ingest-requests/{request-id}

GET    /api/v1/projections
GET    /api/v1/resources/{resource-id}/projections
POST   /api/v1/projection-replays

POST   /api/v1/searches
GET    /api/v1/evidence/{expression-id}
POST   /api/v1/comparisons
GET    /api/v1/expressions/{expression-id}/lineage

GET    /api/v1/review-candidates
GET    /api/v1/review-candidates/{candidate-id}
POST   /api/v1/review-decisions

POST   /api/v1/concepts
GET    /api/v1/concepts/{concept-id}

POST   /api/v1/research-questions
GET    /api/v1/research-questions/{question-id}
```

Use resource reads for normal `GET` requests. Use durable request resources for potentially asynchronous operations, such as ingestion and projection replay. Use `POST /searches` for structured hybrid search requests that exceed practical query-string scope.

HTTP response semantics:


| Status | Use |
| --: | :-- |
| `200 OK` | Completed query or synchronous action |
| `201 Created` | New immediately available resource |
| `202 Accepted` | Durable command/work request accepted, execution pending |
| `400 Bad Request` | Invalid request syntax/shape |
| `404 Not Found` | Requested public resource absent |
| `409 Conflict` | Invalid state transition or idempotency conflict |
| `422 Unprocessable Content` | Valid request shape but domain validation failed |
| `503 Service Unavailable` | Required Git source or dependent service unavailable |

### 9. Use JSON by default and EDN by negotiation

REST accepts and returns JSON by default:

```text
Content-Type: application/json
Accept: application/json
```

For trusted local/native clients, the API may additionally support:

```text
Content-Type: application/edn
Accept: application/edn
```

EDN support is an adapter-format capability, not an alternative domain protocol. JSON remains the primary REST interoperability format.

### 10. Use RFC 9457 Problem Details for REST errors

REST errors use:

```text
Content-Type: application/problem+json
```

RFC 9457 standardizes a machine-readable problem-details object for HTTP API errors.[^29_5][^29_6]

Example:

```json
{
  "type": "https://local.corpus/errors/source-unavailable",
  "title": "Git source object is unavailable",
  "status": 503,
  "detail": "Blob 31f95c… is not reachable from the registered source.",
  "instance": "/api/v1/evidence/sec_01J...",
  "code": "source-unavailable",
  "request_id": "req_01J...",
  "resource_id": "res_01J...",
  "commit_oid": "31f95c...",
  "path": ".ημ/architecture/identity.md",
  "retryable": false
}
```

The system uses RFC-standard fields (`type`, `title`, `status`, `detail`, `instance`) and adds stable local fields only where necessary for evidence, diagnosis, and recovery.

### 11. Publish an OpenAPI description after the first contract stabilizes

The REST API is described by a version-controlled OpenAPI document once the first resource, ingestion, search, evidence, lineage, and review contracts have integration tests.

The OpenAPI document is derived from or checked against the same Malli contract definitions where practical. It must not become an independently maintained, contradictory API description.

OpenAPI is designed for language-agnostic API descriptions and can support documentation, generated clients, and automated tests.[^29_4][^29_3]

## Consequences

### Positive

- The CLI is operational before, during, and after REST service availability.
- Direct CLI use does not bypass validation, event logging, review semantics, or infrastructure boundaries.
- REST, CLI, web workbench, and future automation share one application behavior model.
- Local Git path semantics remain explicit and safe.
- Distributed/remote operation remains available without forcing HTTP into every local command.
- Tests can validate core application behavior without binding network ports or starting a web server.
- HTTP clients receive stable, documented resource and error contracts.
- Explicit profiles avoid accidental commands against the wrong database, node, or corpus.
- Command-oriented actions preserve event-sourced review/projection semantics.


### Negative

- The CLI needs configuration/bootstrap support for two modes.
- Developers must maintain transport parity tests for direct and HTTP adapter behavior.
- Direct mode requires local availability of configured infrastructure adapters.
- Remote use requires authentication, TLS, authorization, and node/service routing work in later phases.
- A local CLI result and a remote API result can differ if they intentionally target different databases, indices, or source scopes; verbose target reporting is therefore mandatory.
- The API does not support arbitrary client-local repository registration without a future import/source-agent protocol.


## Non-goals

This ADR does not decide:

- authentication, authorization, user accounts, or multi-user tenancy;
- TLS certificates, ingress, DNS, or service-mesh configuration;
- K3s adoption, node assignment, queues, worker scheduling, or process supervision;
- MongoDB collection/index schemas;
- LMDB/Redis cache implementations;
- event envelope fields or stream ordering;
- Graph/vector/search engine selection;
- websocket/SSE event streaming;
- browser UI implementation;
- external corpus import or remote-source acquisition;
- a source-agent/import protocol for registering repositories on a client machine through a central server.


## Implementation notes

1. Define Malli schemas for every initial command/query before implementing a CLI subcommand or HTTP endpoint.
2. Implement application-service functions against ports, returning typed result/error maps rather than HTTP or CLI response objects.
3. Implement the direct CLI adapter first using `clojure.tools.cli`.
4. Add profile resolution and clear target reporting.
5. Implement REST as a thin JSON/EDN adapter over the same service functions.
6. Add paired parity tests: the same valid command/query must yield equivalent application outcomes through direct CLI and REST.
7. Use idempotency/request IDs for all durable commands, particularly registration, ingest requests, replay requests, and review decisions.
8. Add OpenAPI only after initial endpoint contracts have integration coverage.
<span style="display:none">[^29_10][^29_11][^29_12][^29_13][^29_14][^29_15][^29_7][^29_8][^29_9]</span>

<div align="center">⁂</div>

[^29_1]: https://github.com/clojure/tools.cli/tree/a741b23f230123179fc518af772f1c057058f7d2

[^29_2]: https://github.com/clojure/tools.cli

[^29_3]: https://spec.openapis.org/oas/v3.2.0.html

[^29_4]: https://github.com/OAI/OpenAPI-Specification

[^29_5]: https://www.rfc-editor.org/info/rfc9457/

[^29_6]: https://dl.acm.org/doi/book/10.17487/RFC9457

[^29_7]: https://clojure.github.io/tools.cli/

[^29_8]: https://medium.com/@thomsmed/handling-http-api-errors-with-problem-details-398a9967aee4

[^29_9]: https://tex2e.github.io/rfc-translater/html/rfc9457.html

[^29_10]: https://medium.com/@shuklanitesh805/the-last-api-error-format-your-apis-will-ever-need-a1d57c0e6853

[^29_11]: https://github.com/clojure/clojure-site/blob/master/content/reference/clojure_cli.adoc

[^29_12]: https://www.openapis.org/

[^29_13]: https://github.com/bkuhlmann/petail

[^29_14]: https://github.com/clojure-cookbook/clojure-cookbook/blob/master/03_general-computing/3-07_parse-command-line-arguments.asciidoc

[^29_15]: https://medium.com/@mhd.umair/understanding-rfc-9457-problem-details-for-http-apis-6bdb675e685f
---
# @Academic @GitHub @Hugging Face How about a TUI and/or GUI?

Do **both eventually**, but make them serve different jobs: build a **TUI first as the Phase 1 power-user interface**, and build the **browser GUI second as the evidence-rich visual workbench**. Do **not** build a native desktop GUI first; it duplicates the browser workbench’s needs while adding packaging and desktop-platform maintenance.

A Clojure TUI can be built over Java Lanterna through Clojure wrappers such as `clj-terminal` or `clojure-lanterna`; JavaFX/Clojure options such as cljfx exist if native desktop becomes justified later.[^30_1][^30_2][^30_3]

## Recommendation

```text
Phase 1A: CLI
  -> deterministic commands, scripting, bootstrap, repair

Phase 1B: TUI
  -> fast interactive corpus navigation and review

Phase 1C: Browser GUI
  -> rich evidence inspection, comparison, timeline, graph/map views

Later only if necessary: native desktop shell
  -> offline bundled workstation experience
```

The TUI and GUI must be **presentation adapters**, just like CLI and REST. They call the existing command/query boundary through direct mode or REST; they do not read Mongo, Elasticsearch, Git objects, or graph storage directly.

```text
CLI ─┐
TUI ─┼─> command/query application boundary ─> ports/adapters
GUI ─┘
```


## What each is for

| Interface | Best at | Not best at |
| :-- | :-- | :-- |
| CLI | Automation, scripts, bootstrap, diagnostics, repair, piping EDN | Rapid browsing of many search hits and candidate links |
| TUI | Keyboard-first search, evidence triage, review queue, queues/jobs, quick comparisons | Dense visual timelines, rich Markdown/table rendering, graph exploration |
| Browser GUI | Source reading, side-by-side diff, timeline/lineage, entity graph, geospatial map, visual review | Headless repair, shell composition, bootstrapping a broken system |
| Native desktop GUI | Fully packaged offline workstation; deep OS integration | Early-phase velocity and avoiding duplicate UI work |

## Why a TUI is worth it

This system’s primary user is a person who has accumulated an enormous textual corpus and wants to ask: “What did I already think about this?”

That loop is extremely keyboard-shaped:

```text
type query
  -> scan ranked results
  -> open evidence
  -> compare older/newer passage
  -> accept/reject a candidate
  -> create research question
  -> return to search
```

A terminal interface makes that loop fast without requiring mouse travel, browser tabs, or a graphical server. It will also work naturally over SSH, which is useful when the Ultra 9/4070 Ti box is doing the work while you are sitting at a different machine.

The TUI is also the best interface for operational work:

```bash
ca tui
```

```text
┌ Corpus Archaeology ─ local / direct ───────────────────────────┐
│ Search: [repository identity after moving notes             ]   │
├──────────────────────── Results ───────────────────────────────┤
│ 1  0.841  2025-03-22  .ημ/architecture/identity.md             │
│    Architecture › Repository continuity                         │
│ 2  0.803  2024-08-14  docs/corpus.md                            │
│    Archaeology › Identity                                       │
│ 3  0.767  2023-04-07  .ημ/archive/continuity.md                │
├──────────────────────── Evidence ──────────────────────────────┤
│ A resource ID follows a repository even when its location…     │
│                                                                  │
│ observed · commit 31f95c · lines 12–29 · hybrid 0.841          │
├────────────────────────────────────────────────────────────────┤
│ Enter open · Tab panels · c compare · l lineage · r review     │
│ q quit · ? help                                                 │
└────────────────────────────────────────────────────────────────┘
```


## TUI scope

The TUI should not try to reproduce every future GUI visualization. It should optimize for the **archaeology loop**.

### Phase 1 TUI screens

| Screen | Primary action | Required detail |
| :-- | :-- | :-- |
| Search | Find sections | Query, mode, scope, ranked results, result status |
| Evidence | Verify a hit | Raw/rendered text, commit/path/span, context, metadata |
| Compare | Decide continuity | Side-by-side sections/revisions, diff, signals |
| Lineage | Follow an idea | Chronological evidence chain with observed/provisional/accepted states |
| Review inbox | Judge candidates | Accept, reject, relabel, defer, suppress similar |
| Research questions | Capture next work | Create/show questions tied to evidence |
| Sources | Manage local repos | Registration state, family, locations, scan state |
| Operations | Keep system healthy | Projection status, failures, retries, source availability |

### TUI interaction rules

- **Keyboard-first; mouse support optional.**
- **One focused action per screen.**
- **Visible status marker everywhere:** `OBSERVED`, `PROVISIONAL`, `ACCEPTED`, `REJECTED`, `STALE`, `UNAVAILABLE`.
- **Never render an inferred link as if it were Git history.**
- **No destructive action without an explicit confirmation.**
- **All actions must have a non-interactive CLI equivalent.**

For example:

```text
TUI action:
  [a] Accept candidate cand_01J...

Equivalent:
  ca review decide cand_01J... accept
```

This keeps the TUI convenient without making it an opaque alternate system.

## TUI technology choice

Because you strongly prefer Clojure, start with **Lanterna on the JVM**, accessed through a Clojure wrapper.

- `clj-terminal` wraps Lanterna 3 for console-mode text UIs.[^30_1]
- `clojure-lanterna` is another Clojure-oriented wrapper around Lanterna.[^30_3]

I would create a thin local TUI architecture:

```clojure
{:tui
 {:state {:screen :search
          :focused-panel :results
          :query ""
          :selected-result-id nil
          :status-message nil}
  :commands {...}
  :keymap {...}
  :renderer {...}}}
```

But its data comes from the same query layer:

```clojure
(search-corpus system query)
(get-evidence system query)
(compare-expressions system query)
(get-lineage system query)
(list-review-candidates system query)
(record-review! system command)
```

The TUI should call the **direct application-service adapter by default** when running on the source-owning machine, and use the explicit REST profile if it is connecting to the centralized service:

```bash
ca tui
ca --profile cluster tui
```


## Browser GUI scope

The browser GUI is where the platform becomes spatial and evidence-dense rather than merely functional.

Build it after the TUI proves the important flows and the REST API has stabilized. The GUI should call REST only; it must not get an in-process MongoDB/Git bypass.

### Phase 1 GUI views

- **Search workspace:** query builder, scopes, lexical/semantic/hybrid toggle, result list, evidence preview.
- **Evidence reader:** rendered Markdown beside exact raw source; source metadata pinned in a stable panel.
- **Revision comparison:** side-by-side rendered/raw sections, diff, continuity signals, boundary-candidate explanation.
- **Lineage timeline:** dated chain of source expressions and relation edges; visual distinction between observed, accepted, and provisional.
- **Review workspace:** evidence-first candidate cards with decision controls and rationale field.
- **Source health:** registered repositories, availability, projection status, failures, repair/replay requests.
- **Research-question board:** explicit user-created questions, supporting evidence, unresolved candidates.

Do **not** lead with a graph visualization. A graph display is attractive but can become an expensive “hairball browser” before the evidence/review loop is good. Add graph views only once curated concepts and accepted edges make it navigable.

Likewise, defer maps until external geopolitical/geospatial datasets actually exist. Elasticsearch may support spatial indexing later, but that is not itself a reason to put a map in Phase 1.

## GUI technology choice

For Phase 1, use a local browser application backed by the REST API:

```text
Clojure service
  -> REST API
  -> local browser GUI
```

Given your Clojure preference, a reasonable long-term front end is **ClojureScript**. But do not let ClojureScript framework selection stall the product. The non-negotiable is the stable REST evidence contract, not whether the initial GUI uses ClojureScript, a lightweight static client, or another browser technology.

The GUI should have no privileged direct access to:

- Git filesystem paths;
- MongoDB;
- Elasticsearch/vector internals;
- graph database;
- worker queues;
- local caches.

That preserves the adapter boundary established in ADR 002.

## Why not native desktop first

A native JavaFX UI is viable. Cljfx is a declarative functional JavaFX wrapper inspired by React/Re-frame, and its example repository demonstrates a packaged desktop application built with cljfx and Java tooling.[^30_2][^30_4]

But native desktop first is the wrong early trade-off:

- You would build a UI-specific state/rendering layer before your REST contract is proven.
- Desktop packaging, JavaFX runtime compatibility, updates, and OS-specific behavior create new work.
- A desktop app is weaker than a browser GUI for sharing views, exposing local API docs, or eventually supporting multiple cluster nodes.
- You would still need a web interface if you want access from different machines.
- The browser is already a capable renderer for Markdown, diffs, timelines, tables, maps, and future graph visualizations.

Native desktop becomes attractive later only if you explicitly want:

- a distributable single-machine “research workstation”;
- offline-first operation with a bundled local service;
- deep filesystem/notification/system-tray integration;
- a deliberate non-browser product experience.

At that point, a **thin JavaFX/Cljfx shell hosting the existing local browser GUI** is preferable to a second full frontend. It can launch the local service, open a controlled WebView, and provide OS integration while preserving the REST API as the UI boundary.

## Proposed interface roadmap

### Milestone 1 — CLI foundation

```text
ca resource register
ca ingest request
ca search
ca evidence show
ca compare
ca lineage show
ca review decide
ca projection status
ca doctor
```

This is the canonical, scriptable interface and must be complete enough to perform every durable action.

### Milestone 2 — TUI archaeology loop

```bash
ca tui
```

Deliver:

- search and result navigation;
- evidence reader;
- comparison;
- lineage navigation;
- review queue;
- projection/failure status;
- command palette/help;
- direct and explicit REST profile modes.

Do not add graph/map visualization yet.

### Milestone 3 — Browser evidence workbench

Deliver:

- evidence-first search;
- comparison and timeline;
- review workspace;
- source/projector health;
- exportable evidence packets;
- REST/OpenAPI-driven integration.


### Milestone 4 — Visual extensions

Only after corpus evidence and reviewed relations are substantial:

- curated concept graph;
- geospatial data views;
- research-question planning board;
- data-analysis notebooks/workspaces;
- model-selection and agent-run history views.


## A decision worth recording

I would add a follow-on ADR:

> **ADR 003 — Phase 1 uses a CLI-first, TUI-second, browser-GUI-third interface strategy. The TUI is a keyboard-first adapter for search, evidence triage, lineage, review, and operations. The browser GUI is the primary rich evidence and visualization workbench. Native desktop GUI development is deferred; if later required, it will be a thin shell over the local REST-backed web workbench rather than a separate domain client.**

The only decision I would ask you to make now is whether you want the TUI to be **part of the single `ca` executable** (`ca tui`) or a separately versioned executable such as `ca-tui`; I recommend `ca tui` for Phase 1.
<span style="display:none">[^30_10][^30_11][^30_12][^30_13][^30_14][^30_15][^30_5][^30_6][^30_7][^30_8][^30_9]</span>

<div align="center">⁂</div>

[^30_1]: https://github.com/middlesphere/clj-terminal

[^30_2]: https://context7.com/cljfx/cljfx

[^30_3]: https://github.com/MultiMUD/clojure-lanterna

[^30_4]: https://github.com/cljfx/hn

[^30_5]: https://github.com/theophilusx/tui/blob/master/project.clj

[^30_6]: https://github.com/charmbracelet/bubbletea

[^30_7]: https://github.com/charmbracelet/bubbletea/blob/main/examples/README.md

[^30_8]: https://coldnew.github.io/5c119546/

[^30_9]: https://github.com/charmbracelet/bubbles

[^30_10]: https://anuj-seth.github.io/clojure/2018/11/22/javafx_clojure.html

[^30_11]: https://pkg.go.dev/github.com/2389-research/mammoth/tui

[^30_12]: https://qiita.com/jiro4989/items/ff52158ee070b2b99e6f

[^30_13]: https://github.com/dgroomes/bubble-tea-playground

[^30_14]: https://github.com/friemen/visuals

[^30_15]: https://pkg.go.dev/github.com/purpleclay/bubbles
