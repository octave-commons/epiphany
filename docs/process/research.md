---
slug: research-practice
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000217
title: "Research Practice Policy"
kind: process-policy
status: draft
implements: ["PROCESS.md#preserve-epistemic-tiers", "PROCESS.md#make-claims-proportionate-to-evidence", "PROCESS.md#preserve-provenance-and-reproducibility"]
description: "A revisable practice for bounded inquiry, evidence, findings, synthesis, and research-to-decision traceability."
labels: [process, research, evidence, provenance, governance]
created: "2026-07-12"
---

# Research Practice Policy

## Purpose

This policy defines research as a bounded activity that reduces a named
uncertainty through declared method and recorded evidence. It implements the
Process Charter's requirements to preserve epistemic tiers, make claims
proportionate to evidence, and retain provenance for material findings.

Research is not a decorative prelude to a decision, a pile of links, an agent
summary, or a claim that a topic was “looked into.” Its purpose is to leave a
successor able to inspect what was asked, what was considered, what was found,
what remains uncertain, and how the result may responsibly be used.

## Scope and non-goals

This policy applies when a person or agent represents work as research, relies
on it for a material design or decision, or records findings intended for reuse.
It supports internal repository inquiry as well as external research.

It does not require every small change to begin with a research report. Routine
work may use a lightweight inquiry record or direct source inspection when the
uncertainty and consequence are low. It also does not prescribe a universal
search engine, citation format, probability model, source ranking algorithm, or
future knowledge-graph implementation.

The terminology in `docs/process/glossary.md` is normative for this policy.

## Research activity

A research activity exists only when all of the following are recorded at a
level proportionate to its tier:

| Element | Required question |
|---|---|
| Question | What named uncertainty is this activity intended to reduce? |
| Scope | What is included and excluded, including time, system boundary, and source types where material? |
| Method | How will sources/inputs be discovered, selected, inspected, and interpreted? |
| Evidence set | Which exact sources, observations, extracts, experiments, or measurements were considered? |
| Findings | What bounded claims follow, with support, contrary evidence, confidence, and limitations? |
| Disposition | What may this research inform, and what does it explicitly not decide or establish? |

If these elements are absent, classify the output accurately as a note, reading
log, orientation record, hypothesis, or unstructured exploration—not as a
completed research activity.

## Research tiers

Research effort is selected by consequence, reversibility, novelty, uncertainty,
and potential blast radius. The selected tier is a provisional classification
until the relevant reviewer or policy accepts it for material work.

| Tier | Appropriate use | Minimum result |
|---|---|---|
| `orientation` | Learn vocabulary, locate existing work, or map an unfamiliar area | Question, inspected artifacts/sources, observations, and explicit non-decision status |
| `decision-support` | Inform a design or consequential recommendation | Brief, method, source records, findings, contrary evidence search, limitations, and decision disposition |
| `implementation-grounding` | Establish behavior of a library, protocol, integration, or implementation seam | Decision-support evidence plus direct specification/API/source verification and a runnable spike or experiment when materially uncertain |
| `evaluation` | Compare alternatives or select a tool/approach | Protocol, criteria, inputs/fixtures, measured or inspected results, analysis, limitations, and recommendation scope |
| `reproduction` | Verify an internal or external result | Inputs, environment, procedure/commands, result/divergence, and retained evidence artifact |

A lower tier may inform a higher-consequence action, but it must not be presented
as sufficient evidence by itself. Orientation research cannot be the sole basis
for an irreversible architectural decision.

## Artifact set

A research report is a readable synthesis over a traceable artifact set, not
the sole source of research truth. The detailed templates will be established by
document-governance policy; until then, use the following minimum roles.

| Artifact | Purpose | Required content |
|---|---|---|
| Research brief | Bound the inquiry before substantive work | Question, intended use, scope/non-goals, tier, method, stop conditions, acceptance/disposition authority |
| Source record | Identify one source or input | Stable locator/identity, version or observation context, source type, access time, authority assessment, availability/status |
| Observation or extract | Preserve directly encountered material | Source link, locator/span or command context, observation method, time, and permitted content/digest |
| Finding | State a bounded derived claim | Claim, basis, method, support/contrary evidence, confidence, limitations, and relevant scope |
| Synthesis | Compare findings | Agreements, conflicts, uncertainty, applicability, implications, and non-implications |
| Research report | Present the investigation for review/reuse | Brief link, method, artifact inventory, findings/synthesis, limitations, disposition, open questions |
| Research gap | Preserve material unanswered uncertainty | Unknown/question, why current evidence is insufficient, impact, and proposed next inquiry |
| Replication record | Show a result was rerun | Inputs, environment, procedure, result/divergence, and evidence links |

These may be separate files, sections of one controlled document, structured
records, or generated views, according to the active document standard. They
must remain addressable and linked where a material finding or decision depends
on them.

## Method and source discipline

A research brief declares a method sufficient for the tier. It does not need to
predict every source, but it does state how the activity will avoid unsupported
selection and interpretation.

At minimum, record when applicable:

- Discovery paths and search queries or workspace paths inspected.
- Selection criteria and material exclusions.
- Source type and authority: primary/canonical, secondary, commentary, derived,
  user testimony, experiment, or unavailable source.
- Version, publication/commit/revision context, and access/observation time.
- Extraction, experiment, comparison, or synthesis method.
- Known conflicts, source limitations, access constraints, and stopping reason.

Search-result snippets, uninspected citations, and model summaries are discovery
aids. They are not adequate standalone support for a durable technical claim.
A model-generated interpretation is a derived artifact and records its source
basis, model/version where available, and limitations before it is treated as a
finding.

## Findings and synthesis

Every material finding is bounded: it states what it claims, the context in
which it applies, and what it does not establish. It links to the evidence that
supports it and to material contrary evidence or the recorded attempt to find
it.

Use calibrated language. “Observed” is reserved for direct observation;
“indicates,” “suggests,” and “is consistent with” describe limited derived
support; “accepted” identifies a separate acceptance event. Avoid words such as
“proves,” “always,” “never,” “best,” or “solves” unless their scope, method, and
evidence support that strength.

Confidence is an assessment, not a substitute for evidence. Where recorded, it
names the assessment method and limitations; a numeric score without method,
inputs, or scope is not a reliable finding.

A synthesis may recommend an action, but it must distinguish:

```text
what was observed
what was inferred
what remains unknown or contested
what is recommended
which authority must decide
```

## Research to design and decision

Research may inform a design, ADR, work item, or further research question. It
does not silently establish any of them.

When a design or decision relies materially on research, it links to the
relevant finding/synthesis or report and records:

- The claim being relied upon.
- Why the evidence is sufficient for the consequence at hand.
- Important limitations, conflicts, and applicability boundaries.
- The open question or risk that remains after using the research.

A design can remain provisional while research is incomplete. An architectural
decision requires the decision record and authorized acceptance prescribed by
the charter and ADR practice. Do not convert “we found a plausible approach”
into “the project has decided” by moving a research card or report alone.

## Research workflow

The active Kanban workflow governs card status after a research work item enters
the board. This policy supplies the research-specific content of responsible
work:

1. **Orient:** identify the request, candidate target artifacts, existing
   research/design/decision records, and the uncertainty that matters.
2. **Brief:** select a research tier; record question, intended disposition,
   method, scope, and stop conditions before substantial inquiry.
3. **Collect:** create source records and direct observations/extracts; preserve
   provenance and availability failures.
4. **Interpret:** derive bounded findings, seek material contrary evidence, and
   record confidence and limitations.
5. **Synthesize:** compare findings and state implications, non-implications,
   open questions, and recommended next action.
6. **Review and dispose:** accept the research outcome for its stated use,
   request more inquiry, record a gap, or supersede it with later work.
7. **Reflect:** identify reusable sources, missing standards, tool gaps, or
   policy improvements.

A stop condition is a successful research outcome when it reveals that the
question is wrongly framed, the required source is unavailable, evidence is too
weak for the intended consequence, or a decision/clarification is needed before
further inquiry can be responsible.

## Review and acceptance

Research review evaluates the activity against its declared tier and intended
use, not merely its writing quality or citation count. The reviewer asks:

- Is the question specific enough to assess?
- Does the method fit the intended consequence?
- Can material findings be traced to inspectable evidence?
- Were conflicts, exclusions, source quality, and limitations treated honestly?
- Does the conclusion overreach the evidence?
- Is the disposition correct: more inquiry, design input, decision input, or no
  action?

Acceptance of a research outcome means only that it is accepted for the stated
purpose and scope. It does not certify every source, make findings immutable, or
replace an architectural/product decision.

## Exceptions and adaptation

A time-sensitive or constrained inquiry may use an exception to omit a normal
research artifact or method. The exception follows `PROCESS.md`: it names the
waived requirement, rationale, scope, risk, authority, expiry/review point, and
required follow-up.

Repeated inability to meet this policy is evidence for improving templates,
source access, tooling, board design, or research tier definitions. Policy
changes follow the charter's deliberate trial and adoption process.

## Initial checker direction

Document governance may progressively automate structural checks. Initial checks
should report, at minimum:

- Missing question, tier, intended disposition, method, scope, evidence set,
  findings, or limitations in a report marked complete/reviewable.
- A material finding without an evidence/source link.
- A design/ADR link to research with no identifiable finding or stated scope.
- A source recorded only as a search-result snippet where direct inspection is
  expected.
- A conclusion stronger than its declared confidence or limitations plausibly
  support.
- No recorded contrary-evidence search for decision-support/evaluation work.

These checks assist review. They must not manufacture semantic confidence or
replace human assessment of relevance, sufficiency, and consequence.

## Operational references

- Process constitution: `PROCESS.md`
- Operational terminology: `docs/process/glossary.md`
- Board workflow: `docs/process/kanban.md`
- Current board operations: `docs/kanban/AGENTS.md`
- Existing research corpus: `docs/research/`
