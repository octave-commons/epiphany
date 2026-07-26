---
labels: ["phase-1", "review", "candidates", "lineage", "provenance", "storage"]
parent: "eng-005a-record-review-decisions-as-append-only-events"
phase: "1"
type: "story"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
write-id: "1784664767340-0.c9sn13nc4mkqlcx30io"
points: "5"
verification: ["unit-test"]
risk: "medium"
title: "ENG-005G: Durable lineage-candidate store (generation, persistence, query)"
priority: "P1"
status: "done"
id: "c95a201b-57b7-47e3-ad04-2865ba1b95d9"
epic: "01900d7c-7f3a-7e8b-9c4d-000000000005"
uuid: "c95a201b-57b7-47e3-ad04-2865ba1b95d9"
created_at: "2026-07-21T19:09:06.893Z"
---

# ENG-005G: Durable lineage-candidate store (generation, persistence, query)

## Intent

ENG-005A built the durable *review-decision* store; it deliberately did NOT
build a store for the *candidates* those decisions are about. That missing
half is the shared root blocker behind several stalled AC bullets: a lineage
candidate is generated (retrieval/heuristic/model output at `PROVISIONAL`
tier), persisted, and queryable — by candidate id, relation type, confidence
band, generator version, and time — with review decisions joining to it. Until
this exists, "query by relation type", "seed a candidate from a diff",
"cross-file candidate edges in trace", and a non-placeholder review inbox are
all unreachable.

## Decision context

Surfaced during the 2026-07-21 review lane. Independent reviews of ENG-005A,
ENG-004B, ENG-004D each traced their one remaining unmet AC to the absence of
a candidate store. Rather than hold those cards open indefinitely, the
candidate-dependent AC bullets are descoped to this card (see "Absorbed
scope"). Follows the epistemic-status ladder (ADR / CLAUDE.md): candidates are
`PROVISIONAL` and are promoted to `ACCEPTED`/`REJECTED` only through the
ENG-005A decision events — never silently.

## Scope

- A closed `observation/lineage-candidate-v1` (and/or redundancy-candidate)
  schema in `law`, registered in `law/operations.clj` + `law/ports.clj`, wrapped
  by the ENG-017B validation gateway — same pattern as review-decision-v1.
- Observations-port ops: `:record-lineage-candidate!` (append-only, idempotent
  by request-id) + query ops (by id, by relation type, by generator version,
  by confidence band, by time), in both in-memory and Mongo adapters, passing
  the ENG-017D law suite.
- The join to review decisions: a candidate resolves its current disposition
  (provisional / accepted / rejected / do-not-suggest) from the 005A decision
  events by candidate-id.
- Candidate *generation* wiring sufficient to populate the store from lineage
  projections (or an explicit, tested seam the lineage cards feed).

## Absorbed scope (descoped here from other cards, 2026-07-21)

- **ENG-005A** AC4 "queryable by relation type" — relation lives on the
  candidate; this card makes it queryable and the 005A AC is amended to
  "queryable by candidate, decision type, and time" for its own delivered scope.
- **ENG-004B** AC4 "a comparison can seed a candidate relation or review
  decision" — `ep diff` gains the ability to seed a candidate once this store
  exists.
- **ENG-004D** AC1/AC4 full breadth — cross-file accepted/provisional/rejected
  edges in `ep trace` require candidates + the decisions join
  (`find-cross-edges` is currently called with `{}`).
- **ENG-006C** inbox — a real, non-placeholder candidate queue to triage.

## Acceptance criteria

- Candidates persist append-only, idempotent by request-id, in both adapters;
  pass the ENG-017D law suite.
- Queryable by candidate id, relation type, generator version, confidence
  band, and time — each with a test.
- A candidate's disposition is derived by joining ENG-005A decision events;
  a rejected/do-not-suggest candidate is never surfaced as established.
- Full unit suite green under `clojure -M:unit-test`.

## Dependencies and interfaces

- Depends on ENG-005A (decision store) and ENG-017B (validation gateway).
- Unblocks the descoped AC bullets on ENG-004B, ENG-004D, ENG-005B, ENG-006C.

## Completion evidence

Test output, schema/registry diff, `git diff --stat`, reviewer named at done.

---
IMPLEMENTED 2026-07-21 (board triage, same session filed): durable lineage-candidate store built, mirroring the reviewed ENG-005A pattern. Committed 98a522e on branch triage/2026-07-21-assurance-fixes-launcher.

AC1 closed schema: law/observation.clj:239-283 observation/lineage-candidate-v1 (closed map; :relation refs the lineage-candidate/relation enum REUSING domain/lineage/relation-types — test asserts candidates/relation-types = lineage/relation-types, no parallel vocab; :confidence [:double 0..1]; source/target evidence spans w/ path+heading-path+git oid; :tier [:= :provisional]; required :observation/request-id).
AC2 write + idempotency + adapters: :record-lineage-candidate! registered (operations.clj:56-59, ports.clj:80-81) — picked up by the ENG-017A derived completeness check. in_memory: idempotent-by-request-id (replay returns nil), validate-write! on new, export/import. mongo: new collection + unique request_id index + mappers — INTEGRATION-UNTESTABLE here, written-not-run. ENG-017D law suite: reference adapter still passes all 6 laws with candidate ops present; the harness is still repository-location-fixtured so it does not exercise candidate ops directly — instead 4 law-shaped candidate tests added in in_memory_test.clj. Generalizing the harness to be record-type-parametric is a noted follow-up.
AC3 queryable: pure filters in domain/candidates.clj (by-candidate-id/by-relation/by-generator-version/by-confidence-band/by-time-range) + :list-lineage-candidates/:find-lineage-candidate-by-id; one test per dimension + end-to-end over the durable list.
AC4 disposition join: candidates/disposition joins ENG-005A decision events by candidate id (latest terminal wins; relabel/deferred/annotated neutral); established? only :accepted; surfaced? excludes :rejected/:do-not-suggest — 6 tests.
AC5 generation seam: from-lineage-candidate maps domain/lineage generate-candidates output onto the durable span shape; deliberately thin per card.
Evidence: clojure -M:unit-test 637 tests / 1638 assertions / 0 failures (+24); boundary-check clean (domain/candidates imports only domain/review, no Java). interop.edn baseline updated (+epiphany.domain.candidates, same interop profile as domain/review). Deferred honestly: mongo verification, harness generalization, live generation/consumer wiring (the absorbed 004B/004D/006C bullets stay with those cards). Independent review + review->done gate still apply. Recommend review.

REVIEW 2026-07-21 (independent adversarial review of 98a522e): APPROVE, minor non-blocking follow-ups. Faithfully mirrors the reviewed ENG-005A pattern; every AC met; core correctness sound and mechanically enforced.
- AC1 schema closed (observation.clj:268), confidence 0..1 bounded (:276), request-id required :uuid (:270), tier fixed [:= :provisional] (:279). Relation reuse GENUINE: relation-vocab-matches-lineage-test asserts (= lineage/relation-types candidates/relation-types) — real drift guard, not tautological.
- AC2 idempotent append-only by request-id (in_memory.clj:156-164, replay→nil, count=1); validate-write! on new path + gateway at composition. nil rid forced by schema.
- AC3 all 5 query dimensions present + tested (per-dimension + end-to-end over durable list); confidence-band inclusive [low,high], nil=open.
- AC4 disposition join by candidate id, terminal-set filter, sort-by decided-at, last wins; rejected/do-not-suggest never established?/surfaced? (tested). CORRECT for all three neutral types.
- AC5 boundary-check clean; domain/candidates pure (interop.edn :imports #{}, only requires domain/review, strictly downward). Gateway auto-wrap confirmed end-to-end (operations.clj:56 + ports.clj:79 + write-operation? + profile.clj:77) — even the Mongo adapter (no internal validate) cannot be reached with an unvalidated candidate.
- Deferred claims honest: mongo path structurally faithful to the reviewed review-decision path (unique request_id index, 11000→nil, span converters) but legitimately unrun (no MongoDB); 017D harness not generalized — substitute is 5 law-shaped candidate tests (card undercounts as 4, safe direction); from-lineage-candidate is real+tested, thin as stated. 25 new tests, 637/1638/0.
NON-BLOCKING follow-ups (recommend a small hardening pass or a follow-up card): (1) disposition test never forces sort-by to reorder (input already chronological) and ties (equal decided-at) are undocumented/untested — add a shuffled-input + same-timestamp test and document the tiebreak (append order); (2) add explicit neutrality assertions for :relabel and :deferred (only :annotated is exercised); (3) relation vocab now exists in THREE hand-maintained copies (lineage/relation-types, candidates/relation-types, the lineage-candidate/relation enum literal in observation.clj:245) — no test ties the law enum to the vocabulary, so an 8th relation would be silently rejected by the schema; add a drift guard or derive the schema-valid test's list from candidates/relation-types. Also inherited (consistent w/ review-decision, not a new defect): same-request-id/different-content is silently swallowed as nil rather than an :idempotency-conflict. Recommend advancing; final done disposition/authority per docs/process/review-and-acceptance.md.

EVIDENCE 2026-07-21 (actual command output, post-hardening f0ccc3f): clojure -M:unit-test

641 tests, 1648 assertions, 0 failures.

clojure -M:boundary-check → Layer-boundary check: clean. The +4 tests over the reviewed 637 are the three hardening follow-ups applied per the independent review (disposition reorder/tie-break, relabel/deferred neutrality, law-enum-vs-vocabulary drift guard). Independent REVIEW disposition (APPROVE) is recorded above.
---