---
id: 01900d7c-7f3a-7e8b-9c4d-000000000007
title: "Phase 1 Cross-Cutting: Operability"
status: incoming
type: cross-cutting
priority: high
phase: 1
design: docs/notes/design/phase-1-corpus-archaeology.md
size: 8
labels: [observability, resilience, telemetry, operability]
---

# Phase 1 Cross-Cutting: Operability

Every ingestion, extraction, embedding, projection, and review decision is observable, attributable, and replayable.

## Required telemetry

- Per-run correlation ID.
- Repository, commit, blob, revision, section, and job IDs in structured logs.
- Queue depth, retry count, dead-letter count, and job latency.
- Parsing success/error rates by repository and extractor version.
- Embedding throughput, cache hit rates, GPU utilization, and index lag.
- Search latency and retrieval metrics by query class.
- Candidate edge volume, acceptance/rejection rate, and reviewer disagreement.
- Projection build time and replay time.

## Required resilience properties

- Every derived projection is disposable and rebuildable from raw blobs + events.
- Each job is idempotent using content hash plus processor/configuration version.
- Failed tasks enter a visible quarantine/dead-letter state rather than disappearing.
- Backups include raw artifacts, event records, graph/document collections, and configuration/version manifests.
- Human review is preserved as source data, not merely UI state.

## Next step

Establish the observability stack (OpenTelemetry, Prometheus, Grafana, Loki) and define the structured logging schema before Phase 1 services are built.
