
---
slug: epiphany-agent-ledgers
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000214
title: "Epiphany Agent Ledgers"
kind: design
description: "Outlines Muses as plugin-defined primary agents and Phases as sub-agents, each with isolated append-only event ledgers."
labels: [architecture, agents, meta, workflow, phase-1]
created: "2026-07-11"
source: "docs/inbox/2026.07.11.14.15.20.md"
---

# The Epiphany Development Process Meta Workflow


## Muses
Muses are plugin defined custom primary agents in opencode

The muse can only interact with the system through sub agents,
by searching through  global ledgers like the receipt.edn and
the session isolated ledgers of the sub agents it is managing.

Without the ability to inspect the truth themselves, they are explicitly less trust worth of the claims of their subagents, requiring evidence supporting their claims discoverable in their ledgers

Every question the user answers is recorded in a seperate ledger

The muse can access all of these ledgers, but that is all.
Their responsibility then is to accumulate evidence
of each phases successful completion of their assigned objectives 

### Tools

- `phase.spawn` -> returns the ledger id
- `phase.list_active` -> returns an array of ledger-ids
- `phase.list_idle` -> array of ledger ids
- `phase.tail` -> the last  n  recorded observations of some ledger i
- `phase.head` -> the first n recorded observations of some ledger i
- `phase.filter` ->

## Phases of Understanding

The Phases of Understanding are plugin defined custom sub agents in opencode, with the ability to be invoked as background agents.


### Discover
search workspace for candidate anchors and artifact relationships that plausibly
correlate to the users intent. interview user until the user confirms the agent
has correctly identified their intent identify artifacts well grounded in the users intent
submit observations, hypothesis, assumptions, speculations,  claims, so on,
ending the session with a conclusion, to a session isolated event ledger in `.eta-mu` target


### Scheme


### Act
  - research
  - design
  - decide
  - develop


## Ledgers

Each Muse and Phase agent has an append only event ledger similar to the `receipt-river`
The muse is able to read the ledgers of all Phases started by her.
Phase agents can only observe events recorded by their own phase

Opencode allows for arbitrarily many sub agent sessions to be started from a single parent session.

So The first Discover Phase's events will be observable by the 99th
The session count the number of each Phase was spawned, to be used as
an easy id for the muse to find ledger entries from specific Phase ledgers



## Grounding phases of understanding: Research → Design -> Decide → Plan -> Specify -> Task
1. **Research** — grounding evidence: papers, physical derivations, prior art,
   measurements, profiling. Lives in `docs/notes/research/` and dated
   `docs/notes/*.md`. Research is *findings*, not plans.
2. **Design** — a high-level approach in `docs/designs/*.md`
  - **cites the - research it rests on** (link the note/paper).
  - Refers to resolved ADRs which support (or required) the designs drafting, if any.
  - One design backs many tasks;
  - answers *what and why*.
  - It may also ask "How?", and still be accepted, implementation pending  resolution of all unresolved architectural questions
3. **Decide** - Open architectural questions cited by a design must be resolved by an Architectural Decision Record (ADRs) `docs/adrs/*.md`and approved by the user.
-  for a task implementing features of a design to be `ready` for work, all open questions must be resolved with an approved decision.
4. **Plan** - Breakdown the implementation details of the design into user stories, estimate each task, anything over 5 points must be converted to an epic, and broken down into smaller stories.
4. **Specify** — a `docs/kanban/stories/*.md` card that **references its design**
   via a `design:` frontmatter key and a `> Design: docs/designs/<file>.md`
   line in the body. The task answers *the next testable slice*.

Rules:


