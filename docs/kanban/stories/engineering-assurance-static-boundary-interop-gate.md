---
id: "01900d7c-7f3a-7e8b-9c4d-000000001708"
title: "ENG-017H: Add static architecture and interop boundary gates"
status: "ready"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
points: 5
labels: ["quality", "static-analysis", "architecture", "interop", "phase-1"]
category: "stories"
dependency: []
verification: ["lint"]
risk: "low"
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
---
