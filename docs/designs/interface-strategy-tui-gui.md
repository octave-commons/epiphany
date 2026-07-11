---
slug: interface-strategy-tui-gui
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000211
title: "Interface Strategy: TUI and Browser GUI"
kind: design
description: "Proposes a CLI-first, TUI-second, browser-GUI-third interface strategy for Phase 1, with native desktop deferred."
labels: [architecture, phase-1, ui, tui, gui]
created: "2026-07-11"
source: "docs/inbox/2026.07.11.06.34.27.md"
---

Do **both eventually**, but make them serve different jobs: build a **TUI first as the Phase 1 power-user interface**, and build the **browser GUI second as the evidence-rich visual workbench**. Do **not** build a native desktop GUI first; it duplicates the browser workbench’s needs while adding packaging and desktop-platform maintenance.

A Clojure TUI can be built over Java Lanterna through Clojure wrappers such as `clj-terminal` or `clojure-lanterna`; JavaFX/Clojure options such as cljfx exist if native desktop becomes justified later. [github](https://github.com/middlesphere/clj-terminal)

## Recommendation

```text
Phase 1A: CLI
  -> deterministic commands, scripting, bootstrap, repair

Phase 1B: TUI
  -> fast interactive corpus navigation and review

Phase 1C: Browser GUI
  -> rich evidence inspection, comparison, timeline, graph/map views

Later only if necessary: native desktop shell
  -> offline bundled workstation experience
```

The TUI and GUI must be **presentation adapters**, just like CLI and REST. They call the existing command/query boundary through direct mode or REST; they do not read Mongo, Elasticsearch, Git objects, or graph storage directly.

```text
CLI ─┐
TUI ─┼─> command/query application boundary ─> ports/adapters
GUI ─┘
```

## What each is for

| Interface | Best at | Not best at |
|---|---|---|
| CLI | Automation, scripts, bootstrap, diagnostics, repair, piping EDN | Rapid browsing of many search hits and candidate links |
| TUI | Keyboard-first search, evidence triage, review queue, queues/jobs, quick comparisons | Dense visual timelines, rich Markdown/table rendering, graph exploration |
| Browser GUI | Source reading, side-by-side diff, timeline/lineage, entity graph, geospatial map, visual review | Headless repair, shell composition, bootstrapping a broken system |
| Native desktop GUI | Fully packaged offline workstation; deep OS integration | Early-phase velocity and avoiding duplicate UI work |

## Why a TUI is worth it

This system’s primary user is a person who has accumulated an enormous textual corpus and wants to ask: “What did I already think about this?”

That loop is extremely keyboard-shaped:

```text
type query
  -> scan ranked results
  -> open evidence
  -> compare older/newer passage
  -> accept/reject a candidate
  -> create research question
  -> return to search
```

A terminal interface makes that loop fast without requiring mouse travel, browser tabs, or a graphical server. It will also work naturally over SSH, which is useful when the Ultra 9/4070 Ti box is doing the work while you are sitting at a different machine.

The TUI is also the best interface for operational work:

```bash
ca tui
```

```text
┌ Corpus Archaeology ─ local / direct ───────────────────────────┐
│ Search: [repository identity after moving notes             ]   │
├──────────────────────── Results ───────────────────────────────┤
│ 1  0.841  2025-03-22  .ημ/architecture/identity.md             │
│    Architecture › Repository continuity                         │
│ 2  0.803  2024-08-14  docs/corpus.md                            │
│    Archaeology › Identity                                       │
│ 3  0.767  2023-04-07  .ημ/archive/continuity.md                │
├──────────────────────── Evidence ──────────────────────────────┤
│ A resource ID follows a repository even when its location…     │
│                                                                  │
│ observed · commit 31f95c · lines 12–29 · hybrid 0.841          │
├────────────────────────────────────────────────────────────────┤
│ Enter open · Tab panels · c compare · l lineage · r review     │
│ q quit · ? help                                                 │
└────────────────────────────────────────────────────────────────┘
```

## TUI scope

The TUI should not try to reproduce every future GUI visualization. It should optimize for the **archaeology loop**.

### Phase 1 TUI screens

| Screen | Primary action | Required detail |
|---|---|---|
| Search | Find sections | Query, mode, scope, ranked results, result status |
| Evidence | Verify a hit | Raw/rendered text, commit/path/span, context, metadata |
| Compare | Decide continuity | Side-by-side sections/revisions, diff, signals |
| Lineage | Follow an idea | Chronological evidence chain with observed/provisional/accepted states |
| Review inbox | Judge candidates | Accept, reject, relabel, defer, suppress similar |
| Research questions | Capture next work | Create/show questions tied to evidence |
| Sources | Manage local repos | Registration state, family, locations, scan state |
| Operations | Keep system healthy | Projection status, failures, retries, source availability |

### TUI interaction rules

- **Keyboard-first; mouse support optional.**
- **One focused action per screen.**
- **Visible status marker everywhere:** `OBSERVED`, `PROVISIONAL`, `ACCEPTED`, `REJECTED`, `STALE`, `UNAVAILABLE`.
- **Never render an inferred link as if it were Git history.**
- **No destructive action without an explicit confirmation.**
- **All actions must have a non-interactive CLI equivalent.**

For example:

```text
TUI action:
  [a] Accept candidate cand_01J...

Equivalent:
  ca review decide cand_01J... accept
```

This keeps the TUI convenient without making it an opaque alternate system.

## TUI technology choice

Because you strongly prefer Clojure, start with **Lanterna on the JVM**, accessed through a Clojure wrapper.

- `clj-terminal` wraps Lanterna 3 for console-mode text UIs. [github](https://github.com/middlesphere/clj-terminal)
- `clojure-lanterna` is another Clojure-oriented wrapper around Lanterna. [github](https://github.com/MultiMUD/clojure-lanterna)

I would create a thin local TUI architecture:

```clojure
{:tui
 {:state {:screen :search
          :focused-panel :results
          :query ""
          :selected-result-id nil
          :status-message nil}
  :commands {...}
  :keymap {...}
  :renderer {...}}}
```

But its data comes from the same query layer:

```clojure
(search-corpus system query)
(get-evidence system query)
(compare-expressions system query)
(get-lineage system query)
(list-review-candidates system query)
(record-review! system command)
```

The TUI should call the **direct application-service adapter by default** when running on the source-owning machine, and use the explicit REST profile if it is connecting to the centralized service:

```bash
ca tui
ca --profile cluster tui
```

## Browser GUI scope

The browser GUI is where the platform becomes spatial and evidence-dense rather than merely functional.

Build it after the TUI proves the important flows and the REST API has stabilized. The GUI should call REST only; it must not get an in-process MongoDB/Git bypass.

### Phase 1 GUI views

- **Search workspace:** query builder, scopes, lexical/semantic/hybrid toggle, result list, evidence preview.
- **Evidence reader:** rendered Markdown beside exact raw source; source metadata pinned in a stable panel.
- **Revision comparison:** side-by-side rendered/raw sections, diff, continuity signals, boundary-candidate explanation.
- **Lineage timeline:** dated chain of source expressions and relation edges; visual distinction between observed, accepted, and provisional.
- **Review workspace:** evidence-first candidate cards with decision controls and rationale field.
- **Source health:** registered repositories, availability, projection status, failures, repair/replay requests.
- **Research-question board:** explicit user-created questions, supporting evidence, unresolved candidates.

Do **not** lead with a graph visualization. A graph display is attractive but can become an expensive “hairball browser” before the evidence/review loop is good. Add graph views only once curated concepts and accepted edges make it navigable.

Likewise, defer maps until external geopolitical/geospatial datasets actually exist. Elasticsearch may support spatial indexing later, but that is not itself a reason to put a map in Phase 1.

## GUI technology choice

For Phase 1, use a local browser application backed by the REST API:

```text
Clojure service
  -> REST API
  -> local browser GUI
```

Given your Clojure preference, a reasonable long-term front end is **ClojureScript**. But do not let ClojureScript framework selection stall the product. The non-negotiable is the stable REST evidence contract, not whether the initial GUI uses ClojureScript, a lightweight static client, or another browser technology.

The GUI should have no privileged direct access to:

- Git filesystem paths;
- MongoDB;
- Elasticsearch/vector internals;
- graph database;
- worker queues;
- local caches.

That preserves the adapter boundary established in ADR 002.

## Why not native desktop first

A native JavaFX UI is viable. Cljfx is a declarative functional JavaFX wrapper inspired by React/Re-frame, and its example repository demonstrates a packaged desktop application built with cljfx and Java tooling. [context7](https://context7.com/cljfx/cljfx)

But native desktop first is the wrong early trade-off:

- You would build a UI-specific state/rendering layer before your REST contract is proven.
- Desktop packaging, JavaFX runtime compatibility, updates, and OS-specific behavior create new work.
- A desktop app is weaker than a browser GUI for sharing views, exposing local API docs, or eventually supporting multiple cluster nodes.
- You would still need a web interface if you want access from different machines.
- The browser is already a capable renderer for Markdown, diffs, timelines, tables, maps, and future graph visualizations.

Native desktop becomes attractive later only if you explicitly want:

- a distributable single-machine “research workstation”;
- offline-first operation with a bundled local service;
- deep filesystem/notification/system-tray integration;
- a deliberate non-browser product experience.

At that point, a **thin JavaFX/Cljfx shell hosting the existing local browser GUI** is preferable to a second full frontend. It can launch the local service, open a controlled WebView, and provide OS integration while preserving the REST API as the UI boundary.

## Proposed interface roadmap

### Milestone 1 — CLI foundation

```text
ca resource register
ca ingest request
ca search
ca evidence show
ca compare
ca lineage show
ca review decide
ca projection status
ca doctor
```

This is the canonical, scriptable interface and must be complete enough to perform every durable action.

### Milestone 2 — TUI archaeology loop

```bash
ca tui
```

Deliver:

- search and result navigation;
- evidence reader;
- comparison;
- lineage navigation;
- review queue;
- projection/failure status;
- command palette/help;
- direct and explicit REST profile modes.

Do not add graph/map visualization yet.

### Milestone 3 — Browser evidence workbench

Deliver:

- evidence-first search;
- comparison and timeline;
- review workspace;
- source/projector health;
- exportable evidence packets;
- REST/OpenAPI-driven integration.

### Milestone 4 — Visual extensions

Only after corpus evidence and reviewed relations are substantial:

- curated concept graph;
- geospatial data views;
- research-question planning board;
- data-analysis notebooks/workspaces;
- model-selection and agent-run history views.

## A decision worth recording

I would add a follow-on ADR:

> **ADR 003 — Phase 1 uses a CLI-first, TUI-second, browser-GUI-third interface strategy. The TUI is a keyboard-first adapter for search, evidence triage, lineage, review, and operations. The browser GUI is the primary rich evidence and visualization workbench. Native desktop GUI development is deferred; if later required, it will be a thin shell over the local REST-backed web workbench rather than a separate domain client.**

The only decision I would ask you to make now is whether you want the TUI to be **part of the single `ca` executable** (`ca tui`) or a separately versioned executable such as `ca-tui`; I recommend `ca tui` for Phase 1.
