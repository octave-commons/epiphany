---
slug: operator-actor-correspondence
uuid: 4a2ec5b8-9f18-4d76-bb76-9e4519afc947
title: "Operator Mental Process and Actor Model Correspondence"
kind: note
status: draft
description: "Preserves the operator's account that inboxes, notes, actors, and external working memory correspond to an actively used mental and operational practice rather than a hypothetical architecture."
created: "2026-07-26"
labels: [actors, operator, inbox, notes, working-memory, provenance, eta-mu]
sources:
  - "User statement in project conversation, 2026-07-26"
informs:
  - "docs/process/inbox.md"
  - "docs/process/notes.md"
  - "docs/notes/cross-repository-notes-synthesis-2026-07-26.md"
---

# Operator Mental Process and Actor Model Correspondence

## What this note is

This note preserves a direct operator account of why the actor, ledger, inbox,
and external-working-memory metaphors recur across eta-mu and Epiphany. It is a
working note, not a psychological theory, architectural decision, or universal
claim about cognition.

## Direct operator observation

The operator reports that the system is not being designed only by mentally
simulating how agents might work. The operator actively runs substantial parts
of the work by writing notes into repository inboxes, allowing partially formed
observations, tensions, proposals, and commitments to become external objects
that can be revisited and processed.

The actor metaphor fits because it resembles how the operator already handles
internal mental processes: multiple concerns or modes can produce messages,
compete for attention, remain unresolved, and later be integrated without first
being forced into one perfectly coherent internal state.

The assistant and repository corpus function as external working memory. This is
not incidental convenience; it is part of how the operator can maintain and
advance a multi-repository system whose shape repeatedly becomes visible and
then difficult to hold in immediate attention.

## Provisional interpretation

The human-facing and agent-facing models are not merely analogous at the UI
level. They share an operational substrate:

```text
partial observation or concern
  -> authored message/note/event
  -> preserved source and provenance
  -> later retrieval and comparison
  -> bounded synthesis or proposal
  -> explicit decision/acceptance where required
```

This suggests that an inbox item is best treated as a message or observation
from an actor in context, not as a direct mutation of canonical state. A note
may be useful precisely because it preserves a partial perspective before the
workspace has resolved it.

The operator remains the acceptance authority for personal intent and
consequential direction. Agent actors may retrieve, compare, synthesize, and
recommend, but must not convert the mere recurrence or polish of a note into
human acceptance.

## Design implications

1. **Preserve plurality before synthesis.** Contradictory or overlapping notes
   can represent real unresolved pressures. Epiphany should retain them and
   expose their relations rather than prematurely producing one authoritative
   summary.
2. **Treat provenance as cognitive context.** Author, time, repository,
   conversation/session, addressed actor, and source span can materially change
   how a note should be interpreted.
3. **Separate message, working representation, and decision.** Inbox items,
   notes, findings, proposals, and accepted decisions need distinct records and
   promotion paths.
4. **Make retrieval an attention operation.** Search and context assembly should
   help the operator recover the relevant partial processes without presenting
   the retrieved set as the whole mind or the whole workspace.
5. **Support reflection over the process itself.** Repeated note shapes,
   unresolved loops, forgotten decisions, and costly re-reading are evidence
   for product capabilities, workflow changes, or Epiphany pattern spores—the
   process term for recurring heuristics that are incubated as proposals rather
   than promoted directly into permanent rules.
6. **Use common actor/event contracts without erasing authority differences.**
   Human, agent, service, monitor, and workflow actors may share an event
   substrate while retaining different permissions, evidence obligations, and
   acceptance powers.

## Limits and countermoves

- This correspondence is an operational metaphor and design input, not evidence
  that internal mental processes are literally software actors.
- Do not infer diagnoses, identities, or stable internal parts from note style or
  topic recurrence.
- Not every note should receive its own formal actor identity.
- The operator's statement is authoritative about the operator's own practice;
  applicability to other users remains an open product-research question.
- External working memory must preserve user autonomy. Retrieval, ranking, and
  synthesis should remain inspectable and correctable rather than becoming a
  hidden system that defines what the operator meant.

## Open questions

- Which source fields are sufficient to link an operator-authored note to the
  active session, repository, work item, and attention context without making
  capture burdensome?
- Should an operator be able to name recurring modes or concerns as actors, or
  should Epiphany keep them as provisional clusters until explicitly accepted?
- How should a context packet display unresolved disagreement among the
  operator's own notes?
- Which parts of this practice belong in generic actor/event contracts, and
  which belong only in an optional personal operating profile?

## Disposition

`note`. This account should inform actor, inbox, context-query, and workbench
design. Promotion into a design or decision requires comparison with the
existing actor contracts and explicit operator review.
