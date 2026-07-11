---
id: 01900d7c-7f3a-7e8b-9c4d-000000000016
title: "Epic 14: Governed External Source Registry"
status: incoming
type: epic
priority: medium
phase: 3
design: docs/notes/design/phase-3-research-operations.md
size: 8
labels: [governance, sources, registry, policy]
---

# Epic 14: Governed External Source Registry

Establish a source registry, access policies, trust tiers, and acquisition contracts before connecting the system to external feeds.

## User outcome

“I can see exactly what the system is allowed to collect, why it collected it, what it is permitted to retain, and how much confidence to assign to it.”

## Scope

- Create a versioned source registry for arXiv, GitHub, Hugging Face, official documentation, and selected open-data sources.
- Define per-source policies: access method/API, rate limit, polling/webhook schedule, allowed artifact types, retention policy, license/terms capture, trust tier, review requirement, and max disk/bandwidth budget.
- Add trust categories: `:primary-source`, `:peer-reviewed`, `:preprint`, `:official-project`, `:maintained-open-source`, `:dataset-card`, `:community-discussion`, `:model-generated-summary`, `:unverified`.
- Require every external artifact to record its acquisition reason.

## Acceptance criteria

- No external fetch occurs without a source-registry entry and policy.
- Every artifact records URL/canonical ID, acquisition time, content hash, source class, trust tier, license/terms metadata, and acquisition reason.
- Every acquisition is rate-limited, cache-aware, and resumable.
- Conditional fetches avoid re-downloading unchanged resources where APIs support ETags/modified timestamps.
- The user can pause, revoke, or purge a source policy without deleting unrelated evidence.
- The system exposes bandwidth, storage, error, retry, and rate-limit dashboards by source.
- A source’s trust tier can be revised without rewriting raw artifacts.

## Domain rule

Source trust is not claim truth. A peer-reviewed paper may still be wrong; an unreviewed GitHub issue may correctly identify a critical bug.

## Next step

Design the source registry schema and policy DSL.
