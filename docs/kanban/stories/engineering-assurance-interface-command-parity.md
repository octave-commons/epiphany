---
category: "stories"
labels: ["quality", "cli", "http", "parity", "boundaries", "phase-1"]
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001702", "01900d7c-7f3a-7e8b-9c4d-000000001601"]
phase: "1"
type: "story"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
write-id: "1784568573871-0.mrkwpbkiifk3gv2xpzj"
points: "5"
verification: ["unit-test"]
risk: "medium"
title: "ENG-017G: Normalize CLI and HTTP command contracts"
priority: "P1"
status: "in_progress"
id: "01900d7c-7f3a-7e8b-9c4d-000000001707"
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
---

# ENG-017G: Normalize CLI and HTTP command contracts

## Intent

One command vocabulary, two thin presentations. CLI argv and HTTP requests
decode into the same named, schema-validated command maps; results encode
from the same normalized outcome categories. This is also the vehicle for
fixing the demoted interface cards: the 9 currently-failing HTTP tests, the
missing parity tests, and the boundary permissiveness found in the audit.

## Decision context

Implements `docs/designs/verification-architecture.md` § "CLI/HTTP command
contract" under ADR-004 decision 5. Anchors from the 2026-07-12 audit:
ENG-006A demoted (suite red in `http_test.clj` — register returns 500,
`exception-returns-problem-json` failing; no parity test exists; `wrap-
exceptions` leaks internals; `Accept` matched by substring; `:limit`
unvalidated). ENG-004A/B/D, 005B, 005F demoted (CLI commands absent from
`main.clj:539-545` dispatch).

## Scope

- Named command/query schemas (`command/register`, `query/search`,
  `query/status`, …) in law; decoders `decode-cli` and `decode-http` produce
  the identical validated command map from equivalent input.
- Encoders map normalized outcome categories (accepted / rejected /
  unavailable / not-found / integrity-failure) to exit codes + text and to
  RFC 9457 problem+json respectively.
- Boundary hardening in the HTTP decoder: `:limit` bounded integer, parse
  failures → 400 problem (never 500), exception mapping returns generic
  safe detail while logging server-side, proper media-type handling.
- Fix the 9 currently failing `http_test.clj` tests as part of the register
  flow normalization (they fail on exactly the seams this card refactors).
- Parity tests: equivalent input → same command; same result → same outcome
  category, for every command both surfaces expose.

## Non-goals

- No new CLI commands (`ep show`/`ep diff`/`ep trace`/`ep inbox`/`ep export`
  remain the demoted cards' scope — this card gives them the contract to wire
  into, it does not absorb their work).
- EDN reader replacement is ENG-017K (already isolated as a security fix);
  this card consumes its safe parser, not the other way around.

## Invariants

- No HTTP handler or CLI subcommand reaches Mongo/Lucene/Git adapters
  directly; all traffic flows decoder → command → application service.
- Invalid client input is a stable boundary error; internal errors never
  leak exception messages to clients.

## Verification

| Claim | Evidence | Location |
|---|---|---|
| Decoder parity | Property: equivalent argv/request → equal command map | parity test ns |
| Outcome parity | Table-driven: same service result → same category via both encoders | parity test ns |
| Boundary rejects bad `:limit` (string/-1/0/huge) | 400 problem responses | `http_test.clj` |
| No internals in error bodies | Assertion on problem-detail fields | `http_test.clj` |
| Suite green | Currently-failing 9 tests pass | `clojure -M:unit-test` |

## Acceptance criteria

- Unit suite fully green (inherits the 9 red tests; going green on them is in
  scope, weakening them is not).
- Every command exposed on both surfaces has a parity test; commands exposed
  on one surface are listed with the reason.
- Handlers verified adapter-free by inspection noted at review (ENG-017H
  later automates this).

## Dependencies and interfaces

- Depends on ENG-017B (validation gateway) and ENG-006A (existing HTTP
  adapter — in review; this card is its rework vehicle).
- Provides to the demoted CLI cards (004A/B/D, 005B/F): the decode/execute/
  encode seam each command wires into.
- Consumes ENG-017K's safe EDN parsing.

## Risks and open questions

- Refactoring `main.clj` (parsing + profile + lifecycle + output in one ns)
  may exceed points once decoders are extracted; if so, split at the
  decoder/encoder seam per the breakdown rule.

## Completion evidence

Test output (including formerly-red tests), parity coverage list,
`git diff --stat`, reviewer named at done.

## Would have gated

ENG-006A's done claim (parity criterion had no test; problem+json criterion
now failing red) and the five phantom-CLI done cards — a normalized command
vocabulary makes "the command doesn't exist" impossible to miss, because the
card's deliverable is an entry in an enumerable, tested table.

---
REWORK 2026-07-12: body rewritten to the story contract (original preserved in git history and scratchpad; see ENG-017A comment for the shared rework rationale). Triage authority: user instruction this session. --tasks-dir docs/kanban

SCOPE ANCHOR 2026-07-12 (after clj -M:test): this card owns two of the three failing clusters in the current red suite. Cluster A (3 http_test failures: register 500-not-201, Content-Type, exception->4xx) is the HTTP-boundary normalization already in scope. Cluster C (3 failures) is a concrete bug this card's command-result contract must fix: registration/register! returns the full observation on the idempotent path (registration.clj:15-16) and a thin map on the fresh path (:33-36) — the normalized result must be one shape both paths and both interfaces produce. Cluster B (3 stale observation-shape assertions) is fixture drift owned by ENG-017C/D. Full breakdown: docs/notes/inbox-synthesis-2026-07-12-board-audit.md. --tasks-dir docs/kanban

PROGRESS 2026-07-13 (session): The 9 unit-test failures this card owns are now resolved (see ENG-006A comment for details). Suite is green: 528 tests, 1344 assertions, 0 failures. The register! return-contract inconsistency (Cluster C) is fixed — both paths return the same thin map. HTTP mock shape (Cluster A) is fixed — mocks match the actual port contracts. Remaining scope for this card: parity tests (CLI vs HTTP produce equivalent outcomes), boundary hardening in HTTP decoder, and the command/schema normalization that gives the demoted CLI cards (004A/B/D, 005B/F) their wiring target. --tasks-dir docs/kanban --tasks-dir docs/kanban

IN PROGRESS 2026-07-20 (session): Landed a scoped, testable slice of this card's boundary-hardening and parity verification. NOT claiming the full card done — see "Remaining scope" below; recommend either continuing here or splitting per rule 7 if the remainder doesn't fit the original 5-point estimate.

Done, with tests, suite green (600 tests, 1513 assertions, 0 failures):

1. `:limit` bounded validation. HTTP `search-handler` previously accepted any `:limit` value unvalidated (string/negative/zero/huge all passed straight to the search adapter). Added `http/valid-limit?` + `http/max-search-limit` (1000), wired into the HTTP handler as a 400, and updated the CLI's `--limit` validator (`main.clj`) to share the same upper bound (it already had `pos?`, was missing the ceiling) — CLI and HTTP now enforce the identical bound. 5 new regression tests in `http_test.clj` (negative/zero/huge/string/at-bound).

2. Internal-error leak fix. `wrap-exceptions`' generic Exception/unrecognized-`:code` branches previously returned `(.getMessage e)` straight into the problem+json `:detail` field — any unexpected exception message (config, driver, whatever it happened to say) reached the client. Now logs the real message to `*err*` and returns a fixed generic detail (`"An internal error occurred."`) for anything not an explicitly-recognized `:code`. 2 new regression tests proving a bare `RuntimeException` with a sensitive payload doesn't leak it.

   Found but did NOT change: `register-handler`'s *own* catch (separate from `wrap-exceptions`) treats any `ExceptionInfo` without `:unavailable`/`:already-exists` as a 400 with the raw message, on the assumption `register!` only ever throws client-facing validation errors (true today — it only calls git-resolve and the observation port). Wrote a test (`register-unrecognized-ex-info-code-currently-echoes-message`) that documents this as current behavior rather than asserting it's a bug, since changing it would require register! to distinguish client-input failures from adapter-internal failures, which it doesn't do yet. Flagging as a follow-up if register! ever gains a failure mode that isn't purely about the caller's input.

3. CLI/HTTP outcome-category parity tests: new `test/epiphany/parity/cli_http_test.clj`, 7 tests covering `register` (missing path, non-Git path, real repo path) and `search` (missing query, invalid mode, valid empty-index query, limit out of bounds) — asserts CLI exit-code and HTTP status-code map to the same accepted/rejected category for equivalent input. Both surfaces build adapters independently (CLI's own `:local` construction vs. a matching `in-memory/make` for HTTP) rather than sharing one instance, since there's no shared decoder yet to exercise — see below.

Remaining scope, deliberately not attempted this pass:
- The actual `decode-cli`/`decode-http`/`execute`/`encode-cli`/`encode-http` command-vocabulary seam the design doc and this card's "Scope" section call for. What exists today is what existed before: CLI and HTTP each independently parse, build adapters, and call the same application/domain functions — parity is *observed and now regression-tested*, not *architecturally guaranteed* by a shared decoder. A future divergence would only be caught by these tests, not prevented by construction.
- Parity tests for `status` and `review-decisions` (only register/search covered).
- `main.clj`'s CLI commands (`show`/`diff`/`trace`) already exist and pass their own tests (`main_test.clj`) — the ENG-017G card body's framing of these as "demoted, phantom CLI cards" appears stale relative to the current tree; didn't touch this since it's not this card's claim to correct.
- `infra.main` shelling out to the real `git` binary via `clojure.java.shell/sh` for `--path-format=absolute --git-common-dir` (`run-register`, `run-serve`) rather than going through JGit — noticed while writing the parity test's `shell-git-resolve` helper (mirrors the same pattern). This looks like it conflicts with ADR-000/CLAUDE.md's "Git is read-only, never shelled out to" principle, but is out of scope for this card and not something to fix as a drive-by; flagging for its own card.

Suite: `clojure -M:unit-test` → 600 tests, 1513 assertions, 0 failures. `clojure -M:lint`, `:boundary-check`, `:interop-inventory` all clean.
---