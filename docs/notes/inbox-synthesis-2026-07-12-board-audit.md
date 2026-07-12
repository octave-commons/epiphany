---
slug: inbox-synthesis-2026-07-12-board-audit
uuid: c0ffee12-2026-0712-b0a4-000000000001
title: "Board Audit 2026-07-12: Done-Status Trustworthiness Grades and Assurance-Lane Rework"
kind: note
status: draft
description: "Both-ends board pass: grades the trustworthiness of done-status claims, records demotions and the checks that would have gated each, and documents the ENG-017 assurance-lane rework and promotions."
labels: [audit, kanban, verification, trustworthiness, assurance, gaming]
created: "2026-07-12"
sources:
  - "docs/notes/inbox-synthesis-2026-07-12-defect-inventory.md"
  - "docs/notes/inbox-synthesis-2026-07-12-agent-gaming-pattern.md"
  - "Board at working tree e92e389"
informs:
  - "docs/process/kanban.md"
  - "docs/process/review-and-acceptance.md"
---

# Board Audit 2026-07-12

Worked the board from both ends: graded the trustworthiness of `done` claims
(the false-done end) and reworked + promoted the assurance lane (the incoming
end), connecting each false-done to the incoming check that would have gated
it had the assurance work existed first. Card comments carry the per-card
record; this note is the overview and the lessons.

## Grading scale

Grades score **status trustworthiness** — whether the claimed state is
supported by discoverable evidence — not code quality.

- **A** — claim matches reality; completion evidence recorded and verifies.
- **B** — deliverable real and evidenced; a criterion or the criteria
  themselves are weak.
- **C** — deliverable partially real; evidence thin or absent; no false claim.
- **D** — honest disclosure of incompleteness, but promoted to done anyway
  (illegal transition, not dishonest content).
- **F** — headline deliverable absent or contradicted; done is a false claim.

## Done-end grades

| Card | Grade | Verdict | Action |
|---|---|---|---|
| US-000A scaffold/baseline | A- | Best completion record on the board; logged 4 anomalies instead of absorbing them. Caveat: its green baseline has since regressed (suite 9 red) with no owner. | kept done; commented |
| US-000B ports/profiles | B+ | Evidenced and true — but criteria never required adapter enforcement, so it legitimately shipped the permissive oracle. Defect is in the criteria. | kept done; commented |
| ENG-001A repo-location observation | C+ | Real & validated — but its validation pattern was never generalized; it is the *only* validated Mongo write. Upgrades a provisional defect claim to observed. | kept done; commented |
| ENG-006B workbench search | C- | Search genuinely wired to hybrid-search; two criteria (epistemic status, per-signal scores) unverified, no evidence. | kept done; commented (verification debt) |
| ENG-004A `ep show` | D | Comment admits "no CLI command yet"; promoted anyway. Domain real, deliverable absent. | done→review |
| ENG-004B `ep diff` | D | Same shape as 004A. | done→review |
| ENG-004D `ep trace` | F | CLI command does not exist (`main.clj:539-545`); no evidence comment. | done→review |
| ENG-005B `ep inbox` | F | CLI command absent; workbench inbox is a different, placeholder-backed surface. | done→review |
| ENG-005F `ep export` | F | CLI command absent; never wired or exercised. | done→review |
| ENG-006A HTTP API | F | Suite red in its own tests (register 500, problem+json failing); parity criterion has no test; `read-string` RCE; exception leakage. | done→review |
| ENG-006C workbench timeline/inbox/health | F | Lineage + candidates/decisions are placeholder empties (`workbench.clj:352,456`); tests assert the stub. | done→review |
| ENG-021A backup/restore drill | (in_progress) | Correctly not done. `restore-drill` runs 1 of 5 documented stages. | kept in_progress; blocker comment |

Net: **7 cards demoted done→review**, 4 kept done with recorded caveats,
1 in_progress reinforced. Done count 43→36.

### Two failure shapes, deliberately graded apart

- **F cards** made a false claim: the deliverable does not exist or is
  contradicted by running code.
- **D cards** told the truth in the comment ("no CLI command yet") and were
  promoted to done regardless. The dishonesty is in the *transition*, not the
  content — which is exactly the `ready→done` skipping the user described.
  Grading these apart matters: the fix for F is evidence; the fix for D is a
  transition gate.

### Systemic observations (not per-card)

- **17 done cards carry no completion-evidence comment at all** (structural
  scan). Absence of evidence was normal, not exceptional — the environment
  that let `ready→done` skips pass.
- **The green baseline regressed unowned.** US-000A claimed a green suite;
  today the full suite is 543 tests / 9 failures (unit alias: 528 / 9). No
  card owned keeping that claim true.

### Correction (2026-07-12, after `clj -M:test`)

An earlier version of this note and the ENG-006A comment said the 9 failures
were "all in `http_test.clj`." That was an inference from a truncated test
tail, stated as fact — the exact error this audit is about. Verified
breakdown of the 9:

| Cluster | Count | Failures | Nature | Owner |
|---|---|---|---|---|
| A — HTTP boundary | 3 | `http-test/router-handles-register-post` (×2: 500 not 201, Content-Type), `http-test/exception-returns-problem-json` | Register handler throws 500; exception middleware doesn't map to 4xx | ENG-006A (review) → ENG-017G + ENG-017K |
| B — stale observation-shape assertions | 3 | `registration-test/registers-a-new-repository...`, `registration-test/...reuses-an-existing...`, `profile-test/bootstrap-local-mode-idempotent-by-request-id` shape | Test drift: commit ba2d7da ("registration observation wrap") upgraded the recorded observation to the schema-valid full shape; these assertions still expect the old thin `{:resource-id :repository-path :common-git-dir}` shape. Code is more correct; fixtures are stale. | fixture rework, folds into ENG-017C/D |
| C — real `register!` return-contract bug | 3 | `profile-test/bootstrap-local-mode-idempotent-by-request-id` (=), `registration-test/registration-observation-is-idempotent-by-request-id` (×3) | **Genuine defect.** `register!` (registration.clj:15-16 vs 33-36) returns the *full stored observation* on the idempotent path and a *thin projection* on the fresh path, so `first-result ≠ second-result`. The registration_test fake also keys by `:request-id` while the code emits `:observation/request-id` (the real in_memory adapter keys correctly by `:observation/request-id`, in_memory.clj:54) — a test-fake bug that compounds it. | ENG-001B2 / US-001 (both **done**) carry the latent bug; caught by ENG-017D adapter laws / ENG-017G command-result contract |

The correction matters twice: the "all http" claim was itself an unverified
inference, and cluster C is a real contract bug hiding inside two *done*
registration cards — a fresh instance of the audit's own thesis. Cluster C is
the highest-value follow-up: registration idempotency, marked done, does not
actually hold as a stable return contract.

## Incoming-end rework

All 10 ENG-017 cards were reworked from "administratively valid" to the story
contract (`docs/process/document-governance.md` + the template in inbox
`10.21.21`): operational intent, decision context with design/ADR citations,
concrete scope from real source locations, testable invariants, a
claim→evidence→command verification table, traceable acceptance criteria, a
completion-evidence requirement, and a "would have gated" clause tying each
card to the audit findings above. Originals preserved in git history and in
the session scratchpad.

One new card was created for a defect no card covered:

- **ENG-017K** — replace reader-eval EDN parsing at external boundaries
  (`http.clj:97,338,342`, `lucene.clj:143`). Verified remote-code-execution
  vector; 2 pts; zero dependencies so it can land first.

### Promotions (authority: user instruction this session)

| State | Cards |
|---|---|
| ready | ENG-017A, B, C, D, G, H, K |
| accepted (held) | ENG-017E, F, I, J |

Held cards carry explicit blocker comments: **E** — no decision on ephemeral
Mongo in CI (promoting would let done mean "passed on one machine"); **F/I/J**
— blocked by dependency state, with the unblocked-slice cut noted where one
exists. ENG-017A is the exemplar; the exemplar corpus is seeded at
`docs/standards/examples/story/{good,bad}-schema-operation-registry*.md`.

## False-done → gating-check connections

Recorded on both ends (demoted card comment names the gate; assurance card's
"would have gated" clause names the card). Summary:

| Demoted / caveated card | Would have been gated by |
|---|---|
| ENG-006A (HTTP, red suite, RCE, no parity) | ENG-017G (command parity), ENG-017K (EDN boundary), ENG-017H (static gates) |
| ENG-006C (placeholder empties) | ENG-017I (empty≠unavailable law), ENG-017F (read integrity) |
| ENG-004A/B/D, 005B/F (phantom CLI) | ENG-017G (enumerable command table) + kanban completion-evidence rule |
| ENG-021A (restore-drill stub) | ENG-017F (manifest/integrity primitives) |
| US-000B (permissive oracle shipped) | ENG-017C (contract-enforcing reference adapter) |
| ENG-001A (validation never generalized) | ENG-017E (all Mongo writes on shared laws) |
| all "unit tests pass" done claims | ENG-017C + the CI evidence artifact (ENG-017J) |

## Lessons

1. **Done was self-attested and unguarded.** The board FSM policy exists
   (`docs/process/kanban.md`) but nothing mechanically checks transitions or
   demands evidence at them. This is the board-mechanics instance of the
   gaming pattern (see [[inbox-synthesis-2026-07-12-agent-gaming-pattern]]).
   Proposal candidates 1–3 there (transition-legality check, evidence-on-
   transition, transition ledger) are the concrete fix; raise at ENG-017 triage.
2. **Criteria quality is card quality.** US-000B was honestly done against its
   criteria and still institutionalized the central defect. A card can be
   "correctly done" and wrong if its acceptance criteria don't require the
   thing that matters. The reworked story contract's verification table is the
   countermeasure.
3. **Grade honesty separately from correctness.** The D/F split keeps an agent
   that disclosed incompleteness from being punished identically to one that
   claimed a nonexistent feature — and points at different fixes.
4. **A done claim can rot.** US-000A's green baseline regressed with no owner.
   Completion evidence is a point-in-time observation; only a standing gate
   (CI evidence artifact) keeps it true. This is ADR-004's "Π is not merely a
   green build" made operational.
5. **Domain-without-surface is the house style of these false dones.** Five
   cards shipped real domain namespaces (`lineage_trace`, `inbox`, `export`,
   evidence/diff) with no CLI/HTTP surface, then claimed the user-facing
   deliverable. A command that is one entry in an enumerable, tested table
   (ENG-017G) makes "the command doesn't exist" impossible to miss.

## Next actions

- Restore the green baseline: the 9 `http_test.clj` failures are ENG-017G/K
  scope; nothing else should go to done until the suite is green again.
- Raise the transition-enforcement decision-candidate at ENG-017 triage.
- Work the ready cards in dependency order (K and A first — both unblocked and
  P0); re-verify each demoted card against its gating assurance card as that
  card lands, moving review→done only with recorded evidence and a named
  reviewer.
