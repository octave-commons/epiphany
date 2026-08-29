---
slug: cross-repository-triage-2026-07-30-task-authority
uuid: 4f764d5f-8c24-47ce-a9a0-e4bba888d7aa
title: "Cross-Repository Triage - Task Authority and Ledger Protocol Packages"
kind: note
status: draft
description: "Revision-scoped triage of eta-mu task synchronization and proposed ledger protocol packages."
created: "2026-07-30"
labels: [triage, provenance, kanban, task-authority, receipt-river, session-mycology, fork-tax]
sources:
  - "open-hax/eta-mu@c06308d940654ab51ae043cde20e10cc71f27b99"
  - "open-hax/eta-mu#164"
  - "open-hax/eta-mu@f0a8d43003583da2977b6fb03ef3786cdb43ceb8"
  - "open-hax/eta-mu#161@aae48e305b97fa524634130ab70c6a93f36d106d"
implements:
  - "docs/process/inbox.md"
  - "docs/process/notes.md"
informs: []
---

# Cross-Repository Triage - Task Authority and Ledger Protocol Packages

## Scope

This note records two distinct developments:

1. a merged correction establishing that repository documentation is not automatically task authority for GitHub issue synchronization; and
2. an open proposal extracting Receipt River, Session Mycology, and Fork Tax into package-owned ledger protocols.

The first is current implementation evidence. The second remains proposed until accepted and merged.

## Merged observation: documentation is not task authority

eta-mu PR #164 was merged as commit `c06308d940654ab51ae043cde20e10cc71f27b99`. It corrected the legacy kanban GitHub exporter after recursive Markdown discovery treated repository prose as active work.

The merged behavior now scopes synchronization to the configured kanban task directory, excludes ordinary documentation and notes by default, supports explicit `sync_github` overrides, preserves task identity across status moves, validates duplicate IDs only among export-eligible tasks, preserves manual issue closures, and reports exclusions in the synchronization plan.

### Interpretation

This makes an authority boundary executable:

```text
repository prose and observations
  != active task records

active task projection
  = explicitly selected task corpus
    plus explicit synchronization intent
```

That is consistent with Epiphany process rules: observations, notes, findings, proposals, accepted decisions, and executable work items have distinct promotion paths. File location or Markdown shape alone must not promote prose into task authority.

### Consequence

A recursive inventory may observe all Markdown, but a task projection should require an explicit task schema, configured projection path, or accepted promotion event. Discovery and activation are separate operations.

## Merged observation: unresolved review threads are not verified defects

eta-mu commit `f0a8d43003583da2977b6fb03ef3786cdb43ceb8` records an incubating Session Mycology spore from PR #142 closeout. Most unresolved-looking bot review threads had already been fixed in later branch commits; the remaining merge block was conversation resolution plus a small amount of real gate residue.

```text
unresolved review thread
  != verified current defect
```

A review finding is an observation tied to a revision. Before it becomes work, it must be re-verified against the branch tip and repository gates. The spore remains incubating process evidence, not an accepted permanent rule.

## Proposed design: package-owned ledger protocols

eta-mu PR #161 proposes sibling packages for `@eta-mu/receipt-river`, `@eta-mu/session-mycology`, and `@eta-mu/fork-tax`. Each package would own its API, schema registry, event envelope, validation, tests, and version components while eta-mu remains the installed command router.

### Current disposition

`proposal / implementation candidate`.

The branch contains executable evidence and tests, but it is still an open pull request. Package ownership, command names, and schema boundaries are not accepted architecture yet.

### Review risks

- accidental duplication of common ledger laws across the three packages;
- compatibility with canonical event-ledger v1 envelopes and Axxium/Katamorph references;
- preservation of explicit proposal and promotion state for Session Mycology spores;
- preservation of source revision, repository identity, and observation method in Receipt River providers; and
- distinction between Fork Tax plans and records of completed actions.

## Dispositions

| Source | Classification | Disposition |
|---|---|---|
| eta-mu PR #164 / `c06308d` | merged implementation evidence | accepted operational boundary: prose is excluded from task synchronization unless explicitly projected or opted in |
| eta-mu `f0a8d43` spore | observation, interpretation, skill candidate | retain as incubating evidence; verify before promotion |
| eta-mu PR #161 at `aae48e3` | proposal with executable branch evidence | review package ownership and envelope compatibility; do not treat as accepted until merged |

## Bounded follow-up

1. When PR #161 changes state, verify the merged revision rather than this branch snapshot.
2. Compare the proposed package envelopes with canonical event-ledger v1 and identity/resource references.
3. Preserve the distinction between inventory, note ingestion, task projection, and GitHub issue synchronization in Epiphany and Rheos designs.
