---
slug: cross-repository-notes-synthesis-2026-07-26
uuid: 98e7dc37-34e2-4f7f-a472-67d20fa2ab2e
title: "Cross-Repository Notes Synthesis — 2026-07-26"
kind: note
status: draft
description: "Bounded source map, findings, and dispositions for notes and inbox material across the eta-mu constellation."
created: "2026-07-26"
labels: [inbox, notes, synthesis, provenance, eta-mu, knoxx, openplanner, muse]
sources:
  - "User direction in project conversation, 2026-07-26"
  - "docs/notes/inbox-synthesis-2026-07-12-source-map.md"
  - "open-hax/eta-mu:docs/notes/INDEX.md"
  - "open-hax/knoxx:docs/notes/2026.06.04.09.47.41.md"
  - "open-hax/knoxx:docs/notes/2026.06.03.09.09.14.md"
  - "open-hax/openplanner:docs/notes/2026.04.13.13.57.07.md"
implements:
  - "docs/process/inbox.md"
  - "docs/process/notes.md"
informs:
  - "open-hax/eta-mu:docs/architecture/contract-dialect-and-data-authority.md"
  - "docs/notes/operator-actor-correspondence.md"
---

# Cross-Repository Notes Synthesis — 2026-07-26

## Purpose

This is a bounded cross-repository intake and disposition record. It extends the
2026-07-12 Epiphany inbox synthesis method to the eta-mu constellation without
pretending that folder names, commit search, or agent summaries establish a
complete inventory or authoritative interpretation.

The immediate goals are to:

1. preserve the operator's current clarification about notes and actors;
2. identify already-routed material versus valuable residue;
3. locate malformed, stale, duplicated, or cross-boundary design notes;
4. produce small repo-local follow-ups rather than a mass metadata rewrite.

## Coverage and limitations

### Directly inspected

- Epiphany's inbox-synthesis skill and inbox/notes/document-governance policies.
- The existing Epiphany 2026-07-12 source map and observational journal.
- eta-mu's curated `docs/notes/INDEX.md` and selected load-bearing notes.
- The two Knoxx timestamped notes discoverable from repository history.
- One OpenPlanner implementation-status note discoverable from repository
  history.
- Current cross-repository architecture record in eta-mu.

### Not established as complete

The available repository interface did not provide a recursive directory tree,
and code search was unavailable or returned no results for several small repos.
Therefore:

- “not discovered” is not represented as “empty”;
- this pass does not claim that every note file was inspected;
- Muse, Katamorph, Proxx, event-ledger, and Uxx are recorded as
  `unavailable/not-observed` for `docs/notes/` and `docs/inbox/`, pending a
  source-tree or Epiphany ingestion pass;
- Sol, Rheos, and Axxium are treated as eta-mu packages rather than independent
  note repositories in this pass.

This limitation is itself product evidence: cross-repository source discovery
and note-corpus inventory should become an Epiphany operation rather than a
manual GitHub search exercise.

## Repository inventory and current disposition

| Repository | Observed corpus shape | Current disposition |
|---|---|---|
| **Epiphany** | Prior batch recorded 41 inbox files / 26 unique contents, mostly one exported assurance conversation; policies, ADRs, designs, findings, stories, and journal events already extracted | Retain originals; use the existing source map; add the present cross-repo batch and operator/actor note |
| **eta-mu** | Curated index lists 43 note entries across contracts, actors, workspace, tooling, Keryx, and research; several key notes still contain literal merge-conflict markers | Repair short malformed metadata blocks without rewriting source prose; extract bounded current syntheses; retain the long historical transcript unchanged |
| **Knoxx** | Two timestamped notes found: one session/OpenPlanner authority observation and one one-line GitHub variable command | Extract the session-authority observation into a durable working note; retain the command snippet as historical operations context with `closed-no-extraction` disposition |
| **OpenPlanner** | One implementation-status note found describing graph counts, vector-search implementation, storage assumptions, and follow-up | Retain as time-bound implementation/verification evidence; do not treat numeric state or architectural interpretations as current authority |
| **Muse** | No `docs/notes/` or `docs/inbox/` item discovered by bounded history search | `unavailable/not-observed`; inspect from a source-tree ingestion pass |
| **Katamorph** | No item discovered by bounded history search | `unavailable/not-observed`; inspect from a source-tree ingestion pass |
| **Proxx** | No item discovered by bounded history search | `unavailable/not-observed`; inspect from a source-tree ingestion pass |
| **event-ledger** | No item discovered by bounded history search | `unavailable/not-observed`; inspect from a source-tree ingestion pass |
| **Uxx** | No item discovered by bounded history search | `unavailable/not-observed`; inspect from a source-tree ingestion pass |

## Cross-repository findings

### 1. Notes are an operational event surface

The operator reports using repository inboxes and notes as active externalized
mental processing. The actor model fits because observations and concerns can
be emitted, remain partially independent, compete for attention, and later be
synthesized. This is preserved separately in
[`operator-actor-correspondence.md`](operator-actor-correspondence.md).

**Tier:** direct operator observation plus provisional design interpretation.

### 2. Resources, ledgers, and projections are a recurrent stable structure

The eta-mu notes repeatedly distinguish:

```text
resources = declarative contracts and identities
ledgers   = append-only operational records
state     = projections derived for current use
```

That structure now also explains the Epiphany/OpenPlanner boundary: Epiphany can
consume source ledgers and build evidence-governed observations and projections;
OpenPlanner can remain a replaceable graph/search projection without becoming
canonical session or knowledge authority.

**Tier:** repeated design intent, now partly reflected in the accepted
cross-repository architecture record; implementation remains incomplete.

### 3. Keryx and Muse occupy the same compiler boundary

eta-mu's indexed Keryx notes describe declaration assembly, target compilation,
OpenCode projection, tools/hooks/plugins, and host-boundary translation. Those
are substantially the responsibilities the current architecture assigns to
Muse.

The notes remain valuable implementation and vocabulary sources, but Keryx
should not silently become a second universal runtime compiler. The next
repo-local synthesis should classify each Keryx artifact as one of:

- Muse requirement or target-adapter input;
- eta-mu-native implementation detail;
- historical naming exploration;
- genuinely separate capability with an explicit boundary.

**Disposition:** `design` residue / decision-candidate; no deletion or bulk
renaming in this batch.

### 4. Knoxx's session-authority ambiguity was visible early

`docs/notes/2026.06.04.09.47.41.md` says sessions may be better stored directly
rather than sent to OpenPlanner and frames OpenPlanner as an API over data.
Later work added an in-process OpenPlanner Mongo client and treated Mongo as a
data plane. The current architecture moves further: Sol plus event-ledger own
basic session continuity; Epiphany optionally enriches it; OpenPlanner is a
projection/compatibility service.

The early note is therefore not obsolete. It is evidence of an unresolved
boundary that later implementation temporarily solved through tighter coupling.
It should inform Knoxx's decoupling design rather than be promoted directly into
a decision.

**Disposition:** `note` -> repo-local synthesis / architectural decision input.

### 5. Implementation-status notes need explicit time and evidence scope

The OpenPlanner note reports exact graph, edge, chunk, and storage counts and
then derives architectural interpretations such as “DB becomes index, not
store.” It is useful operational evidence from one revision, but:

- the counts are time-bound;
- the observation method and revision should remain attached;
- the interpretation is not a permanent data-authority decision;
- later Epiphany work may intentionally alter the storage/projection boundary.

**Disposition:** retain as historical implementation evidence; candidate for a
`verification-record` or source-linked finding if reused.

## Per-item dispositions

| Source item or cluster | Disposition | Basis / output |
|---|---|---|
| User statement, 2026-07-26 | `note` | `operator-actor-correspondence.md` |
| Epiphany 2026-07-12 inbox batch | retain prior dispositions | Existing source map already records all 41 files and extracted residue |
| eta-mu `dev/katamorph-resources-fsm-contracts.md` | repair metadata + retain | Six-line frontmatter conflict removed; core resources/ledgers/state insight retained |
| eta-mu `design/eta-mu-worlds-projections-ledger-design.md` | retain historical + extract | Long transcript left unchanged; current model extracted into `workspace-resources-ledgers-projections-synthesis.md` |
| eta-mu `design/eta-mu-init-experience-vision.md` | repair metadata + retain design intent | Seven-line frontmatter conflict replaced with one coherent summary; product-experience vision retained |
| eta-mu Keryx cluster | `design` / `decision-candidate` residue | Reconciliation note created; no source deletion or bulk move |
| Knoxx `2026.06.04.09.47.41.md` | `note` -> extract | Source-linked session-authority synthesis created in Knoxx |
| Knoxx `2026.06.03.09.09.14.md` | `closed-no-extraction` | One historical `gh variable set` command; no durable design content observed |
| OpenPlanner `2026.04.13.13.57.07.md` | retain / verification-record candidate | Time-bound implementation report; index now preserves revision/evidence scope |
| Muse/Katamorph/Proxx/event-ledger/Uxx note corpus | defer | Corpus not reliably enumerable with current interface |

## Product and process observations

1. **Cross-repository inventory is still manual.** Epiphany should ingest declared
   workspace repositories and produce a path/revision/source map for note and
   inbox corpora.
2. **Chat provenance is weak.** A user statement can be recorded by date and
   context, but there is no stable, repository-addressable source locator for a
   conversation turn.
3. **Merge-conflict markers survived note curation.** Document analysis should
   detect unresolved conflict markers even when Markdown/frontmatter otherwise
   parses.
4. **Routing needs cross-repo relations.** A note in eta-mu may inform Muse or
   Epiphany; copying it into every repository would destroy authority and
   provenance. Typed external relations or workspace-level source IDs are
   needed.
5. **Operational notes and design notes need different aging behavior.** Counts,
   commands, and status reports should become stale by revision/time, while
   durable design intent remains revisable but discoverable.

## Artifacts produced by this batch

- Epiphany operator/actor correspondence note, cross-repository source map, and
  three append-only observation events.
- eta-mu metadata repairs, workspace synthesis, Keryx/Muse reconciliation note,
  and updated note index.
- Knoxx session-boundary synthesis and note index.
- OpenPlanner implementation-note index.

## Unresolved items

- Complete source-tree inventory for all constellation repositories.
- Inspect the remaining eta-mu indexed notes against current code and the new
  contract dialect/data-authority architecture record.
- Reconcile Keryx and Muse implementation without losing useful prototype work.
- Inventory Knoxx's larger `kanban/` and contract corpus as source material for
  the eta-mu-module refactor.
- Determine whether OpenPlanner's remaining APIs merit preservation as an
  Epiphany projection protocol.

## Highest-value next pass

Inspect the actual Keryx and Muse implementations against the reconciliation
note, then classify portable compiler behavior, the OpenCode adapter, eta-mu
native runtime behavior, and historical naming artifacts with exact source
paths and migration consequences.
