---
labels: ["quality", "cli", "http", "parity", "boundaries", "phase-1"]
parent: "eng-017g-normalize-cli-and-http-command-contracts"
phase: "1"
type: "story"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
write-id: "1784941491906-0.hvg9nmkqjxk7ssvlgrs"
points: "3"
verification: ["unit-test"]
risk: "medium"
title: "ENG-017G2: Shared CLI/HTTP command-vocabulary decoder seam"
priority: "P1"
status: "done"
design: "docs/designs/verification-architecture.md"
uuid: "03c76b89-2ba4-4c54-a3a0-61ed9dabbf95"
created_at: "2026-07-21T18:58:36.696Z"
---

# ENG-017G2: Shared CLI/HTTP command-vocabulary decoder seam

## Intent

Make CLI/HTTP outcome parity guaranteed *by construction*, not merely
*observed by regression test*. Today (after ENG-017G's landed slice) the CLI
argv parser and the HTTP request handler each independently parse input, build
their own adapters, and call the same application/domain functions. Parity
happens to hold and is now regression-tested (`test/epiphany/parity/cli_http_test.clj`,
register/search), but a future divergence would only be *caught* by those
tests, never *prevented*. This card builds the shared seam the parent card's
Scope section called for so that both surfaces decode into one named,
schema-validated command map and encode from one set of normalized outcome
categories.

## Decision context

Carved out of ENG-017G on 2026-07-21 per breakdown rule 7 and the parent's
own 2026-07-20 implementer recommendation. Implements
`docs/designs/verification-architecture.md` § "CLI/HTTP command contract"
under ADR-004 decision 5. The parent card retains the shipped boundary
hardening (`:limit` bound, `wrap-exceptions` leak fix) and the observed-parity
regression tests; this card owns the architectural guarantee.

## Scope

- Named command/query schemas in `law` (`command/register`, `query/search`,
  `query/status`, `command/review-decision`, …) — the single vocabulary both
  surfaces target.
- `decode-cli` and `decode-http`: equivalent input → the identical validated
  command map. Invalid client input → a stable boundary error (400 / non-zero
  exit), never an adapter reached directly.
- `encode-cli` / `encode-http`: map normalized outcome categories
  (accepted / rejected / unavailable / not-found / integrity-failure) to exit
  codes + text and to RFC 9457 problem+json respectively — one source of
  truth, not two parallel maps.
- Rewire CLI (`main.clj`) and HTTP (`http.clj`) handlers to go through
  decode → execute → encode, so parity is structural.
- Extend parity coverage to the commands ENG-017G left uncovered: `status`
  and `review-decisions` (ENG-017G covered only register/search).

## Non-goals

- No new user-facing commands (the `ep show/diff/trace/inbox/export` cards own
  their own wiring; this gives them the seam to target).
- No re-litigation of the `:limit`/leak hardening already landed on ENG-017G.
- Not fixing `register-handler`'s raw-ExceptionInfo echo or `infra.main`'s
  shell-out to the `git` binary (ADR-000 violation) — both flagged on ENG-017G
  for their own cards.

## Invariants

- No CLI subcommand or HTTP handler reaches Mongo/Lucene/Git adapters
  directly; all traffic flows decoder → command → application service.
- The parity property is enforced by a shared decoder, not asserted by
  duplicated per-surface tests — the regression tests become a guard on the
  seam, not the sole guarantor.

## Acceptance criteria

- `decode-cli` and `decode-http` produce an equal command map from equivalent
  input, proven by a property/table test over every command both surfaces
  expose.
- Every command exposed on both surfaces has a parity test; single-surface
  commands are listed with the reason.
- CLI and HTTP handlers demonstrably route through the shared seam (no
  independent adapter construction per surface for the covered commands).
- Full unit suite green under `clojure -M:unit-test`.

## Dependencies and interfaces

- Depends on ENG-017G (landed slice) and ENG-017B (validation gateway).
- Consumes ENG-017K's safe EDN parsing.
- Provides the decode/execute/encode seam the demoted CLI cards
  (004A/B/D, 005B/F) wire into.

## Completion evidence

Test output, parity coverage list (command → both surfaces), `git diff --stat`,
reviewer named at done per `docs/process/review-and-acceptance.md`.

---
TRIAGE 2026-07-24: incoming -> accepted. Points 3 honest (named law/ command-query schemas + decode/encode seam + rewire of two surfaces + parity extension to status/review-decisions). Dependencies verified done: ENG-017G (01900d7c-...-001707, done), ENG-017B (done), ENG-017K (done). Acceptance criteria present, design + ADR links present. Note for implementation: ENG-003G (in review) just added `ep ingest` to main.clj's dispatch — the seam must cover ingest too, or list it as single-surface with reason per AC2.

IMPLEMENTED 2026-07-24 (commit 7977f2a).

Delivered:
- law/commands.clj: named schemas for command/register, query/search, query/status, command/review-decision — closed maps, semantic intent only (no format/profile/index-dir). Registered in law/registry. Decision vocabulary duplicated per repo precedent (cf. lineage/relation-types) and pinned by test.
- application/commands.clj: decode (candidate -> validated command map | stable :rejected outcome, adapters never reached on invalid input), execute (one executor, normalized outcome categories accepted/rejected/unavailable/not-found), encode tables (single HTTP category->status map).
- Rewired: main.clj run-register/run-search/run-status/run-inbox-decide; http.clj search/register/status/review-decisions handlers. Both surfaces now flow decode -> execute -> encode.
- Ollama probe moved into the shared executor (:service-available? port): semantic/hybrid with Ollama down is UNAVAILABLE (503 / exit 1) on BOTH surfaces — previously HTTP would 500 and CLI exit 1 with divergent categories.
- AC evidence: decode-parity table test (8 cases x 4 commands) proves equivalent CLI/HTTP input yields the IDENTICAL validated command map; parity suite extended to status (2 cases) and review-decisions (3 cases).
- Behavior changes: (1) CLI `status -p local` now reports the shared cross-stage status (empty stages) instead of a surface-specific "does not persist" error — parity; (2) CLI `inbox decide` requires the candidate to EXIST and records under the candidate's resource-id — phantom decisions are now impossible on both surfaces (previously CLI recorded decisions against nonexistent candidates under the repo's resource-id).
- Single-surface commands listed per AC2: show/diff/trace/inbox(list)/export/ingest are CLI-only (their cards own their wiring; this seam is what they target); htmx/* + workbench pages are HTTP-only (UI, not command vocabulary).

Gates: clojure -M:unit-test => 707 tests, 1839 assertions, 0 failures. boundary-check clean. Interop baseline regenerated — law/commands has zero interop; application/commands 4 dot-calls (UUID parsing). Integration 17/53, 2 pre-existing baseline failures only.

AUDIT 2026-07-24 (ultra-code wave, .ημ/workflows/eng-017e-017g2-review.edn): 6 reviewer jobs, 44 skeptic votes, quorum 2. 3 confirmed findings, all should-fix (no blockers), ALL FIXED in 26a4c18:
1. --embedding-version accepted-but-dropped: now an optional field in the law/commands query/search schema, wired on both surfaces.
2. run-search exception-handling gap after the seam rewrite: raw adapter exceptions now caught and exit 1 cleanly.
3. Unused clojure.string require in application/commands: removed.
19 other findings were refuted by skeptic quorum (out-of-scope re-litigation of documented behavior changes, pre-existing structure, style). Gates after fixes: 712 unit tests, 1876 assertions, 0 failures; kondo 0 warnings on touched files.

REVIEW 2026-07-24: approve. All 3 confirmed should-fix findings from the ultra-code wave fixed in 26a4c18 with green gates (712/1876/0; kondo clean on touched files). No blockers surfaced. The seam now guarantees CLI/HTTP parity by construction for register/search/status/review-decisions.

GATE 2026-07-24: bin/kanban-done-gate exit 0. document->done via rheos MCP failed server-side ('paths[0] must be of type string, got object' — same hook bug as ENG-003G; now tracked as docs/kanban/chores/chore-rheos-done-transition-paths-type-error.md). Status advanced by direct frontmatter edit per the gate script's completion instructions, with this comment as audit trail.
---