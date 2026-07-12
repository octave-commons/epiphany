---
id: "01900d7c-7f3a-7e8b-9c4d-000000001706"
title: "ENG-017F: Validate decoded and imported observation data"
status: "accepted"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
points: 5
labels: ["quality", "backups", "decoding", "integrity", "phase-1"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001701", "01900d7c-7f3a-7e8b-9c4d-000000001705"]
verification: ["unit-test", "integration-test"]
risk: "medium"
---

# ENG-017F: Validate decoded and imported observation data

## Intent

Write validation alone leaves the read side trusting whatever is stored:
Mongo decodes, backup imports, and fixture loads can admit malformed maps
that then masquerade as evidence. This slice validates everything decoded or
imported against its claimed schema, and makes corrupt / unsupported-version
/ unavailable / empty four distinct, non-collapsible outcomes.

## Decision context

Implements `docs/designs/verification-architecture.md` § "Read path" under
ADR-004 decision 6 ("unavailable, corrupt, empty, and not-implemented
outcomes must remain distinguishable"). Directly targets the verified backup
defects: `import-from-file` checks only `:format` (defect inventory, claimed
set), `restore-drill` executes one of five documented stages
(`backup.clj:81-108`, observed), and the suspected logical-vs-physical
collection-name mismatch that would make `inaccessible-sources` silently
inspect an empty list.

## Scope

- Post-decode validation for every Mongo read path, via the ENG-017A registry.
- Backup manifest contract: format/version, expected collections, per-
  collection counts, and a SHA-256 of the canonical payload; `import-from-file`
  validates all of it **before** any mutation.
- Resolve the logical-vs-physical collection-name question with a recorded
  observation and an explicit two-way mapping + round-trip test.
- Distinct outcome data for: `:integrity/corrupt`, `:integrity/unsupported-
  version`, `:source/unavailable`, and genuinely empty results.
- Canonical export → import → export round-trip test proving semantic
  equivalence.

## Non-goals

- Implementing the full restore drill (that is ENG-021A's declared scope —
  this card provides the integrity primitives 021A's drill must call).
- No new backup format design beyond the manifest fields above.

## Invariants

- A backup that fails any manifest check mutates nothing.
- A malformed stored document becomes a named integrity finding — never
  silently omitted, defaulted, or returned as an empty result.
- An unknown future schema version is never decoded as the nearest known one.

## Verification

| Claim | Evidence | Location |
|---|---|---|
| Truncated/altered/unknown-version backup fails pre-mutation | Corruption fixtures: truncate, edit manifest count, flip checksum, bump version | backup integrity tests (unit) |
| Malformed stored doc = integrity finding | Seed invalid doc, read path returns finding not `[]` | Mongo decode tests (integration) |
| Round trip preserves canonical data | export→import→export equivalence | integration |
| Four outcomes stay distinct | One test per pair that must not collapse | unit |

## Acceptance criteria

- All corruption fixtures fail before any write; assertions include
  state-unchanged proof.
- The collection-name mapping question is answered with observed evidence in
  a card comment and covered by a round-trip test.
- Unit + integration verification rows all pass.

## Dependencies and interfaces

- Depends on ENG-017A (schemas per collection) and ENG-017E (validated
  write side).
- Provides to ENG-021A: manifest validation + integrity outcomes its restore
  drill must consume. Provides to ENG-017I: the outcome vocabulary its
  epistemic laws generate against.

## Risks and open questions

- Existing dev backups may fail the new manifest checks; define a documented
  one-time re-export path rather than a permissive legacy branch.

## Completion evidence

Test output for every corruption fixture, the collection-name observation,
`git diff --stat`, reviewer named at done.

## Would have gated

ENG-021A could not have drifted toward an export-only "drill" with these
primitives required, and the workbench placeholder-empties (ENG-006C, demoted
in the 2026-07-12 audit) violate exactly the empty-vs-unavailable law this
card makes testable.

---
REWORK 2026-07-12: body rewritten to the story contract (original preserved in git history and scratchpad; see ENG-017A comment for the shared rework rationale). Triage authority: user instruction this session. --tasks-dir docs/kanban

HELD AT ACCEPTED 2026-07-12: dependency ENG-017E is accepted but not ready (CI service decision pending). Per the unblocked-slice rule (docs/process/design.md), the manifest/corruption-fixture portion is insulated from that question and could be split out if E stalls — record the cut as a child card rather than starting F whole. --tasks-dir docs/kanban
---
