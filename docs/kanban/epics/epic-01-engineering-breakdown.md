---
id: "01900d7c-7f3a-7e8b-9c4d-000000000001-breakdown"
title: "Epic 1: Engineering breakdown and delivery order"
status: "done"
type: "planning-record"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
decision-inputs: ["docs/adrs/adr-000-authoritative-data-boundary.md", "docs/adrs/adr-001-git-backed-resource-identity.md", "docs/adrs/adr-002-cli-rest-adapter-boundary.md"]
---

# Epic 1: Engineering breakdown and delivery order

This is a delivery map for Epic 1, not a second architecture specification. ADRs remain authoritative. Existing `US-001` through `US-006`, `US-020`, and `US-021` remain product-outcome cards; the `ENG-001*` cards are the smaller engineering slices that make those outcomes implementable.

## Dependency graph

```text
ENG-001A Mongo registration observation ──> ENG-001B direct register command
                                      └──> ENG-001C registration schemas
                                               └──> ENG-001D commit traversal
                                                        └──> ENG-001E Markdown selection
                                                                 └──> ENG-001F revision-at-path observations
ENG-001D + ENG-001F ───────────────────────────────────────────> ENG-001G ingestion/checkpoints
ENG-001D + ENG-001G ───────────────────────────────────────────> ENG-001H replacement evidence
ENG-001F ──────────────────────────────────────────────────────> ENG-001I diff lineage candidates
ENG-001G + ENG-001H ───────────────────────────────────────────> ENG-001J operational diagnostics
```

## Delivery gates

### Gate 0: executable baseline

`clojure -M:test` is green; direct-mode composition and the Mongo integration-test configuration are explicit. This is prerequisite infrastructure, not ledger behavior.

### Gate 1: registration is trustworthy

Complete ENG-001A and ENG-001B. A user can register a normal repository, bare repository, or linked worktree; the system retains a minimal Git-local resource ID and an idempotent Mongo location observation. No history ingestion is implied.

### Gate 2: Git facts are observable

Complete ENG-001C through ENG-001F. For a fixed source/ref policy, the system can reproduce commit and selected Markdown-revision observations from Git object access, preserving exact path strings and object identifiers.

### Gate 3: runs can recover

Complete ENG-001G, ENG-001H, and ENG-001J. Interrupted processing, unavailable sources, and changed reachability have durable, inspectable evidence; no prior fact is overwritten.

### Gate 4: revision-level lineage evidence

Complete ENG-001I. Git diff produces policy-versioned candidate evidence only. It does not establish document or idea identity.

## Definition of ready

Do not start an ENG card unless its ADR inputs are accepted (or an explicit unresolved decision is named), its dependency cards are complete, its source authority is stated, its adapter/test environment is identified, and its acceptance criteria can be demonstrated without implementing a later card.

## Definition of done

A completed card includes its domain/adapter tests, provenance and status semantics, direct-mode error behavior, and any required Mongo indexes/configuration. It must not broaden Phase 1 scope by adding an HTTP service, queue, vector store, object-storage service, or semantic inference not named by the card.
