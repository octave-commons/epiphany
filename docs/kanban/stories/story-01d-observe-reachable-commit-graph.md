---
id: "01900d7c-7f3a-7e8b-9c4d-000000001104"
title: "ENG-001D: Observe a bounded reachable Git commit graph"
status: "ready"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/adrs/adr-001-git-backed-resource-identity.md"
points: 3
labels: [phase-1, archaeological-ledger, engineering-breakdown]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001103"]
---

# ENG-001D: Observe a bounded reachable Git commit graph

Traverse an explicitly selected ref set and emit immutable commit observations.

## Acceptance criteria

- Given a fixture repository and fixed selected refs, traversal order and emitted commit set are deterministic.
- Each observation retains commit OID, parent OIDs, tree OID, author/committer identity and timestamps, message bytes/text policy, selected ref context, and observed time.
- Existing commit observations are reused by immutable Git identity; reruns add an ingestion-run observation rather than rewriting source facts.
- Malformed/unreadable object failures are recorded per item and do not conceal completed observations.

## Out of scope

Do not infer repository-family equivalence, file rename continuity, or document identity.
