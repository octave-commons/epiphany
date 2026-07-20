---
category: "stories"
labels: ["phase-1", "review", "events", "provenance"]
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001403"]
phase: "1"
type: "story"
write-id: "1784570727853-0.el3rcjgh4j2x6dn1xu"
points: "3"
title: "ENG-005A: Record review decisions as append-only events"
priority: "P1"
status: "review"
id: "01900d7c-7f3a-7e8b-9c4d-000000001501"
epic: "01900d7c-7f3a-7e8b-9c4d-000000000005"
design: "docs/kanban/epics/epic-05-redundancy-tension-review.md"
---

# ENG-005A: Record review decisions as append-only events

Accept, reject, relabel, defer, annotate, or mark do-not-suggest — durably and idempotently.

## Acceptance criteria

- A review action appends an event; it never rewrites the candidate or Git evidence.
- Rejected candidates remain in audit mode; do-not-suggest suppresses similar candidates in default views.
- Events carry request IDs; retries do not duplicate decisions.
- Decisions are queryable by candidate, relation type, and time.

---
AUDIT 2026-07-13 (found while investigating the ep show/diff/trace/inbox/export pattern): this card is marked done with ZERO completion-evidence comment -- the only card of the six affected that no prior audit caught. Independently verified it is not done: grep -rn 'review-decision|record-decision' src/epiphany/infra/ (in_memory.clj, mongo.clj, law/ports.clj) returns nothing. domain/review.clj has real pure functions (make-decision, by-candidate, by-decision-type, by-time-range, rejected-candidates, visible-decisions) and test/epiphany/domain/review_test.clj presumably exercises them in isolation, but there is no port anywhere that durably persists or queries a review decision. The AC bullet 'Decisions are queryable by candidate, relation type, and time' is unmet -- there is nothing to query. This is the actual root blocker for ENG-005B (ep inbox) and ENG-005F (ep export), which both depend on decision/candidate storage that was never built, despite this card claiming it's done. Demoting done->in_progress. Real remaining work: an observations-port write op (e.g. :record-review-decision!) plus a query capability, wired through the same schema-registry enforcement pattern as ENG-017A-C. --tasks-dir docs/kanban

TRIAGE 2026-07-20 (board review): confirmed critical path. This card is the single root blocker for the entire review/export CLI lane — ENG-005B (ep inbox), ENG-005F (ep export), and the still-open AC gaps on ENG-004B (diff "seed a candidate/review decision") and ENG-004D (trace cross-file candidate edges) all depend on a durable, queryable candidate/review-decision store that does not exist yet. domain/review.clj + domain/inbox.clj + domain/export.clj are solid at the pure layer; nothing persists or queries. Defined remaining scope (from the 2026-07-13 audit, unchanged): an observations-port write op (:record-review-decision!) + candidate persistence + query capability, wired through the same schema-registry enforcement pattern as ENG-017A–C (now landed). Recommend this is the next card worked. Suite green at 600 tests / 1513 assertions / 0 failures as of this review. No status change made by this triage pass.

IMPLEMENTED 2026-07-20: the durable review-decision port now exists — the root blocker the 2026-07-13 audit identified is closed. Changes: (1) law/observation.clj adds the closed observation/review-decision-v1 schema (envelope + :review-decision/* payload; :observation/request-id is the idempotency key). (2) law/operations.clj registers :record-review-decision! -> that schema and adds it to port-write-operations, so application/validation.clj (ENG-017B) auto-wraps it — no application flow can reach the adapter with an unvalidated decision. (3) law/ports.clj adds :record-review-decision!, :list-review-decisions, :list-review-decisions-by-candidate to the closed observations-port-schema. (4) domain/review.clj gains pure decision->observation (wraps make-decision output into the durable record; carries the decision's request-id through as the idempotency key). (5) in_memory.clj implements the write (idempotent by request-id — a retry does NOT append a second decision) + both queries + export-all/import-all wiring; still passes the closed application/ports schema check. (6) mongo.clj mirrors it (new review-decision-v1 collection, doc<->record mappers, unique request_id index for idempotency, record/list/export/import) — integration-untestable in this environment (no MongoDB), so that path is unverified here.

AC status: (a) append-only, never rewrites candidate/Git evidence — met; (b) rejected/do-not-suggest retained + suppressed flag persisted — met (domain visible-decisions/rejected-candidates operate over the durable list); (c) request-ids, retries don't duplicate — met, test review-decision-idempotent-by-request-id; (d) queryable by candidate/relation-type/time — met, list ops + domain by-decision-type/by-time-range, test review-decisions-queryable-by-type-and-time.

Evidence: clojure -M:unit-test — 608 tests, 1540 assertions, 0 failures (was 600/1513; +8 tests). New tests: 4 in in_memory_test.clj (record+list, idempotency, invalid-rejected, export/import round-trip) + 3 in review_test.clj (schema-valid wrapping across all 6 decision types, request-id-as-idempotency-key, provenance-required). Not committed — left in the working tree; 017G work is also uncommitted in parallel. This is implementation evidence only; independent review + the review->done gate still apply (note: bin/kanban-done-gate is currently broken — it shells to the stale eta-mu CLI and errors 'unknown task', so the mechanical floor must be run manually or fixed first). Moving to review.
---