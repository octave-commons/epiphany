---
slug: design-practice
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000219
title: "Design Practice Policy"
kind: process-policy
status: draft
implements: ["PROCESS.md#keep-work-accountable-and-bounded", "PROCESS.md#make-claims-proportionate-to-evidence", "PROCESS.md#make-acceptance-explicit-and-scoped"]
description: "A revisable practice for producing bounded proposed system shapes from evidence, constraints, alternatives, and open questions."
labels: [process, design, architecture, planning, δ]
created: "2026-07-12"
---

# Design Practice Policy δ

## Purpose

This policy governs design as the creation and review of a **proposed shape**:
a bounded account of how a system, behavior, boundary, data model, process, or
migration could satisfy a stated purpose under known constraints.

Design is neither a decision nor an implementation promise. It is valid,
reviewable design data before all of its structures exist in code, tests,
schemas, adapters, or tools. A design may expose uncertainty; concealing that
uncertainty to make execution appear ready is a design failure.

## The δ form

```clojure
(δ design-brief
   research-inputs
   observed-system
   constraints
   alternatives
   open-questions
   -> proposed-shape)
```

`δ` names the design transformation. It takes a bounded design brief together
with inputs and produces a proposed shape with explicit claims, boundaries,
tradeoffs, risks, and next relations. It is pseudocode and valid design data;
it need not compile for the form to constrain how Epiphany reasons about and
constructs systems.

A proposed shape may inform a decision, a research activity, a spike, an epic,
or a story. It does not silently authorize any of them.

## Scope and non-goals

This policy applies to material technical, architecture, interface, data,
workflow, migration, and cross-cutting assurance designs. Use it when a change
would introduce or alter a boundary, durable record, public contract, foreign
integration, consequential behavior, or implementation sequence that cannot be
responsibly improvised in a single story.

It does not require a stand-alone design for trivial, reversible, local work
whose relevant contracts and authority already exist. It does not prescribe a
visual diagram notation, design-tool vendor, universal level of detail, or a
single document per subsystem.

## Design in the construction kernel

Design is where the engineering kernel's intent, specification, laws, and
construction responsibilities are made visible before they are scattered across
implementation.

```clojure
(η Discovery
   (-> (-> Describe specify
           (δ design-brief
              research-inputs
              observed-system
              constraints
              alternatives
              open-questions
              -> proposed-shape)
           define)
       (-> μ shape extern domain infra))
   Π)
```

The nesting does not require a linear waterfall. Discovery may revise a design;
a design may request research; an experiment may demonstrate that a proposed
boundary is wrong. The requirement is traceability: the actor records what
changed, why, and which downstream assumptions must be reconsidered.

## What a design claims

A design can make proposals and bounded derived claims. It distinguishes these
from observations and accepted choices:

| Statement | Correct classification |
|---|---|
| “JGit returned this object/value under this command and revision.” | Observation, with source/context |
| “This boundary appears to preserve exact Git path representation.” | Derived finding, with method and limits |
| “Use an `extern.git` boundary that emits the declared record shape.” | Design proposal |
| “Phase 1 will use this boundary and record shape.” | Decision only after authorized ADR/acceptance |
| “The boundary has been implemented and passes this check.” | Verification/implementation claim, with record |

A design must never use vocabulary such as *decided*, *required*, *accepted*,
or *implemented* for a weaker claim without identifying the relevant authority
or evidence artifact.

## Design brief

A material design begins with a brief sufficient to bound δ. The brief may be a
section of a design document or a related artifact, but it identifies:

- **Purpose:** the capability, problem, or decision context addressed.
- **Intended use:** whether the output informs research, an ADR, a work plan, a
  story, a migration, or an implementation spike.
- **Scope and non-goals:** the system boundary, explicitly excluded work, and
  temporal/phase constraints where material.
- **Known inputs:** authoritative ADRs/policies, research, source facts,
  existing behavior, user/product constraints, and prior designs.
- **Consequence profile:** reversibility, uncertainty, blast radius, and
  affected users/data/interfaces.
- **Review and disposition:** who may review the design and what next outcome
  is expected.

A design brief does not need to pretend unknowns are resolved. It names them so
that the work can stop, branch, or narrow responsibly.

## Proposed-shape contract

The proposed shape is proportional to consequence. For material designs it
contains the following, expressed in prose, diagrams, pseudocode, schemas, or
examples as appropriate.

| Concern | Design must make visible |
|---|---|
| Context | The current system facts and the problem/constraint being addressed |
| Scope | In-scope outcome, non-goals, assumptions, and applicability boundary |
| Inputs | Research, source evidence, ADRs, policies, existing code, and external constraints |
| Concepts | Important categories and their meanings; what remains merely provisional |
| Laws/contracts | Record/port/boundary contracts required before implementation; validation and error/absence expectations |
| Shapes | Important transformations, preservation/loss rules, and data movement |
| Foreign boundary | External systems/libraries, raw representations, decoding point, failure translation, availability assumptions |
| Domain | Pure decisions, state/transition rules, and distinctions that must not collapse |
| Infrastructure | Ports, adapter composition, lifecycle, retries/transactions/configuration/operational consequences where material |
| Alternatives | Feasible alternatives, rejected/deferred options, and tradeoffs |
| Risks | Failure modes, migration/compatibility concerns, evidence gaps, and stop/reorientation conditions |
| Verification | Properties, examples, measurements, fixtures, or checks that would test the design's claims |
| Next relations | ADR candidates, research gaps, spikes, epics/stories, and superseded artifacts |

The table is not a ritual checklist. A design says why a concern does not apply
rather than omitting a material boundary. A novel Java/SDK integration, for
example, normally requires an explicit `extern.*` account; a pure value-model
change may state that `extern.*` and `infra.*` are out of scope.

## Alternatives and open questions

A design compares alternatives that are plausible at its stated consequence
level. It need not enumerate every imaginable option, but it records why the
considered set is adequate and distinguishes:

- **Rejected alternative:** considered and not recommended for stated reasons.
- **Deferred alternative:** deliberately left for a later scope/phase.
- **Unresolved design question:** affects proposed shape but may be narrowed by
  further design work.
- **Research gap:** requires evidence not currently available.
- **Decision candidate:** requires authorized choice because it fixes a
  consequential direction, boundary, commitment, or irreversible cost.
- **Implementation uncertainty:** can be resolved by a bounded spike without
  materially changing the design's stated contract.

Open questions are not a sign of weak design. Unclassified open questions are:
they hide whether the next responsible action is research, a decision, a spike,
or a narrower design.

## Design to decision

A design may recommend a choice; an ADR or other authorized decision record
makes the choice binding. A design produces a decision candidate when the
proposal would materially affect architecture, durable data, public contracts,
security/safety, operational commitments, reversibility, or future work
options.

A decision record that relies on a design names the specific proposal/claim it
accepts, applicable alternatives and limitations, and the acceptance authority.
A design may be superseded without making the resulting decision invalid; a
decision may supersede a design's current recommendation while preserving the
design as history and input.

## Design to work

An epic or story may implement a design only through an explicit `implements`
relation and an implementation-relevant scope. The work item links the design
sections, contracts, and acceptance conditions it relies upon.

A story is ready to execute when its remaining questions cannot materially
change its declared:

- Outcome or non-goals.
- Applicable laws/contracts or external boundary behavior.
- Acceptance and verification conditions.
- Safety, data, compatibility, reversibility, or dependency profile.

If a remaining question can change any of these, the next action is research,
a design revision, a decision, a bounded spike, or decomposition—not
implementation theater. A design does not need all future questions resolved;
it must resolve or explicitly delegate the questions that affect its next slice.

### Unblocked-slice rule

Open questions block only the work whose declared outcome, applicable contract,
verification, dependency, or consequence profile they can materially change.
They do not justify holding all work associated with a broader design.

When a material question remains unresolved, the responsible actor partitions the
design into:

- **Unblocked slices:** independently verifiable work whose governing authority,
  boundary, contract, and acceptance conditions are already sufficient.
- **Blocked slices:** work that would require inventing a material contract,
  authority, safety rule, compatibility decision, or verification condition.
- **Resolution work:** the bounded research, design, ADR, experiment, or
  dependency action that can unblock the blocked slice.

A valid unblocked slice states the unresolved question it is insulated from and
the seam it leaves for later work. It must not precommit the answer to that
question by smuggling a provisional choice into a supposedly independent
implementation.

Decomposition is an output of design, not a reason to wait. When no existing
card represents the smallest coherent unblocked slice, create one and relate it
to its blocked sibling/parent.

## Review and disposition

Design review evaluates the shape against its declared purpose and consequence.
Review asks:

- Is the problem and scope bounded enough to evaluate?
- Are the source facts, research inputs, and authoritative constraints linked
  and distinguished from recommendations?
- Does the proposal account for relevant `law`, `shape`, `extern`, `domain`,
  and `infra` responsibilities or explicitly exclude them?
- Are foreign values decoded before domain interpretation and are failures
  represented honestly?
- Are alternatives, risks, non-goals, and open questions sufficient for the
  consequence of acting now?
- Is the verification strategy capable of falsifying material design claims?
- Does the disposition correctly request research, an ADR, stories, a spike,
  revision, or rejection?

A review result is preserved in a review record or other explicit acceptance
mechanism. “Approved design” means approved for the stated use; it does not
mean every future story is accepted or every decision has been made.

## Pseudocode and diagrams

Clojure-shaped pseudocode, EDN, diagrams, tables, and examples are legitimate
design data. They may introduce future symbols, contracts, functions, macros,
namespaces, and values before executable implementation exists.

A design artifact is responsible for declaring what its pseudocode means:

```clojure
;; Design data: not a runtime call and not a false claim of implementation.
(extern.git/read-revision repository-ref revision-id)
;; => declared boundary result: observed revision record | boundary failure
```

Do not reject a form solely because it does not compile in the current tree.
Do reject a form that conceals its intended boundary, inputs, result shape,
status, or relation to current evidence when those are material.

## Change and migration

A material design revision preserves its earlier proposal and records what
changed, why, affected decisions/work items, compatibility/migration impact,
and whether prior verification must be repeated. A replacement design uses an
explicit `supersedes` relation; it does not overwrite history into apparent
continuity.

Existing design documents are valid data before full conformance to this policy.
On material revision, classify their status, identify their intended use,
separate observations/recommendations/decisions where ambiguous, and add
relations to relevant research, ADRs, and work items. Do not mass-rewrite the
corpus merely to make headings uniform.

## Exceptions and policy evolution

A design may omit a normal concern only when it records why that concern is
inapplicable or an exception under `PROCESS.md` identifies the waiver, scope,
risk, authority, expiry/review point, and follow-up.

A recurring design-pattern gap is evidence to improve the document template,
engineering kernel, research practice, or decision policy. Changes to this
policy are proposed, trialed, assessed, and explicitly adopted, revised,
superseded, or retired under the Charter.

## Operational references

- Process Charter: `PROCESS.md`
- Engineering kernel: `STYLE.md`
- Operational terminology: `docs/process/glossary.md`
- Research practice: `docs/process/research.md`
- Document governance λ: `docs/process/document-governance.md`
- Kanban workflow: `docs/process/kanban.md`
- Architecture decisions: `docs/adrs/`
- Existing design corpus: `docs/designs/`
