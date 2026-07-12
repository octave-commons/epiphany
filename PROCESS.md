# Epiphany Process Charter

## Purpose

Epiphany's process exists to help people and agents build understanding, make
bounded commitments, preserve the basis for consequential claims, and revise
course without misrepresenting uncertainty or acceptance.

This is a constitution, not an execution manual. It states durable constraints
on how work, evidence, decisions, and acceptance relate. Current workflows,
templates, commands, board states, and tool-specific checks are delegated to
process policies and operational guides.

## Scope and non-goals

This charter governs work performed for or represented as work on Epiphany,
whether it is performed by a person, an agent, or automation. It applies to
research, planning, design, architectural decisions, implementation, review,
and process change.

It does not prescribe:

- A particular Kanban tool, board layout, state graph, WIP limit, or command.
- A universal test technique, quality metric, or CI provider.
- A required sequence for trivial or low-risk work.
- A particular multi-agent, actor, ledger, or graph implementation.
- The detailed schema for every document kind or record.

Those details belong in revisable policies, templates, and operational guides.
They may add safeguards but may not weaken this charter without an explicit
charter amendment.

## Authority and interpretation

When guidance conflicts, apply this order within its proper scope:

1. This charter constrains process claims, evidence, authority, and acceptance.
2. Approved ADRs constrain architectural decisions within the charter.
3. Active process policies in `docs/process/` operationalize this charter.
4. Document standards, templates, examples, and automated checks define the
   current artifact contract for their declared kind.
5. Operational guides such as `AGENTS.md` and `docs/kanban/AGENTS.md` explain
   how to execute the current policy and tooling.
6. A work item, plan, comment, or agent instruction governs only its declared
   local scope.

A lower-order artifact must not silently override a higher-order one. If the
applicable authority is ambiguous or conflicts, record the ambiguity and seek
clarification rather than inventing an interpretation as settled policy.

The operational meanings of *artifact*, *observation*, *evidence*, *claim*,
*finding*, *proposal*, *decision*, *commitment*, *verification*, *acceptance*,
*policy*, and *exception* are defined in `docs/process/glossary.md`.

## Constitutional commitments

### Preserve epistemic tiers

No actor may silently promote an observation into an inference, an inference
into a finding, a finding into a decision, a plan into completion, or similarity
into identity.

A material claim must retain its epistemic tier and basis. At minimum, process
artifacts distinguish:

```text
observed -> derived -> provisional -> accepted
```

Promotion requires an explicit record of the authority, basis, scope, and time
of promotion. Rejection, unknown, unavailable, corrupt, stale, and not
implemented are meaningful states; they must not be represented as a successful
or empty result merely for convenience.

### Make claims proportionate to evidence

The required depth of inquiry, review, and verification is proportional to the
consequence, reversibility, uncertainty, and blast radius of the claim or
change. A typo does not require an ADR. A durable identity rule, data migration,
security boundary, or new autonomous agent authority requires stronger grounding
and explicit acceptance.

Policies may classify work by risk and select concrete gates. No classification
may be used to make a claim more certain, tested, reviewed, or accepted than it
is.

### Keep work accountable and bounded

Material work must have a discoverable owner or responsible actor, a target
outcome, a bounded scope, and a current state. Before an actor represents work
as complete, it must be possible to discover what was attempted, what changed,
what remains uncertain, and what evidence was produced.

A commitment is revisable. When new evidence invalidates its basis, exceeds its
scope, or reveals a material unknown, the actor pauses, records the reason, and
reorients rather than silently continuing under an obsolete plan.

### Separate inquiry, recommendation, and authority

Research may reduce uncertainty; it does not automatically authorize action.
Design may propose an approach; it does not automatically decide architecture.
An ADR records a decision; it does not automatically implement or verify that
decision. A board card authorizes a bounded attempt; it does not prove its
outcome.

An accepted decision or completion claim identifies the authority that accepted
it and the evidence considered. An actor may state its confidence or
recommendation but may not impersonate that authority.

### Preserve provenance and reproducibility

Material findings, decisions, implementations, and verification results retain
enough provenance for another actor to inspect, challenge, reproduce, or
supersede them responsibly. The exact form varies by policy, but it normally
includes inputs/sources, method or command, relevant version/context, result,
limitations, and producer.

Generated reports, summaries, indexes, and agent statements are derived
artifacts. They must not be their own sole authority for a material claim.

### Make acceptance explicit and scoped

Acceptance is an explicit act, not an implication of silence, merge status, a
passing check, or a status field. It identifies what was accepted, for which
purpose and scope, by which authority, and on what basis.

Acceptance does not erase rejected alternatives, unresolved uncertainty, or the
provenance of the evidence that led to it. A later decision may supersede an
earlier one, but the earlier record remains discoverable.

### Favor reversible progress

Prefer small, reviewable, testable, and recoverable steps over broad
irreversible change. Preserve canonical sources and durable decisions; treat
rebuildable projections, caches, summaries, and indexes as derived artifacts.

Where irreversible action is necessary, record the risk, authority, recovery or
rollback path where feasible, and the evidence sufficient to justify it.

### Adapt deliberately, not silently

The process is expected to change as Epiphany, its contributors, and its agents
change. Process friction, escaped defects, ambiguous tasks, failed research,
and gaming attempts are evidence for improving policy.

A process change must be proposed, scoped, and recorded. A trial policy states
its hypothesis, owner, affected scope, measurement or review method, and review
or expiry date. Adoption, revision, supersession, and retirement are explicit.

## Lifecycle of responsible work

Work does not need to pass every stage. The applicable policy selects a path
proportionate to risk. When a stage is used, it must leave a successor enough
context to continue, challenge, or revise the work.

```text
request
  -> intake and orientation
  -> inquiry or research, when uncertainty matters
  -> proposal or design
  -> decision, when authority or architecture is implicated
  -> plan and specification
  -> bounded work item
  -> implementation or other action
  -> verification and review
  -> accepted outcome
  -> reflection and policy improvement
```

The recurring working modes are:

- **Explore:** inspect relevant artifacts, sources, constraints, and current
  state before asserting a direction.
- **Orient:** compare observations with the requested outcome; identify the
  next appropriate artifact, authority, and uncertainty.
- **Commit:** record a bounded, revisable course of action with basis, scope,
  stop conditions, and expected evidence.
- **Act:** perform the committed inquiry, design, implementation, review, or
  other work while preserving material observations.
- **Verify:** execute the applicable checks and record what they establish and
  what they do not establish.
- **Reflect:** record lessons, follow-ups, contradictions, and process changes
  revealed by the work.

Modes recur at any lifecycle stage. They are not board states and do not imply
that a particular agent architecture is required.

## Required evidence by claim type

Policies specify detailed templates and gates. The following minimums are
constitutional:

| Claim or action | Minimum accountable basis |
|---|---|
| Material observed fact | Source/context and observation method sufficient to inspect it |
| Derived finding | Inputs/evidence, method, scope, and limitations |
| Research result | Named uncertainty, declared method, evidence set, findings, and disposition |
| Proposal | Scope, rationale/basis, consequences, and accepting authority |
| Architectural decision | Explicit decision record, alternatives proportionate to consequence, and authorized acceptance |
| Bounded work item | Outcome, scope/non-goals, dependencies, completion conditions, and responsible actor/state |
| Completion claim | Applicable verification records, remaining limitations, and accepting authority |
| Process exception | Waived policy, rationale, scope, risk, approver, expiry/review, and follow-up |

A policy may define a lightweight form for routine work, but it may not replace
these minimums with an unsupported assertion.

## Delegated policies

The following policy areas implement this charter and may evolve independently:

- **Kanban workflow** — board intake, triage, planning, transition rules, WIP,
  handoff, and completion mechanics (`docs/process/kanban.md`).
- **Research practice** — research tiers, source/finding records, methods,
  confidence, and disposition. This policy is to be established.
- **Documentation governance** — document kinds, templates, exemplars,
  anti-exemplars, document analysis, and exception handling. This policy is to
  be established.
- **Decision practice** — ADR thresholds, decision review, supersession, and
  architecture-compliance checks. This policy is to be established.
- **Review and acceptance** — reviewer roles, evidence expectations, and
  acceptance recording. This policy is to be established.
- **Engineering practice** — coding conventions, static checks, test selection,
  release/process evidence, and environment operations. Operational guidance is
  currently in `AGENTS.md`; an Epiphany-specific engineering policy is to be
  established.

Until a delegated policy exists, use the charter, existing approved ADRs, and
explicitly recorded judgment. Do not manufacture a detailed rule and represent
it as settled policy.

## Exceptions

An exception permits a bounded departure from an active policy; it does not
amend this charter or create a hidden precedent. It must be explicit before the
departure when feasible, and otherwise recorded as soon as the emergency allows.

Every exception identifies:

- The policy and exact requirement waived
- The affected scope and duration
- The rationale and known risk
- The owner and accepting authority
- The expiry or review condition
- The required remediation, follow-up, or decision point

Expired exceptions are not authority for continued deviation. Repeated
exceptions are evidence that the underlying policy needs review.

## Charter amendments

Amending this charter is a consequential process decision. A proposed amendment
must state:

- The constitutional commitment being added, removed, clarified, or changed
- The observed problem and evidence motivating it
- Effects on active policies, templates, tools, and work in progress
- Alternatives considered, including retaining the current charter
- The acceptance authority and effective date
- A migration or review plan where the amendment changes existing obligations

The amendment record must preserve the prior text and identify whether it
supersedes or merely clarifies it. Routine policy changes belong in
`docs/process/`, not in this charter.

## Current transition

This charter replaces the former root `PROCESS.md` workflow/FSM document as the
source of constitutional process guidance. The prior content remains available
in Git history. Board-specific workflow remains delegated to
`docs/process/kanban.md` and the active board operational guide; those documents
must be reconciled with this charter before they are treated as complete policy.
