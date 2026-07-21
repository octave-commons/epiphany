---
uuid: "03c76b89-2ba4-4c54-a3a0-61ed9dabbf95"
title: "ENG-017G2: Shared CLI/HTTP command-vocabulary decoder seam"
status: "incoming"
priority: "P1"
labels: ["quality", "cli", "http", "parity", "boundaries", "phase-1"]
created_at: "2026-07-21T18:58:36.696Z"
parent: "eng-017g-normalize-cli-and-http-command-contracts"
points: "3"
phase: "1"
type: "story"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
design: "docs/designs/verification-architecture.md"
verification: ["unit-test"]
risk: "medium"
write-id: "1784660316696-0.w75r4r8c18onthxwbh"
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

