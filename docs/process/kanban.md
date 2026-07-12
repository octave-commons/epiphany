---
slug: kanban-workflow
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000216
title: "Kanban Workflow Policy"
kind: process-policy
status: draft
implements: ["PROCESS.md#keep-work-accountable-and-bounded", "PROCESS.md#make-acceptance-explicit-and-scoped", "PROCESS.md#adapt-deliberately-not-silently"]
operational-guide: "docs/kanban/AGENTS.md"
description: "The revisable workflow for board-backed work after it enters the Epiphany Kanban system."
labels: [process, kanban, planning, review, governance]
created: "2026-07-12"
---

# Kanban Workflow Policy

## Purpose

This policy governs how bounded work moves through the Epiphany board after it
has entered the board system. It implements the Process Charter's requirements
for accountable work, explicit acceptance, recorded evidence, and deliberate
adaptation.

The board is not proof that work is correct or accepted. It is the current,
reviewable coordination view of a work item's scope, state, dependencies,
commitments, and evidence.

## Scope and non-goals

This policy applies to implementation work, significant maintenance, research,
design, and planning represented by a board card. It defines the workflow and
entry/exit expectations for those cards.

It does not define product architecture, document-kind templates, engineering
commands, or the data format/CLI implementation of the board. Those are
separately governed by ADRs, document standards, engineering policy, and the
operational guide at `docs/kanban/AGENTS.md`.

## Board record

A work item is represented by one authoritative card with stable identity. The
card is the coordination record for the item; generated board snapshots are
views and must not be edited as source.

The current tool, card layout, frontmatter contract, generated artifacts, and
commands are defined by `docs/kanban/AGENTS.md`. Tooling must preserve a
reviewable history of material status, scope, dependency, and acceptance-evidence
changes. Until provenance support is automated, the actor records the material
change and its basis in the card's append-only progress history or comment
mechanism.

Every card has, at minimum:

- A stable ID, title, type, status, priority, estimate, and current responsible
  actor or explicit unassigned state.
- A bounded intended outcome, scope, and non-goals appropriate to its type.
- Dependencies and authoritative inputs where they exist.
- Completion conditions and expected verification/acceptance evidence.
- A current account of material uncertainty, blocker, scope change, or handoff.

A card may be incomplete while in intake/planning states. It may not be treated
as execution-ready merely because it exists.

## State model

The active state vocabulary is:

```text
icebox -> incoming -> accepted -> breakdown -> ready -> todo
       -> in_progress -> review -> document -> done
```

`rejected` is reachable when a card is determined not to be pursued. A
`breakdown` card may also represent a decomposed parent: it remains a durable
planning record but is not itself executable. The exact allowed edges and WIP
limits are operational rules maintained with the active board configuration.

States describe process position, not truth:

| State | Meaning | It does not mean |
|---|---|---|
| `icebox` | Known work intentionally deferred | Rejected, forgotten, or ready |
| `incoming` | Captured for initial triage | Understood or committed |
| `accepted` | Worth planning; owner/priority and initial inputs are known | Scoped for implementation |
| `breakdown` | Being clarified, decomposed, or held as decomposed parent | Executable work |
| `ready` | Meets current readiness policy | Automatically assigned or started |
| `todo` | Selected for the execution queue | In active implementation |
| `in_progress` | An actor has an active bounded commitment | Complete or reviewable |
| `review` | A reviewable result and its evidence await evaluation | Accepted or done |
| `document` | Required documentation/acceptance record is being finalized | Proof that acceptance occurred |
| `done` | Outcome accepted for the card's declared scope | Globally correct, permanent, or beyond revision |
| `rejected` | Deliberately not pursued in the stated form | Erased or disproven |

A future state-model change must be proposed and adopted under the governance
change rules; it must preserve the ability to distinguish planning, active work,
review, documentation/acceptance recording, and accepted completion.

## Entry and planning

### Intake

New work enters `incoming` or `icebox`. Intake records the request/source,
initial outcome, known context, and the next responsible triage step. Intake is
not an implementation commitment.

### Triage

Triage decides whether the item is worth planning now, deferred, or rejected.
It records priority, initial authority/input links, material uncertainty, and
why the chosen disposition is appropriate. A rejection preserves the reason and
any relevant evidence; it does not silently discard the request.

### Breakdown

Planning makes the next executable slice explicit. It must:

- Identify the outcome that the slice—not the broader topic—will deliver.
- Separate scope from non-goals and list material dependencies.
- Identify the governing design/ADR/research inputs when the work changes a
  behavior, boundary, contract, or architectural policy.
- Resolve, defer explicitly, or escalate open decisions in proportion to risk.
- State observable completion conditions and the verification evidence expected.
- Estimate honestly; a slice above the current ready-size limit is decomposed
  before it becomes `ready`.

Research, design, policy, and hygiene work may be their own bounded work items.
They are not exempt from clear outcomes and evidence; they may be exempt from a
pre-existing design link when creating that design or its grounding is the
outcome.

## Readiness and execution

A card enters `ready` only when another qualified actor could begin the stated
slice without inventing material scope, authority, or verification rules.

At readiness, the card has:

- A bounded outcome and non-goals.
- Honest estimate within the active ready-size limit.
- Dependencies in a state permitted by the active board policy.
- Applicable authoritative inputs linked or an explicit statement that none are
  required for the work profile.
- Completion conditions, verification approach, and known risks/open questions.

Moving `ready -> todo` selects the card for the execution queue. Moving
`todo -> in_progress` records an actor's commitment to the current scope and
identifies a stop/reorientation condition where material uncertainty remains.
WIP limits protect attention and review capacity; they are policy controls, not
proof of progress.

## Execution, handoff, and reorientation

An actor in `in_progress` performs the smallest coherent action that can produce
reviewable evidence for the card's declared outcome. It records material
observations, scope changes, failures, and results as they occur.

The actor must return the card to planning/breakdown or otherwise follow the
active transition policy when:

- The slice exceeds its estimate or needs decomposition.
- A dependency, authority, source, or environment required for responsible work
  is unavailable.
- New evidence contradicts the current design, plan, or acceptance conditions.
- The card's scope would need material expansion to claim completion.

A handoff is not “someone else can read the diff.” It records current state,
what was tried, observed evidence, changed artifacts, remaining uncertainty,
next recommended action, and active stop conditions.

## Review, documentation, and completion

A card enters `review` only with a coherent, inspectable result and the evidence
needed for the applicable reviewer to evaluate it. Review evaluates the declared
scope and acceptance criteria; it may accept, reject, request changes, or return
the work to planning.

The `document` state exists to finish durable documentation, completion evidence,
and acceptance recording when that work is material and not already complete.
Documentation is not clerical theater: it preserves the basis on which a later
actor can understand, challenge, reproduce, or extend the work.

A card enters `done` only when the authorized reviewer/process has accepted the
outcome for its declared scope. The completion record identifies:

- What outcome was accepted and what remains out of scope or uncertain.
- Applicable verification records and their actual results.
- Documentation, migration, operational, or follow-up artifacts required by the
  card's scope.
- The accepting authority and the date/context of acceptance.

A green command, merged change, or card status alone is not acceptance evidence.
A failure in review returns the card to the state where the next responsible
work can occur; it is not hidden by marking the item done.

## Dependencies and blockers

A dependency names the outcome or condition another item needs—not merely a
related topic. Cards record dependencies by stable identity where possible.

A blocker is a current condition preventing responsible progress. It records the
blocked outcome, basis, owner or external condition, attempted remediation, and
what would unblock it. A blocker is not a place to conceal an oversized or
unclear card; those return to breakdown.

## Evidence and board history

The board records coordination evidence, not every raw artifact. Cards link to
the relevant source, design, ADR, test/CI record, report, commit, or other
artifact rather than copying large evidence sets into prose.

Material card history is append-oriented: preserve prior scope, decision,
transition, and acceptance context sufficiently to explain why the card reached
its current state. Corrections may clarify a record but must not erase a material
prior claim without noting the correction and basis.

## Policy adaptation

This workflow is intentionally changeable. A proposed change to state rules,
WIP limits, card requirements, or transition gates must state:

- The observed flow, quality, comprehension, or agent-governance problem.
- The proposed rule and affected cards/actors.
- The expected benefit, risk, and success or review measure.
- The owner, trial scope, and review/expiry date.
- Whether the change needs tool/configuration support before it is enforceable.

Trials are explicit. Their outcome is recorded as adoption, revision,
supersession, or retirement. Urgent deviation uses a documented exception under
`PROCESS.md`; repeated exceptions trigger policy review.

## Operational references

- Process constitution: `PROCESS.md`
- Operational terminology: `docs/process/glossary.md`
- Current board layout, card fields, CLI, and tool behavior:
  `docs/kanban/AGENTS.md`
- Current delivery order and gate map: `docs/kanban/BOARD-BREAKDOWN.md`

## Transition note

This policy replaces the prior `docs/process/kanban.md` text, which combined an
older Rheos workflow with later eta-mu commands and duplicated a root process
FSM. The older content remains in Git history. The active operational guide is
currently the eta-mu-based `docs/kanban/AGENTS.md`; any unimplemented tool
behavior described there is an operational gap to resolve, not a reason to
invent a parallel workflow.
