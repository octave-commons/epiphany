---
id: "01900d7c-7f3a-7e8b-9c4d-000000001301"
title: "ENG-003A: Build the Lucene lexical index over sections"
status: ready
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000003"
design: "docs/kanban/epics/epic-03-retrieval-substrate.md"
points: 5
labels: [phase-1, search, lucene, lexical]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001202"]
---
# ENG-003A: Build the Lucene lexical index over sections

Index section text, heading path, exact path, front-matter fields, tags, and commit messages. Rebuildable, versioned.

## Acceptance criteria

- Exact phrases and heading terms retrieve the expected sections from fixtures.
- The index is fully rebuildable from Mongo records + Git blobs; rebuild is a command, not a migration.
- Index schema changes bump an index version; stale indexes are detectable.
- Unicode paths and text index and retrieve correctly.
