---
slug: process-glossary
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000215
title: "Epiphany Process Glossary"
kind: process-policy
status: draft
description: "Operational terminology for process artifacts, claims, evidence, commitments, verification, and acceptance."
labels: [process, governance, terminology, epistemic-integrity]
created: "2026-07-12"
implements: ["PROCESS.md"]
source:
  - "docs/process/epiphany-meta-workflow.md"
  - "docs/process/epiphany-development-process.md"
  - "docs/process/epiphany-agent-ledgers.md"
---

# Epiphany Process Glossary

## Purpose

This glossary gives people and agents one operational vocabulary for describing
work. It prevents a document, a claim, an observed result, a proposal, and an
accepted decision from being treated as interchangeable.

It is a draft legislative policy. The process charter in `PROCESS.md` will
supply the constitutional rules this glossary implements. This glossary does
not itself approve an architectural or product decision.

## Reading the terms

Terms are intentionally narrower than ordinary speech. A word may have a
broader everyday meaning; when used in Epiphany process artifacts, use the
definition here.

The terms describe different axes. An artifact can contain an observation. An
observation can be evidence for a claim. A finding can support a proposal. A
proposal can be accepted as a decision. None of those promotions is automatic.

## Core objects

### Artifact

A durable, addressable object used or produced by work. An artifact has an
identity or stable locator and enough provenance to identify its version or
observation context.

Examples include a Markdown document, EDN record, Kanban card, Git commit,
source extract, test result, CI report, benchmark dataset, generated index, or
ledger entry.

An artifact is not automatically evidence, correct, current, accepted, or
canonical merely because it exists.

### Record

A structured artifact intended to preserve a discrete fact, event, relation, or
result for later inspection or computation.

Examples include an observation record, a review-decision event, a test-run
record, and a Kanban transition event. A Markdown document may describe a
record but is not necessarily the authoritative record itself.

### Document

A human-readable artifact whose primary purpose is to communicate context,
claims, decisions, instructions, or interpretation. A document may be a source
of evidence, but its assertions must still be assessed by provenance and tier.

### Source

An artifact from which an actor obtains information. A source may be canonical,
primary, secondary, derived, local, external, available, or unavailable. These
are attributes of the source, not synonyms.

A source record identifies the source being relied upon; it does not claim that
the source is trustworthy or that a particular interpretation follows from it.

### Canonical source

The authority designated for a particular class of facts. Canonical does not
mean universally true, immutable, complete, or available. For Git-originated
commit/tree/blob/path facts, Git is canonical under ADR-000.

### Projection

A derived, rebuildable representation produced from other artifacts or records.
Indexes, embeddings, caches, graph views, parsed trees, and reports can be
projections. A projection must retain enough provenance to identify its inputs,
method/version, and production time.

## Claims and epistemic status

### Claim

A proposition that can be assessed as supported, unsupported, contradicted,
unknown, or accepted under a stated scope. A claim is not evidence; it is what
evidence may bear on.

Example: “These two sections continue the same line of thought” is a claim.

### Observation

A record that reports what an actor directly encountered or executed, including
its source/context and method of observation. It does not add an interpretation
beyond what the observation method can establish.

Examples: a Git tree entry at a commit, a command exit status and output digest,
a Mongo document returned by a query, or a user answer recorded with its time
and question.

### Evidence

An artifact, observation, or relation used to assess a specific claim. Evidence
must name or be linkable to the claim it bears on. Relevance is not proof, and
evidence may support, weaken, or contradict a claim.

“Evidence” is therefore a relation in context, not a permanent honorary label
for a file.

### Finding

A bounded, derived claim reached by interpreting one or more pieces of evidence
with a stated method and limitations. A finding must preserve links to its
basis, including materially contrary evidence when known.

A finding is not an accepted decision, even when it is highly credible.

### Hypothesis

A provisional, testable claim offered to guide inquiry. A hypothesis identifies
what evidence would increase, decrease, or fail to resolve confidence in it.

### Inference

The act or result of deriving a claim from observations, evidence, rules, or a
model. The method, actor, and inputs must be recorded where the inference has
material effect. An inference cannot silently inherit the authority of its
inputs.

### Provenance

The recorded origin and transformation history needed to understand an
artifact, record, claim, or result: who/what produced it, from which inputs,
using which method/version, and when. Provenance supports inspection; it does
not prove correctness.

### Epistemic tier

The explicit status of a claim or record in the chain below:

```text
observed -> derived -> provisional -> accepted
```

- **Observed**: directly recorded from a source or execution.
- **Derived**: computed or transformed from identified inputs.
- **Provisional**: proposed for review or further inquiry.
- **Accepted**: explicitly adopted by the authority declared for that claim.

A record may be rejected, stale, unavailable, corrupt, or unknown in addition
to its tier where those statuses apply. Do not collapse those states into an
empty result.

## Work and commitments

### Request

An expression of desired outcome, question, concern, or instruction from a
user, system, or authorized actor. A request establishes intent to understand;
it does not by itself establish scope, design, acceptance criteria, or priority.

### Intake

The act of recording a request and identifying its initial context, candidate
artifacts, uncertainty, and next responsible process step. Intake is not a
promise to implement.

### Inquiry

A bounded effort to reduce a named uncertainty. Inquiry can use reading,
workspace inspection, interviews, experiments, measurements, or reproduction.
Research is a kind of inquiry with a declared method and evidence set.

### Research activity

A bounded inquiry that reduces a named uncertainty through a declared method
and recorded evidence set. It produces source records, observations/extracts,
findings, limitations, and a stated disposition. A long summary without these
is a note, not necessarily research.

### Commitment

A recorded, revisable statement that an actor intends to pursue a bounded course
of action. A commitment identifies its target, basis, scope, stop conditions,
and expected next artifact or evidence.

A commitment is not acceptance, a delivery promise, or evidence that the work
succeeds.

### Plan

A proposed sequence of work intended to reach a stated outcome under known
constraints. A plan names dependencies, assumptions, checkpoints, and how it
will be revised. A plan is not a decision unless explicitly accepted as one.

### Specification

A testable statement of required behavior, constraints, interfaces, and
acceptance conditions for a bounded change. A Kanban story may contain a
specification; it is not automatically complete merely because it has a title
and estimate.

### Work item

A bounded, trackable unit of planned work, normally represented by a board
card. A work item records scope, status, dependencies, expected evidence, and
its relationship to relevant design/decision artifacts.

### Scope

The explicitly included outcome, boundary, and responsibility of an artifact or
work item. Non-goals state what is deliberately outside that scope. Scope is
revisable, but revisions must be recorded rather than silently substituted.

### Stop condition

A condition requiring an actor to pause, reorient, escalate, or end the current
commitment. Examples: contradictory evidence, missing authority, exceeded time
or complexity budget, unavailable required source, or invalidated assumption.

## Decisions and authority

### Proposal

A provisional recommended action, interpretation, or policy. A proposal states
its rationale, supporting evidence, alternatives where material, and the
authority required to accept or reject it.

### Decision

An explicitly accepted choice that authorizes or constrains later action within
its stated scope. An ADR records an architectural decision; a review event can
record an interpretive decision. A decision does not prove its consequences or
implement itself.

### Acceptance

The explicit act by an authorized reviewer, user, or defined process of adopting
a claim, proposal, decision, artifact, or work outcome for its declared
purpose. Acceptance must identify what was accepted, by whom/which authority,
on what basis, and within what scope.

### Authority

The person, role, policy, or process permitted to accept, reject, or change a
specific class of claim. Authority is scoped: a test runner may establish an
observed pass result, but it cannot accept an architectural decision.

### Policy

A revisable rule set that operationalizes constitutional commitments for a
specific context. A policy declares its scope, owner, status, review or expiry
condition, and enforcement mechanism where feasible.

### Exception

A deliberate, temporary authorization not to follow a policy in a defined case.
An exception records the policy waived, rationale, scope, owner/approver, risk,
expiry/review date, and required follow-up. An exception is not an amendment and
must not silently become normal practice.

## Verification and completion

### Verification

A defined procedure for checking whether a claim, implementation, or artifact
satisfies stated criteria. Verification records the procedure, inputs,
environment where material, result, and limitations.

Verification can increase confidence in a bounded claim; it does not establish
all desired properties by default.

### Verification record

An observation artifact recording that a verification procedure was actually
run and what it produced. Examples include CI output, a test report, a manual
review checklist, a benchmark result, or a reproduction log.

### Acceptance criterion

An observable condition that a work outcome must satisfy for its declared scope.
An acceptance criterion identifies its verification method or evidence type.
“Tests pass” is not sufficient unless it names the relevant claim and test.

### Completion claim

A claim that a work item or phase has satisfied its declared completion
conditions. It must link to verification records and the accepting authority.
A completion claim is not established by a status field alone.

### Review

An evaluation by an authorized actor of an artifact, change, claim, or evidence
set against declared criteria. Review may accept, reject, request changes, or
identify unresolved uncertainty.

### Handoff

A transfer of responsibility in which the receiving actor is given enough
context to continue, challenge, or revise work. A handoff includes current
state, basis/evidence, open questions, next action, and any stop conditions.

## Relations and failure states

### Supports / contradicts

Relations between evidence and a claim. They state relevance and direction, not
certainty. A relation records its asserted tier and basis; it does not turn an
artifact into a conclusive proof.

### Dependency

A relation in which work, a decision, an input, or an external condition must be
satisfied before another bounded action can responsibly proceed. Dependencies
must name the blocked outcome and the condition that resolves them.

### Unknown

The system or actor lacks sufficient basis to make the claim. Unknown is not
false, empty, rejected, or unavailable.

### Unavailable

A required source, service, or artifact cannot currently be read or used.
Unavailable is an observation about access, not a conclusion about the content.

### Corrupt

An artifact or representation fails integrity, decoding, or format checks.
Corrupt data must not silently be treated as absent or valid.

### Stale

A derived artifact was produced under inputs, methods, models, schemas, or
policies that no longer meet the current applicability requirement. Stale is
not necessarily incorrect; it requires explicit treatment.

## Usage rules

- Name the epistemic tier whenever a material claim could be mistaken for an
  observation, decision, or accepted outcome.
- Link a material finding, proposal, decision, completion claim, or exception
  to its basis and authority.
- State `unknown`, `unavailable`, `corrupt`, `stale`, or `not implemented`
  explicitly rather than using an empty result as a substitute.
- Do not use a document's existence, an agent statement, a plan, or a green
  check as evidence of a broader claim than it actually supports.
- When ordinary language conflicts with a defined term, use the defined term in
  structured metadata and governance artifacts.
