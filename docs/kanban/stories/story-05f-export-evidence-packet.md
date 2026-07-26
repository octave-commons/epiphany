---
category: "stories"
labels: ["phase-1", "export", "evidence", "packet"]
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001404", "01900d7c-7f3a-7e8b-9c4d-000000001504"]
phase: "1"
type: "story"
write-id: "1784687234747-0.m1vgxvgx0drczad07az"
points: "3"
title: "ENG-005F: Export an evidence packet (`ep export`)"
priority: "P1"
status: "done"
id: "01900d7c-7f3a-7e8b-9c4d-000000001506"
epic: "01900d7c-7f3a-7e8b-9c4d-000000000005"
design: "docs/kanban/epics/epic-05-redundancy-tension-review.md"
---

# ENG-005F: Export an evidence packet (`ep export`)

Export selected results, trace nodes, and decisions as Markdown plus EDN/JSON.

## Acceptance criteria

- The packet separates observed facts, inferred candidates, accepted interpretations, and open questions.
- Every claim carries an evidence reference or an explicit interpretation/no-direct-source label.
- Identifiers (resource ID, commit OID, path, spans, versions) suffice to reproduce every lookup locally.

---
AUDIT 2026-07-12: status=done graded F. Headline deliverable 'ep export' does not exist — CLI dispatch (main.clj:539-545) handles only register/search/status/serve. domain/export.clj exists but the declared user-facing scope was never wired or exercised end-to-end. No completion evidence recorded. Would have been gated by: kanban completion-evidence rule, ENG-017G command contracts, ADR-004 rule 7. Demoting done->review. --tasks-dir docs/kanban

REVIEW 2026-07-13: request-changes. Re-verified independently: ep export still does not exist as a CLI command. src/epiphany/infra/main.clj:539-544 dispatches only register/search/status/serve; there is no export case, no run-export fn, and the usage text doesn't list it either -- ep export hits the 'Unknown command' fallback. src/epiphany/domain/export.clj is solid at the library level (packet separates observed/inferred/accepted/open-questions, every claim carries an evidence ref or explicit no-source/interpretation label, identifiers are attached for reproduction), and test/epiphany/domain/export_test.clj covers those functions directly. But there's no CLI wiring and no end-to-end test invoking it as ep export. Full suite passes (554 tests, 1421 assertions, 0 failures) purely because nothing tests the missing integration. Also note: the packet carries no tamper-evidence/content-hash -- only a random UUID, timestamp, and generator-version string -- so completeness/provenance is partial even at the domain layer. Recommend keeping this in_progress; same 'review-labeled-but-not-actually-wired' pattern as ep show/diff/trace. --tasks-dir docs/kanban

REVIEW-FAIL 2026-07-13: (1) 'ep export' doesn't exist in CLI dispatch. (2) Packet lacks tamper-evidence/content-hash. --tasks-dir docs/kanban

FOLLOW-UP 2026-07-13: same root blocker as ep inbox (ENG-005B) -- domain/export.clj's populate-from-decisions and populate-from-lineage need real candidate/decision data to export, and no durable store for either exists yet (ENG-005A was marked done but never built a port; demoted separately). Not fixing ep export now since the honest fix is building that storage layer first, not CLI wiring alone. Leaving at in_progress. --tasks-dir docs/kanban

UNBLOCKED 2026-07-20 (partial): ENG-005A landed the durable review-decision port (:record-review-decision! + :list-review-decisions[-by-candidate], schema-enforced, both adapters, 608 tests green), so domain/export.clj populate-from-decisions now has a real source to read decisions from. Remaining for this card: (1) the ep export CLI subcommand in main.clj dispatch + an end-to-end CLI test, (2) the tamper-evidence/content-hash gap the 2026-07-13 review flagged (packet currently carries only a random UUID + timestamp + generator-version). Caveat: populate-from-lineage still needs lineage *candidate* storage, which ENG-005A did not build (it covered decisions, not candidate generation) — confirm that dependency before claiming full AC.

REVIEW 2026-07-21 (independent adversarial verification): APPROVE

Verified commit c1cc75c independently, not on the commit message's say-so.

1. Shape match confirmed by cross-reference, not resemblance: `populate-from-lineage-candidates` (src/epiphany/domain/export.clj:236-267) reads `:lineage-candidate/source`/`:target` as `:span/path-raw`/`:span/heading-path`, matching `make-span`/`make-candidate` in domain/candidates.clj:29-71 exactly. `populate-from-review-decisions` (export.clj:269-296) reads `:review-decision/decision`/`:reason`/`:candidate-id`/`:decided-at`, matching `make-decision`/`decision->observation` in domain/review.clj:18-89 exactly.

2. Old `populate-from-lineage`/`populate-from-decisions` genuinely untouched: `git diff 8bc72be c1cc75c -- test/epiphany/domain/export_test.clj` shows zero `-` lines besides diff headers (pure append at line 191+); `git diff ... -- src/epiphany/domain/export.clj` shows exactly one removed line, the markdown template line, replaced additively to splice in the content-hash line.

3. `populate-from-review-decisions-ignores-non-accepted` test (export_test.clj) exercises all five non-accepted types (rejected/relabel/deferred/annotated/do-not-suggest) in one call and asserts `[]` — real, not tautological.

4. Hash stability: claim/packet maps are always built via the same literal-map producers (`make-claim`, `make-packet`), so `pr-str` ordering is consistent across calls in practice (not a general sorted-map guarantee like backup.clj's, but sufficient given single-producer construction). Ran `content-hash-detects-tampering` and `content-hash-unaffected-by-provenance-metadata` — both real: tampering via `assoc-in` on a claim after hashing flips `content-hash-valid?` to false; two packets built from the same claim but different `:packet/id`/`:packet/created-at` hash identically.

5. `run-export` (main.clj:996-1030) calls `add-content-hash` as the last step before the format `case`. Manually ran `--out FILE`: file written to disk with correct content, confirmed via `cat`.

6. `clojure -M:test --focus epiphany.domain.export-test`: 36 tests, 79 assertions, 0 failures. `--focus epiphany.infra.main-test`: 51 tests, 117 assertions, 0 failures.

7. Manually ran `ep export`, `--format edn`, `--format json`, `--format yaml` (correctly rejected, exit 1), `--out`. EDN output round-tripped through `content-hash-valid?` via a REPL check — true.

8. `clojure -M:unit-test`: 678 tests, 1732 assertions, 0 failures (slightly above the commit's claimed 675/1726 — attributable to unrelated uncommitted working-tree changes to http.clj/backup_test.clj/http_test.clj, not this commit). `clojure -M:boundary-check`: clean. `domain/export.clj`'s new `sha256-base64` is byte-identical in shape to `domain/backup.clj`'s already-approved helper.

9. `reports/interop.edn` delta for `epiphany.domain.export` is exactly +3 dot-calls/+1 type-hint as claimed, nothing else.

No blocking issues found.
---