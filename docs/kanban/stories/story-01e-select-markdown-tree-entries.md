---
id: "01900d7c-7f3a-7e8b-9c4d-000000001105"
title: "ENG-001E: Select Markdown tree entries under explicit path policy"
status: "done"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/adrs/adr-001-git-backed-resource-identity.md"
points: 2
labels: ["phase-1", "archaeological-ledger", "engineering-breakdown"]
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

---
Implemented. All acceptance criteria met: (1) selection/policy schema records include/exclude globs + version; default v1 documented as markdown-tree-v1; (2) selection/entry preserves commit-oid, path-raw, blob-oid, mode, policy-version; (3) tests cover nested paths, Unicode .ημ/ paths, non-Markdown exclusion, deleted entries, no path normalization; (4) tree walking via JGit never checks out revisions. Files: law/selection.clj, domain/markdown_selection.clj, infra/git.clj (commit-tree-entries), test/epiphany/domain/markdown_selection_test.clj. 94 tests, 301 assertions, 0 failures. --tasks-dir docs/kanban

Implementation complete. Created law/selection.clj (policy + entry schemas), domain/markdown_selection.clj (glob matching + selection), added commit-tree-entries to infra/git.clj. 22 tests covering nested paths, Unicode, non-Markdown, deleted entries. 94 tests, 301 assertions, 0 failures. Ready for review. --tasks-dir docs/kanban

Final review approved. All acceptance criteria met, 94 tests, 301 assertions, 0 failures. Moving to done. --tasks-dir docs/kanban
---
