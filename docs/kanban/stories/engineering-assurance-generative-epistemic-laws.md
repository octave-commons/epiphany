---
id: "01900d7c-7f3a-7e8b-9c4d-000000001709"
title: "ENG-017I: Add generative and epistemic verification laws"
status: "accepted"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
points: 5
labels: ["quality", "property-testing", "metamorphic", "epistemic", "phase-1"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001704", "01900d7c-7f3a-7e8b-9c4d-000000001706"]
verification: ["unit-test"]
risk: "medium"
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
---
