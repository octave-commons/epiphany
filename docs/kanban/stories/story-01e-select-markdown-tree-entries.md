---
id: "01900d7c-7f3a-7e8b-9c4d-000000001105"
title: "ENG-001E: Select Markdown tree entries under explicit path policy"
status: "ready"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/adrs/adr-001-git-backed-resource-identity.md"
points: 2
labels: [phase-1, archaeological-ledger, engineering-breakdown]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001104"]
---

# ENG-001E: Select Markdown tree entries under explicit path policy

Turn an observed commit tree into exact candidate Markdown entries using a versioned selection policy.

## Acceptance criteria

- The policy records exact include/exclude globs and policy version; its initial default is documented rather than implicit.
- Each selected entry preserves commit OID, exact Git path string, blob OID, mode, and selection-policy identity.
- Tests cover nested paths, Unicode paths, non-Markdown files, and deleted/absent entries without path normalization.
- Selection does not check out historical revisions.
