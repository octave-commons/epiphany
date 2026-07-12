---
slug: bad-story-schema-operation-registry-original
uuid: b1d0a1c2-2026-0712-bad0-000000000001
title: "Anti-example: ENG-017A as originally bulk-authored"
kind: anti-example
status: active
description: "The original ENG-017A card, preserved verbatim as a deliberately weak engineering-story example. Demonstrates the 'administratively valid, not executable' failure the story contract exists to prevent."
labels: [example, anti-example, story, documentation-governance]
created: "2026-07-12"
violates:
  - "docs/process/document-governance.md#minimum-document-contracts (story row)"
examples:
  - "docs/standards/examples/story/good-schema-operation-registry.md"
---

# Anti-example: ENG-017A as originally bulk-authored

This is the ENG-017A card exactly as it was produced in the bulk card-writing
pass (frozen from the working tree at `e92e389`, before the 2026-07-12
rework). It is preserved as a curated anti-example, not as a card. The source
conversation that produced it (inbox `2026.07.12.10.21.21.md`) judged the
whole batch "administratively valid cards, not sufficiently executable ones,"
and this file makes that judgment concrete and reviewable.

It is a good anti-example precisely because nothing in it is *false* — it is
plausible, on-topic, and correctly scoped at a high level. It fails by
omission, which is the failure mode a heading-presence checker cannot catch
and a human reviewer skims past.

## Why it fails the story contract

Measured against the story row of `docs/process/document-governance.md`
(intent/outcome, scope/non-goals, authoritative inputs, dependencies,
acceptance conditions, verification approach, completion evidence):

1. **No operational intent.** It states what to build ("a registry") but not
   the defect it changes. A reader cannot tell this card exists to retire the
   false-green oracle.
2. **No decision context / authoritative inputs.** `design:` frontmatter
   points at the verification architecture, but the body never cites the
   design section or ADR rule it implements, so the grounding is a link, not
   a traceable relation.
3. **Scope names no concrete boundary.** "all existing public `:record-*`
   operations" — which ones? A card should name the namespace and the five
   operations so "done" is checkable. (The rework lists them in a table drawn
   from `law/ports.clj:62-66`.)
4. **Acceptance criteria are not traceable to verification.** "Tests fail for
   missing, orphaned, or version-inconsistent entries" names no test, no
   command, no location. This is the exact gap ADR-004 targets: the criterion
   is unfalsifiable as written.
5. **No verification section.** There is no claim → evidence → command table,
   so nothing distinguishes a real implementation from a green-looking stub.
6. **No completion-evidence requirement.** Nothing says what must be recorded
   before the card may move to done — which is how 17 cards on this board
   reached done with no evidence at all (2026-07-12 audit).
7. **No "would have gated" / failure-mode framing.** The card cannot say what
   it prevents, so its priority is an assertion rather than an argument.

A checker can only flag items 5–6 structurally (missing required sections).
Items 1–4 and 7 are the semantic-quality gap that exemplars and human review
exist to teach — which is why this file is kept alongside the good version.

## The card, verbatim

```markdown
---
id: "01900d7c-7f3a-7e8b-9c4d-000000001701"
title: "ENG-017A: Define the schema operation registry"
status: "incoming"
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
points: 3
labels: [quality, schemas, contracts, verification, phase-1]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000000000-b"]
---

# ENG-017A: Define the schema operation registry

Create the authoritative data registry mapping every public durable observation
write operation to its named, versioned Malli schema and persistence semantics.

## Scope

- Define registry entries for all existing public `:record-*` observation operations.
- Provide lookup and completeness checks against the law schema registry.
- Enforce the operation-selected schema version against the record's claimed version.
- Produce stable, safe schema-validation error data.

## Out of scope

- Port wrapping, adapter implementation changes, Mongo integration, or CLI/HTTP decoding.

## Acceptance criteria

- An unregistered public write operation fails explicitly; there is no permissive fallback.
- Every current durable observation write maps to one named schema and version.
- Tests fail for missing, orphaned, or version-inconsistent registry entries.
- Validation errors include a stable code and schema name without exposing raw repository content.
```

## Note

The scope and out-of-scope bullets here are genuinely useful and were carried
forward into the reworked card. Anti-example does not mean worthless — it
means insufficient as a commitment another actor could execute and verify
without reinterpreting it.
