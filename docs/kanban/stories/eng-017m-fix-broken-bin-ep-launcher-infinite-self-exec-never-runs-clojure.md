---
labels: ["quality", "cli", "infra", "tooling", "regression", "phase-1"]
parent: "eng-017g-normalize-cli-and-http-command-contracts"
phase: "1"
type: "chore"
write-id: "1784662732758-0.swfq0jreeq87mfgmok"
points: "1"
verification: ["unit-test"]
risk: "low"
title: "ENG-017M: Fix broken bin/ep launcher (infinite self-exec, never runs clojure)"
priority: "P0"
status: "review"
uuid: "a1146c30-11d6-43e0-bb2a-7d5bf5cdfac8"
created_at: "2026-07-21T19:05:57.758Z"
---

# ENG-017M: Fix broken bin/ep launcher (infinite self-exec, never runs clojure)

## Intent

The documented executable entrypoint (`bin/ep --help`, per CLAUDE.md and
AGENTS.md) does not run. `bin/ep` symlinks to `bin/epiphany`, and the
committed `bin/epiphany` is:

```bash
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$script_dir/epiphany" "$@"
```

`$script_dir/epiphany` **is the script itself** — it `exec`s itself forever,
never invokes `clojure`, and produces no output (`timeout 2 bin/ep --help` →
rc=124 hang, 0 bytes; `grep -c clojure bin/epiphany` → 0). The comment
"Short alias for epiphany. Delegates to bin/epiphany" is a copy-paste artifact:
this file IS the canonical launcher, and the real launch line was never
written. Regression introduced in commit `0a99597`.

## Why this matters (cross-cutting)

This breaks every subcommand through the shipped entrypoint, not one feature.
It was surfaced during the 2026-07-21 review of ENG-004B and directly
falsifies the recorded acceptance evidence on the CLI cards
(ENG-004A/B/D and others), whose FIX comments claim "verified via
`ep show/diff/trace` against this repo." Those features actually run only via
`main/run` in tests or `clojure -M:run -- <cmd>`; the `bin/ep` path is dead.
Per PROCESS.md and ADR-004, a card whose headline verification command does
not execute cannot be accepted — so this blocks reproducible acceptance for
the whole CLI review lane.

## Scope

- Rewrite `bin/epiphany` to actually invoke the program from the repo root,
  e.g. `exec clojure -M:run -- "$@"` (resolve the project root correctly so it
  works regardless of CWD; the current `set -euo pipefail` + `script_dir`
  resolution can stay). Fix the misleading comment.
- Confirm `bin/ep` (the symlink) inherits the fix.

## Acceptance criteria

- `bin/ep --help` prints the CLI usage and exits 0 (currently hangs at rc=124).
- `bin/ep show AGENTS.md@HEAD` returns real evidence (exit 0); a missing path
  reports UNAVAILABLE with exit 1 — reproducing the CLI cards' recorded
  acceptance evidence through the shipped executable.
- A smoke check guards the launcher against re-breaking (a test or CI step that
  runs `bin/ep --help` and asserts non-hang + expected output), so this
  regression cannot silently return.

## Non-goals

- No change to `main.clj` dispatch or any subcommand behavior — this is purely
  the launcher wrapper.

## Completion evidence

`bin/ep --help` output, one real `bin/ep show` run, `git diff --stat`
(bin/ only + any smoke-test file), reviewer named at done.

---
IMPLEMENTED 2026-07-21 (board triage, same session it was filed): launcher fixed. bin/epiphany now resolves the repo root from its own location, cd's there, and `exec clojure -M:run "$@"` — it no longer exec's itself. Note: the `--` from the documented `clojure -M:run -- --help` form must NOT be forwarded by the launcher — tools.cli/clojure.main treat `--` as end-of-options, which turned `--help` into a positional "Unknown command"; dropping it fixes global flags while commands (which come first) are unaffected. Verified through the shipped binary: `bin/ep --help` → usage, exit 0 (was rc=124 hang); `bin/ep` (no args) → usage, exit 0; `bin/ep show AGENTS.md@HEAD` → real blob + OID, exit 0; `bin/ep show <missing>` → UNAVAILABLE, exit 1 — reproducing the ENG-004A/B/D acceptance evidence through bin/ep for the first time. Guard added: test/epiphany/infra/launcher_test.clj (static, no JVM boot) asserts the launcher invokes clojure via -M:run and does not exec itself — 1 test / 4 assertions green — so regression 0a99597 cannot silently return. Uncommitted in working tree. Recommend review; this is a P0 unblocker for the whole CLI review lane.
---