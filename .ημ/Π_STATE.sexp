(Π-state
  (timestamp "2026-07-13T04:00:00Z")
  (branch "main")
  (base-commit "7f3dd3d")
  (staged-count 37)
  (unstaged
    (docs/inbox/.#2026.07.12.10.17.57.md "Emacs lockfile symlink — not committed"))
  (untracked
    (.claude/settings.local.json "Local agent settings — not committed")
    (.mcp.json "MCP server config — not committed")
    (CLAUDE.md "Claude Code guidance — not committed")
    (opencode.json "OpenCode config — not committed")
    (.clj-kondo/imports/hiccup/ "Linter cache — not committed"))
  (test-result
    (total 583)
    (assertions 1493)
    (failures 2)
    (regressions
      (integration-suite "integration tests require running services — no assertions"
        (tests
          epiphany.integration-suite-test/services-are-reachable
          epiphany.integration-suite-test/integration-suite-is-wired)))))
