---
title: Phase 3 — External Research Acquisition and Experiment Design
slug: phase-3-research-operations
created: 2026-07-11
source: docs/inbox/clojure Natural Language Processing.md
kind: design
parent: docs/notes/design/knowledge-platform-overview.md
---

# Phase 3 — External Research Acquisition and Experiment Design

## Objective

Given a question, contradiction, gap, or emerging concept in the local corpus, discover relevant external prior art and datasets; assess evidence quality and provenance; identify research opportunities; and generate reviewable, reproducible experiment designs.

## In scope

- Allowlisted acquisition from arXiv, GitHub, Hugging Face, official documentation, and selected open-data/research sources.
- Immutable external-source snapshots with provenance, terms/license metadata, source trust, and temporal versioning.
- Paper/repository/model/dataset extraction into structured research components.
- Literature-to-local-corpus grounding.
- Prior-art and gap analysis.
- Candidate taxonomy, research-question, and experiment-plan generation.
- Reproducibility contracts, simulation/experiment event records, and review gates.
- Evaluation of acquisition, extraction, retrieval, and design quality.

## Out of scope

- Indiscriminate internet crawling.
- Autonomous publication, grant applications, or external outreach.
- Treating arXiv preprints, README claims, model cards, or model-generated summaries as verified truth.
- Automatically downloading every dataset/model found on the internet.
- Executing costly or potentially harmful experiments without an approved resource and safety policy.
- Real-world intervention, surveillance, or decision-making about people.
- Calling an LLM “the scientist” and accepting its novelty claims unverified.

## Epics

| Epic | Name | Goal |
|---|---|---|
| Epic 14 | Governed External Source Registry | Establish source registry, access policies, trust tiers, and acquisition contracts before connecting to external feeds. |
| Epic 15 | External Artifact Ingestion | Acquire and preserve external research artifacts as immutable, provenance-rich objects. |
| Epic 16 | Research Component Extraction | Convert papers, repos, model cards, dataset cards, and docs into structured, evidence-linked research components. |
| Epic 17 | Research Knowledge Graph and Evidence Ranking | Link external research components to the local corpus while preserving differences in authority, time, and evidence type. |
| Epic 18 | Prior-Art, Gap, and Contradiction Analysis | Identify what has likely been tried, where local/external claims diverge, and where a question is genuinely unresolved. |
| Epic 19 | Taxonomy and Research-Question Studio | Use LLMs to propose taxonomies, research questions, and schemas from grounded evidence while keeping humans in control. |
| Epic 20 | Experiment Design and Reproducibility Contracts | Turn an approved question into a machine-checkable, reproducible experiment plan before expensive execution begins. |
| Epic 21 | Bounded Research Agent Workflows | Introduce bounded research agents that acquire evidence, draft analyses, propose designs, and run safe evaluations. |
| Cross-cutting | Research integrity and governance | Make the platform suitable for honest, inspectable research rather than merely fast content synthesis. |

## Epic 14: Governed External Source Registry

**Goal:** Establish a source registry, access policies, trust tiers, and acquisition contracts before connecting the system to external feeds.

**User outcome:** “I can see exactly what the system is allowed to collect, why it collected it, what it is permitted to retain, and how much confidence to assign to it.”

### Source registry entries

- arXiv categories and authors
- GitHub organizations, repositories, topics, releases, issues, PRs
- Hugging Face models, datasets, Spaces, dataset/model cards
- Official documentation and selected professional/academic sources
- Later, explicit open-data providers

### Per-source policies

- access method/API
- rate limit and backoff
- polling/webhook schedule
- allowed artifact types
- retention policy
- license/terms capture
- trust tier
- review requirement
- maximum disk/bandwidth budget

### Trust categories

- `:primary-source`
- `:peer-reviewed`
- `:preprint`
- `:official-project`
- `:maintained-open-source`
- `:dataset-card`
- `:community-discussion`
- `:model-generated-summary`
- `:unverified`

### Domain rule

Source trust is not claim truth. A peer-reviewed paper may still be wrong; an unreviewed GitHub issue may correctly identify a critical bug. Trust tiers tell the system how to present and prioritize evidence, not what conclusions it is allowed to assert.

## Epic 15: External Artifact Ingestion

**Goal:** Acquire and preserve external research artifacts as immutable, provenance-rich objects that can be reprocessed as extraction improves.

**User outcome:** “A paper, repository, dataset, model card, or release can be inspected as it existed when I acquired it, alongside the metadata and source context that made it relevant.”

### Acquisition adapters

- **arXiv:** metadata via API/OAI-PMH; PDF/source links where permitted and requested; category, authors, submission/update history, abstract, identifiers, citations where available.
- **GitHub:** repo metadata, default branch commit, releases, README, license, issues/PRs where policy permits; webhook-driven incremental updates; API queries for bounded discovery.
- **Hugging Face:** model cards, dataset cards, repository metadata, configs, license, revisions, metadata; selective dataset samples/metadata rather than blind full downloads; model/dataset revision pinning.
- **Official documentation:** page snapshots, version, canonical URL, extraction timestamp, content hash.
- **Datasets:** catalog metadata first; schema/sample/statistics/card/license; explicit approval required before materializing a large dataset locally.

## Epic 16: Research Component Extraction

**Goal:** Convert papers, repositories, model cards, dataset cards, and documentation into structured, evidence-linked research components.

**User outcome:** “I can ask: what did this work claim, assume, evaluate, use, and release—and inspect the exact text, code, or metadata supporting each answer.”

### Common component model

```clojure
{:component/id ...
 :component/type :method | :model | :dataset | :metric | :task
                 | :hypothesis | :assumption | :result | :limitation
                 | :implementation | :license | :claim
 :artifact/id ...
 :revision/id ...
 :evidence [{:span ...
             :kind :abstract | :method-section | :dataset-card
                   | :readme | :source-code | :model-card}]
 :value ...
 :status :observed | :extracted | :human-accepted
 :extractor {:name ...
             :version ...
             :configuration-hash ...}}
```

### Extract and link

- Research question/problem
- Claimed contribution
- Hypothesis and assumptions
- Methods/models/algorithms
- Datasets and data splits
- Metrics, baselines, controls, and ablations
- Reported results and uncertainty
- Hardware/resource claims
- Threats to validity and stated limitations
- Reproducibility assets: code, config, seeds, environment, license
- Citations, implementation references, model/dataset lineage
- Repository signals: maintenance activity, release history, open issues, test/config evidence
- Dataset signals: card quality, license, task/domain, schema, split, sample statistics, limitations

### Domain rule

Do not flatten a paper into a single “summary.” Its claims, methods, datasets, results, limitations, and evidence must be separately addressable.

## Epic 17: Research Knowledge Graph and Evidence Ranking

**Goal:** Link external research components to the Phase 1/2 local corpus while preserving differences in authority, time, and evidence type.

**User outcome:** “I can trace a local design idea to prior art, implementations, datasets, and criticism—and distinguish a verified link from a semantic suggestion.”

### Example graph relationships

- `:paper/studies` → task/problem
- `:paper/proposes` → method
- `:paper/evaluates-on` → dataset
- `:paper/measures-with` → metric
- `:paper/reports` → result
- `:paper/acknowledges` → limitation
- `:repository/implements` → method
- `:dataset/supports` → task
- `:model/trained-on` → dataset
- `:artifact/cites` → artifact
- `:local-concept/has-prior-art` → external component
- `:local-code/implements-similar-method` → external method
- `:external-claim/conflicts-with` → local/external claim
- `:research-gap/suggested-by` → evidence set

## Epic 18: Prior-Art, Gap, and Contradiction Analysis

**Goal:** Identify what has likely already been tried, where local/external claims diverge, and where a question is genuinely unresolved enough to justify research.

**User outcome:** “Before I build or write, I can see the adjacent literature, existing implementations, known failure modes, and the exact gap I might be able to investigate.”

### Risk screen

- missing control/baseline
- inappropriate metric
- data leakage risk
- underpowered sample
- inaccessible/restricted data
- incompatible license
- compute cost beyond local budget
- claims that cannot be falsified

### Critical rule

Absence of retrieved evidence is not evidence of novelty. The platform can say: “within these sources, queries, dates, and retrieval settings, I did not retrieve a close match.” It cannot responsibly say: “nobody has done this.”

## Epic 19: Taxonomy and Research-Question Studio

**Goal:** Use LLMs to propose taxonomies, research questions, and classification schemas from grounded evidence, while keeping humans in control of the conceptual vocabulary.

**User outcome:** “I can ask the system to organize a new research area, show competing taxonomies, identify ambiguities, and turn a real corpus gap into a crisp question.”

### Question template

```clojure
{:question/id ...
 :question/text ...
 :motivation [...]
 :claims-to-test [...]
 :scope {:population ...
         :task ...
         :conditions ...}
 :prior-art [...]
 :candidate-methods [...]
 :candidate-datasets [...]
 :candidate-metrics [...]
 :known-risks [...]
 :resource-estimate ...
 :status :proposed | :under-review | :approved | :rejected}
```

### Acceptance criteria

- Every taxonomy node and research-question candidate cites local/external evidence.
- The user can split, merge, rename, reject, or create taxonomy concepts.
- Candidate questions specify falsifiable claims or explicitly state why they are exploratory.
- The system generates at least one alternative framing and one strongest-obvious objection for each research question.
- Research questions are filtered through license, source-trust, compute, and ethics/governance policies.
- The system remembers accepted/rejected taxonomy decisions as review events.

## Epic 20: Experiment Design and Reproducibility Contracts

**Goal:** Turn an approved question into a machine-checkable, reproducible experiment plan before expensive execution begins.

**User outcome:** “I can review a proposed experiment as a concrete contract: hypothesis, data, baselines, metrics, controls, compute budget, risks, and expected evidence.”

### Experiment specification

```clojure
{:experiment/id ...
 :question/id ...
 :hypotheses [...]
 :datasets [{:id ... :revision ... :license ... :split ...}]
 :methods [{:id ... :implementation ... :parameters ...}]
 :baselines [...]
 :controls [...]
 :metrics [...]
 :analysis-plan [...]
 :seeds [...]
 :environment {:container-image ...
               :hardware-class ...
               :dependency-lock ...}
 :budget {:gpu-hours ...
          :cpu-hours ...
          :ram-gb ...
          :disk-gb ...
          :network-gb ...}
 :risks [...]
 :ethics-review ...
 :approval/status :draft | :approved | :rejected
 :provenance ...}
```

### Static design checks

- missing baseline
- metric/objective mismatch
- train/test leakage
- absent seed/reproducibility plan
- incompatible data/model license
- unsatisfied compute/storage/network budget
- unstated confound
- no success/failure criterion
- no analysis plan

## Epic 21: Bounded Research Agent Workflows

**Goal:** Introduce bounded research agents that can acquire evidence, draft analyses, propose designs, and run safe evaluations—but never collapse the human research process into an opaque autonomous loop.

**User outcome:** “Agents continuously keep the research map current and prepare useful proposals; I intervene at decisions that require judgment, values, or a change in research direction.”

### Agent roles

- **Scout agent:** watches allowlisted sources and proposes artifacts for ingestion.
- **Reader agent:** extracts components and creates evidence-linked literature briefs.
- **Prior-art agent:** answers bounded “what existing work resembles this?” tasks.
- **Critic agent:** finds scope mismatch, missing baselines, threats to validity, and strongest counterarguments.
- **Designer agent:** composes candidate experiment specifications.
- **Reproduction agent:** attempts approved low-risk reruns or benchmark evaluations.
- **Librarian agent:** proposes taxonomy changes, deduplication, source-trust adjustments, and link repairs.
- **Supervisor gate:** human approval and policy engine before expensive or high-impact actions.

### Acceptance criteria

- Every agent action has an assigned task, tool permissions, time/token/resource budget, and trace.
- Agents write proposals and evidence packets; they do not directly promote claims, change trusted schemas, or publish results.
- Expensive downloads, model runs, external writes, and experiment execution require explicit approval policies.
- Agent performance is evaluated separately for retrieval, extraction, critique, design, and execution.
- The system records task outcomes, reviewer feedback, and failure categories for future agent improvement.
- Agents can request a human decision with a concise decision card: context, options, evidence, consequence, and reversibility.

## Cross-cutting epic: Research integrity and governance

### Required controls

- Provenance and source-tier visible in every answer, graph edge, and experiment plan.
- Copyright/license and terms metadata preserved for external artifacts.
- Explicit separation between observed data, author claim, model extraction, agent hypothesis, human-accepted interpretation, and experimental result.
- Dataset documentation, access restrictions, and known limitations surfaced before use.
- Reproducibility manifests and immutable result artifacts.
- Evaluation against human-curated research tasks.
- Clear uncertainty language and no unsupported novelty/reliability claims.
- Red-team review for source poisoning, prompt injection in crawled content, malicious repository content, and contaminated dataset/model cards.

## Delivery sequence

1. Epic 14: Governed External Source Registry
2. Epic 15: External Artifact Ingestion
3. Epic 16: Research Component Extraction
4. Epic 17: Research Knowledge Graph and Evidence Ranking
5. Epic 18: Prior-Art, Gap, and Contradiction Analysis
6. Epic 19: Taxonomy and Research-Question Studio
7. Epic 20: Experiment Design and Reproducibility Contracts
8. Epic 21: Bounded Research Agent Workflows
9. Research-integrity controls throughout

Do not deploy autonomous research agents before the source registry, provenance model, retrieval evaluation, and experiment contract exist.

## Phase-three exit test

Phase 3 is complete when you can choose a real question emerging from notes/code—for example, a graph-based retrieval or ACO-inspired semantic traversal question—and produce an inspectable research dossier containing:

- the local notes, code, and prior experiments that motivate the question;
- an allowlisted, reproducible search over arXiv, GitHub, and Hugging Face;
- retrieved prior art, implementations, models, and datasets with dates, licenses, trust tiers, and evidence;
- structured comparison of methods, assumptions, datasets, metrics, baselines, reported results, and limitations;
- a calibrated statement of what appears known, disputed, missing, or merely unverified;
- one or more candidate taxonomies and falsifiable research questions;
- a human-reviewed experiment specification with pinned code/data/model versions, controls, metrics, resource budget, and safety/governance review;
- a bounded agent workflow that can update the dossier or draft follow-up work without silently promoting claims or spending significant compute.
