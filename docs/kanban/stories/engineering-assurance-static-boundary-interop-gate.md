---
category: "stories"
labels: ["quality", "static-analysis", "architecture", "interop", "phase-1"]
dependency: [""]
phase: "1"
type: "story"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
write-id: "1784568011290-0.72ctui02u8iwj1iepp"
points: "5"
verification: ["lint"]
risk: "low"
title: "ENG-017H: Add static architecture and interop boundary gates"
priority: "P1"
status: "review"
id: "01900d7c-7f3a-7e8b-9c4d-000000001708"
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
---

# ENG-017H: Add static architecture and interop boundary gates

## Intent

Give the repo a deterministic static floor: a pinned linter with a clean
baseline, an architecture-boundary check for the STYLE.md layer laws, and a
Java-interop inventory with a ratchet. Verified today: no
`.clj-kondo/config.edn`, no `.splint.edn`, no `:lint` exists — the
"Chore-000: static analysis" draft (inbox `06.12.04`: splint, clj-kondo,
heretic) folds into this card. cloverage is handled separately by ENG-017L.

## Decision context

Implements `docs/designs/verification-architecture.md` §§ "Static and
structural checks" and "Interop inventory" under ADR-004 decision 5, and the
STYLE.md engineering kernel's layer laws (`law`/`shape`/`extern`/`domain`/
`infra` dependency direction).

## Scope

- `:lint` alias + `.clj-kondo/config.edn`: unresolved-symbol/var and
  invalid-arity as errors; establish and commit a reviewed clean baseline
  first, then ratchet warnings.
- Splint evaluation per the chore draft: add `.splint.edn` reporting-only;
  promote selected rules later — adoption decision recorded, not assumed.
- Layer-boundary checker (clj-kondo analysis output or namespace scan):
  `law` requires no `domain`/`application`/`infra`; `domain`/`application`
  require no `infra`; `infra.http`/`infra.main` call services, not adapters.
- Interop inventory artifact (`reports/interop.edn` or CI artifact):
  namespace-level imports, type hints, constructor/static calls, dot calls.
  Baseline reviewed and committed; ratchet = no new direct interop in
  `law`/`domain`/`application` without a dated exception.
- Wire `:lint` + boundary check into CI as a required job alongside
  `unit-test`.

## Non-goals

- No blanket Java prohibition — adapter interop (Mongo 238 / Lucene 65 /
  Git 47 dot calls per the conversation's counts) is measured, not banned.
- No `extern.*` migration (STYLE.md's staged-migration posture; moves happen
  at natural seams under their own cards).
- No mutation tooling (ENG-017J). Coverage reporting is ENG-017L.

## Invariants

- Static checks are deterministic and run locally via `deps.edn` aliases.
- Baselines change only by reviewed commit — no silent reset.
- An exception to the ratchet is a dated, owned record, not a config tweak.

## Verification

| Claim | Evidence | Location |
|---|---|---|
| Lint runs clean on baseline | `clojure -M:lint` exit 0 in CI + locally | CI job `static` |
| Boundary rule has teeth | Fixture ns violating a layer law fails the check | boundary-check test |
| Inventory is reproducible | Two consecutive runs produce identical EDN | inventory test |
| Ratchet detects new domain interop | Fixture with a dot call in `domain.*` fails | ratchet test |

## Acceptance criteria

- CI has a required `static` job running lint + boundary + ratchet.
- Baseline files committed with a card comment recording the initial counts
  and any grandfathered exceptions.
- Splint adoption decision (adopt rules X/Y as warnings | defer) recorded in
  a card comment with its basis.

## Dependencies and interfaces

- No code dependencies (deliberately parallel to ENG-017A–D).
- Provides to ENG-017J: the interop delta and lint results its evidence
  artifact records.

## Risks and open questions

- Initial baseline may reveal many warnings; the card commits the baseline,
  not a full cleanup — ratchet forward, don't boil the ocean.

## Completion evidence

CI run link/output, committed baselines, initial inventory EDN, adoption
decisions as comments, reviewer named at done.

## Would have gated

The bare `read-string` boundary calls (ENG-017K's defect) are exactly what a
`discouraged-var` lint rule catches; domain-layer `UUID`/`Date` leakage would
have been ratchet findings at introduction time instead of an audit discovery.

---
REWORK 2026-07-12: body rewritten to the story contract (original preserved in git history and scratchpad; see ENG-017A comment for the shared rework rationale). Triage authority: user instruction this session. --tasks-dir docs/kanban

CORRECTION 2026-07-13: Removed cloverage from scope (line 27 and non-goals). Coverage reporting is now owned by ENG-017L. The original chore draft (06.12.04) listed 4 tools: splint, clj-kondo, heretic, cloverage. ENG-017H owns the first 3; ENG-017L owns cloverage. --tasks-dir docs/kanban

IN PROGRESS 2026-07-20 (session): Implementation complete against all four scope items.

1. `:lint` + `.clj-kondo/config.edn`: `unresolved-symbol`, `unresolved-var`, `invalid-arity` set to `:error` (already clj-kondo defaults, made explicit); added a `discouraged-var` error on `clojure.core/read-string` referencing ENG-017K, so a `discouraged-var` lint rule now catches exactly the class of defect that card fixed, per this card's own "Would have gated" note. Baseline: `clojure -M:lint` (alias runs clj-kondo 2025.07.28 with `--fail-level error`) → 0 errors, 90 warnings across src/test/dev, exit 0. All 90 warnings are pre-existing test-file style items (unused requires/bindings, redundant let, a couple of genuinely-unresolved test namespaces worth a follow-up: `epiphany.domain.inbox_test.clj:50`, `epiphany.infra.workbench_test.clj:148`, `epiphany.law.operations_test.clj:13` reference `clojure.string`/`clojure.java.io`/`clojure.set` without requiring them — flagged as warnings, not blocking, ratchet forward from here).

2. Splint (`noahtheduke/splint` 1.24.0, fetched fresh from Clojars — no prior pin existed): ran reporting-only via `clojure -M:splint` → 822 style warnings across 96 files, concentrated in test files (`workbench_test.clj` alone: ~50 `style/prefer-clj-string`/`lint/prefer-method-values` hits; `repository_identity_test.clj`: ~20 `prefer-method-values`). Adoption decision: **defer** — none promoted to errors this card. Committed `.splint.edn` as `{}` (accepts full default rule set, non-gating) with the deferral rationale inline. Not wired into the required CI `static` job; `clojure -M:splint` remains a manual/advisory command. Basis: the volume is almost entirely test-file style debt, not a structural signal on par with the lint/boundary/interop findings, and picking winners among 822 warnings without a dedicated cleanup pass would be assumed adoption, which the card explicitly says not to do.

3. Layer-boundary checker: `tools/epiphany/static/boundary_check.clj` + `test/epiphany/static/boundary_check_test.clj` (8 tests, 19 assertions). Pure `find-violations` function operates over a `ns-sym -> required-ns-syms` graph (testable without fixture files on disk); `scan-source-tree` builds that graph from real `.clj` files by reading only the leading `(ns ...)` form (not evaluating). Enforces exactly STYLE.md's table: `law` ⊆ `law`; `shape` ⊆ `law,shape`; `domain` ⊆ `law,shape,domain`; `application` ⊆ `law,shape,domain,application`; `infra` unrestricted. Fixture tests prove each of the four restricted quadrants fails when it reaches one layer too far (`law→domain`, `shape→domain`, `domain→infra`, `application→infra`), and a real-tree scan (`scan-source-tree-current-repo-is-clean-test`) confirms today's `src/` has zero violations. `clojure -M:boundary-check` → "Layer-boundary check: clean." exit 0.

   Scope note: did NOT attempt to mechanically enforce "infra.http/infra.main call services, not adapters" — `infra/main.clj` legitimately requires `infra.adapters.in-memory`/`infra.adapters.mongo` directly as the composition root that builds the profile-selected adapter map (matches `infra/profile.clj`'s documented job in CLAUDE.md). A precise version of that rule needs to distinguish composition-root wiring from request-path adapter calls, which is more than a namespace-require graph can tell apart; flagging as a follow-up rather than shipping a rule that would either false-positive on `main.clj` or be too narrow to mean anything.

4. Interop inventory: `tools/epiphany/static/interop_inventory.clj` + `test/epiphany/static/interop_inventory_test.clj` (9 tests, 10 assertions, including a reproducibility test — two consecutive `scan-source-tree` calls over the real tree produce an identical map — and a test that the real tree ratchets clean against the committed baseline). Counts per namespace: Java `:import` classes, instance dot-calls (`(.method ...)` ), static/constructor calls (`(Class/method ...)`), and type hints (`^Class`), via deterministic regex over each file's source text. Baseline written to `reports/interop.edn` (45 namespaces, committed) via `clojure -M:interop-inventory --write`. Ratchet only fires on `law`/`domain`/`application` namespaces growing past their baseline count — `infra`/`shape` interop (Mongo/Lucene/Git/flexmark) is measured but never ratcheted, matching the card's non-goal ("no blanket Java prohibition"). Found and recorded, not fixed (out of this card's scope): `epiphany.domain.status` already has 12 direct interop points (`.getMessage`, `.getTime`, `System/currentTimeMillis`); `epiphany.domain.backup`, `.review`, `.continuity`, `.lineage`, `.lineage_trace`, `.section_extraction`, `.inbox`, `.evidence`, `.extraction_projection`, `.benchmark`, `.markdown_selection` each have 1-4. These are exactly the "two assumption systems" leakage the chore draft (`docs/inbox/2026.07.12.06.12.04.md`) described — grandfathered into the baseline, ratcheted forward, not cleaned up here (that's a separate follow-up card, likely paired with injected `:now`/`:new-id` capabilities per the draft's suggested refactor). `clojure -M:interop-inventory` → "Interop ratchet: clean" exit 0.

5. CI: added a required `static` job to `.github/workflows/test.yml` alongside `unit-test`, running `:lint`, `:boundary-check`, `:interop-inventory` in sequence (splint excluded per item 2).

Full suite: `clojure -M:unit-test` → 586 tests, 1489 assertions, 0 failures (up from 569/1460 pre-card; +17 tests from the two new checker test namespaces).

Moving to review.
---