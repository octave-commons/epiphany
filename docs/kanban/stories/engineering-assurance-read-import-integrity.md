---
category: "stories"
labels: ["quality", "backups", "decoding", "integrity", "phase-1"]
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001701", "01900d7c-7f3a-7e8b-9c4d-000000001705"]
phase: "1"
type: "story"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
write-id: "1784944911032-0.bgt09zi0goaeg73inuo"
points: "5"
verification: ["unit-test", "integration-test"]
risk: "medium"
title: "ENG-017F: Validate decoded and imported observation data"
priority: "P1"
status: "done"
id: "01900d7c-7f3a-7e8b-9c4d-000000001706"
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
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

TRIAGE 2026-07-25: accepted -> ready. Dependencies now done: ENG-017A (done), ENG-017E (done 2026-07-24, commit 91ab60f + review wave). Points 5 at cap, acceptance criteria present, design link present.

IMPLEMENTED 2026-07-25 (commit 5d9f9e8).

Delivered:
- law/operations: collection-schemas — the logical export/import vocabulary ("repository-location" etc.) mapped to each collection's schema — plus expected-record-version. THE COLLECTION-NAME QUESTION, ANSWERED: the logical keys are the canonical backup/export vocabulary; physical Mongo collections are `<prefix><logical>-v1`; the two-way mapping is (a) logical->schema in law/operations/collection-schemas, (b) logical->physical in mongo connect!/import case; export-all keys == collection-schemas keys is now pinned by test (collection-vocabulary-is-explicit-and-consistent). inaccessible-sources reads the logical "repository-location" key from backup payloads — consistent with export-all, so it cannot silently inspect an empty list due to a naming mismatch (and the mapping is now test-pinned).
- domain/backup: validate-record + validate-backup-payload — parse, format, version, collections, counts, content-hash, and per-record schema+version all validated BEFORE any mutation; read-backup-file gives missing file = :source/unavailable, truncated/unparseable = :integrity/corrupt, never a raw reader exception.
- mongo: every read path (find/list/export) decodes through decode-validated — decode exceptions AND schema failures AND unknown versions become named integrity findings (:integrity/corrupt / :integrity/unsupported-version), never silently omitted, defaulted, or collapsed into an empty result. import-all validates the whole payload before any insert.
- in-memory: import-all validates upfront (mutation-safety parity); export-all now returns vectors — it returned LISTS, which broke canonical payload equality against Mongo's vector shape (found via the round-trip test).
- Tests: 7 corruption fixtures (truncate, edited count, flipped checksum, bumped manifest version, schema-violating record, unknown record version, unknown collection) each proven to fail BEFORE mutation with state-unchanged proof; 4-outcome distinctness; canonical export→import→export round trip (in-memory + real Mongo); seeded corrupt + future-version docs on real Mongo surface the named categories.

Verification rows: corrupt backup fails pre-mutation (unit) ✓; malformed stored doc = integrity finding (integration) ✓; round trip preserves canonical data (integration) ✓; four outcomes distinct (unit) ✓.

Gates: 723 tests, 1900 assertions, 0 failures (unit); 20 tests, 99 assertions, 2 failures (integration — both pre-existing baseline). Kondo 0 warnings on touched files.

AUDIT 2026-07-25 (ultra-code wave, .ημ/workflows/eng-017f-review.edn): 3 lenses, 18 skeptic votes, quorum 2. 3 confirmed findings (2 unique, all should-fix), ALL FIXED in 69670ea:
1. Manifest :collections/:content-hash were opt-out (when-guarded) — deleting them bypassed all tamper detection, the permissive legacy branch the card's own Risks section forbids. Both are now REQUIRED; absence is itself :integrity/corrupt. New regression tests for both deletion fixtures.
2. Existing-but-unreadable backup file collapsed :source/unavailable into :integrity/corrupt. read-backup-file now splits exists/canRead/IOException (:source/unavailable) from EDN parse failure (:integrity/corrupt). Regression test with chmod-000 fixture.
9 other findings refuted by quorum. Gates after fixes: 727 tests, 1908 assertions, 0 failures (unit); integration 20/99, 2 pre-existing baseline only.

REVIEW 2026-07-25: approve. All confirmed findings from the ultra-code wave fixed in 69670ea with regression tests; no blockers. Gates: 727 tests, 1908 assertions, 0 failures.

GATE 2026-07-25: bin/kanban-done-gate exit 0. document->done via rheos MCP failed server-side ('paths[0]' type error — known bug, chore card exists). Status advanced by direct frontmatter edit with this audit trail.
---