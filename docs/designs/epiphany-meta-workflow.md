---
slug: epiphany-meta-workflow
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000213
title: "Epiphany Development Process Meta Workflow"
kind: design
description: "Refined meta-workflow for Epiphany, including Muses, Phase agents, ledgers, and grounding phases of understanding."
labels: [process, meta, workflow, agents, phase-1]
created: "2026-07-11"
source: "docs/inbox/2026.07.11.12.37.56.md"
status: exploratory
---

The kanban tool needs to be more flushed out...
We need to get the `eta-mu kanban serve` running here.

I'm also lookin at my eta-mu-sol project... We had an actor model protocol defined?

No, I shouldn't bring that over here. It's a seperate thing, that's what I always do.
that's how I create the problem that inspired this project.

Thinkin "ooo I remember doin that!" but at that moment, the agents stop understanding me clearly.

They never stopped on eta-mu-sool, cause I stopped before I did that.

Here, we are committing to aggressive prompt engineering.
We are writing a script, a contract, a constitution.

A charter.

But this, starting by describing it all in english before we get any code goin.
Rather than trying to figure out how to communicate it in Clojure, as expresssive of a language as it is.

It's still easier to write markdown.



---

Each phase is about increasing understanding of a problem until it is solved.
Every phases produce artifacts which demonstrate the actors level of understanding clearly enough
that another actor understands the problem clearly enough to attempt the next phase of understanding.


---

Am I using the term *Artifact* correctly when I refer to these text files as claimed evidence to support a hypothesis?

---


## Phases of Understanding

- discovery
  - User prompt
  - Search workspace
  - interview user
  - claim target artifacts as possible targets of the users request
  - claim primary research report artifact to support hypothesis
  - claim secondary artifacts to support hypothesis
  - test claims against the knowledge graph as... bayesian propbality? as in like 
- plan
- act
  - research
  - design
  - decide
  - develop

As in P(w| w_1, ... w_n)
Where n is the number of edges... but it is gonna be more complex than that, if the relationships between the artifacts claimed as evidence produces a digraph?


---

Phase 0 tools I can add immediately:
- read_mailbox
- monitor_mailbox -> wakes the agent actor up if the mailbox meets some condition.
- manage_mailbox_policies -> prevent messages matching some rule from ever being recieved, a message ttl
- spawn_agent_actor
- spawn_process_actor -> accepts a subscribe message which sends every line of the processes stdout and/or stderr as a message to the the subscribers mailbox
- list_known_agents
- send_agent_actor_message
- 

---


# The Epiphany Development Process Meta Workflow

## Phases of Understanding

- discovery: test hypothesis, interview the user, find most stable claimable subject artifact starting from some research claimed as an anchor with some cost based traversal rules or something
  - User prompt
  - Search workspace
  - interview user
  - claim target artifacts as possible targets of the users request
  - claim primary research report artifact to support hypothesis
  - claim secondary artifacts to support hypothesis
- plan
- act
  - research
  - design
  - decide
  - develop




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

----
## Phases of Understanding

## discovery

The agent works to form and refine a hypothesis about what your intentions were,
grounded in increasingly more sophiscated mechanisms as the project grows.

For now, 
- test hypothesis,
- interview the user, find most stable claimable subject artifact starting from some research claimed as an anchor
  - User prompt
  - Search workspace
  - interview user
  - claim target artifacts as possible targets of the users request
  - claim primary research report artifact to support hypothesis
  - claim secondary artifacts to support hypothesis
- plan
- act
  - research
  - design
  - decide
  - develop

