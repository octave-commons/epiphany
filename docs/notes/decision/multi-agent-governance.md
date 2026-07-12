---
title: Decision — Multi-Agent Governance and Autonomy Boundaries
slug: multi-agent-governance
created: 2026-07-11
source: docs/inbox/clojure Natural Language Processing.md
kind: decision
status: proposed
---

# Decision — Multi-Agent Governance and Autonomy Boundaries

## Status

Proposed — pending human review before becoming an approved ADR.

## Relationship to ADR-000

[ADR-000: Foundational Technology Stack](docs/adrs/ADR-000.md) establishes related policies (LLM usage, simulation policy, observability, source governance) but does not define a full multi-agent governance framework, role taxonomy, or decision-card protocol. This decision remains open and should be reconciled with ADR-000 before approval.

## Context

The platform is intended to delegate exploration, prototyping, and iteration to auto-research and dev agents, but agents cannot responsibly invent and validate the overall system without human supervision. A clear governance model is needed before autonomous agents are deployed.

## Decision

### 1. Agents operate in bounded, machine-verifiable loops

Agents excel when given:

1. A fixed set of tools.
2. A clear objective metric.
3. An automated verifier.

Autonomous loops are permitted for:

- parameter tuning
- retrieval/graph algorithm comparison
- simulation calibration on approved historical data
- visualization anomaly detection
- benchmark replication
- low-cost ablation studies

### 2. Human architect remains responsible for

- Ontology design (entity types, relations, taxonomies).
- Evaluation metrics that matter for each subsystem.
- Safety and autonomy boundaries.
- Training and benchmark sets.
- Architectural tradeoffs and hardware coordination.
- Definitions of “what problem are we solving” and “what counts as success.”

### 3. Agent roles and permissions

| Role | Permitted actions | Prohibited actions |
|---|---|---|
| Scout | Propose artifacts for ingestion from allowlisted sources | Fetch without source-registry entry |
| Reader | Extract components and write evidence-linked briefs | Promote extracted claims as accepted facts |
| Prior-art agent | Answer bounded “what resembles this?” queries | Declare novelty or lack of prior art absolutely |
| Critic | Flag scope mismatch, missing baselines, threats, counterarguments | Block or reject approved experiments unilaterally |
| Designer | Compose candidate experiment specifications | Run experiments without approval policy |
| Reproduction agent | Execute approved low-risk reruns/benchmarks | Exceed resource budget or alter objectives |
| Librarian | Propose taxonomy/source-trust/link adjustments | Modify trusted schemas without review |
| Supervisor gate | Human approval and policy engine | N/A (human role) |

### 4. Every agent action is traceable

Every agent action must record:

- assigned task
- tool permissions
- time/token/resource budget
- trace/correlation ID
- source evidence used
- outcome and failure category

### 5. Human review gates

Require explicit human approval for:

- new model families
- new external datasets
- materially higher resource budgets
- altered objectives/metrics
- politically, socially, or safety-sensitive scenarios
- public claims or publication drafts
- changes to trusted schemas or ontology
- expensive or externally visible actions

### 6. Decision card format

When an agent needs human direction, it must provide:

```clojure
{:decision/context ...
 :decision/options [...]
 :decision/evidence [...]
 :decision/consequence ...
 :decision/reversibility ...
 :decision/recommendation ...}
```

### 7. Safety and epistemic controls

- Provenance and source-tier visible in every answer, graph edge, and experiment plan.
- Explicit separation between observed data, author claim, model extraction, agent hypothesis, human-accepted interpretation, and experimental result.
- Clear uncertainty language: absence of retrieved evidence is not evidence of novelty.
- Negative and failed results are retained and influence future proposal ranking.
- Autonomous loops detect repetitive or non-informative cycles and halt them.
- Global pause mechanism for all autonomous loops.

## Consequences

Positive:

- Keeps humans in the loop for consequential decisions while automating routine verification.
- Prevents agents from silently drifting toward locally optimal but globally wrong solutions.
- Creates an auditable record of agent activity and human oversight.

Negative:

- Requires building and maintaining a policy/permission system before agents are useful.
- Adds friction to fast experimentation loops.
- Human bandwidth can become a bottleneck if the review queue is not well-designed.

## Alternatives considered

### Fully autonomous agent swarm

**Rejected.** Empirical studies show unsupervised deployment leads to non-mergeable pull requests and slower performance for experienced developers. Agents need human supervision for architectural and value-laden decisions.

### No agents at all

**Rejected.** The user’s objective is to use agents as an active research substrate. A no-agent approach would defeat the purpose of the platform.

## Approval

Pending human review and approval before this becomes an ADR in `docs/adrs/`.
