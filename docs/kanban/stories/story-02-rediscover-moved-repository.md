---
id: "01900d7c-7f3a-7e8b-9c4d-000000001002"
title: "US-002: Rediscover a moved repository"
status: "incoming"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 1
labels: [git, ingestion, provenance, identity, relocation]
category: "stories"
---

# US-002: Rediscover a moved repository

## Acceptance Criteria

- Given a repository moved to a new filesystem location with its `.git/corpus-archaeology/repository.edn` retained, registering/scanning the new path resolves the existing `:resource-id`.
- The system records the new path as a new location observation rather than creating a duplicate source.
- The old location is retained as historical observation and marked unavailable only after a failed accessibility check.
- The system never uses the absolute filesystem path as the repository's primary identity.
- If a copied repository carries the same `:resource-id` but does not share expected Git object/history evidence, the system flags an identity conflict for review rather than merging silently.

## Notes

**As a corpus owner,** I want a registered local repository to retain its platform identity after I move its directory.
