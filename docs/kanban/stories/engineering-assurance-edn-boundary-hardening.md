---
id: "01900d7c-7f3a-7e8b-9c4d-000000001711"
title: "ENG-017K: Replace reader-eval EDN parsing at external boundaries"
status: ready
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
points: 2
labels: ["quality", "security", "http", "boundaries", "phase-1"]
category: "stories"
dependency: []
verification: ["unit-test"]
risk: "low"
---
# ENG-017K: Replace reader-eval EDN parsing at external boundaries

## Intent

Remove a remote-code-execution vector. `application/edn` HTTP bodies are
parsed with bare `clojure.core/read-string` (`infra/http.clj:97`, again at
`:338` and `:342`), and the Lucene version file is read the same way
(`infra/adapters/lucene.clj:143`). With default `*read-eval*`, `#=(...)` in a
request body executes arbitrary code; even with eval off, the runtime reader
accepts constructs a data-only boundary parser must reject. Observed and
verified in the 2026-07-12 audit (defect inventory item 1). Small, urgent,
self-contained.

## Decision context

ADR-004 decision 1 (every externally received command is schema-validated
data) presupposes the payload was read as *data*. STYLE.md extern-boundary
rule: foreign input is decoded at the boundary or rejected — never evaluated.

## Scope

- Replace all four call sites with `clojure.edn/read-string` with no default
  data readers (`{:readers {}}`); unknown tags are a parse error.
- HTTP: EDN parse failure → 400 problem+json with a stable
  `:boundary/malformed-edn` type — never a 500, never evaluation.
- Lucene version file: parse failure → explicit
  `:integrity/corrupt-version-file` outcome, not an exception escaping the
  adapter.
- Regression tests: a body containing `#=(...)` is rejected as 400 and — the
  critical assertion — its side effect provably did not execute; malformed
  EDN and unknown-tag bodies are 400; valid EDN round-trips unchanged.
- Repo sweep confirming no other `read-string` on external input remains
  (`git grep -n 'read-string'` result recorded in a card comment).

## Non-goals

- No broader content-negotiation or `:limit` validation work (ENG-017G).
- No general EDN schema validation of the parsed value (ENG-017B/G own that).

## Invariants

- No external byte stream ever reaches `clojure.core/read-string`.
- A parse failure is a client error with stable data; it mutates nothing.

## Verification

| Claim | Evidence | Location |
|---|---|---|
| `#=` payload rejected, side effect not executed | Test posts eval payload; asserts 400 + sentinel untouched | `http_test.clj` |
| Unknown tag / malformed EDN → 400 | Negative parse tests | `http_test.clj` |
| Valid EDN unchanged | Round-trip test on an accepted request | `http_test.clj` |
| Corrupt version file surfaces as integrity outcome | Fixture file test | lucene adapter test |
| No remaining boundary `read-string` | Sweep output in card comment | review |

## Acceptance criteria

- All verification tests pass under `clojure -M:unit-test`; the eval-payload
  test fails against the pre-fix code (demonstrated once, noted in comment).
- Sweep comment posted; any intentional internal `read-string` (trusted
  input) is listed with justification.

## Dependencies and interfaces

- No dependencies; deliberately independent so it can land first.
- Provides to ENG-017G: the safe EDN decode it builds its request decoding on.

## Risks and open questions

- None material; the change is local and behavior-narrowing. Watch for tests
  that (wrongly) relied on tagged-literal support in request bodies.

## Completion evidence

Test output, the pre-fix red demonstration, sweep results,
`git diff --stat`, reviewer named at done.

## Would have gated

ENG-006A's done claim: this call site shipped inside its declared scope. A
`discouraged-var` lint on `read-string` (ENG-017H) prevents recurrence.

---
CREATED 2026-07-12 (inbox-synthesis audit): new card for the verified remote-code-execution defect — bare clojure.core/read-string on application/edn HTTP bodies (infra/http.clj:97,338,342) and the Lucene version file (lucene.clj:143). Observed in the 2026-07-12 audit (defect inventory item 1); shipped inside ENG-006A's declared scope (that card demoted to review, comment cross-references this one). Deliberately zero-dependency so it can land before the rest of the lane. Triage authority: user instruction this session. Transitions: incoming->accepted->ready (2pts, criteria+verification present, no dependencies). --tasks-dir docs/kanban
---
