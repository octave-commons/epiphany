---
id: "01900d7c-7f3a-7e8b-9c4d-000000001707"
title: "ENG-017G: Normalize CLI and HTTP command contracts"
status: "ready"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
points: 5
labels: ["quality", "cli", "http", "parity", "boundaries", "phase-1"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001702", "01900d7c-7f3a-7e8b-9c4d-000000001601"]
verification: ["unit-test"]
risk: "medium"
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
---
