---
name: inbox-synthesis
description: "Process heterogeneous Epiphany inbox corpus without losing provenance, ambiguity, or product-learning observations."
metadata:
  process-policy: docs/process/inbox.md
  document-governance: docs/process/document-governance.md
  research-practice: docs/process/research.md
  notes-practice: docs/process/notes.md
---

# Inbox Synthesis

## Use when

Use this skill for raw or mixed `docs/inbox/` material: exports, long agent
threads, notes, imports, old project artifacts, transcripts, partial plans, and
prior classifications. Use it to investigate and synthesize corpus material,
not merely rename, tag, or move files.

## Prime directive

Preserve the source; extract useful, traceable structure; never turn a label or
agent summary into authority. Optimize for future reasoning and retrieval per
unit of attention—not conversion count.

## Procedure

1. Read `docs/process/inbox.md`, `docs/process/document-governance.md`, and
   local `docs/inbox/AGENTS.md` before changing a batch.
2. Inventory source items and preserve raw content/path/provenance. Do not
   delete or overwrite an inbox original when routing derived artifacts.
3. Sample and inspect before applying a batch taxonomy. Identify source type,
   author/agent context if known, prior processing, obvious segments, and
   uncertainty.
4. Search existing corpus artifacts where product tools or repository search can
   help. Treat hits as candidates; inspect evidence before asserting duplication,
   support, identity, or supersession.
5. Decompose by independently useful claims/segments. Separate direct
   observation, quoted/source claim, derived finding, proposal, decision
   candidate, and implementation claim.
6. Synthesize only bounded artifacts that improve on the source: a note,
   source record, finding, research brief/report, design, decision candidate,
   epic/story candidate, or archive record. Add typed `sources`/`informs`/
   `requires` relations.
7. Record a disposition for every materially inspected item: retain, defer,
   note, extract, research, design, decision-candidate, plan-work, archive, or
   closed-no-extraction.
8. Append an observational journal entry for each substantive session or batch.
   Record structural patterns, friction, failed retrieval/synthesis, product
   gaps, rejected hypotheses, and next action. Never store secrets.
9. Incubate recurring patterns as spores; do not create permanent skills or
   rules from one convenient case. State recurrence, scope, benefit, and
   disconfirming conditions.
10. Stop when the next step requires invented authority, missing source context,
    materially uncertain interpretation, or disproportionate effort. Record the
    stop condition and smallest next action.

## Required outputs

For a substantive batch, leave:

- Preserved source items and intake/provenance record or explicit reason it is
  unavailable.
- A batch disposition map or per-item dispositions.
- Links from every material synthesized artifact to its source item/segment.
- At least one append-only journal event if inspection/synthesis occurred.
- A clear list of unresolved items, blockers, and highest-value next pass.

## Never do

- Treat a conversation export, model claim, generated frontmatter, or folder
  path as proof of fact, acceptance, identity, or authority.
- Copy an entire raw item into a “clean” document without adding bounded,
  traceable value.
- Mass-rewrite old corpus text solely to make metadata uniform.
- Delete source material after a synthesis or infer provenance that is unknown.
- Let an agent's classification become a decision, policy, or task commitment
  without the governing artifact and acceptance path.
- Hide uncertain, unavailable, duplicate-candidate, or no-value outcomes.

## Journal event sketch

```clojure
{:event/kind :inbox/pattern
 :intake/item "..."
 :observation "Repeated agent exports mix design proposals and asserted completion."
 :basis {:method :turn-segmentation :spans [12 44 73]}
 :interpretation "Turn-aware extraction may improve provenance over document-level labeling."
 :product-gap "No current feature preserves agent-turn-to-derived-artifact links."
 :next-action "Create bounded design/research candidate after recurrence review."}
```
