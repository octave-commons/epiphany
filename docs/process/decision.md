---
slug: decision-practice
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000220
title: "Decision Practice Policy"
kind: process-policy
status: draft
implements: ["PROCESS.md#make-acceptance-explicit-and-scoped", "PROCESS.md#make-claims-proportionate-to-evidence", "PROCESS.md#preserve-provenance-and-reproducibility"]
description: "A revisable practice for identifying, authorizing, recording, superseding, and checking consequential decisions."
labels: [process, decisions, adr, architecture, governance]
created: "2026-07-12"
---

# Decision Practice Policy

## Purpose

This policy governs decisions that materially constrain Epiphany's architecture,
durable data, public contracts, safety, operations, or future options. It makes
the transition from proposal to authorized commitment explicit.

A decision is not a persuasive design, a merged change, a green check, or an
unopposed conversation. It is an authorized acceptance of a stated choice for a
bounded scope, with preserved basis and consequences.

## Decision form

```clojure
(κ decision-candidate
   authority
   evidence
   alternatives
   consequences
   -> accepted-decision)
```

`κ` is valid design data before every decision workflow or checker is executable.
It names the transformation that only an appropriate authority may complete.

## When a decision is required

Create or revise a decision record when a choice materially affects one or more
of: system boundaries; durable record semantics; identity/continuity rules;
public commands/APIs; security, privacy, safety, or retention; external service
commitments; operational/recovery guarantees; compatibility/migration; or a
future option that would be costly to reverse.

Do not create ADR theater for a local reversible implementation detail already
constrained by an accepted decision. Record the applied decision in the story
or implementation evidence instead.

## Decision record

A decision record states its context, decision question, accepted choice,
status, authority, evidence/design inputs, alternatives, consequences,
constraints/compliance implications, reversibility, and supersession relation
where material. It distinguishes observations, findings, recommendations, and
the decision itself.

A proposed decision may be `draft` or `review`; it is `accepted` only after the
identified authority records acceptance. A rejected option remains historical
data and may be reconsidered by a later record; it is not silently deleted.

## Authority and review

The authority is named by role/person/process, not inferred from edit access or
merge rights. Review evaluates whether the candidate is within scope, evidence
is proportionate to consequence, alternatives are represented honestly,
consequences are understood, and the selected choice has enforceable follow-up.

Acceptance is scoped. It does not assert that all implementations are correct,
all future facts agree, or every subordinate question has been resolved.

## Supersession and compliance

A new decision explicitly `supersedes` an earlier one for named scope; it does
not rewrite prior history into apparent continuity. Existing implementations are
checked against accepted applicable decisions when changed, when a relevant
incident occurs, or through bounded compliance work.

A compliance finding records the decision, observed implementation fact,
assessment method, status (`conforms`, `deviates`, `unknown`, or `not-applicable`),
and required follow-up. It does not silently convert a suspected deviation into
a rejected implementation claim.

## Relations and exceptions

Design/research may `inform` a decision; a decision may `require` research,
`supersede` another decision, and constrain designs/stories through explicit
relations. Departures from an accepted decision use a Charter exception or a
new decision; implementation convenience does not create precedent.

## Operational references

- Process Charter: `PROCESS.md`
- Design practice δ: `docs/process/design.md`
- Research practice: `docs/process/research.md`
- Document governance λ: `docs/process/document-governance.md`
- ADR corpus and current ADR guidance: `docs/adrs/`
