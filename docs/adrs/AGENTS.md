# Architecture Decision Records

An Architecture Decision Record (ADR) is a short document that captures a single important architecture decision, the context behind it, and the consequences of choosing it. [learn.microsoft](https://learn.microsoft.com/en-us/azure/well-architected/architect-role/architecture-decision-record)

It usually answers these questions:
- What decision was made?
- Why was this decision necessary?
- What alternatives were considered?
- What tradeoffs, constraints, or consequences came with the choice?
- What decision is this replacing, if any? [martinfowler](https://martinfowler.com/bliki/ArchitectureDecisionRecord.html)

In practice, ADRs are used to preserve architectural **history**, so teams can understand not just what the system is, but why it ended up that way. They are meant to be concise and focused on decisions that matter structurally or are hard to reverse. [docs.defguard](https://docs.defguard.net/in-depth/architecture-decision-records)

A common ADR entry includes:
- Title.
- Date.
- Status.
- Context.
- Decision.
- Consequences.
- Alternatives considered.
- Links or supporting notes.

## Front-matter Shape
```md
---
status: draft | open | rejected | approved
title: "ADR-123"
id: 123
---
```
