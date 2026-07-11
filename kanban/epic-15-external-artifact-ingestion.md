---
id: 01900d7c-7f3a-7e8b-9c4d-000000000017
title: "Epic 15: External Artifact Ingestion"
status: incoming
type: epic
priority: medium
phase: 3
design: docs/notes/design/phase-3-research-operations.md
size: 8
labels: [ingestion, external, arxiv, github, huggingface]
---

# Epic 15: External Artifact Ingestion

Acquire and preserve external research artifacts as immutable, provenance-rich objects that can be reprocessed as extraction improves.

## User outcome

“A paper, repository, dataset, model card, or release can be inspected as it existed when I acquired it, alongside the metadata and source context that made it relevant.”

## Scope

Implement acquisition adapters:

- **arXiv:** metadata via API/OAI-PMH; PDF/source links where permitted and requested.
- **GitHub:** repo metadata, default branch commit, releases, README, license, issues/PRs where policy permits; webhook-driven incremental updates; API queries for bounded discovery.
- **Hugging Face:** model cards, dataset cards, repository metadata, configs, license, revisions, metadata; selective dataset samples/metadata rather than blind full downloads; model/dataset revision pinning.
- **Official documentation:** page snapshots, version, canonical URL, extraction timestamp, content hash.
- **Datasets:** catalog metadata first; schema/sample/statistics/card/license; explicit approval before materializing a large dataset locally.

## Acceptance criteria

- Each adapter produces normalized external artifacts plus raw snapshots.
- The same remote revision does not create duplicate artifact content.
- The system pins external resources to immutable references where platforms provide them.
- Large artifacts require an explicit quota/approval decision before download.
- Ingestion continues gracefully through transient API failures, rate limiting, partial content, and unavailable revisions.
- Every artifact can be re-extracted without contacting the external service again.
- Copyrighted material is stored and used only under applicable terms.

## Next step

Implement the arXiv and GitHub adapters first, then Hugging Face.
