---
slug: phase-1-decision-status
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000203
title: "Phase 1 Decision Status Summary"
kind: design
description: "Tracks the current state of Phase 1 architectural decisions: which are decided, partially decided, or still open."
labels: [architecture, phase-1, decisions, status]
created: "2026-07-11"
source: "docs/inbox/2026.07.11.05.08.54.md"
---

We have resolved part of decisions **1 and 4**, and we have a strong direction for cache policy. The remaining work is to turn those directions into precise contracts: what is retained, what is only referenced, when a Git source is promoted to an archive, and how a cache proves its entry is valid.

## Current state

| Order | Decision | Current direction | Still open? |
|---|---|---|---|
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
   - They are a plausible seam between persisted ledger events and asynchronous processing, but that does not decide whether a separate queue/broker is needed. [perplexity](https://www.perplexity.ai/search/036ac07c-8e6f-456e-8d89-3c241660cfd5)

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
