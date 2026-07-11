---
id: "01900d7c-7f3a-7e8b-9c4d-000000000000-a"
title: "US-000A: Project scaffold, executable entry point, and green test baseline"
status: "review"
type: "story"
priority: "P0"
phase: 0
parent: "01900d7c-7f3a-7e8b-9c4d-000000000000"
points: 4
labels: ["bootstrap", "clojure", "build", "testing", "phase-0"]
category: "stories"
dependency: []
---

# US-000A: Project scaffold, executable entry point, and green test baseline

First slice of US-000. After this card, a fresh clone can run the REPL, the tests, and the executable — nothing else.

## Scope

- Add `deps.edn` as the canonical Clojure CLI dependency and alias manifest with `:test`, `:unit-test`, `:integration-test`, `:repl`, `:dev`, and `:run` aliases.
- Establish the source topology: `src/domain`, `src/infra`, `src/law`, `src/shape`, `test`, `dev`, `bin`.
- Provide the single executable entry point for `epiphany` and its `ep` alias; it may only report version/help at this stage.
- Add a test runner and one passing smoke test so `clojure -M:test` is the green baseline.
- Document supported JDK/Clojure versions and the exact bootstrap, test, and run commands.

## Out of scope

Profiles, ports, adapters, and local services — those are US-000B and US-000C. No HTTP, TUI, vector, graph, queue, or browser services.

## Acceptance criteria

- On a supported JDK, a fresh clone succeeds with `clojure -M:test` without manually editing local files.
- `clojure -M:run -- --help` succeeds and identifies the canonical executable as `epiphany`; `ep` invokes the same entry point.
- `clojure -M:repl` starts a usable REPL with production code on its classpath and does not require external services.
- `clojure -M:unit-test` runs tests that require no Docker or network access.
- CI can execute the unit-test baseline headlessly.

---
US-000A implemented 2026-07-11. deps.edn rewritten to contract: Clojure 1.12.2, kaocha, tools.cli, malli; aliases :test/:unit-test/:integration-test/:repl/:dev/:run (cognitect runner + dead :test-runner alias removed). Added epiphany.infra.main (--help/--version via tools.cli), bin/epiphany + bin/ep symlink, dev/user.clj, tests.edn (unit skips ^:integration, integration focuses it), .gitignore, resources/, GitHub Actions unit-test workflow, README Development section. Evidence: clojure -M:test = 22 tests/43 assertions/0 failures; -M:unit-test = 21/42/0; -M:integration-test = 1/1/0; -M:run -- --help exit 0 and identifies epiphany; bin/ep --version exit 0; -M:repl starts nREPL; clj-kondo 0 warnings; cljfmt clean (2 pre-existing files auto-fixed). ANOMALIES (logged, not absorbed): (1) tree already contained registration/repo-identity code with 17 passing tests despite 'no application code yet' — kept as-is, belongs to ENG-001A/B1/B2 review; (2) epiphany.application/ layer exists outside the four-quadrant namespace law (domain/infra/shape/law); (3) infra/git.clj shells out to git — AGENTS.md mandates JGit, ENG-001B1's concern; (4) integration suite anchored by a placeholder wiring test until US-000C delivers real UNAVAILABLE readiness semantics. --tasks-dir docs/kanban
---
