---
slug: phase-1-cache-policy
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000204
title: "Phase 1 Cache Policy: LMDB and Redis"
kind: design
description: "Defines LMDB as the default embedded Phase 1 cache and Redis as an explicitly justified shared cache capability."
labels: [architecture, phase-1, cache, lmdb, redis]
created: "2026-07-11"
source: "docs/inbox/2026.07.11.05.01.59.md"
---

Exactly. **LMDB should be the default Phase 1 cache primitive; Redis should be an explicitly justified shared-cache capability, not infrastructure we deploy because “distributed system.”** LMDB is embedded and memory-mapped, so it gives each worker fast local key-value caching without introducing a separate network service or operational dependency. [lmdb.readthedocs](https://lmdb.readthedocs.io/en/latest/)

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
|---|---|---|---|
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

Redis supports memory-limit eviction policies such as LRU, LFU, TTL-based eviction, random eviction, and no-eviction; that flexibility is useful once you have a specific shared-cache workload and an explicit loss/eviction policy. [redis](https://redis.io/docs/latest/operate/rs/databases/memory-performance/eviction-policy/)

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

Otherwise, routing a worker through Redis can add another network hop without reducing total latency. Redis also supports server-assisted client-side caching, which can reduce repeated remote Redis reads while coordinating invalidation, but that is a later optimization—not a Phase 1 dependency. [redis](https://redis.io/docs/latest/develop/reference/client-side-caching/)

## Practical Phase 1 stance

I would record this as a preliminary architecture position, not a final full ADR:

> **Phase 1 uses per-node LMDB caches for rebuildable, operation-specific artifacts. Redis is not deployed by default. A shared Redis cache may be introduced for a measured cross-node reuse, coordination, or remote-rate-limit problem, with one documented keyspace, TTL, size budget, and invalidation/eviction policy per use case.**

That gives you a simple default and leaves a clean door for distributed caching when the cluster proves it needs it.
