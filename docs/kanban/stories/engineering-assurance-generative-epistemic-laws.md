---
category: "stories"
labels: ["quality", "property-testing", "metamorphic", "epistemic", "phase-1"]
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001704", "01900d7c-7f3a-7e8b-9c4d-000000001706"]
phase: "1"
type: "story"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
write-id: "1784947707957-0.dsu8d5wy04whzpl8rm4"
points: "5"
verification: ["unit-test"]
risk: "medium"
title: "ENG-017I: Add generative and epistemic verification laws"
priority: "P1"
status: "done"
id: "01900d7c-7f3a-7e8b-9c4d-000000001709"
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
---

# ENG-017I: Add generative and epistemic verification laws

## Intent

Fixed fixtures prove only the cases someone thought of — and can be written
to match a stub (the audit found `workbench_test.clj` asserting its own
placeholder). Generative and metamorphic laws test what must remain true
across generated inputs: schema closure, idempotency, backup round trips,
and above all the epistemic rule that unknown / unavailable / corrupt /
empty are never collapsed.

## Decision context

Implements `docs/designs/verification-architecture.md` § "Domain properties"
under ADR-004 decisions 5–6. Anchors: ENG-006C demotion (placeholder-empties
presented as data), the charter's "empty evidence is a claim" rule, and the
adversarial test classes listed in inbox `06.22.56` (routed via ADR-004 §8).

## Scope

- Generators for valid records per registered schema (Malli generators where
  structural, hand-curated for identity/Git evidence types).
- Mutation-style properties: valid closed record + undeclared key → rejected;
  UUID→string → rejected; version bump → version-mismatch; single-field
  mutations rejected without state change (run via ENG-017D harness).
- Metamorphic laws: same request-ID replay idempotent / changed-content
  conflict; export→import→export equivalence; corruption fixtures fail
  pre-mutation (extends ENG-017F fixtures generatively).
- Epistemic laws: for each read surface, generated scenarios prove
  `:source/unavailable`, `:integrity/corrupt`, unsupported-version, and
  genuinely-empty are pairwise distinguishable — a placeholder/unimplemented
  query must surface as not-implemented/unavailable, never `[]`.
- Every generated suite prints its seed; failed seeds replay locally; minimal
  counterexamples preserved where the runner supports shrinking.
- Machine-readable coverage matrix: required law categories per registered
  operation; a registered op missing a category fails a completeness test.

## Non-goals

- No mutation-testing tooling (ENG-017J). No new outcome vocabulary —
  consumes ENG-017F's. No workbench UI changes (the demoted ENG-006C rework
  consumes these laws; it stays its own card).

## Invariants

- A property failure is reproducible from its printed seed.
- The coverage matrix is data; adding a persistence op without declaring its
  law coverage turns the suite red.

## Verification

| Claim | Evidence | Location |
|---|---|---|
| Generated invalid writes rejected statelessly | Property over all registered ops via law harness | generative test ns |
| Epistemic outcomes not collapsed | Pairwise distinguishability properties | epistemic law ns |
| Seeds replay | Re-run with recorded seed reproduces failure (demonstrated once in card comment) | runner output |
| Coverage matrix enforced | Completeness test red on undeclared op (fixture) | matrix test |

## Acceptance criteria

- All properties green under `clojure -M:unit-test` with seeds printed.
- Coverage matrix lists every registered operation with its satisfied law
  categories; the negative fixture proves the matrix has teeth.

## Dependencies and interfaces

- Depends on ENG-017D (harness) and ENG-017F (outcome vocabulary +
  corruption fixtures).
- Provides to ENG-017J: replayable properties its evidence artifact records;
  provides to the ENG-006C rework: the empty-vs-unavailable laws its screens
  must satisfy.

## Risks and open questions

- Generator quality for Git-evidence types may exceed structural schemas;
  hand-curate rather than over-trust `malli.generator` defaults.

## Completion evidence

Property run output with seeds, one demonstrated replay, the coverage matrix
EDN, `git diff --stat`, reviewer named at done.

## Would have gated

ENG-006C's placeholder screens (empty-as-data violates the distinguishability
law) and the test-the-stub pattern generally: a generated law cannot be
written to match a hardcoded placeholder.

---
REWORK 2026-07-12: body rewritten to the story contract (original preserved in git history and scratchpad; see ENG-017A comment for the shared rework rationale). Triage authority: user instruction this session. --tasks-dir docs/kanban

HELD AT ACCEPTED 2026-07-12: dependencies ENG-017D (ready) and ENG-017F (accepted, not ready). Promote when F meets the gate. --tasks-dir docs/kanban

TRIAGE 2026-07-25: accepted -> ready. Dependencies now done: ENG-017D (done), ENG-017F (done 2026-07-25, commits 5d9f9e8 + 69670ea + clean review wave). Points 5 at cap, acceptance criteria present, design link present.

IMPLEMENTED 2026-07-25 (commits e42564c + 77d636a).

Delivered:
- test/epiphany/law_suite/generators.clj: hand-curated test.check generators per write op (Git-evidence fields vary within legal alphabets per the card's risk note; malli.generator defaults not trusted for OID/inst/enum). UUIDs derive from test.check's PRNG — never the global RNG — so seeds replay exactly.
- Mutation laws over ALL 7 registered write ops: undeclared-key, uuid-as-string, drop-envelope-key → rejected WITHOUT state change (export-all before/after); version-bump → :schema-version-mismatch. A sanity test proves the generators themselves produce schema-valid records (else the mutation laws would be vacuous).
- Metamorphic laws: replay idempotency + changed-content conflict on generated repository-locations; export→import→export payload equivalence on generated payloads.
- Epistemic law: on generated scenarios, :source/unavailable, :integrity/corrupt, :integrity/unsupported-version, and genuinely-empty import are pairwise distinguishable, never collapsed.
- Coverage matrix as data: every registered write op declared with its satisfied law categories; completeness test reds on an undeclared op (negative fixture included).
- Seeds: every property prints `SEED <label> <n>`; re-run with :seed.
- REPLAY DEMONSTRATION (card AC): deliberately-failing property (undeclared key must be accepted — false) run twice with :seed 424242 → identical shrunk counterexample both runs (`identical replay: true`). Note this required making the generators PRNG-pure; an earlier version using (random-uuid) replayed the FAILURE but not the counterexample.
- Dependency policy: org.clojure/test.check 1.1.1 added to test aliases only — justification: this card's generative scope. Production :deps untouched.

Gates: 737 tests, 1988 assertions, 0 failures. Kondo 0 warnings on new files.

AUDIT 2026-07-25 (ultra-code wave, .ημ/workflows/eng-017i-review.edn): 3 lenses, 18 skeptic votes, quorum 2. 12 findings raised, ALL refuted by quorum — 0 confirmed. Vacuity probes (generator variance, mutation teeth, epistemic collapse scenarios, coverage holes, seed replayability) all failed to land.

REVIEW 2026-07-25: approve. Ultra-code wave returned zero confirmed findings; replay demonstration (seed 424242, identical shrunk counterexample) is in the IMPLEMENTED comment. Gates: 737 tests, 1988 assertions, 0 failures.

GATE 2026-07-25: bin/kanban-done-gate exit 0. document->done via rheos MCP failed server-side ('paths[0]' type error — known bug, chore card exists). Status advanced by direct frontmatter edit with this audit trail.
---