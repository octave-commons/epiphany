---
slug: inbox-practice
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000223
title: "Inbox Practice Policy"
kind: process-policy
status: draft
implements: ["PROCESS.md#preserve-epistemic-tiers", "PROCESS.md#preserve-provenance-and-reproducibility", "PROCESS.md#keep-work-accountable-and-bounded"]
description: "A revisable practice for preserving, investigating, decomposing, synthesizing, and routing heterogeneous raw corpus material."
labels: [process, inbox, corpus, synthesis, extraction, η, μ, Π]
created: "2026-07-12"
---

# Inbox Practice Policy

## Purpose

The inbox is Epiphany's intake corpus: heterogeneous material retained because
it may contain observations, sources, requirements, designs, decisions,
questions, vocabulary, history, or evidence whose future value is not yet
known. It can include personal writing, agent output, exported conversations,
old repository material, plans, logs, partial classifications, and the
intermediate states of prior reorganizations.

Inbox processing is not a filing chore. It is a research, synthesis, feature
extraction, and product-learning activity. High-quality initial processing
improves every later artifact; poor processing can launder an agent summary into
false authority, erase useful ambiguity, or make rediscovery more expensive.

## Intake form

```clojure
(ι raw-corpus-item
   provenance
   context
   observations
   candidate-structures
   -> intake-record)
```

`ι` accepts raw material without requiring premature classification. The result
is an accountable intake record, not a claim that the item is understood,
canonical, useful, or ready to route.

```clojure
(-> intake-record
    inspect
    decompose
    extract
    synthesize
    relate
    route
    retain-provenance
    Π)
```

This is recursive. New observations, failed extraction, or later corpus context
may return an item to inspection. The original item remains addressable.

## Principles

- **Preserve before interpreting.** Keep the source item or a recoverable
  immutable reference before materially transforming it.
- **Extract; do not merely copy.** The goal is not perfect duplicated prose in
  cleaner folders. Produce the smallest useful findings, source records,
  questions, designs, decisions, notes, or work items with faithful relations.
- **Retain ambiguity.** Unknown authorship, incomplete context, mixed claims,
  and uncertain dates are observations, not defects to be silently repaired.
- **Separate source from synthesis.** An agent's classification, summary, or
  label is derived material and never replaces the original source.
- **Route by use, not appearance.** A long conversation may yield research,
  design, decisions, stories, vocabulary, and no durable artifact for some
  portions. One source item can produce many related outputs.
- **Progress is not perfection.** A thin but honest intake record, source map,
  or “not worth further conversion” disposition can save later time and is a
  successful outcome.
- **Learn from friction.** Repeated ambiguity, manual steps, failed retrieval,
  weak synthesis, or costly re-reading are product evidence for Epiphany.

## Inbox and notes

`docs/inbox/` is the least structured intake boundary. It preserves raw or
near-raw incoming material before its value and destination are known.

`docs/notes/` contains durable but non-authoritative working material: captured
observations, intermediate syntheses, local context, and thought in progress.
A note is more processed than an inbox item but is not automatically research,
design, policy, decision, or accepted truth.

Neither folder is a trash can. Both are valid corpus. The difference is the
current processing claim:

| Location/kind | Current claim |
|---|---|
| Inbox item | Received/preserved; not yet sufficiently understood or routed |
| Intake record | Initial provenance/context/inspection has been recorded |
| Note | Useful working representation; non-authoritative and revisable |
| Routed artifact | A separately governed kind with explicit source relation |
| Archive | Retained history; not active guidance without reactivation |

## Initial intake

An item may enter with only a timestamped filename. Initial processing adds the
smallest honest record sufficient to avoid losing context:

- Stable item identity and observed path; capture/import time.
- Origin/provenance: author or agent if known, source repository/service/device,
  export context, and retrieval method.
- Content condition: complete, partial, redacted, duplicate candidate,
  unavailable attachment, or uncertain.
- Minimal content description and language/media form.
- Initial handling state: `unread`, `inspected`, `decomposing`, `synthesized`,
  `routed`, `deferred`, `archived`, or `closed-no-extraction`.
- Explicit uncertainty rather than invented labels.

Labels, title, and doc kind are useful retrieval aids but are not processing
completion. They must not be the only recorded result for a material item.

## Inspection and decomposition

Inspection answers: what is present, where did it come from, what types of
claims occur, what prior processing exists, and which parts might warrant more
work. It records direct observations before proposing meaning.

Decomposition finds independently useful segments without forcing the whole
item into one type. Typical segments include source facts, excerpts, questions,
research leads, findings, designs, decision candidates, work candidates,
examples, terminology, tool traces, prior classifications, and narrative
context. Segment boundaries are derived claims; retain their method/basis and
never present them as source-native structure when they are not.

For exported agent conversations, preserve speaker/turn order and tool/source
context where available. Do not treat an assistant assertion, even one that
sounds authoritative, as an observed fact without inspecting its cited source or
recording it as an unverified claim.

## Synthesis and routing

Synthesis creates a new artifact only when it gives a later reader more usable,
traceable value than the source alone. Every material synthesized artifact has a
`sources` relation to the inbox item and, where useful, segment/extract locator.
It identifies whether its content is observation, finding, proposal, decision
candidate, implementation claim, or open question.

Possible dispositions are:

| Disposition | Meaning |
|---|---|
| `retain-unprocessed` | Preserve; current value/context is insufficient for responsible work |
| `defer` | Worth revisiting under a named trigger/priority |
| `note` | Create/revise a non-authoritative working note |
| `extract` | Create source records, extracts, observations, terminology, or indexable features |
| `research` | Create/extend a bounded research activity or finding |
| `design` | Create/revise a proposed shape |
| `decision-candidate` | Create a decision question/ADR candidate, not an accepted decision |
| `plan-work` | Create an epic/story only with bounded outcome and acceptance conditions |
| `archive` | Preserve as historical/imported material with context |
| `closed-no-extraction` | Conclude that further conversion is not presently worth its cost, recording why |

Routing is additive where appropriate. Do not delete the inbox item simply
because a better artifact was produced; link it and preserve provenance.

## Observational journal

Every substantive inbox-processing session writes an append-only observational
journal entry. The journal is not a diary and not a replacement for extracted
artifacts. It captures the evidence generated by doing corpus work that neither
the source item nor a final artifact fully retains.

The initial location is:

```text
docs/inbox/.observations/YYYY-MM.jsonl
```

A journal entry contains, proportionately:

```clojure
{:event/id ...
 :event/at ...
 :event/kind :inbox/observation | :inbox/pattern | :inbox/friction
 :actor {:kind :human | :agent | :tool :id ...}
 :intake/item ...
 :source/locator ...
 :observation "directly encountered structural/content fact"
 :basis {:method ... :span-or-turn ...}
 :interpretation "optional derived hypothesis"
 :impact {:artifact-kinds [...] :routing ...}
 :product-gap "optional missing capability or failed assistance"
 :next-action ...
 :confidence ...}
```

Journal entries are append-only. Corrections add a later entry that names the
prior event and basis. Do not record secrets, private credentials, or copied
sensitive text unnecessarily.

## Pattern spores and product feedback

When a session reveals a recurring processing pattern, friction, or reliable
heuristic, record a **spore** rather than immediately hard-coding an agent rule.
A spore names the pattern, recurrence evidence, scope, observed benefit/cost,
proposed assistance, and disconfirming conditions.

A spore can propose a future prompt, skill, template, document checker rule,
feature extractor, index/query, data model, or UI workflow. It becomes active
practice only after review appropriate to its consequence. Recurrent agent
friction is evidence for product capability, not evidence that an agent should
silently invent a permanent rule.

This draws on the useful separation in Muse's session-mycology and receipt-river
patterns: append observations/receipts, incubate recurring patterns, and promote
only with explicit evidence. The Epiphany implementation must retain its own
provenance and acceptance rules.

## Agent responsibilities

An inbox agent works as a corpus investigator and synthesizer, not a bulk file
renamer. For each batch it must:

1. Preserve originals and establish intake/provenance context.
2. Inspect representative and high-value material before choosing a global
   classification strategy.
3. Record direct observations separately from inferred categories and summaries.
4. Seek existing related artifacts using Epiphany capabilities where available;
   treat retrieval results as candidates, not identity/authority proofs.
5. Create only bounded artifacts with explicit source relations.
6. Journal structural patterns, product gaps, rejected hypotheses, and costly
   manual simulations of intended product behavior.
7. Stop or defer honestly when value is insufficient, ambiguity is material, or
   the next useful action needs a human/decision/research boundary.

The quality target is better future reasoning per unit of attention, not maximum
file count, maximum label count, or maximum conversion rate.

## Quality and risk

The main risk is opportunity cost: investing too much time to extract artifacts
that later prove unhelpful. The policy therefore favors cheap, reversible,
provenance-preserving early passes; small sampled batches; and explicit
`closed-no-extraction` dispositions. Even that disposition yields useful corpus
knowledge and journal observations.

Higher-risk cases—sensitive material, uncertain ownership, destructive
migration, claims of prior human acceptance, or artifacts that may drive an
irreversible decision—require narrower scope, stronger provenance, and human
review before consequential routing.

## Evolution

The inbox process is a primary product-discovery loop. A repeated inability to
locate, compare, extract, relate, de-duplicate, preserve source context, or
reconstruct a prior synthesis is a candidate Epiphany capability gap.

Process changes and skills are proposed from journal evidence, trialed on a
bounded corpus, assessed for quality/time/reversibility, then adopted, revised,
or retired explicitly. Never improve apparent throughput by dropping source
provenance or promoting a derived synthesis to accepted authority.

## Operational references

- Process Charter: `PROCESS.md`
- Document governance λ: `docs/process/document-governance.md`
- Research practice: `docs/process/research.md`
- Design practice δ: `docs/process/design.md`
- Engineering kernel: `STYLE.md`
- Inbox local guide: `docs/inbox/AGENTS.md`
- Notes practice: `docs/process/notes.md`
