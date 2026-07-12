# Π Handoff — 2026-07-11T20:07:12Z

- **Branch:** `main`
- **Commit:** `a7cbfd1`
- **Tag:** `pi/2026-07-11T20-07-12Z-a7cbfd1`
- **Files:** 33 changed (2,699 insertions, 48 deletions)
- **Tests:** 114 tests, 351 assertions, 0 failures

## New source (this Π)
- `src/epiphany/infra/repository_identity.clj` — common-git-dir resolution + .ημ identity
- `src/epiphany/domain/markdown_selection.clj` — pure selection logic
- `src/epiphany/domain/revision_at_path.clj` — pure revision-at-path logic
- `src/epiphany/law/` — Malli schemas: git, markdown, observation, registry, selection
- `src/epiphany/shape/markdown.clj` — flexmark parsing + typed span tree
- `src/epiphany/infra/git.clj` — expanded: identity, registration, commit graph, tree walk

## Tests (new)
- `test/epiphany/domain/markdown_selection_test.clj`
- `test/epiphany/domain/revision_at_path_test.clj`
- `test/epiphany/infra/git_commit_test.clj`
- `test/epiphany/infra/repository_identity_test.clj`
- `test/epiphany/law/registry_test.clj`
- `test/epiphany/shape/markdown_test.clj`
- `test/epiphany/law/fixtures/` — 6 observation fixture files (EDN + JSON)

## Modified docs
- Kanban stories updated: US-00a, US-01b1, US-01c, US-01d, US-01e, US-01f, US-02a
- Epic-05 redundancy tension review
- `board.json`, `openhax.kanban.json`

## Intentionally unstaged
- `.lsp/.cache/db.transit.json` — LSP runtime cache, not repo-relevant
