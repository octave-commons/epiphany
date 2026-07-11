---
title: "Deep Research: Vector Search, Hybrid Retrieval, and Knowledge Graph Platforms"
slug: vector-search-and-knowledge-graphs-deep-research
created: 2026-07-11
kind: research
source_note: docs/notes/research/vector-search-knowledge-graphs.md
design_docs:
  - docs/notes/design/knowledge-platform-overview.md
  - docs/notes/design/phase-1-corpus-archaeology.md
  - docs/adrs/ADR-000.md
---

# Deep Research: Vector Search, Hybrid Retrieval, and Knowledge Graph Platforms

This note synthesizes external research on the platform’s search, vector, and knowledge-graph choices. It evaluates the claims in the existing research note and design documents, identifies contradictions, and offers a recommendation for the ADR-000 vs. knowledge-platform-overview stack debate.

## Research questions

1. Does research support combining lexical (BM25) and dense-vector signals in hybrid search? What are the best practices for rank fusion and score normalization?
2. How capable is Elasticsearch as a vector search engine? When should a dedicated vector database be preferred?
3. For a 4-node homelab property-graph workload, what are the tradeoffs between Neo4j and ArangoDB for traversal performance, query expressiveness, and operational complexity?
4. What are proven methods and failure modes for building knowledge graphs from text and code, and for graph-enriched retrieval?
5. When is a multi-model database (ArangoDB) justified versus best-of-breed components (Elasticsearch + Neo4j + PostgreSQL)?

## Summary of findings per subtopic

### 1. Hybrid search: BM25 + dense vectors + metadata filtering

Research consistently supports hybrid retrieval. Pinecone’s work cites an internal study showing that hybrid vectors outperform keyword-only and semantic-only search both in-domain and out-of-domain, and Nils Reimers (Sentence Transformers) notes that combining semantic search with BM25 solves many keyword-specific shortcomings [[Pinecone hybrid search](https://www.pinecone.io/learn/hybrid-search/)]. Elastic’s documentation also states that hybrid search can be “much better than just the sum of those parts,” because lexical and dense retrieval capture different failure modes: lexical search misses vocabulary mismatch, while dense search can miss rare or domain-specific terms [[Elastic hybrid search overview](https://www.elastic.co/search-labs/blog/hybrid-search-elasticsearch)].

Best-practice for fusion is not settled. The two dominant approaches are:

- **Reciprocal Rank Fusion (RRF)** — rank-based, no score normalization, robust to different score scales, but the constant `k` and result-window size affect quality.
- **Convex Combination (CC)** / linear combination of scores — requires normalized scores and per-dataset weight tuning, but can be more effective.

Bruch, Gai, and Ingber’s analysis (2022, revised 2023) found that **CC outperforms RRF in both in-domain and out-of-domain settings**, is sample-efficient, and that the choice of score normalization is less important for CC than often assumed. They also found RRF is more sensitive to its parameters than usually claimed [[arXiv:2210.11934](https://arxiv.org/abs/2210.11934)]. Benham et al. (2018) provide the theoretical basis for RRF and rank fusion [[arXiv:1811.06147](https://arxiv.org/abs/1811.06147)].

Elastic offers both: the `retriever` API supports `standard` (lexical), `knn` (dense), and `rrf` retrievers, plus a `linear` retriever for weighted score combination, and as of 2025 weighted RRF with per-retriever weights [[Elastic RRF blog](https://www.elastic.co/search-labs/blog/weighted-reciprocal-rank-fusion-rrf)][[Elastic hybrid search](https://www.elastic.co/search-labs/blog/hybrid-search-elasticsearch)].

Metadata filtering is supported by modern vector systems through payload indexes that extend the HNSW graph (Qdrant) or by combining `bool`/`filter` clauses with `knn` queries (Elasticsearch) [[Qdrant overview](https://qdrant.tech/documentation/overview/)][[Elastic dense vector docs](https://www.elastic.co/docs/solutions/search/vector/dense-vector)].

### 2. Elasticsearch for vector search vs. dedicated vector databases

Elasticsearch has become a credible vector database. As of version 8.15 it supports:

- `dense_vector` fields with HNSW (`hnsw`), flat (`flat`), `int8_hnsw`, `int4_hnsw`, and bit vectors.
- Better Binary Quantization (BBQ), claimed to shrink vectors ~29× without large recall loss.
- `knn` query and `semantic_text` field type for automatic model management and chunking.
- Hybrid search via `retriever` API with RRF, linear combination, and weighted RRF.
- Multi-segment parallelism, SIMD acceleration, and disk-oriented vector search (DiskBBQ) [[Elastic vector improvements](https://www.elastic.co/search-labs/blog/vector-search-improvements)][[Elastic dense vector docs](https://www.elastic.co/docs/solutions/search/vector/dense-vector)].

A 2026 Elastic benchmark on network-attached storage claimed Elasticsearch DiskBBQ achieved up to 7× higher throughput than Qdrant 1.18.1 at comparable recall, though this is vendor-authored and should be treated cautiously [[Elastic vs. Qdrant benchmark](https://www.elastic.co/search-labs/blog/vector-search-benchmark-elasticsearch-vs-qdrant)].

When to prefer a dedicated vector database (Qdrant, Weaviate, Pinecone, pgvector):

- Vector search is the primary workload and the corpus is expected to grow to tens or hundreds of millions of vectors.
- You need model-agnostic embedding, multi-tenancy, or specialized quantization tuned for vectors.
- You want to avoid licensing limitations of Elastic’s enterprise-only vector features (e.g., DiskBBQ).
- You need the simplest possible operational footprint for a vector-only service.

For a personal/team knowledge platform with mixed text, code, metadata, temporal, and geospatial queries, keeping lexical + vector + filtering in one engine reduces integration and is consistent with the existing design rationale.

### 3. Knowledge graph stores: Neo4j vs. ArangoDB in a 4-node homelab

Both Neo4j and ArangoDB implement property graphs. The DB-Engines ranking (July 2026) places Neo4j first among graph DBMS with a popularity score of 50.49, while ArangoDB is fourth with 3.54 [[DB-Engines graph ranking](https://db-engines.com/en/ranking/graph+dbms)].

Neo4j advantages:

- Mature Cypher ecosystem, extensive graph data science (GDS) library, and strong visualization tools.
- Native graph storage and index-free adjacency optimized for traversal-heavy workloads.
- Large community, proven production deployments, and extensive documentation.

ArangoDB advantages:

- Multi-model: documents, key-value, graph, search, and (more recently) vectors in one engine with one query language (AQL).
- Can reduce component count and cross-system data movement.
- Provides named graphs, anonymous graphs, traversals, shortest-path algorithms, Pregel analytics, and enterprise graph sharding features (SmartGraphs / EnterpriseGraphs) [[ArangoDB graphs docs](https://docs.arangodb.com/3.11/graphs/)].

Tradeoffs for a 4-node homelab:

- **Traversal performance:** Neo4j’s native graph engine is generally regarded as stronger for deep, unpredictable traversals. ArangoDB’s graph layer is built on top of its document/edge collections; it is fast for many workloads but can lag on complex graph analytics unless SmartGraphs or SatelliteGraphs are used.
- **Query expressiveness:** Cypher is graph-centric and concise for pattern matching. AQL is SQL-like and flexible across models but can be more verbose for pure graph queries.
- **Operational complexity:** ArangoDB promises fewer moving parts. Neo4j adds another stateful service but is operationally well understood and mature.
- **Resource constraints:** Total 64 GB RAM across four nodes is modest. Running ArangoDB as a multi-model primary may consume more RAM than running smaller, specialized stores. The design overview correctly notes that the two weak nodes should not host memory-heavy services.

### 4. Building knowledge graphs from text and code

Proven methods:

- **Entity/relation extraction:** Rule-based or ML-based Named Entity Recognition (NER) and relation extraction (RE) using BERT-family models or fine-tuned language models, followed by entity linking and coreference resolution. LLMs are increasingly used for open-domain extraction into structured schemas.
- **Ontology design:** Define entity types, relation types, and constraints before extraction; iterate with domain experts. IBM’s summary emphasizes that schemas, identities, and context give structure to heterogeneous data [[IBM knowledge graph](https://www.ibm.com/think/topics/knowledge-graph)].
- **Graph-enriched retrieval:** Microsoft’s GraphRAG work (Edge et al., 2024) uses an LLM to build an entity knowledge graph from source documents, then pre-generates community summaries for groups of related entities. At query time, partial answers from community summaries are combined into a final response. This improves global, query-focused summarization over conventional RAG for “main themes”-style questions [[arXiv:2404.16130](https://arxiv.org/abs/2404.16130)].

Failure modes:

- **Hallucinated entities/relations:** LLM extraction can invent nodes or edges that do not reflect the source text.
- **Ontology drift:** Without a stable schema, different extraction runs produce incompatible entity/relation types.
- **Over-reliance on similarity:** A vector or lexical similarity edge is not the same as a causal, temporal, or dependency relationship.
- **Noise at scale:** Dense text extraction yields many candidate edges; ranking and review are essential before accepting them.
- **Code-specific issues:** Tree-sitter gives syntax structure, not semantics. For Clojure, `clj-kondo` analysis is needed; for other languages, native analyzers may be required. Dynamic dispatch, macros, and metaprogramming create statically unresolved edges that must be marked as uncertain.

The design documents already capture most of these failure modes: the platform treats LLM outputs as candidates requiring structured output and provenance, and distinguishes observed, inferred, and human-accepted edges.

### 5. Multi-model database vs. best-of-breed components

ArangoDB’s marketing case for multi-model is reduced integration and fewer moving parts. ArangoDB combines document, graph, key-value, full-text search, and vector search with a single AQL interface and provides GraphRAG/AutoGraph tooling [[ArangoDB docs](https://docs.arangodb.com/)].

Best-of-breed arguments:

- Each engine is mature and independently tunable for its primary workload.
- Elasticsearch is a stronger search/relevance engine with decades of Lucene optimization.
- Neo4j is a more mature graph engine with a richer graph-algorithm ecosystem.
- PostgreSQL is a stronger transactional/relational/event store than a multi-model document layer.
- Avoiding a single-vendor stack reduces coupling and makes future migration easier.

When multi-model is justified:

- The workload is genuinely mixed but not extreme in any single dimension.
- Operational simplicity (fewer clusters, fewer query languages, fewer backup strategies) outweighs peak capability in any one area.
- The team values a single, consistent data platform over specialized tooling.
- Licensing and resource constraints are acceptable (ArangoDB Community Edition has a 100 GB cluster limit and changed to Business Source License in 2023) [[ArangoDB Wikipedia](https://en.wikipedia.org/wiki/ArangoDB)].

## Evaluation of the design claims

### Claims that are supported

- **Hybrid retrieval is the right default.** The research notes and design documents are consistent with the literature. Combining BM25, dense vectors, and metadata filters is well supported, and RRF/linear combination are the standard fusion methods.
- **Elasticsearch can handle the vector + lexical + geospatial workload.** The platform’s Phase 1 design to start with Elasticsearch for all three is technically sound, avoiding premature vector-DB fragmentation.
- **Graph traversal is a core capability, not an afterthought.** Neo4j is the right choice for a relationship-first investigation engine.
- **Provenance and candidate-status are essential.** The research on GraphRAG and KG construction confirms that unreviewed LLM/extracted edges are unreliable and must be labeled with status, confidence, and source spans.

### Claims that are contradicted or overstated

- **Knowledge-platform-overview’s casual preference for ArangoDB is not well justified by the research for Phase 1.** The document itself notes that the formalized stack is in ADR-000 and should be reconciled. The research supports that reconciliation: for a search-heavy, graph-heavy, event-sourced platform, best-of-breed engines are better justified than a unified multi-model store.
- **The research note’s suggestion that a “separate vector database is justified later” is not yet contradicted**, but it should be framed as “only if Elasticsearch becomes a measured bottleneck,” because current ES capabilities are strong enough for the first phases.
- **Any implication that the KG can be built primarily by LLMs without human review is contradicted.** GraphRAG and entity-extraction research show that LLM-built graphs are useful but require schema control, validation, and review.

### Uncertain claims

- **Elasticsearch vector performance vs. Qdrant on the actual homelab hardware.** The Elastic benchmark is vendor-authored and used network-attached storage. The platform’s 500 Mbps LAN and 64 GB total RAM may produce different results. A local benchmark with the actual corpus is required before deciding to migrate vectors to a dedicated store.
- **Neo4j vs. ArangoDB traversal performance on the specific corpus graph.** No directly applicable benchmark was found; the platform should measure both if a consolidation experiment is conducted later.
- **NPU utility.** Research notes mention the NPU but current ecosystems (as of mid-2026) are still maturing; this is a known uncertainty also captured in ADR-000.

## Gaps or missing considerations

1. **No hybrid-ranking benchmark plan.** The design documents call for evaluation but do not specify a dataset or metrics for Phase 1. Recommend adopting a 30–50 question set (already suggested) and measuring Recall@k, MRR, and human relevance scores for lexical-only, vector-only, RRF, and linear-combination runs.
2. **No explicit model selection for embedding/reranking.** The choice of embedding model (e.g., all-mpnet-base-v2, E5, GTE) and reranker materially affects hybrid results. This should be specified before implementation.
3. **Sparse learned retrieval (SPLADE/ELSER) is mentioned but not evaluated.** SPLADE-style sparse models can outperform dense models on first-stage retrieval without a separate keyword index. Elastic ELSER and open SPLADE models are options to consider alongside BM25+dense.
4. **Vector quantization strategy.** With 64 GB RAM total, quantization (int8, int4, or BBQ) is likely essential. The design should specify whether vectors are stored quantized or in full precision with quantization at query time.
5. **Graph update throughput and concurrency.** Event-sourced projections will write many graph edges from worker queues. The design should estimate write throughput and decide whether Neo4j’s transactional model will keep up, or whether batch projection windows are needed.
6. **Backup and disaster recovery for projections.** The design correctly states projections are rebuildable, but does not specify how long a full rebuild takes and how that affects backup policy.

## Open questions

1. What is the measured hybrid search quality of the actual corpus on Elasticsearch vs. Qdrant/Weaviate? Is the difference large enough to justify a second vector engine?
2. What is the memory/disk footprint of Elasticsearch dense-vector indices at the projected corpus size, and does quantization keep the working set within the strong-node RAM budget?
3. Does Neo4j Community Edition provide enough capabilities, or will the platform need Enterprise Edition features (e.g., clustering, GDS) for a 4-node deployment?
4. What is the expected graph size and traversal depth for lineage queries? Will the Ryzen 7/16 GB node be sufficient?
5. Which extraction pipeline will be used for entities/relations from Markdown and code: rule-based, fine-tuned model, or LLM-based schema-constrained extraction? What is the cost per document?

## Recommendations for the design

1. **Adopt ADR-000’s stack (Elasticsearch + Neo4j + PostgreSQL) as the Phase 1–2 architecture.** The research supports this over the knowledge-platform-overview’s ArangoDB preference. The best-of-breed stack gives each primary concern a mature, independently tunable engine and aligns with the evidence-first, projection-rebuildable philosophy of the design.
2. **Keep Elasticsearch as the sole vector/lexical/geospatial engine for Phase 1.** Do not add a dedicated vector database until a measured bottleneck appears. The current Elasticsearch feature set is sufficient for the initial corpus size and mixed retrieval needs.
3. **Implement hybrid ranking with both RRF and a calibrated linear-combination option.** Start with RRF because it is parameter-light and requires no score normalization. Add linear combination once a curated query set exists to tune weights; follow Bruch et al. and use only a small labeled set for calibration.
4. **Use Neo4j for the property graph.** It is the most mature property-graph engine for the traversal-heavy, lineage-oriented questions in the platform. Revisit ArangoDB only if operational consolidation becomes a measured priority and a local benchmark shows it does not regress search or graph quality.
5. **Treat LLM-based KG extraction as a candidate-generation stage, not a source of truth.** Use schema-constrained extraction, record provenance, require human review for accepted edges, and seed the graph with deterministic, tool-derived edges (Git parentage, section containment, symbol references) before adding LLM-derived ones.
6. **Plan a local benchmark as an early Phase 1 task.** Compare lexical-only, vector-only, and hybrid retrieval on the 30–50 question set, and record the memory/disk cost of each approach. This will answer whether ES remains sufficient or whether a dedicated vector store should be revisited.

## References

- Bruch, S., Gai, S., & Ingber, A. (2022/2023). *An Analysis of Fusion Functions for Hybrid Retrieval*. arXiv:2210.11934. https://arxiv.org/abs/2210.11934
- Benham, R., Mackenzie, J., Moffat, A., & Culpepper, J. S. (2018). *Boosting Search Performance Using Query Variations*. arXiv:1811.06147. https://arxiv.org/abs/1811.06147
- Edge, D., et al. (2024). *From Local to Global: A Graph RAG Approach to Query-Focused Summarization*. arXiv:2404.16130. https://arxiv.org/abs/2404.16130
- Elastic. *Vector search improvements in Elasticsearch & Lucene*. https://www.elastic.co/search-labs/blog/vector-search-improvements
- Elastic. *Dense vector search in Elasticsearch*. https://www.elastic.co/docs/solutions/search/vector/dense-vector
- Elastic. *Elasticsearch hybrid search*. https://www.elastic.co/search-labs/blog/hybrid-search-elasticsearch
- Elastic. *Weighted reciprocal rank fusion (RRF) in Elasticsearch*. https://www.elastic.co/search-labs/blog/weighted-reciprocal-rank-fusion-rrf
- Elastic. *Elasticsearch DiskBBQ delivers 7x faster vector search than Qdrant on network-attached storage*. https://www.elastic.co/search-labs/blog/vector-search-benchmark-elasticsearch-vs-qdrant
- Weaviate. *Hybrid Search Explained*. https://weaviate.io/blog/hybrid-search-explained
- Pinecone. *Introducing the hybrid index to enable keyword-aware semantic search*. https://www.pinecone.io/learn/hybrid-search/
- Qdrant. *Overview*. https://qdrant.tech/documentation/overview/
- ArangoDB. *Graphs documentation*. https://docs.arangodb.com/3.11/graphs/
- DB-Engines. *Ranking of Graph DBMS*. https://db-engines.com/en/ranking/graph+dbms
- IBM. *What is a knowledge graph?* https://www.ibm.com/think/topics/knowledge-graph
- Wikipedia. *ArangoDB*. https://en.wikipedia.org/wiki/ArangoDB
- Formal, T., Piwowarski, B., & Clinchant, S. (2021). *SPLADE: Sparse Lexical and Expansion Model for First Stage Ranking*. arXiv:2107.05720. https://arxiv.org/abs/2107.05720
