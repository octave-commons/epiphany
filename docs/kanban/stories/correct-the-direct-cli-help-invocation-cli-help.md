---
category: "stories"
labels: "docs, cli, recovery"
type: "task"
write-id: "1788047750506-0.hd8jncjmu2jpqlb58es"
points: "1"
title: "Correct the direct CLI help invocation"
priority: "P2"
status: "testing"
uuid: "epiphany-issue-16-correct-direct-cli-help"
created_at: "2026-08-29T23:53:51.713Z"
---

# Correct the direct CLI help invocation

GitHub issue: https://github.com/octave-commons/epiphany/issues/16

## Outcome

Every live contributor-facing command guide names a supported Epiphany help
invocation that exits zero and prints the usage banner.

## Problem

`clojure -M:run -- --help` forwards the literal separator as the first positional
argument on current main, so Epiphany reports `Unknown command: --help` and exits
one. The shipped `bin/ep --help` entrypoint already has an executable smoke test
and is the simplest canonical command.

## Scope

- Replace the stale command in the root README and root AGENTS guide with
  `bin/ep --help`.
- Preserve command-specific examples and historical board evidence.
- Add Rheos comments to the historical parent/scaffold cards that identify the
  corrected live command without rewriting their accepted history.

## Acceptance criteria

- The live README and AGENTS guide use `bin/ep --help`.
- `bin/ep --help` exits zero and prints the Epiphany usage banner.
- No command-specific help example changes.
- The old card acceptance text remains discoverable and is superseded by an
  append-only Rheos comment rather than edited in place.

## Non-goals

No CLI dispatch, launcher, subcommand, dependency, service, or generated-board
change.

---
Accepted bounded repair: GitHub issue #16 and the constellation cleanup authorization establish scope. Preserve historical card bodies; update only live contributor docs and record supersession comments through Rheos.

Implementation plan: replace the two live root-guide occurrences with the shipped bin/ep --help entrypoint, run its executable smoke plus focused unit evidence when available, and leave generated board.json and historical acceptance text untouched.

Implementation evidence (2026-08-29): root README and AGENTS now use bin/ep --help; the PR workflow runs that command and checks both the Epiphany banner and Usage line. actionlint and git diff --check pass, and no stale direct invocation remains in those live guides. Local executable/unit runs were attempted but stopped before application startup because this harness could not resolve org.clojure:data.json through its unavailable Maven proxy at 127.0.0.1:39999; hosted CI remains required evidence.
---