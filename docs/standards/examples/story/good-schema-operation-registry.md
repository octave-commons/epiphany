---
slug: good-story-schema-operation-registry
uuid: 900d0a1c-2026-0712-900d-000000000001
title: "Example: ENG-017A reworked to the engineering-story contract"
kind: example
status: active
description: "The reworked ENG-017A card as the canonical good engineering-story exemplar: operational intent, traceable grounding, concrete scope, verification table, and completion-evidence requirement."
labels: [example, story, documentation-governance]
created: "2026-07-12"
examples:
  - "docs/process/document-governance.md#minimum-document-contracts"
sources:
  - "docs/kanban/stories/engineering-assurance-schema-operation-registry.md"
violates: []
---

# Example: ENG-017A reworked to the engineering-story contract

The live card is `docs/kanban/stories/engineering-assurance-schema-operation-registry.md`;
this exemplar points to it rather than duplicating it, so the two cannot
drift. Read them side by side with
[the anti-example](bad-schema-operation-registry-original.md) — same slice,
same author, same points; the difference is executability.

## Which rules it demonstrates

Against the story row of `docs/process/document-governance.md` and the story
template sketched in inbox `2026.07.12.10.21.21.md`:

1. **Operational intent.** The `## Intent` section states the defect it
   changes ("Unit tests currently pass writes that production storage would
   reject") before what it builds. A reviewer learns *why* in one sentence.
2. **Traceable grounding, not a bare link.** `## Decision context` cites the
   exact design section ("Schema ownership") and ADR-004 decisions (1–2),
   plus the two audit anchors it responds to. The `design:` and `adr:`
   frontmatter are backed by prose that says how.
3. **Concrete scope.** The operation→schema→version table is drawn from real
   source (`law/ports.clj:62-66`, `law/observation.clj`). "Done" is checkable
   because the deliverable is enumerable.
4. **Explicit non-goals.** Names what abuts this slice (port wrapping,
   adapter changes) and assigns each to its sibling card — preventing scope
   creep and implied cross-card work.
5. **Invariants as testable statements.** Each invariant is phrased so a test
   could fail it ("an operation absent from the registry is representable only
   as the stable error datum").
6. **Verification table.** Every claim maps to evidence and a named test
   location. This is the ADR-004 requirement made local: a criterion you
   cannot point a command at is not a criterion.
7. **Acceptance criteria traceable to verification.** Each acceptance bullet
   refers back to a verification row, so "tests pass" is insufficient by
   construction.
8. **Completion-evidence requirement.** Names what must be recorded before
   review→done, and that acceptance requires a named authority — the direct
   fix for the 17 evidence-free done cards found in the 2026-07-12 audit.
9. **"Would have gated" framing.** States which past defect this card, had it
   existed earlier, would have prevented — making its P0 priority an argument
   rather than an assertion.

## What "good" does not mean here

- Not "long." The card is longer than the original because each section
  carries a decision, not because verbosity is rewarded. A one-line non-goal
  that genuinely has no non-goals should say so explicitly and survive review.
- Not "certain." Its `## Risks and open questions` section keeps two real
  unknowns open (the `:import-all` payload shape, which schemas are persisted
  through which port) instead of pretending they are resolved. Preserved
  uncertainty is part of a good card.
- Not "final." It is `status: ready`, not `done`. The exemplar is of a card
  that another actor could pick up and execute without reinterpreting it —
  which is exactly the `ready` bar in `docs/process/kanban.md`.
