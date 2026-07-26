---
category: "chores"
labels: ["kanban", "tooling", "muse", "cross-repo", "process"]
dependency: [""]
type: "chore"
write-id: "1784693680984-0.gwab3za11qviwadnkh2"
title: "CHORE: Rebase the kanban done-gate on Rheos parsing, a shared muse hook, and ledger-sourced state"
priority: "P2"
status: "incoming"
id: "6d1febaa-7057-464e-a469-c0efc29559bc"
design: "docs/process/review-and-acceptance.md"
---

# CHORE: Rebase the kanban done-gate on Rheos parsing, a shared muse hook, and ledger-sourced state

Captured from a 2026-07-20 board walk, at the user's direction. Not an
emergency fix — `bin/kanban-done-gate` works as it stands today (it blocks
the phantom-CLI/no-evidence pattern it was built for). This card captures
follow-on rework the user flagged while reviewing it, to go through the
normal triage/review process rather than being hand-patched mid-conversation.

## Findings (verified this session, not assumed)

1. **`bin/kanban-done-gate` reimplements card parsing in Python that already
   exists, twice over, in the Rheos backend it's supposed to complement.**
   `rheos.backend.shape.content-parser` (`eta-mu/packages/rheos/src/rheos/backend/shape/content_parser.cljs`)
   parses frontmatter + body/comment sections; `rheos.backend.infra.task-store`
   (`.../infra/task_store.cljs`) has its *own* independent frontmatter parser
   on top of that. The done-gate's inline `python3 -c` heredoc is a third,
   bash-embedded reimplementation of the same `---`-fenced frontmatter/body/
   comment split. The gate's own comment says it avoids "the eta-mu CLI"
   because that binary is stale post-Rheos-cutover — true, but conflates the
   unreliable *CLI binary* with the *Rheos MCP/HTTP server*, which is running
   (`rheos.service`) and already serves parsed task content correctly (see
   `kanban_read_task` in this same session). The gate should ask Rheos for
   the parsed card (HTTP/MCP), not re-parse the file from disk in a fourth
   language.

2. **The enforcement hook only exists for Claude Code.** `kanban-mcp-status-gate.sh`
   and `kanban-direct-edit-guard.sh` are wired solely via `.claude/settings.json`
   `PreToolUse` matchers. There is no OpenCode-side equivalent, so the same
   `kanban_update_status`/direct-edit path from an OpenCode session is
   unenforced. Muse already has the mechanism for exactly this: the
   `eta_mu.dsl/defhook` macro (`muse/src/cljs/eta_mu/dsl.cljc`) defines a
   hook once, as data (`{:ημ/kind :hook :event ... :handler ...}`), that the
   muse build compiles into native hooks for every target it publishes to —
   Claude Code *and* OpenCode (see the `claude-integration` / `muse-plugin-authoring`
   skills). The gate belongs there as a muse-authored plugin hook, not as a
   `.claude/`-scoped bash script.

3. **The kanban board's state is not actually reproducible from a ledger
   today**, despite Rheos already having the pieces for it:
   `rheos.backend.domain.events` + `rheos.backend.infra.ledger` append every
   status-change/frontmatter/comment mutation to an EDN-file ledger at
   `<board-dir>/.events/ledger.edn` (`emit-status-change!`, `query-events`,
   etc.) and fan it out over an in-process pub/sub bus for the SSE stream.
   But `rheos.backend.infra.task-store` loads board/task state directly from
   the markdown files, independent of the ledger — the ledger is a side
   audit trail today, not the source state is replayed from. If the intent
   (stated this session) is that board state should be reconstructable by
   replaying the ledger, that's real, currently-unmet scope, not something
   already covered by the existing events/ledger code.

## Explicitly NOT in scope / not being revisited

- The requirement that `ep show`/`ep diff`/`ep trace`/`ep inbox`/`ep export`
  actually exist as wired CLI commands (the thing the done-gate's dispatch
  check enforces) was read correctly by the 2026-07-12/13 audits and by the
  gate itself. This card does not loosen that; it only changes *how* the
  gate gets its facts and *where* it's wired.

## Suggested shape (for whoever triages/points this — likely needs splitting per hard rule 1)

- Replace the done-gate's Python parsing with a call to the running Rheos
  server for the card's parsed frontmatter/body/comments; fall back
  loudly (not silently) if Rheos is unreachable rather than re-parsing.
- Author the status-gate + direct-edit-guard checks as muse `defhook`
  plugin(s) so the same enforcement reaches Claude Code and OpenCode from
  one definition; retire `.claude/hooks/kanban-*.sh` once the muse-published
  hook covers the same matchers.
- Decide, as its own explicit design question (not assumed): should Rheos's
  `task-store` be rebuilt to derive board state by replaying
  `domain/events`'s ledger (true event-sourcing), or is the ledger meant to
  stay a parallel audit/SSE feed alongside file-truth? Whichever is chosen,
  make it explicit in Rheos's own docs — right now it's ambiguous by omission.

## Dependencies and interfaces

- Touches three repos: `epiphany` (`bin/kanban-done-gate`, `.claude/hooks/`),
  `eta-mu` (`packages/rheos/src/rheos/backend/{shape,infra,domain}/*`), and
  `muse` (a new `defhook`-based plugin, published via the existing
  Claude/OpenCode build targets).
- No code changes made yet — this card is the capture step; implementation
  is separate work once pointed.

---
FOLLOW-UP 2026-07-21/22 (discussion + code reading, no changes made): re-surfaced this card while thinking through a same-session incident (several `git stash`/`git stash pop` calls raced against Rheos's concurrent writes to this project's tracked `docs/kanban/.events/ledger.edn` and story `.md` files, silently reverting 7 cards' review dispositions back to an earlier state — recovered by hand, no code lost, only kanban metadata). That incident is real, concrete evidence for finding #3 below, not a hypothetical.

## Findings #1 and #2 are already implemented, verified by reading the actual code

- **#1 (Rheos-backed parsing instead of a third re-parse)**: done. `muse/.ημ/plugins/kanban_gate.cljs` fetches parsed card content from the running Rheos HTTP API (`GET /api/task/:uuid/content`) and re-expresses the same three mechanical-floor checks (`evidence-re`, `disposition-re`, `missing-dispatch-commands!`) over Rheos's own parsed sections, not a local re-parse.
- **#2 (muse `defhook` plugin reaching both hosts)**: done. Same file defines `kanban-done-gate` and `kanban-direct-edit-guard` via `eta-mu.dsl/defhook`, bundled by `defplugin`, referenced from `muse/.ημ/config/shared/plugins/kanban-gate.edn` (`{:resource plugins.kanban-gate/plugin :expose [:policy/kanban-done-gate :policy/kanban-direct-edit-guard]}`), imported into `muse/.ημ/config/opencode/root.edn`. One definition compiles to native hooks for Claude Code and OpenCode — exactly as suggested. Whoever picks this card up should confirm whether `bin/kanban-done-gate` + `.claude/hooks/kanban-*.sh` in this repo have actually been retired in favor of this yet, or are still running in parallel (they were still what gated every transition in this session, per the exit-0/exit-1 checks in the transcript — so the muse-hook path may not be wired as this repo's actual enforcement yet, even though the plugin itself exists and works).

## Finding #3 (ledger as source-of-truth) — new evidence, still unresolved, now sharper

Read `rheos.backend.domain.transition/move-task!` (`eta-mu/packages/rheos/src/rheos/backend/domain/transition.cljs`) and `rheos.backend.law.fsm` (`.../law/fsm.cljs`) in full. The actual gate-and-emit path, precisely:

`move-task!` → FSM structural check (`fsm/evaluate-transition`) → `fsm/run-gate` (runs the project's configured build/lint/test commands against `(:gate-cwd project)`) → only on success: writes the task file, then calls `events/emit-status-change!` → the single private constructor `kanban-envelope` (shared by every kanban event type) → `protocols/append-event!` (the `open-hax.records.*.event-admission` protocol — edn/mongo/rest/socket-io implementations already exist).

The gap this session's incident exposes: **a ledger event carries no anchor to the git commit it's supposedly vouching for.** `run-gate` returns only `{:allowed? :reason}`. When Rheos accepts a transition (e.g. `review -> done`), that acceptance is supposed to mean "the work at this point in the code's history passed the necessary checks" — but nothing records *which point*. Wall-clock `:event/time` proximity is not a signature; a "done" event and the commit it was actually validated against can drift apart silently (a later force-push, an amend, or — as happened today — a git operation that reverts tracked kanban state without touching code state at all).

Proposed fix, now concretely scoped to real code: right after `fsm/run-gate` succeeds in `move-task!` (before `emit-status-change!`), resolve `git rev-parse HEAD` and a clean/dirty check (or a hash of the dirty diff if not clean — "passed at commit X" and "passed at commit X plus these uncommitted changes" are different claims and should be distinguishable) against the same `(:gate-cwd project)` `run-gate` already used, and thread it through as `:git/commit`/`:git/dirty?` on the envelope.

Schema note: the envelope (`open-hax.openplanner-protocols/envelope-schema`, hand-duplicated verbatim in `event-ledger/src/open_hax/event_ledger/schema.cljs` with an acknowledged "TODO: Extract shared schema package if this drifts") is a malli **open** map — every field but `:event/type` is `{:optional true}`. Adding `:git/commit`/`:git/dirty?` is non-breaking for old events by construction; no migration needed. Worth adding an explicit `:event/schema-version` alongside it, though — right now schema evolution is inferred by field absence rather than stated, unlike this repo's own `:observation/schema-version` convention. If this card's implementer touches the schema at all, that's also the moment to collapse the two duplicated copies into one, per the file's own TODO.

## New scope: the FSM definition itself has the same JSON-vs-real-data problem the gate-hook already solved

`rheos.backend.law.fsm/resolve-fsm` takes a project's `:fsm` config and either uses a hardcoded `default-fsm`/`promethean-fsm` CLJS map, or — for a JSON-authored config — only supports `{:extends "promethean" :buildGateCommands [...] :cwd ...}`, overlaying just the command list onto `promethean-fsm`. The function's own comment says why: JSON string values "can't become the `:command` keyword the built-in checks match on." So a project cannot author custom states/transitions/checks from its JSON kanban config at all today — only pick a hardcoded FSM and maybe swap its build command array.

This is the same shape of problem findings #1/#2 already solved for the gate-hook, and there's already a working precedent for the fix in the very same plugin file: `kanban_gate.cljs`'s own `dispatch-gate-config!` reads `.ημ/kanban-done-gate.edn` via `cljs.reader/read-string` specifically because project config needs real Clojure data (keywords), not JSON. The FSM should get the identical treatment — a `.ημ/fsm.edn` (real keywords for `:check` ids and richer check specs) read the same way — which also kills the `:extends "promethean"` string-matching hack.

Whether this also warrants a real `deffsm` DSL macro (sibling to `eta-mu.dsl/deftool`/`defhook`, making FSMs muse-published/importable the way `kanban-gate.edn`'s plugin is imported into `root.edn`) is a separate, larger question — worth it only if FSMs need to be shared/versioned across projects, not required just to fix the JSON limitation. Read `eta_mu/dsl.cljc` in full (102 lines): only `deftool`/`defhook`/`defplugin` exist today, and none fit declarative FSM data as-is — a hook/tool implies a host runtime invoking something at an event; an FSM is just data `evaluate-transition`/`run-gate` consult, nothing invokes it.

This also gives the git-anchor idea above a natural home: today's `:checks` only has `{:type :built-in}` and `{:type :command}`; a `{:type :git-anchor}` check-spec is the natural third case for the same `run-gate` interpreter to grow into, rather than a bolt-on side mechanism.

## Suggested shape, updated

- Confirm/retire `bin/kanban-done-gate` + `.claude/hooks/kanban-*.sh` in this repo in favor of the already-built muse plugin, if not already done (see #1/#2 note above).
- Decide finding #3 explicitly: ledger-as-replay-source vs. ledger-as-parallel-audit-feed. If the former (or even for the latter), commit-anchor every event at admission time as described above — this is now the more urgent half, given today's incident.
- Separately (new): move FSM authoring from JSON `:fsm` config to a `.ημ/fsm.edn`-style file, mirroring the gate-hook's own `.ημ/kanban-done-gate.edn` precedent; decide separately whether a `deffsm` DSL macro is worth building on top of that.
- Touches the same three repos as before (`epiphany`, `eta-mu/packages/rheos`, `muse`), plus now `eta-mu/packages/protocols` and `event-ledger` for the envelope schema if the git-anchor fields are added.

Still no code changes made — this remains a capture/scoping comment, per the card's own stated intent.
---