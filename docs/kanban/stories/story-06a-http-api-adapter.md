---
id: "01900d7c-7f3a-7e8b-9c4d-000000001601"
title: "ENG-006A: Expose the HTTP API adapter (`/api/v1`)"
status: "review"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000006"
design: "docs/kanban/epics/epic-06-temporal-research-workbench.md"
points: 4
labels: ["phase-1", "http", "api", "adapter"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001304", "01900d7c-7f3a-7e8b-9c4d-000000001401"]
---

# ENG-006A: Expose the HTTP API adapter (`/api/v1`)

reitit + ring adapter over the same command/query services the CLI uses. Search, evidence, trace, inbox, review-decisions.

## Acceptance criteria

- Errors are RFC 9457 problem+json; JSON default, EDN accepted locally.
- Adapter parity tests: direct CLI and HTTP produce equivalent outcomes for the same query.
- Review decisions are `POST /review-decisions` command resources, not mutable candidate updates.
- No business logic in the adapter; no direct Mongo/Lucene/Git access from handlers.

---
AUDIT 2026-07-12: status=done graded F. Observed contradictions: (1) unit suite is red right now in this card's own tests — 9 failures in http_test.clj incl. exception-returns-problem-json (a named acceptance criterion) and register returning 500 instead of 201; (2) acceptance criterion 'adapter parity tests: CLI and HTTP produce equivalent outcomes' — no parity test exists in test/epiphany/infra/http_test.clj; (3) EDN bodies parsed with bare clojure.core/read-string (http.clj:97,338,342) = remote code execution risk with default *read-eval*; (4) wrap-exceptions returns exception messages to clients. No completion-evidence comment recorded. Would have been gated by: ENG-017G (command contracts + parity), new ENG-017K (EDN boundary hardening), ENG-017H (static gates), ADR-004 rule 7. Demoting done->review; the 9 red tests belong to this card's rework. --tasks-dir docs/kanban

CORRECTION 2026-07-12 (after clj -M:test full run): my earlier comment said 'the 9 red tests belong to this card's rework'. That over-claimed — verified, only 3 of the 9 are this card's (http_test.clj: register 500-not-201, Content-Type, exception-returns-problem-json). The other 6 are registration/profile: 3 stale observation-shape assertions (test drift from commit ba2d7da) and 3 from a real register! return-contract bug (idempotent path returns the full observation, fresh path returns a thin map). See docs/notes/inbox-synthesis-2026-07-12-board-audit.md Correction table. This card owns cluster A only. --tasks-dir docs/kanban
---
