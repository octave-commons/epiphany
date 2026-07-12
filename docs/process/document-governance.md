---
slug: document-governance
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000218
title: "Document Governance Policy"
kind: process-policy
status: draft
implements: ["PROCESS.md#preserve-epistemic-tiers", "PROCESS.md#preserve-provenance-and-reproducibility", "PROCESS.md#adapt-deliberately-not-silently"]
description: "A revisable policy for document kinds, lifecycle, templates, examples, relations, analysis, and migration."
labels: [process, documentation, governance, λ, provenance]
created: "2026-07-12"
---

# Document Governance Policy λ

## Purpose

This policy makes documents first-class operational artifacts. It defines how
Epiphany names document kinds, preserves their claims and provenance, evolves
their forms, and analyzes them without confusing a document's existence with
the truth or acceptance of what it says.

`λ` names the document-governance transformation: a declared document form,
when supplied with bounded content, provenance, relations, and status, becomes
a reviewable artifact of a known kind. λ is valid design data before every
instance, template, parser, or checker is executable.

```clojure
(λ document-form
   content
   provenance
   relations
   status
   -> governed-artifact)
```

The result is not automatically correct, authoritative, accepted, or complete.
It is legible enough for a person or tool to determine what it claims, which
rules apply, and what remains to review.

## Scope and non-goals

This policy applies to durable Markdown documents and structured records that
are represented, reviewed, or relied upon as Epiphany process artifacts. It
covers authored documents, generated documentation views, templates, examples,
and document-analysis findings.

It does not require every scratch note, inbox capture, import, temporary
investigation, or historical artifact to satisfy current templates. It does not
turn prose quality, a linter pass, frontmatter validity, or a document status
into evidence that the document's substantive claims are true.

The process glossary supplies the normative meaning of *artifact*, *record*,
*claim*, *evidence*, *finding*, *decision*, *acceptance*, *policy*, and
*exception*.

## Governing principles

- **Kind before convention.** A durable document declares what kind of artifact
  it is, rather than relying on filename, folder, or author habit as the only
  classification.
- **Form before automation.** A template or checker implements a declared
  document contract; it does not invent the contract from prose regularities.
- **Relation before implication.** A link alone does not prove support,
  supersession, implementation, or acceptance. Material relations are named.
- **Status before authority.** Draft, exploratory, imported, active, accepted,
  superseded, retired, and archived artifacts have different operational roles.
- **Evolution without erasure.** Documents may be corrected, superseded, or
  migrated; their prior material claims and provenance remain discoverable.
- **Structural checking is not semantic judgment.** Automation finds missing
  shape, invalid links, and declared-rule violations. Review assesses relevance,
  sufficiency, truthfulness, and consequence.

## Document form and identity

A governed document has a stable identity independent of its current path.
Paths remain observed source facts and are never silently normalized or used as
the sole basis for semantic identity. The canonical metadata form will be
specified by the document standards; until then, a new governed Markdown
document should include frontmatter with:

```yaml
slug: stable-human-readable-name
uuid: stable-durable-identity
title: "Human-readable title"
kind: declared-kind
status: declared-lifecycle-status
description: "Bounded purpose"
created: "YYYY-MM-DD"
```

Additional fields are kind-specific. Existing artifacts with legacy `id`,
`type`, `category`, or missing metadata remain valid historical data. Their
metadata differences are migration observations, not permission to guess a new
identity or rewrite history.

## Kinds and responsibilities

Kinds describe the primary operational responsibility of a document. A document
should have one primary kind; relations express secondary roles instead of
forcing one file to impersonate several artifacts.

| Kind | Primary responsibility | It must not be mistaken for |
|---|---|---|
| `note` | Capture an observation, thought, import, or working context | Reviewed research, design, or decision by default |
| `research-brief` | Bound a research activity before substantive inquiry | A finding or decision |
| `source-record` | Identify and contextualize one source/input | Evidence that an interpretation follows |
| `finding` | State a bounded derived claim with basis and limits | Accepted decision |
| `research-report` | Synthesize an inquiry for reuse/review | The sole record of every source/finding |
| `design` | Propose a bounded technical/product/process approach | Authorized architectural decision or task plan |
| `decision` | Record an explicitly accepted consequential choice | Proof of implementation or outcome |
| `process-policy` | State a revisable operational rule implementing the Charter | The Charter itself or an immutable law |
| `epic` | Coordinate an outcome across related work items | An executable implementation slice |
| `story` | Specify one bounded, testable, reviewable slice | A design, research report, or acceptance event |
| `review-record` | Preserve evaluation and disposition of an artifact/change | The artifact reviewed |
| `verification-record` | Preserve a check run, inputs/context, result, and limitations | General proof of correctness |
| `example` | Demonstrate a document contract or practice | Production policy or evidence |
| `anti-example` | Demonstrate a known invalid/weak pattern and why | An artifact eligible for normal acceptance |
| `generated-view` | Render a projection from declared source artifacts | Canonical source or authoritative record |
| `archive` | Preserve historical/imported material with context | Active guidance without explicit reactivation |

New kinds require a proposal under this policy before broad adoption. A one-off
unclassified document is permitted only as `note`, `archive`, or an explicit
exception; it must not silently establish a new governance category.

## Lifecycle status

Status describes the operational maturity and current use of a governed
artifact. It does not by itself establish the truth of a claim.

| Status | Meaning |
|---|---|
| `exploratory` | Valid early design/thought data; not active policy or accepted direction |
| `draft` | Deliberately authored but incomplete or awaiting review |
| `review` | Submitted for identified review/acceptance process |
| `active` | Current operational policy, standard, or guide within its declared scope |
| `accepted` | Explicitly accepted claim/decision/outcome within its declared scope |
| `superseded` | Replaced for current use by an identified successor; retained for history |
| `retired` | No longer applicable; retained with reason/context |
| `archived` | Preserved historical/imported material; not current guidance |
| `rejected` | Explicitly considered and not adopted in the stated form |

Kinds constrain permitted statuses. For example, a `decision` becomes
`accepted` only through its declared authority; a `process-policy` becomes
`active` only after adoption; an exploratory design is not “accepted” merely
because it influenced a later document.

## Required relations

Relations make traceability explicit. A material relation has a typed verb,
source and target identity/path, and enough context to inspect the assertion.

| Relation | Meaning | Typical source -> target |
|---|---|---|
| `sources` | Identifies origin or input | finding/report/design -> source record/note |
| `supports` | Supplies evidence relevant to a claim | evidence/finding -> claim/design/decision |
| `informs` | Provides bounded input without deciding | research -> design/decision/work item |
| `implements` | Operationalizes a constraint or design | policy/story/code change -> Charter/design/ADR |
| `requires` | Cannot responsibly proceed without target condition | story/design -> decision/research/dependency |
| `supersedes` | Replaces target for declared current use | decision/policy/design -> prior artifact |
| `reviews` | Evaluates target under stated criteria | review record -> artifact/change |
| `verifies` | Records a procedure checking declared criteria | verification record -> claim/story/implementation |
| `accepts` | Records authoritative acceptance | acceptance/review event -> artifact/outcome |
| `examples` | Demonstrates a contract | example -> template/rule |
| `violates` | Deliberately demonstrates a rule failure | anti-example -> rule/template |

A relation may be observed, derived, provisional, or accepted. A Markdown link
without a typed relation is navigation, not evidence of any particular
relationship.

## Minimum document contracts

Detailed templates are maintained under `docs/standards/` once established.
Until then, the following minimums apply to new or materially revised governed
documents.

| Kind | Required minimum content |
|---|---|
| Research brief | Question, intended use, tier, scope/non-goals, method, stop conditions, disposition authority |
| Finding | Bounded claim, evidence/source basis, method, scope, limitations, confidence, disposition |
| Research report | Brief/question, method, artifact inventory, findings, conflicts/limitations, implications/non-implications, open questions |
| Design | Purpose/context, scope/non-goals, constraints, proposed shape, interfaces/data/failure considerations, alternatives/open decisions, verification/implementation implications |
| Decision | Context, decision, authority/status, alternatives, consequences, compliance/enforcement, supersession/reversibility where material |
| Process policy | Purpose, scope/non-goals, Charter clauses implemented, current rules, exceptions/adaptation, operational references |
| Epic | Outcome, scope/non-goals, success signals, dependencies, child-work strategy, current risks |
| Story | Intent/outcome, scope/non-goals, authoritative inputs, dependencies, acceptance conditions, verification approach, completion evidence |
| Review/acceptance record | Target, criteria, observed basis, disposition, authority, unresolved limitations/follow-up |
| Verification record | Procedure, inputs/environment as material, actual result, artifact/version context, limitations |
| Example/anti-example | Target contract, scenario, rules demonstrated or violated, explanation, expected checker/review outcome |

A required heading may be expressed under a clear synonymous title while the
standards remain in draft. A checker must report ambiguity rather than silently
misclassify it.

## Templates, examples, and anti-examples

Templates are the authoring interface for a document kind. They provide
structure and prompts; they do not permit placeholder compliance such as
“none” or “tests pass” where a material claim requires a basis.

Examples teach the judgment a schema cannot capture. An example is admitted
only after review against the current template and must state which rules it
illustrates. An anti-example is deliberately preserved invalid/weak data: it
states why it fails, which rule it violates, and the expected checker/review
finding.

The initial standards corpus should be organized as:

```text
docs/standards/
  document-kinds/
    research-brief.md
    finding.md
    research-report.md
    design.md
    decision.md
    process-policy.md
    story.md
  examples/
    research/{good-...,bad-...}.md
    design/{good-...,bad-...}.md
    story/{good-...,bad-...}.md
```

Do not treat existing documents as examples merely because they predate the
standard. Curate them deliberately, preserve their historical form, and state
which version of a contract they demonstrate.

## Document analysis

Document analysis is an assistive projection over source documents. It may parse
frontmatter, headings, links, embedded forms, status, and declared relations;
it may not silently decide that semantically similar documents are identical or
that a plausible claim is accepted.

The initial checker produces structured findings:

```clojure
{:document/path "docs/process/research.md"
 :document/kind :process-policy
 :finding/rule-id :document/missing-operational-references
 :finding/severity :error
 :finding/location {:line 1 :column 1}
 :finding/message "Active process policy requires operational references."
 :finding/basis {:parser-version "..." :standard-version "..."}}
```

Rule classes are staged:

| Class | Typical result | Initial enforcement |
|---|---|---|
| Parse/identity | malformed frontmatter, duplicate UUID, invalid declared kind | Error for new governed docs |
| Structural | missing required section/key, duplicate unambiguous section | Error after template adoption |
| Relation | missing target, invalid relation/status combination, broken local link | Error where mechanically decidable |
| Completeness smell | empty limits, vague criterion, absent contrary-evidence treatment | Warning; human review decides |
| Semantic judgment | unsupported conclusion, insufficient method, bad architecture | Review finding; never automatically accepted/rejected |

The checker is itself a derived artifact. It records parser/rule versions and
returns `unknown`/diagnostic findings on unsupported syntax rather than treating
unrecognized content as compliant.

## Migration and historical material

Migration preserves evidence before it standardizes form.

1. **Inventory:** classify existing documents by observed current shape; do not
   infer intended authority from folder name alone.
2. **Mark:** add only minimally invasive kind/status/provenance metadata when
   the responsible authority is known.
3. **Relate:** record source, supersession, and active-policy relations where
   they are supported by evidence.
4. **Normalize on touch:** apply current templates when a document undergoes
   material revision, rather than mass-rewriting archival text.
5. **Record gaps:** a legacy field shape, broken link, or unknown status is an
   analysis finding, not a reason to fabricate information.

The current exploratory process-design documents are the first dogfood case:

```text
docs/process/epiphany-meta-workflow.md
docs/process/epiphany-development-process.md
docs/process/epiphany-agent-ledgers.md
```

They are valid design artifacts and source material for policy. They are not
active process policy. Their immediate migration target is `kind: design`,
`status: exploratory`, with explicit source/provenance and references from any
policy that draws on them.

## Exceptions and evolution

When a document must depart from its kind contract, record an exception under
`PROCESS.md` with the waived requirement, scope, rationale, authority, expiry,
and follow-up. A novel, repeated, or high-consequence departure requires a
proposal to revise the template or introduce a new kind.

A document-standard or checker-rule change states the observed problem, affected
kinds/artifacts, rule/template change, migration impact, test fixtures, owner,
and review/expiry date. It is tried, accepted, revised, superseded, or retired
explicitly. Template drift is process evidence, not author failure by default.

## Operational references

- Process Charter: `PROCESS.md`
- Operational terminology: `docs/process/glossary.md`
- Research practice: `docs/process/research.md`
- Kanban workflow: `docs/process/kanban.md`
- Engineering kernel: `STYLE.md`
