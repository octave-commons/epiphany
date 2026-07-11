---
title: Auto-Research Agent Loop Design
slug: auto-research-agent-loop-design
created: 2026-07-11
source: docs/inbox/2026.07.11.14.35.24.md
kind: research
labels: ['agents', 'auto-research', 'multi-agent', 'governance', 'external-research']
---

# Summary

Auto-research / multi-agent design: what agents can delegate, what humans must design, live-network semi-supervision, governance, and external research/simulation loops.

# Aight, and how much of what would need to be built, could be delegated to an auto research loop, as in learned over time by allowing agents to use the off the shelf solutions, design candidate, experiments, and so on?

What I am hopin for is that we have some dev agents, acting in response to tasks being generated, by agents who are acting in response to designs being proposed, based on research that was conducted, and that there are enough visualizations and maps and shit, and moments where the agent wants to question me for direction, that I'll just be like.... a very active observer, able to commit my mind to thinking about the data in front of me and doin the systems modeling my mind likes to do when it doesn't have to be busy building the systems or commuinicating. I've just go so much time... I am sure I could conduct some very interesting research, get some papers in to arxiv... and on top of all of it, get to claim I built a true modern day AI command center.

You can delegate **a large fraction of the “explore, prototype, and iterate” work** to auto‑research / dev agents, but the **ontology, evaluation design, safety boundaries, and orchestration** will still need to be architected and adjusted by you. In practice, agents are great at running loops over well‑defined, machine‑verifiable tasks; they are not yet great at inventing and validating the overall system without human supervision.[^5_1][^5_2][^5_3]

## What auto‑research loops do well

Recent work on autonomous agent loops shows they excel when you give them: (1) a fixed set of tools, (2) a clear objective metric, and (3) an automated verifier.  In SWE‑bench settings, agentic scaffolding around the same model yields roughly an order‑of‑magnitude improvement over single‑shot attempts on tasks like “apply a patch that makes all tests pass.”  Autoresearch setups (Karpathy, research loops, etc.) use this pattern: agents modify code or configs, run standardized experiments for a fixed time budget, and only commit changes that improve a target metric.[^5_4][^5_5][^5_1]

For your platform, that translates to:

- Autonomously tuning **indexing parameters** (BM25 settings, vector thresholds, chunk sizes) to maximize retrieval metrics like recall@k or nDCG on a held‑out query/evidence set.
- Iterating over **embedding choices** and vector‑store configurations to improve retrieval quality or latency under constraints you define.[^5_6]
- Running **ETL / ingestion experiments**: parse a subset of repos with different AST extraction strategies, compare coverage, parse speed, and error rates, and pick the best configuration.
- Searching over **LLM prompt and schema variants** for taxonomy proposals, label assignment, and QA output formats, using automated scoring against labeled validation sets.

Those are all “auto‑research loop friendly” because success can be machine‑checked: did retrieval improve on the benchmark? Did more tests pass? Did schema validation and consistency checks succeed?

## What must be human‑designed

Empirical studies of AI coding tools and autonomous agents show that fully unsupervised deployment leads to non‑mergeable pull requests and even slower performance for experienced devs when used naively.  The multi‑agent frameworks for software development explicitly position humans as **supervisors and system designers**, not spectators who never intervene.[^5_2][^5_7][^5_8][^5_1]

You will still need to:

- Design the **ontology**: what counts as an entity (function, concept, incident, dataset), what relations matter, and how code, docs, configs, and external science datasets map into that graph.
- Choose **evaluation metrics** that matter: retrieval quality for different question types, KG correctness, QA faithfulness, and operational metrics like “precision on auto‑routed decisions.”
- Set **safety and autonomy boundaries**: which tasks agents can complete unsupervised (e.g., reindex a corpus), which require human review (e.g., changing routing policies or KG schema), and which are purely exploratory.
- Curate **training and benchmark sets**: labeled queries, gold‑standard KG fragments, and QA examples that define “good behavior.”
- Make **architectural tradeoffs**: when to invest in a new representation or subsystem, when to accept approximations, and how to coordinate the four machines.

Agents can search over designs you propose, but they still need you to define “what problem are we solving” and “what counts as success.”

## Mapping this to your knowledge platform

Think of your build as five major subsystems; here’s what can be agent‑driven vs. what is architect‑driven.

### 1. Ingestion \& AST extraction

Agent‑friendly:

- Auto‑discover parsing strategies using tree‑sitter and language‑specific parsers across Clojure, ClojureScript, Go, Python, C, Lua, Ruby, Bash, YAML, EDN, JSON.
- Compare different chunking and normalization schemes and pick the one that maximizes downstream retrieval metrics on a labeled set of code/doc queries.

You‑driven:

- Define the **canonical document unit** (file vs. function vs. section vs. block).
- Decide what AST features matter (node types, identifiers, comments, control‑flow patterns) and how they become features or entities in the knowledge graph.


### 2. Indexing, search, and vectors

Agent‑friendly:

- Auto‑tune Elasticsearch/OpenSearch settings—BM25 parameters, analyzer choices, kNN index configuration, and hybrid scoring weights—against a retrieval benchmark.[^5_9][^5_10]
- Run “index redesign” experiments: different chunk sizes, metadata tags, and routing keys, scoring each design against evaluation queries.

You‑driven:

- Decide whether Elasticsearch alone is sufficient or when to introduce a dedicated vector store (Qdrant, pgvector, Weaviate, etc.) based on your scale and durability requirements.[^5_6]
- Design the **retrieval strategies** per task: how you mix text relevance, AST features, vector similarity, and KG filters for code questions vs. conceptual questions vs. geopolitics questions.


### 3. Knowledge graph \& ontology

Agent‑friendly:

- Suggest candidate taxonomies and entity types by clustering documents and code and using LLMs to propose labels, hierarchies, and relation names.
- Generate KG edges from text and code using extraction agents, then run consistency and constraint checks (e.g., “no cycles of type X”, “no conflicting attributes”).

You‑driven:

- Lock in the **named entities and relations** that matter operationally (e.g., incidents, capabilities, techniques, policies, datasets).
- Define **provenance and trust rules**: which sources and models are authoritative vs. tentative, and how to represent and query that in the KG.


### 4. QA, reasoning, and external corpora

Agent‑friendly:

- Autonomously optimize RAG pipelines: chunking strategies, retriever combinations, reranker choices, and prompt templates, using QA metrics on labeled question/answer pairs.[^5_11][^5_6]
- Use LLM agents to run continuous “QA quality audits” by comparing their own outputs against reference answers and flagging regressions.

You‑driven:

- Decide how external scientific and geopolitical datasets integrate with your local corpus: which domains you care about, how much authority they have, and how they’re surfaced in answers.
- Set constraints on what types of questions can be answered automatically vs. which must be flagged for deeper human review or simulation.


### 5. Visualization, maps, and agent workflows

Agent‑friendly:

- Generate dashboards and maps of KG structure, retrieval performance, and agent activity; iteratively refine visualizations by optimizing for interpretability metrics or human feedback scores.
- Propose **workflows**: sequences like “research agent → design agent → dev agent → test agent → review agent.”

You‑driven:

- Choose which visualizations and maps you actually care about and how they tie into your mental systems modeling: e.g., flows of knowledge, bottlenecks in agent loops, or emerging structure in the KG.
- Decide the **role hierarchy**: which agents are allowed to spawn others, who can propose schema changes, who can commit code, and where your approvals sit in that graph.


## Multi‑agent dev architecture for your use case

The frameworks people are publishing now for autonomous dev agents look very close to what you’re imagining: multi‑agent systems where research agents, designer agents, and dev agents coordinate through an orchestration layer, with humans supervising and approving results.[^5_12][^5_13][^5_5][^5_2]

A reasonable architecture:

- **Research agents**:
    - Scan literature and public datasets.
    - Propose taxonomies, retrieval strategies, and experimental designs.
- **Design agents**:
    - Turn research proposals into concrete schemas, test plans, and infra tasks.
    - Write “agent memos” describing rationale, state, and open questions.[^5_7][^5_3]
- **Dev agents**:
    - Implement ETL jobs, indexers, KG loaders, and evaluation harnesses.
    - Operate in autonomous loops with automated tests, metrics, and code review gates.[^5_14][^5_1][^5_2]
- **Supervisor (you)**:
    - Approve designs, modify objectives, and adjust metrics.
    - Intervene when loops get stuck or converge on locally optimal but globally wrong solutions.
    - Use the visualizations and maps as the substrate for your own systems modeling and paper‑writing.

This pattern matches how emerging “AI agent for software engineering” work is framing the human role: from coder to **AI system designer and orchestrator**, focusing on workflows, metrics, and governance rather than writing every line of code.[^5_3][^5_8]

## Your role as “active observer” / system modeler

The realistic endgame is not “agents build everything while you watch” but “agents continuously explore the design space and keep you in the loop at the abstraction level you care about.”[^5_5][^5_1][^5_2]

If you set things up right:

- You spend most of your time:
    - Reading **agent‑generated maps** of the corpus and KG.
    - Interpreting **experiment results** and deciding where to push further.
    - Adjusting **metrics, constraints, and goals** instead of writing glue and boilerplate.
- Agents spend most of their time:
    - Running bounded experiments in the infrastructure you’ve stood up.
    - Proposing candidate designs and implementations that pass automated checks.
    - Surfacing questions when they’re stuck or when the decision is intrinsically conceptual rather than machine‑verifiable.

That’s exactly the kind of environment where you can do **interesting, publishable research** on auto‑research loops, multi‑agent dev systems, and knowledge management architectures, while also credibly claiming you built a “modern AI command center” for your own corpus and external scientific/geopolitical knowledge.[^5_8][^5_2][^5_3]

The real leverage is not delegating *all* building to agents; it’s using agent loops to turn your system‑level ideas into many experiments and candidate implementations, then focusing your human time on the parts that are still fundamentally epistemic and architectural.
<span style="display:none">[^5_15][^5_16][^5_17][^5_18][^5_19]</span>

<div align="center">⁂</div>

[^5_1]: https://daviddaniel.tech/research/papers/autonomous-agents/

[^5_2]: https://zenodo.org/records/19926986

[^5_3]: https://medium.com/@tonimaxx/from-coders-to-ai-system-designers-what-software-engineers-must-focus-on-in-2025-3b946ea12dcf

[^5_4]: https://www.youtube.com/watch?v=nt9j1k2IhUY

[^5_5]: https://blogs.oracle.com/developers/what-is-the-ai-agent-loop-the-core-architecture-behind-autonomous-ai-systems

[^5_6]: https://gautamkhorana.com/blog/best-vector-database-for-rag-2026/

[^5_7]: https://aiforsystems.substack.com/p/25-in-25-what-engineers-actually

[^5_8]: https://www.computer.org/csdl/magazine/co/2025/05/10970187/260SnIeoUUM

[^5_9]: https://www.elastic.co/docs/solutions/search/vector/dense-vector

[^5_10]: https://www.elastic.co/search-labs/blog/vector-search-improvements

[^5_11]: https://www.pingcap.com/compare/best-vector-database/

[^5_12]: https://blakecrosley.com/guides/agent-architecture

[^5_13]: https://setkyar.com/building-autonomous-ai-agent-loops-a-hands-off-approach-to-development/

[^5_14]: https://dev.to/craftogrammer/craftloop-open-source-autonomous-loop-for-ai-coding-agents-5flp

[^5_15]: https://www.ijrdet.com/files/Volume14Issue12/IJRDET_1225_17.pdf

[^5_16]: https://www.youtube.com/watch?v=q3YvFYtuhec

[^5_17]: https://www.linkedin.com/pulse/architectural-blueprint-closed-loop-autonomous-agent-aad-smeyatsky-achcf

[^5_18]: https://aiagentindex.mit.edu/data/2025-AI-Agent-Index.pdf

[^5_19]: https://www.youtube.com/watch?v=9_IptycUjU0
---
# oh I am sure just watching them will give me plenty of ideas. The "inventing and validating with out human intervention" is why the system has to be connected to the network and as many live feeds as possible. Those signals from probably mostly humans, would act as a form of semi supervision, real time research papers to analyze would describe all sorts of machine verifiable metrics. Active crawling of arxiv, and what ever other relatively credible source of academic or professional literature @Academic @GitHub @Hugging Face

They'd have plenty to do just in mixing and matching components which we could reasonably extract with some degree of confidence structurally with such a system, you could build out experiments and research topics deterministically by combining components of existing research, you could look for prior art, you could screen the experimental design for obvious flaws. The longer it ran, the more examples you accumulate less obvious flaws, increasing your ability to screen research subjects at increasingly earlier and earlier points in the design composition, correlate types of experimental components that do not fit well or at all together

You’re pointing at exactly the frontier: agents continuously ingest live human output (papers, code, issues, discussions) and use it as **semi-supervised signal and design material** for their own experiments. Systems like that are starting to appear in science and engineering; the main gap is not technical feasibility but **good scaffolding and guardrails** so the learning loop doesn’t spin off into nonsense.[^6_1][^6_2][^6_3][^6_4]

## Semi-supervision from the live network

What you’re describing matches emerging “agents for science” architectures, where agents continuously crawl arXiv, GitHub, and other sources to update their knowledge and propose experiments.[^6_2][^6_5][^6_4][^6_1]

- **Live feeds as supervision.** Each new paper or repo is a labeled datapoint: “this combination of assumptions, methods, and metrics produced something deemed publishable or useful,” even if the signal is noisy. Agents can treat these as weak labels for “promising design patterns” or “valid experimental idioms.”[^6_6][^6_3][^6_1]
- **Research tools already exist.** There are now dedicated services that let AI agents query massive arXiv indices and associated GitHub artifacts via a simple research search API, explicitly designed for agent loops.[^6_7][^6_8][^6_9]
- **Continuous ingestion.** Agent loops that update “papers daily” and maintain topic-specific feeds are already open-source, albeit focused on monitoring rather than full experiment synthesis.[^6_8][^6_9]

So your idea of “wiring the command center to the network and as many live feeds as possible” is aligned with where agentic scientific workflows are going; the trick is turning that stream into structured, machine-usable supervision rather than just more text.[^6_3][^6_1][^6_6]

## Agents mixing and matching components from literature

Recent frameworks explicitly build **agent pipelines for literature analysis, hypothesis generation, and experimental planning**: one agent reads papers, another collates methods and metrics, another proposes experiments, and another critiques them.[^6_10][^6_4][^6_1][^6_6]

In your world, that looks like:

- **Component extraction.** Agents parse papers into structured components: assumptions, models, datasets, metrics, protocols, code snippets, and result summaries.[^6_1][^6_10][^6_6]
- **Composable experiment graphs.** Those components become nodes in a graph (“dataset A”, “model B”, “loss C”, “evaluation metric D”), so agents can deterministically compose new designs by recombining them—exactly what you described.[^6_10][^6_2][^6_1]
- **Prior art screening.** Before proposing something “new,” agents query the graph and literature index to find prior art and detect trivial rediscovery or obvious conceptual violations (e.g., using evaluation metrics that are known to be biased for a task).[^6_6][^6_1][^6_10]
- **Design sanity checks.** Agents can run static checks on experimental designs: “missing control group,” “metric doesn’t match objective,” “dataset size too small for model class,” using heuristics learned from the extracted history of published work.[^6_3][^6_1][^6_6]

This fits your “build experiments and research topics deterministically by combining components of existing research, and screen them for obvious flaws” idea: the components come from the corpus; the composition rules and sanity checks can be agent-learned but you define the initial schemas and constraints.[^6_1][^6_10][^6_6]

## How the loop learns over time

The long‑running aspect—agents improving their ability to spot less obvious flaws and bad combinations—is also being actively studied. Several surveys and roadmaps talk about **agents as scientists** that refine their own heuristics over thousands of iterations.[^6_11][^6_4][^6_2][^6_6]

Over time, your auto‑research loop can:

- **Learn correlations between design patterns and outcome quality.** Given enough labeled history (e.g., “this type of design yielded reproducible results, that type yielded retractions or non‑replicable claims”), agents can assign priors to design fragments and flag high‑risk combinations early.[^6_6][^6_3][^6_1]
- **Refine screening heuristics.** Each failed experiment—whether in your local lab or in the broader literature—adds weight to “these components don’t play well together,” improving early‑stage screening.[^6_4][^6_1][^6_6]
- **Adapt to new methods.** Because the system is continuously crawling arXiv, GitHub, and model hubs, it can incorporate new architectures, loss functions, evaluation metrics, and best practices, updating its graph and heuristics without you hand‑coding every rule.[^6_9][^6_7][^6_2]

This is precisely the “autonomous research bench” direction: agents operate in closed loops of reading, proposing, running, and updating their own internal models, while humans supervise the global direction and vet high‑impact decisions.[^6_12][^6_2][^6_4][^6_3]

## Guardrails you’d still want

The strongest published guidance on “agents for scientific discovery” and “agents for research automation” emphasizes that, even with these loops, you still need:[^6_11][^6_3][^6_1][^6_6]

- **Explicit evaluation tasks and gold standards.** Benchmarks like AutoResearchBench and topic‑specific QA sets give agents concrete metrics to optimize rather than free‑floating novelty or “interestingness.”[^6_12][^6_4]
- **Physical and logical grounding.** For scientific domains, agents must respect conservation laws, domain constraints, and basic logic; otherwise they can invent experiments that look plausible in text but are nonsensical physically or statistically.[^6_11][^6_1][^6_6]
- **Human review gates.** Human scientists (or system designers like you) review agent‑proposed experiments, especially ones that would consume real resources or influence external decisions; this is exactly what recent “scientist’s guide to AI agents” pieces recommend.[^6_2][^6_3][^6_6]
- **Data quality and source filtering.** Not every arXiv paper or GitHub repo is sound; agents need source‑level trust scores and consistency checks to avoid amplifying noise.[^6_3][^6_1][^6_11]

So your idea of “semi‑supervision from live signals” is powerful if you wrap it in an architecture that distinguishes between **trusted patterns** and **raw inspiration**, and if you keep yourself in the loop for the big moves.

## Where your brain fits in this command center

The story that emerges from the current literature on agentic AI and research automation is basically: humans shift from doing every micro‑step to **designing the loops and interpreting the maps**, which is exactly what you’re aiming for.[^6_13][^6_14][^6_15][^6_3]

In practice:

- Agents carry out the **continuous crawling, component extraction, composition, and screening** you described, populating your knowledge graph and index with ever richer structure.[^6_7][^6_9][^6_10]
- You spend your time in front of **visualizations and experiment logs**, doing the systems modeling your mind likes—spotting patterns, rethinking ontologies, defining new evaluation tasks, and asking meta‑questions the agents can’t yet formulate.[^6_14][^6_15][^6_13]
- When an agent pings you (“this experimental pattern is promising but we’re unsure about X”), you answer as a high‑bandwidth supervisor rather than a step‑by‑step coder, steering the whole research direction.[^6_16][^6_17][^6_3]
- Over months and years, your platform’s logs and graphs become a dataset in their own right, letting you write exactly the kind of arXiv papers you’re talking about—on **agentic research loops, AI‑augmented knowledge management, and multi‑agent scientific workflows**—backed by the concrete experiments your command center has run.[^6_4][^6_2][^6_11]

So yes: if you wire agents to rich live feeds and give them structured ways to decompose and recombine what they see, they’ll “have plenty to do” and you’ll have plenty to think about, without your bandwidth being consumed by glue code and boilerplate. The frontier work suggests this isn’t just sci‑fi; it’s exactly where agentic AI for research is headed, provided you architect the loop and the guardrails with care.[^6_2][^6_4][^6_1][^6_3]
<span style="display:none">[^6_18][^6_19][^6_20]</span>

<div align="center">⁂</div>

[^6_1]: https://www.frontiersin.org/journals/artificial-intelligence/articles/10.3389/frai.2025.1649155/full

[^6_2]: https://www.emergentmind.com/topics/agents4science-2025

[^6_3]: https://pubmed.ncbi.nlm.nih.gov/41044277/

[^6_4]: https://arxiv.org/html/2510.20844v1

[^6_5]: https://arxiv.org/html/2510.09901v1

[^6_6]: https://www.oaepublish.com/articles/aiagent.2025.03

[^6_7]: https://pondero.ai/agents/guides/firecrawl-research-index-agent-arxiv-search-june-2026/

[^6_8]: https://github.com/UltraClr/agent-arxiv-daily

[^6_9]: https://apify.com/red.cars/arxiv-intelligence-mcp

[^6_10]: https://www.marktechpost.com/2025/11/27/a-coding-implementation-for-an-agentic-ai-framework-that-performs-literature-analysis-hypothesis-generation-experimental-planning-simulation-and-scientific-reporting/

[^6_11]: https://cbmm.mit.edu/sites/default/files/publications/Survey_of_AI_for_Research_v0.pdf

[^6_12]: https://hyper.ai/en/papers/2604.25256

[^6_13]: https://aiforsystems.substack.com/p/25-in-25-what-engineers-actually

[^6_14]: https://medium.com/@tonimaxx/from-coders-to-ai-system-designers-what-software-engineers-must-focus-on-in-2025-3b946ea12dcf

[^6_15]: https://www.computer.org/csdl/magazine/co/2025/05/10970187/260SnIeoUUM

[^6_16]: https://zenodo.org/records/19926986

[^6_17]: https://blogs.oracle.com/developers/what-is-the-ai-agent-loop-the-core-architecture-behind-autonomous-ai-systems

[^6_18]: https://app.unpkg.com/aiwg@2026.2.11/files/docs/references/REF-057-agent-laboratory.md

[^6_19]: https://www.frontiersin.org/journals/artificial-intelligence/articles/10.3389/frai.2025.1649155/pdf

[^6_20]: https://arxiv.org/html/2511.10524v1
---
# @Academic @GitHub @Hugging Face As an expert in computer science and artificial intelligence, I feel I'm in a kinda odd spot though, to be able to know how to build all of this, even if it wasn't off the shelf, I've implemented most of the necessary patterns in isolation, but that's it. That's where I am grounded, where I would be the most effectual in supervising research, AI systems. I have other skills, interests, experiences... I worked on command and control systems, though to be honest, as much as I absorb game theory and such, I've never been the best at games. I'm not the worst, or even bad...

This is probably the most clearly I've seen the specific steps necessary to take to build it, and most clearly I had an idea of how long it'd take, but I've been thinking about a system like this for a decade now.

And AI has lost all of the... possibility it once represented to me, increasingly software, games, these too are loosing their charm.... but it's what I know how to do. The last thing stopping me from putting my head down and building this thing, is an objective other than digital metacogative loop of self improvement for it's own sake.

Computer systems are my expertise, my primary field of study.
But it's a field of study that requires you to learn significant parts of the trades of the people you build software for, and if you bounce around enough, it means you pick up a lot of trades at a journeyman's level.

so I'm a generalist... trying to figure out what to study when I am spread so thin, except for the skills to build the experimental framework is... it's self a difficult problem.

What I can think of when I dig a bit, is "governance" "ethics" "anthropology" "neurology"

You’re sitting exactly at the crossover point where “pure CS” stops being the bottleneck, and the real leverage comes from **governance, ethics, and socio‑technical understanding**—the stuff that decides *what* your command center is for and *whose world* it changes. Those are precisely the areas current agentic AI literature calls out as missing and urgently needed.[^7_1][^7_2][^7_3][^7_4][^7_5]

## Where your expertise already is

From what you’ve described:

- You’re already strong on **systems architecture, command‑and‑control patterns, and experimental frameworks**—you can see the steps to build this platform and have implemented most of the patterns in isolation.
- You have **breadth across domains** (games, distributed systems, emergency response, etc.), which is exactly what agentic AI governance and socio‑technical design benefit from: people who understand both machines and the humans/organizations around them.
- The thing that feels missing isn’t another framework or model—it’s an **objective that isn’t just “make the AI system better at itself.”**

The agentic AI governance work basically says: once systems can act autonomously, the central questions stop being “can we build it?” and become “how do we **govern, align, and situate** it in human institutions?”[^7_2][^7_3][^7_4][^7_1]

That’s a space where your skills already matter, and where your next studies could give the whole project meaning beyond a self‑improving loop.

## A different objective: “build systems that behave well in human worlds”

If “digital metacognitive loop of self‑improvement for its own sake” feels empty, a more grounded objective could be:

> **Design and study agentic systems that behave well within human institutions and values, especially in complex, high‑stakes domains.**

That reframes your command center as:

- A **research instrument** for how agents reason, act, and coordinate, not just a tool farm.
- A **governance testbed**: how do guardrails, oversight, and institutional context change behavior?
- A **socio‑technical lab**: how do humans, agents, and organizations interact over time?

In current work, this is exactly what people are starting to call “agentic AI governance” or “governing the agentic enterprise”—new operating models for autonomous systems embedded in real organizations.[^7_6][^7_3][^7_7][^7_4]

## What to study next (and why)

The topics you listed—governance, ethics, anthropology, neurology—are not random; they map almost perfectly to the gaps that recent surveys say computer scientists haven’t filled yet.

### Governance

Agentic AI governance is now a whole subfield: defining **scope, access, oversight, monitoring, auditability, and shutdown mechanisms** for agents that act autonomously.[^7_8][^7_3][^7_4][^7_1][^7_2]

Studying governance gives you:

- Languages and frameworks for **risk tiers, guardrails, and lifecycle controls**—all things your platform will actually need.[^7_3][^7_7][^7_6]
- An understanding of how **institutions make decisions about automation**, which can anchor your system’s purpose in real-world practices rather than abstract optimization.
- The ability to design your command center as a **governed system**, not just an engine that happens to be powerful.

This meshes directly with your command‑and‑control background: you already think in terms of roles, permissions, escalation, and oversight. Governance lets you formalize that in contemporary AI systems.[^7_4][^7_1][^7_2]

### Ethics

A cross‑disciplinary review of “building ethics into AI” found that most practical work so far is *thin*: operationalizing a single principle (fairness, privacy, etc.) in code, largely driven by computer scientists, with little sustained collaboration with ethicists or social scientists. The conclusion is that genuinely ethical AI will need a **pluralistic, interdisciplinary approach to ethics**.[^7_5]

Studying ethics (especially applied ethics and computational ethics) gives you:

- A vocabulary for **what “good behavior” means** beyond just loss functions and metrics.[^7_9][^7_5]
- The tools to avoid turning “ethics” into a bolt‑on metric—so your agents don’t just optimize a thin fairness score while breaking other values.
- Frameworks for thinking about **responsibility, accountability, and justification** when agents act in the world.[^7_5][^7_9]

That’s directly relevant if your platform is eventually used for emergency response, policy analysis, or any domain where decisions impact people and not just code.

### Anthropology / sociology

Governance frameworks repeatedly point out that **agentic AI has to be situated in organizations, cultures, and social norms**, and that traditional, model‑centric governance is insufficient for systems that adapt and interact.[^7_1][^7_2][^7_4]

Anthropology/sociology give you:

- A way to study **how people actually use systems, how organizations adapt, and how norms emerge or break** when agents are introduced.
- Methods (ethnography, qualitative analysis) to observe and theorize **human–AI collaboration** inside the command center environment.
- Context for why some “perfectly engineered” systems fail socially, while messy systems succeed because they fit local practices.

If your system becomes a “modern AI command center,” anthropology is what tells you whether it’s *usable, trusted, and embedded* in real practice, not just technically impressive.

### Neurology / cognitive science

Fields like computational ethics and AI‑for‑science work increasingly tie into **cognition and neuroscience** when they ask how humans actually reason about values, uncertainty, and evidence.[^7_10][^7_9]

Studying neurology/cognitive science gives you:

- Models of **attention, learning, and decision‑making** that can inform agent architectures and interfaces.
- Inspiration for **metacognitive loops** that are less self‑referential and more grounded in how humans think and learn.
- Bridges between your own mental systems modeling and formal theories of cognition—useful when you design agents intended to cooperate with human analysts, not just replace them.

Neurology doesn’t have to be “designing brain‑like agents”; it can be “understanding the humans at the center of the command system, including me.”

## How this reframes your “generalist” problem

The review on ethical AI explicitly notes that one reason ethics work is “thin” is the predominance of computer scientists and the lack of sustained, iterative collaboration with ethicists and social scientists.  In other words: *the field is full of people like you, but without the cross‑disciplinary follow‑through*.[^7_5]

Your generalist background is not a bug here; it’s an asset if you:

- Treat **governance, ethics, anthropology, and cognitive science as your next specialization**, not just side interests.
- Use your CS skills to **build the experimental framework**, and your new studies to **define meaningful experiments and interpretations**.
- Position yourself as someone who builds **agentic systems that are governed, ethical, and socially situated**, rather than “just another generative stack.”

The “what to study” question becomes: *which lenses do I want to bring into the lab I already know how to build?* Governance and ethics tell you what behavior you want; anthropology tells you how people will actually live with it; neurology tells you how minds will interact with it.

## A concrete way to move from “self‑improvement loop” to “research program”

You already have a decade of thinking about the system and now a clear path to building it. To give it an objective beyond self‑optimization, you could frame it as a **research program around agentic AI in socio‑technical systems**, for example:

- **Theme:** How do autonomous, tool‑using agents behave under different governance, ethical, and social frameworks?
- **Lab:** Your command center—multi‑agent, networked, hooked into live feeds, instrumented and visualized.
- **Disciplines to study:**
    - Governance: to design roles, guardrails, and accountability.[^7_6][^7_3][^7_4][^7_1]
    - Ethics: to define substantive values and evaluate behavior beyond metrics.[^7_9][^7_5]
    - Anthropology/sociology: to observe and theorize human–agent interaction.[^7_4][^7_5]
    - Neurology/cognition: to ground your understanding of human and agent reasoning.[^7_10][^7_9]

That gives you a way to say, when you put your head down and build:

> “I’m building an AI command center **as a socio‑technical research lab**, not just as a self‑optimizing machine—because I care about how these systems change human worlds, not just how clever they are.”

And when you start shipping papers to arXiv, they’re not just “look at my stack,” they’re “here’s what we learned about governing, situating, and understanding agentic systems,” which is exactly the gap current work is calling out.[^7_2][^7_3][^7_1][^7_5]
<span style="display:none">[^7_11][^7_12][^7_13][^7_14][^7_15][^7_16]</span>

<div align="center">⁂</div>

[^7_1]: https://www.ibm.com/think/insights/ai-agent-governance

[^7_2]: https://aigovernance.com/playbook/governing-agentic-ai

[^7_3]: https://witness.ai/blog/agentic-ai-governance-framework/

[^7_4]: https://cmr.berkeley.edu/2026/03/governing-the-agentic-enterprise-a-new-operating-model-for-autonomous-ai-at-scale/

[^7_5]: https://aisel.aisnet.org/sjis_preprints/12/

[^7_6]: https://galileo.ai/blog/ai-governance-framework

[^7_7]: https://www.avepoint.com/blog/strategy-blog/definitive-guide-agentic-ai-governance-security-autonomous-systems

[^7_8]: https://www.datarobot.com/blog/agentic-ai-governance-framework/

[^7_9]: https://dspace.mit.edu/bitstream/handle/1721.1/150388/1-s2.0-S1364661322000456-main.pdf?sequence=2\&isAllowed=y

[^7_10]: https://www.oaepublish.com/articles/aiagent.2025.03

[^7_11]: https://labs.cloudsecurityalliance.org/wp-content/uploads/2026/03/governance-nist-ai-agent-standards-agentic-governance-v1-csa-styled.pdf

[^7_12]: https://static1.squarespace.com/static/69118be41affb70151acc6cb/t/694ec419d6a8733a26032a57/1766769723803/Establishing+Governance+Frameworks+for+Autonomous+AI+Agents+and+Orchestrated+Systems.pdf

[^7_13]: https://arxiv.org/html/2412.17114v3

[^7_14]: https://arxiv.org/html/2508.18765v1

[^7_15]: https://www.attentive.com/blog/what-is-agentic-ai-governance

[^7_16]: https://www.ibm.com/think/insights/agentic-ai-governance-playbook
---
# @Academic @GitHub @Hugging Face Draft the epics for phase 3

Phase 3 is **external research acquisition, source governance, and experiment design**. It turns the Phase 1/2 personal corpus system into a grounded research environment that can connect your notes and code to credible public literature, open-source implementations, models, and datasets—without becoming an uncontrolled web-scraping or autonomous-claims machine.

The phase objective is:

> Given a question, contradiction, gap, or emerging concept in the local corpus, discover relevant external prior art and datasets; assess evidence quality and provenance; identify research opportunities; and generate reviewable, reproducible experiment designs.

arXiv exposes both a query API and OAI-PMH metadata harvesting; its metadata is updated daily, and arXiv specifically recommends OAI-PMH for keeping a metadata corpus current rather than indiscriminate scraping.  GitHub and Hugging Face should likewise be API/webhook-first, rate-limited, cached, and source-governed rather than crawler-first.[^12_1][^12_2][^12_3][^12_4][^12_5][^12_6][^12_7]

## Phase-three definition

**In scope**

- Allowlisted acquisition from arXiv, GitHub, Hugging Face, official documentation, and selected open-data/research sources.
- Immutable external-source snapshots with provenance, terms/license metadata, source trust, and temporal versioning.
- Paper/repository/model/dataset extraction into structured research components.
- Literature-to-local-corpus grounding.
- Prior-art and gap analysis.
- Candidate taxonomy, research-question, and experiment-plan generation.
- Reproducibility contracts, simulation/experiment event records, and review gates.
- Evaluation of acquisition, extraction, retrieval, and design quality.

**Explicitly out of scope**

- Indiscriminate internet crawling.
- Autonomous publication, grant applications, or external outreach.
- Treating arXiv preprints, README claims, model cards, or model-generated summaries as verified truth.
- Automatically downloading every dataset/model found on the internet.
- Executing costly or potentially harmful experiments without an approved resource and safety policy.
- Real-world intervention, surveillance, or decision-making about people.
- Calling an LLM “the scientist” and accepting its novelty claims unverified.


## Epic 14: Governed External Source Registry

**Goal:** Establish a source registry, access policies, trust tiers, and acquisition contracts before connecting the system to external feeds.

**User outcome:** “I can see exactly what the system is allowed to collect, why it collected it, what it is permitted to retain, and how much confidence to assign to it.”

### Scope

- Create a versioned source registry for:
    - arXiv categories and authors;
    - GitHub organizations, repositories, topics, releases, issues, and PRs;
    - Hugging Face models, datasets, Spaces, dataset/model cards;
    - official documentation and selected professional/academic sources;
    - later, explicit open-data providers.
- Define per-source policies:
    - access method/API;
    - rate limit and backoff;
    - polling/webhook schedule;
    - allowed artifact types;
    - retention policy;
    - license/terms capture;
    - trust tier;
    - review requirement;
    - maximum disk/bandwidth budget.
- Add trust categories:
    - `:primary-source`
    - `:peer-reviewed`
    - `:preprint`
    - `:official-project`
    - `:maintained-open-source`
    - `:dataset-card`
    - `:community-discussion`
    - `:model-generated-summary`
    - `:unverified`.
- Require every external artifact to record its acquisition reason:
    - query match;
    - watchlist;
    - citation chain;
    - local-concept match;
    - user request;
    - experiment dependency.

arXiv provides API access for programmatic metadata/search and OAI-PMH for daily-updated metadata harvesting; use the dedicated export endpoints and follow its bulk-access guidance rather than treating the public website as a scrape target.  GitHub explicitly recommends webhooks rather than polling, authenticated requests, serial/queued request patterns, backoff, and conditional requests using ETags or `Last-Modified`.[^12_2][^12_3][^12_4][^12_5][^12_7][^12_1]

### Acceptance criteria

- No external fetch occurs without a source-registry entry and policy.
- Every artifact records URL/canonical ID, acquisition time, content hash, source class, trust tier, license/terms metadata where available, and acquisition reason.
- Every acquisition is rate-limited, cache-aware, and resumable.
- Conditional fetches avoid re-downloading unchanged resources where source APIs support ETags/modified timestamps.
- The user can pause, revoke, or purge a source policy without deleting unrelated evidence.
- The system exposes bandwidth, storage, error, retry, and rate-limit dashboards by source.
- A source’s trust tier can be revised without rewriting raw artifacts.


### Domain rule

**Source trust is not claim truth.** A peer-reviewed paper may still be wrong; an unreviewed GitHub issue may correctly identify a critical bug. Trust tiers tell the system how to present and prioritize evidence, not what conclusions it is allowed to assert.

## Epic 15: External Artifact Ingestion

**Goal:** Acquire and preserve external research artifacts as immutable, provenance-rich objects that can be reprocessed as extraction improves.

**User outcome:** “A paper, repository, dataset, model card, or release can be inspected as it existed when I acquired it, alongside the metadata and source context that made it relevant.”

### Scope

Implement acquisition adapters:

- **arXiv**
    - metadata via API/OAI-PMH;
    - PDF/source links where permitted and requested;
    - category, authors, submission/update history, abstract, identifiers, citations where available.
- **GitHub**
    - repo metadata, default branch commit, releases, README, license, issues/PRs only where source policy permits;
    - webhook-driven incremental updates for owned/watched repos;
    - API queries for bounded discovery.
- **Hugging Face**
    - model cards, dataset cards, repository metadata, configs, license, revisions, metadata;
    - selective dataset samples/metadata rather than blind full downloads;
    - model/dataset revision pinning.
- **Official documentation**
    - page snapshots, version, canonical URL, extraction timestamp, content hash.
- **Datasets**
    - catalog metadata first;
    - schema/sample/statistics/card/license;
    - explicit approval required before materializing a large dataset locally.

Hugging Face provides Hub APIs and webhooks, while its Datasets tooling supports dataset inspection, loading, processing, streaming, and Arrow-backed operations; that makes metadata-first and sample-first ingestion feasible before you commit disk/network resources to a full dataset.[^12_8][^12_9][^12_10][^12_6][^12_11]

### Acceptance criteria

- Each adapter produces normalized external artifacts plus raw snapshots.
- The same remote revision does not create duplicate artifact content.
- The system pins external resources to immutable references where platforms provide them: arXiv ID/version, Git commit SHA/release, Hugging Face revision SHA, dataset version/config/split.
- Large artifacts require an explicit quota/approval decision before download.
- Ingestion continues gracefully through transient API failures, rate limiting, partial content, and unavailable revisions.
- Every artifact can be re-extracted without contacting the external service again.
- Copyrighted material is stored and used only under applicable terms; summaries preserve provenance and do not reconstruct restricted source text.


## Epic 16: Research Component Extraction

**Goal:** Convert papers, repositories, model cards, dataset cards, and documentation into structured, evidence-linked research components.

**User outcome:** “I can ask: what did this work claim, assume, evaluate, use, and release—and inspect the exact text, code, or metadata supporting each answer.”

### Common research component model

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


### Scope

Extract and link:

- Research question/problem.
- Claimed contribution.
- Hypothesis and assumptions.
- Methods/models/algorithms.
- Datasets and data splits.
- Metrics, baselines, controls, and ablations.
- Reported results and uncertainty where available.
- Hardware/resource claims.
- Threats to validity and stated limitations.
- Reproducibility assets: code, config, seeds, environment, license.
- Citations, implementation references, model/dataset lineage.
- Repository signals: maintenance activity, release history, open issues, test/config evidence.
- Dataset signals: card quality, license, task/domain, schema, split, sample statistics, limitations.


### Acceptance criteria

- Every extracted component links to at least one source artifact and evidence span.
- The system distinguishes author-reported claims from independently observed repository/dataset metadata.
- Extraction records model/prompt/extractor version and confidence.
- Low-confidence extraction becomes a review candidate, not an accepted fact.
- A user can compare components across papers/repositories, for example datasets, metrics, methods, or limitations.
- The system retains the original artifact even when a later extraction model revises the interpretation.
- Extraction quality is evaluated against a curated gold set of papers, model cards, and repositories.


### Domain rule

Do not flatten a paper into a single “summary.” Its **claims, methods, datasets, results, limitations, and evidence** must be separately addressable. That is what permits prior-art comparison and experimental design later.

## Epic 17: Research Knowledge Graph and Evidence Ranking

**Goal:** Link external research components to the Phase 1/2 local corpus while preserving differences in authority, time, and evidence type.

**User outcome:** “I can trace a local design idea to prior art, implementations, datasets, and criticism—and distinguish a verified link from a semantic suggestion.”

### Scope

Build graph relationships such as:

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
- `:research-gap/suggested-by` → evidence set.

Use hybrid retrieval plus graph traversal to discover candidates. Require evidence and review state for high-value cross-domain links.

### Acceptance criteria

- A local concept can return relevant papers, repos, models, datasets, and explicit evidence spans.
- A paper/method can return local notes, code, and experiments that resemble or build on it.
- Search results disclose source class, trust tier, publication/revision date, extraction confidence, and evidence status.
- Graph traversals are bounded by relationship types, provenance filters, and time.
- The user can distinguish:
    - explicit citation/link;
    - lexical/semantic resemblance;
    - shared method component;
    - human-accepted lineage;
    - LLM-proposed hypothesis.
- The system produces an evidence packet for any “prior art” claim rather than a bare similarity score.


## Epic 18: Prior-Art, Gap, and Contradiction Analysis

**Goal:** Identify what has likely already been tried, where local/external claims diverge, and where a question is genuinely unresolved enough to justify research.

**User outcome:** “Before I build or write, I can see the adjacent literature, existing implementations, known failure modes, and the exact gap I might be able to investigate.”

### Scope

- Prior-art search:
    - map local concepts/designs to external methods/tasks/implementations;
    - identify direct matches, close analogues, and missing comparisons;
    - highlight explicit citations and temporal precedence.
- Gap analysis:
    - unsupported local claims;
    - local ideas with no known external match;
    - external open problems with relevant local assets;
    - method/dataset/metric combinations that appear underexplored;
    - contradictory reported outcomes under comparable conditions.
- Contradiction analysis:
    - extract claim scope: task, dataset, metric, environment, time, assumptions;
    - reject false contradictions caused by different scope;
    - flag potential conflicts for review.
- Risk screen:
    - missing control/baseline;
    - inappropriate metric;
    - data leakage risk;
    - underpowered sample;
    - inaccessible/restricted data;
    - incompatible license;
    - compute cost beyond local budget;
    - claims that cannot be falsified.


### Acceptance criteria

- Every gap/risk/contradiction candidate links to the evidence that generated it.
- The system can state why two apparently conflicting papers may not actually conflict.
- “Novelty” is never declared; use calibrated language such as “no close prior art found within configured sources and search coverage.”
- Prior-art searches preserve exact query, source coverage, dates, and retrieval configuration.
- The user can promote a candidate gap into a research question or dismiss it with a reason.
- The system records false positives to improve future screening.


### Critical rule

Absence of retrieved evidence is **not evidence of novelty**. Your platform can say: “within these sources, queries, dates, and retrieval settings, I did not retrieve a close match.” It cannot responsibly say: “nobody has done this.”

## Epic 19: Taxonomy and Research-Question Studio

**Goal:** Use LLMs to propose taxonomies, research questions, and classification schemas from grounded evidence, while keeping humans in control of the conceptual vocabulary.

**User outcome:** “I can ask the system to organize a new research area, show competing taxonomies, identify ambiguities, and turn a real corpus gap into a crisp question.”

### Scope

- Generate taxonomy candidates from selected artifact sets:
    - topics/subtopics;
    - tasks;
    - methods;
    - data types;
    - evaluation metrics;
    - limitations/failure modes;
    - governance/risk categories.
- Preserve multiple competing taxonomies rather than forcing one hierarchy.
- Generate research-question candidates from:
    - explicit contradictions;
    - missing evidence;
    - underexplored combinations;
    - local implementation capabilities;
    - public datasets and feasible compute budgets.
- Build a question template:

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
- The system remembers accepted/rejected taxonomy decisions as review events, not hidden prompt history.


## Epic 20: Experiment Design and Reproducibility Contracts

**Goal:** Turn an approved question into a machine-checkable, reproducible experiment plan before expensive execution begins.

**User outcome:** “I can review a proposed experiment as a concrete contract: hypothesis, data, baselines, metrics, controls, compute budget, risks, and expected evidence—not just a paragraph generated by an LLM.”

### Scope

Define a versioned experiment specification:

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

- Generate candidate experiments by composing compatible extracted components.
- Run static design checks:
    - missing baseline;
    - metric/objective mismatch;
    - train/test leakage;
    - absent seed/reproducibility plan;
    - incompatible data/model license;
    - unsatisfied compute/storage/network budget;
    - unstated confound;
    - no success/failure criterion;
    - no analysis plan.
- Create containerized/replayable execution envelopes.
- Emit execution events and retain outputs as immutable artifacts.
- Support local resource scheduling:
    - GPU workloads on the Ultra 9/4070 Ti node;
    - CPU/ETL/routing work on other nodes;
    - hard concurrency and disk limits.


### Acceptance criteria

- No experiment can run without a pinned specification and approval status.
- Every result records the exact experiment spec, code revision, data revision, container/environment, model version, seeds, hardware, and resource use.
- An experiment has explicit success, failure, and inconclusive outcomes.
- Static checks identify common design defects before execution.
- Failed experiments remain first-class evidence and are searchable.
- Re-running an experiment from retained inputs reproduces the plan and environment, subject to documented nondeterminism.
- The system can compare results across parameter changes and show which variables changed.


## Epic 21: Research Agent Workflows

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
- **Supervisor gate:** your approval and policy engine before expensive, externally visible, or high-impact actions.


### Acceptance criteria

- Every agent action has an assigned task, tool permissions, time/token/resource budget, and trace.
- Agents write proposals and evidence packets; they do not directly promote claims, change trusted schemas, or publish results.
- Expensive downloads, model runs, external writes, and experiment execution require explicit approval policies.
- Agent performance is evaluated separately for retrieval, extraction, critique, design, and execution.
- The system records task outcomes, reviewer feedback, and failure categories for future agent improvement.
- Agents can request a human decision with a concise decision card: context, options, evidence, consequence, and reversibility.


## Cross-cutting epic: Research integrity and governance

**Goal:** Make the platform suitable for honest, inspectable research rather than merely fast content synthesis.

### Required controls

- Provenance and source-tier visible in every answer, graph edge, and experiment plan.
- Copyright/license and terms metadata preserved for external artifacts.
- Explicit separation between:
    - observed data;
    - author claim;
    - model extraction;
    - agent hypothesis;
    - human-accepted interpretation;
    - experimental result.
- Dataset documentation, access restrictions, and known limitations surfaced before use.
- Reproducibility manifests and immutable result artifacts.
- Evaluation against human-curated research tasks.
- Clear uncertainty language and no unsupported novelty/reliability claims.
- Red-team review for source poisoning, prompt injection in crawled content, malicious repository content, and contaminated dataset/model cards.

Hugging Face’s ecosystem exposes datasets, model/dataset cards, APIs, and revisioned repositories; those are valuable metadata surfaces, but they should be treated as artifacts requiring provenance, license checks, and quality assessment rather than as trusted labels.[^12_9][^12_12][^12_6][^12_11]

## Delivery sequence

1. **Epic 14: Governed External Source Registry**
2. **Epic 15: External Artifact Ingestion**
3. **Epic 16: Research Component Extraction**
4. **Epic 17: Research Knowledge Graph and Evidence Ranking**
5. **Epic 18: Prior-Art, Gap, and Contradiction Analysis**
6. **Epic 19: Taxonomy and Research-Question Studio**
7. **Epic 20: Experiment Design and Reproducibility Contracts**
8. **Epic 21: Bounded Research Agent Workflows**
9. **Research-integrity controls throughout**

Do not deploy autonomous research agents before the source registry, provenance model, retrieval evaluation, and experiment contract exist. Otherwise they will automate accumulation and plausible synthesis before you have a way to audit whether they are learning anything true.

## Phase-three exit test

Phase 3 is complete when you can choose a real question emerging from your notes/code—for example, a graph-based retrieval or ACO-inspired semantic traversal question—and produce an inspectable research dossier containing:

- the local notes, code, and prior experiments that motivate the question;
- an allowlisted, reproducible search over arXiv, GitHub, and Hugging Face;
- retrieved prior art, implementations, models, and datasets with dates, licenses, trust tiers, and evidence;
- structured comparison of methods, assumptions, datasets, metrics, baselines, reported results, and limitations;
- a calibrated statement of what appears known, disputed, missing, or merely unverified;
- one or more candidate taxonomies and falsifiable research questions;
- a human-reviewed experiment specification with pinned code/data/model versions, controls, metrics, resource budget, and safety/governance review;
- a bounded agent workflow that can update the dossier or draft follow-up work without silently promoting claims or spending significant compute.

That is the point at which your system moves from personal knowledge archaeology to a credible, grounded **research operating environment**.
<span style="display:none">[^12_13][^12_14][^12_15]</span>

<div align="center">⁂</div>

[^12_1]: https://info.arxiv.org/help/api/user-manual.html

[^12_2]: https://docs.github.com/en/rest/rate-limit/rate-limit

[^12_3]: https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api

[^12_4]: https://docs.github.com/en/rest/using-the-rest-api/best-practices-for-using-the-rest-api

[^12_5]: https://info.arxiv.org/help/oa/index.html

[^12_6]: https://huggingface.co/docs/hub/en/api

[^12_7]: https://info.arxiv.org/help/bulk_data.html

[^12_8]: https://huggingface.co/docs/datasets/en/index

[^12_9]: https://huggingface.co/docs/hub/datasets

[^12_10]: https://github.com/huggingface/datasets

[^12_11]: https://huggingface.co/docs/hub/en/index

[^12_12]: https://huggingface.co/docs

[^12_13]: https://github.com/huggingface/hub-docs/blob/main/docs/hub/api.md

[^12_14]: https://huggingface.co/docs/huggingface_hub/v0.8.0/en/package_reference/hf_api

[^12_15]: https://huggingface.co/docs/hub/datasets-usage
---
# @Academic @GitHub @Hugging Face Draft the epics for phase 4

Phase 4 is **simulation, spatial-temporal analysis, and operational research**. It turns the research operating environment from Phase 3 into a reproducible laboratory for modeling systems that change over time: physical systems, weather/environmental conditions, agent behavior, emergency-response dynamics, information diffusion, and the behavior of the platform’s own multi-agent workflows.

The phase objective is:

> Given a bounded real or synthetic scenario, compose validated models and datasets into a reproducible simulation; explore uncertainty and interventions; connect results to assumptions and evidence; and present maps, timelines, causal structure, and limits clearly enough for a human to make the next research decision.

Use the Functional Mock-up Interface (FMI) where interoperating simulations are warranted: it is an open standard for exchanging dynamic models via XML plus binaries/C code, and its co-simulation mode standardizes interaction with independently executed simulation tools.  Use STAC-compatible catalogs and APIs for external spatiotemporal assets where available, since STAC is explicitly designed to search and retrieve spatiotemporal data assets.[^13_1][^13_2][^13_3][^13_4][^13_5]

## Phase-four definition

**In scope**

- A common experiment/simulation contract built on Phase 3’s reproducibility spec.
- Physical, graph-dynamical, weather/environmental, and agent-based behavioral simulation adapters.
- Spatial and temporal data ingestion, cataloging, and query.
- Scenario composition from versioned inputs, assumptions, and models.
- Parameter sweeps, uncertainty analysis, sensitivity analysis, calibration, and counterfactual comparison.
- Distributed execution across the four-node cluster.
- Simulation evidence graphs and interactive visual analytics.
- Bounded agent-assisted model selection, critique, and experiment scheduling.
- Full provenance, resource accounting, safety gates, and human review.

**Explicitly out of scope**

- A general-purpose replacement for dedicated scientific computing ecosystems.
- Real-time command-and-control of people, vehicles, or emergency services.
- Autonomous recommendations in high-stakes public safety, medical, political, or security settings.
- Treating a simulation output as a factual prediction without calibration, uncertainty, and validation.
- Building a universal world model.
- Downloading large environmental datasets by default.
- Claiming that behavioral/sentiment inference reveals a person’s actual intent, mental state, or future action.


## Epic 22: Simulation Kernel and Experiment Ledger

**Goal:** Establish one data-oriented, reproducible contract for all simulations, whether they are graph dynamics, agent-based scenarios, weather models, or external scientific tools.

**User outcome:** “Every simulation is a first-class research artifact: I can inspect its inputs, assumptions, code, environment, outputs, uncertainty, and lineage—and rerun it later.”

### Scope

Create a versioned simulation manifest that extends Phase 3’s experiment contract:

```clojure
{:simulation/id ...
 :simulation/kind :graph-dynamics
 :simulation/status :draft
 :question/id ...
 :scenario/id ...
 :model {:id ...
         :version ...
         :implementation-ref ...
         :interface :native-clojure | :container | :fmi}
 :inputs [{:artifact/id ...
           :revision ...
           :role :initial-state | :boundary-condition | :observation}]
 :parameters {:ticks 1000
              :seed 42
              :evaporation-rate 0.03}
 :assumptions [...]
 :interventions [...]
 :outputs [{:name :state-series
            :format :parquet
            :artifact-ref ...}]
 :metrics [...]
 :validation-plan [...]
 :uncertainty-plan [...]
 :environment {:container-image ...
               :dependency-lock ...
               :hardware-class :gpu-primary}
 :resources {:cpu ...
             :gpu ...
             :ram-gb ...
             :disk-gb ...
             :max-runtime ...}
 :provenance {:created-from [...]
              :approved-by ...
              :created-at ...}}
```

Support three execution interfaces:

- **Native Clojure simulation:** pure state-transition functions plus explicit effects at the outer boundary.
- **Containerized simulation:** external Python, Rust, Julia, Modelica, or domain-tool executables with pinned environment.
- **FMI/co-simulation adapter:** for standardized exchange/execution of compatible external dynamic models. FMI supports model exchange and co-simulation models; co-simulation provides a standard interface for executing models/tools in a coordinated environment.[^13_6][^13_3][^13_5]


### Acceptance criteria

- A simulation cannot execute without a pinned manifest, resource budget, and approval state.
- Every run records code/data/model revisions, container/image digest, seed, environment, hardware class, start/end time, and exit state.
- Outputs are immutable artifacts linked to their manifest.
- Any run can be replayed from retained inputs, subject to documented nondeterministic behavior.
- A failed or cancelled run remains visible and analyzable.
- The platform can diff two manifests and identify exactly what changed.
- Simulation state transitions, logs, and metric emissions correlate to a trace/run ID.


### Domain rule

A simulation is an **argument under assumptions**, not a forecast and not a discovered fact. The UI and graph model must consistently show the assumptions and validation status alongside every output.

## Epic 23: Spatial-Temporal Data Fabric

**Goal:** Ingest, normalize, index, and query spatial-temporal observations and reference data as evidence-linked assets.

**User outcome:** “I can ask what happened in a place and time, what data supports it, what geometry/time resolution it has, and whether it is suitable for a particular simulation.”

### Scope

- Support spatial primitives:
    - points, lines, polygons, raster footprints, bounding boxes;
    - coordinate reference system metadata;
    - administrative boundaries and named places;
    - geocoded/reverse-geocoded entities where allowed.
- Support temporal primitives:
    - instant;
    - interval;
    - observation time;
    - acquisition/publication time;
    - valid time versus transaction/ingestion time.
- Use STAC-compatible metadata for cataloged spatial assets where practical:
    - collection;
    - item;
    - spatial extent;
    - temporal extent;
    - asset URLs;
    - media type;
    - license;
    - provider;
    - version.
- Ingest external sources only through Phase 3 source governance:
    - weather observations/forecasts;
    - climate/environmental reference data;
    - public geospatial layers;
    - historical incident/simulation datasets where legally and ethically appropriate.
- Build spatial-temporal joins between:
    - external observations;
    - simulation inputs/outputs;
    - local notes/code/experiments;
    - derived geographic entities.

STAC APIs are designed around search/retrieval of spatiotemporal assets; the specification covers STAC catalogs, collections, items, and item collections exposed through API endpoints.[^13_2][^13_4][^13_1]

### Acceptance criteria

- Every spatial-temporal observation retains source, acquisition time, geometry, temporal validity, resolution, license, and transformation history.
- The system distinguishes observed data, forecasts, synthesized scenarios, and simulation outputs.
- Queries support spatial containment/intersection/proximity plus time-window filtering.
- Dataset suitability checks surface coverage, resolution, missingness, coordinate system, license, and known limitations.
- Transformations between coordinate systems/resolutions are versioned and replayable.
- The user can inspect a map result and pivot to the artifact/source that produced every layer.
- Storage policy prevents accidental materialization of massive raster/forecast archives without an approved quota.


## Epic 24: Weather and Environmental Scenario Modeling

**Goal:** Build a bounded environmental modeling layer that can use weather and spatial conditions as scenario inputs without pretending to replace professional forecasting systems.

**User outcome:** “I can construct a scenario with weather/environmental conditions, know the source and uncertainty of those conditions, and test how they affect a model outcome.”

### Scope

- Model weather/environmental data in three clearly separated modes:
    - **historical observation:** what a source reported for a past place/time;
    - **forecast input:** a source’s prediction captured at a specified issuance time;
    - **synthetic scenario:** explicitly generated perturbation or hypothetical condition.
- Provide a normalized environmental state contract:

```clojure
{:environment/time ...
 :environment/geometry ...
 :weather {:temperature ...
           :wind {:speed ... :direction ...}
           :precipitation ...
           :visibility ...
           :pressure ...}
 :surface {:condition ...
           :flooding-risk ...}
 :source {:artifact/id ...
          :observation-type :historical | :forecast | :synthetic}
 :uncertainty {...}}
```

- Implement:
    - interpolation/resampling;
    - missing-data reporting;
    - scenario perturbations;
    - weather-sensitive model inputs;
    - environment-to-impact mappings only when an explicit domain model supports them.
- Capture forecast provenance: issue time, valid time, model/source, and retrieval date.
- Support comparison of weather-conditioned versus weather-neutral simulations.


### Acceptance criteria

- The platform never labels a synthetic perturbation as an observed or forecast weather fact.
- Historical and forecast data preserve distinct time semantics.
- Every weather-sensitive result identifies the input source/version and uncertainty assumptions.
- Environmental inputs can be reused across multiple scenarios without duplication.
- A user can compare outputs under baseline, historical, forecast, and synthetic conditions.
- Domain-specific impact functions are independently versioned, testable, and reviewable.
- Data gaps and spatial/temporal interpolation are visible in the visualization and result packet.


### Research constraint

Weather is a powerful confounder. Treat it as an explicit variable with provenance and uncertainty, not scenery pasted behind an emergency-response or mobility simulation.

## Epic 25: Agent-Based Behavioral Modeling

**Goal:** Provide a safe, interpretable framework for simulating populations of abstract agents, organizations, services, or information flows.

**User outcome:** “I can model how different assumptions about policies, resources, communication, incentives, or network topology produce different system-level behavior—without claiming to predict individuals.”

### Scope

- Define a generic agent-based model contract:
    - agent types and state;
    - environment state;
    - interaction topology;
    - transition rules;
    - policy/intervention definitions;
    - observables;
    - calibration/validation evidence.
- Support models for:
    - resource allocation;
    - emergency-response logistics;
    - communication and information diffusion;
    - service queues;
    - organizational coordination;
    - network resilience;
    - abstract behavioral strategies and game-theoretic scenarios.
- Keep model components composable:
    - state transition;
    - observation;
    - policy;
    - topology;
    - scheduler;
    - random distribution;
    - metric collector.
- Use a NetLogo adapter only if it offers value for a particular model; retain a native Clojure representation of the scenario and results so the platform is not bound to a GUI-oriented runtime. NetLogo has an established agent-based-modeling ecosystem and publication reference base, making it a useful comparative or interoperability target rather than the platform’s mandatory core.[^13_7][^13_8]


### Acceptance criteria

- No model makes claims about identifiable people or groups without an explicit ethical review and permitted data basis.
- Each agent rule is inspectable and traceable to code, evidence, or declared exploratory assumption.
- Model outputs include distributions and scenario ranges, not only a single “best” trajectory.
- Seeds, random generators, topology, and policy choices are recorded.
- Users can compare interventions against the same baseline and identify changed assumptions.
- A model can be run as a parameter sweep rather than a single anecdotal run.
- The system makes calibration status visible: uncalibrated exploratory, partially calibrated, historically validated, or invalidated.


### Domain rule

A behavior model represents **rules and assumptions**, not a claim that actual humans are reducible to those rules. It should support reasoning about system dynamics and policy tradeoffs, not profiling or prediction of specific persons.

## Epic 26: Graph Dynamics and Semantic Physics Laboratory

**Goal:** Formalize your ACO/semantic-gravity work as a reproducible experimental subsystem rather than a one-off visualization or untestable metaphor.

**User outcome:** “I can run controlled experiments on graph topology, semantic affinity, information flow, clustering, and path selection—and compare the results to retrieval, human labels, and baseline graph algorithms.”

### Scope

- Integrate your existing ACO-inspired semantic graph work as a versioned model family.
- Define model elements explicitly:
    - nodes: concepts, documents, symbols, events, agents;
    - edges: observed, inferred, accepted, temporal, semantic;
    - fields/weights: affinity, charge/potential, distance, decay, trust, recency;
    - particles/agents: walkers, pheromone, attention, resource, signal;
    - constraints: conservation/bounds, decay rules, capacity, stopping conditions.
- Compare against conventional baselines:
    - shortest path;
    - personalized PageRank;
    - community detection;
    - embedding-neighbor retrieval;
    - random walk;
    - graph neural/network heuristics only where justified.
- Evaluate on Phase 1/2/3 tasks:
    - retrieving relevant evidence;
    - identifying concept clusters;
    - ranking lineage candidates;
    - finding code/note boundaries;
    - routing research-agent attention.
- Provide parameter sweeps and ablation experiments:
    - remove pheromone;
    - remove temporal decay;
    - remove user labels;
    - alter edge-cost functions;
    - compare static versus dynamically updated graphs.

Your existing ACO model already frames semantic relationships as gradients/vector fields and uses particle-like exchange between graph nodes, with planned Datalog-based path optimization; Phase 4 should make those mechanisms falsifiable against retrieval and clustering baselines rather than assuming the metaphor is self-validating.[^13_9]

### Acceptance criteria

- Every dynamic rule has an executable specification and a stated hypothesis.
- Each experiment includes at least one non-semantic or established graph baseline.
- Results report retrieval/cluster/lineage metrics, resource cost, stability, and failure modes.
- Parameter changes are reproducibly attributable to output changes.
- The platform can show why a path or cluster was chosen: edge sequence, cost terms, pheromone/field state, and time.
- The system detects unstable/divergent dynamics and halts according to explicit safety/resource bounds.
- Human feedback can be incorporated as a separately weighted signal, not silently conflated with semantic similarity.


## Epic 27: Calibration, Uncertainty, and Counterfactual Analysis

**Goal:** Make models useful for research by testing their fit, identifying sensitivity, and separating robust results from artifacts of arbitrary assumptions.

**User outcome:** “I can tell whether a result is stable, what assumptions drive it, what evidence supports calibration, and what I would need to observe to reduce uncertainty.”

### Scope

- Implement:
    - parameter sweeps;
    - Monte Carlo / seed ensembles;
    - sensitivity analysis;
    - scenario comparison;
    - calibration against historical data where ethically and methodologically appropriate;
    - holdout validation;
    - backtesting;
    - counterfactual analysis with explicitly bounded causal assumptions.
- Separate uncertainty categories:
    - measurement/data uncertainty;
    - model-structure uncertainty;
    - parameter uncertainty;
    - scenario uncertainty;
    - computational/numerical uncertainty;
    - unknown/uncaptured factors.
- Produce uncertainty-aware result structures:

```clojure
{:result/metric :response-time
 :estimate ...
 :interval {:lower ... :upper ... :level 0.95}
 :sensitivity [{:parameter :resource-count
                :effect ...}]
 :calibration {:status :partial
               :evidence [...]}
 :limitations [...]}
```


### Acceptance criteria

- A simulation cannot be presented as validated unless it passes a declared validation procedure.
- Result charts distinguish individual runs, ensembles, intervals, and observed reference values.
- Counterfactual questions require explicit intervention and causal-assumption declarations.
- Sensitivity reports identify which inputs dominate output variation.
- Calibration datasets are versioned and never mixed with evaluation datasets without disclosure.
- The system flags overfitting risks, missing data, insufficient repetitions, and unsupported causal interpretation.
- “Inconclusive” is a valid first-class outcome.


## Epic 28: Distributed Experiment Scheduling

**Goal:** Use your four machines as a resource-aware research cluster, without treating weak nodes as failed versions of the strong ones.

**User outcome:** “Approved simulations, data preparation, model inference, and visualization jobs run on the right machines with visible resource budgets, recoverable failures, and no accidental starvation of interactive work.”

### Scope

- Extend the event/job infrastructure with resource-aware scheduling:
    - CPU cores;
    - RAM;
    - GPU/VRAM;
    - NPU if usable through a proven adapter;
    - disk space;
    - network budget;
    - job priority;
    - expected duration;
    - retry policy.
- Establish workload classes:
    - `:interactive-query`
    - `:ingestion`
    - `:batch-embedding`
    - `:simulation-small`
    - `:simulation-sweep`
    - `:gpu-inference`
    - `:archive`
    - `:visualization-precompute`.
- Assign hardware roles:
    - Ultra 9/4070 Ti: GPU inference, embedding/reranking, high-value simulation batches.
    - Ryzen 7: primary database/index projections, CPU experiments, orchestration.
    - Ryzen 3/i5: object storage, crawling, archive, metrics/logging, low-priority or embarrassingly parallel preparation work.
- Add quotas and preemption:
    - interactive research requests outrank background sweeps;
    - no simulation can consume unbounded disk;
    - GPU jobs have VRAM and runtime ceilings;
    - low-priority work pauses under resource pressure.


### Acceptance criteria

- Every job declares a resource class and maximum budget.
- The scheduler can explain why a job is pending, running, paused, retried, or failed.
- Interactive retrieval remains available under batch/simulation load.
- No weak node is scheduled for an out-of-memory-prone service by default.
- Job queues survive worker restart and retain idempotency keys.
- Resource telemetry is visible by node, workload class, experiment, and user/project.
- A run can be resumed or cleanly restarted from checkpoints where the model supports it.


## Epic 29: Simulation Visual Analytics Workbench

**Goal:** Make simulation output inspectable through maps, timelines, distributions, graph views, and comparison tools—not merely static charts or opaque “AI conclusions.”

**User outcome:** “I can understand what happened in a simulation, compare scenarios, inspect uncertainty, trace data lineage, and identify the next question to investigate.”

### Core views

- **Scenario composer:** assumptions, inputs, interventions, resource estimates, approval state.
- **Spatial-temporal map:** layers for observed data, forecast data, synthetic conditions, agent state, and simulation outputs.
- **Timeline explorer:** events, state transitions, interventions, uncertainty intervals, and selected entity tracks.
- **Parameter-space explorer:** sweep matrices, parallel coordinates, response surfaces, and sensitivity rankings.
- **Distribution/ensemble view:** individual trajectories, percentile bands, histograms, failure/outlier runs.
- **Graph-dynamics view:** node/edge state, signals/particles, traversal paths, cluster evolution, cost fields.
- **Evidence panel:** source artifacts, assumptions, code/model version, calibration evidence, and known limitations.
- **Comparison workspace:** baseline versus intervention, historical versus synthetic, model A versus model B.
- **Run ledger:** resource cost, queue time, failures, retries, reproducibility state, and exportable manifests.


### Acceptance criteria

- No visualization hides uncertainty by default when uncertainty data exists.
- Every visible result can be traced to an experiment manifest and input artifacts.
- Maps clearly distinguish observed, forecast, inferred, and simulated layers.
- Visualizations support time-window selection, scenario comparison, and evidence drill-down.
- Large datasets use progressive loading, aggregation, and bounded detail—not unbounded client-side graph rendering.
- Every visualization can export a data/provenance bundle suitable for a research notebook or paper figure workflow.
- The system marks exploratory simulations as exploratory in the UI.


## Epic 30: Bounded Autonomous Experiment Loops

**Goal:** Allow agents to propose, run, critique, and learn from low-risk simulation experiments within fixed budgets and explicit human-defined objectives.

**User outcome:** “The platform can continuously test bounded hypotheses and surface surprising patterns, while I retain control over objectives, models, resources, and interpretation.”

### Scope

- Define an autonomous experiment loop:

```text
Observe evidence/results
  -> propose bounded hypothesis or parameter change
  -> static design/risk check
  -> select a low-cost approved experiment
  -> execute under resource policy
  -> evaluate against predefined metrics
  -> retain result and critique
  -> request human direction or schedule next bounded iteration
```

- Restrict autonomous loops to:
    - parameter tuning;
    - retrieval/graph algorithm comparison;
    - simulation calibration on approved historical data;
    - visualization anomaly detection;
    - benchmark replication;
    - low-cost ablation studies.
- Require human approval for:
    - new model families;
    - new external datasets;
    - materially higher resource budgets;
    - altered objectives/metrics;
    - politically, socially, or safety-sensitive scenarios;
    - public claims or publication drafts.
- Support critic agents that challenge:
    - metric gaming;
    - data leakage;
    - invalid comparison;
    - confounding;
    - unsupported causal claims;
    - brittle conclusions.


### Acceptance criteria

- Every autonomous action is linked to a task, budget, objective, policy, and trace.
- Agents cannot redefine their own success metric or resource cap.
- A loop commits only results that meet predeclared validity and improvement conditions.
- Negative/failed results are retained and influence future proposal ranking.
- The agent provides a concise human decision card when a conceptual, ethical, or strategic choice is required.
- Autonomous loops are benchmarked against manual baselines and can be paused globally.
- The system detects repetitive or non-informative experiment cycles and halts them.


## Cross-cutting epic: Simulation safety and epistemic governance

**Goal:** Prevent a sophisticated simulation platform from manufacturing unjustified certainty.

### Required controls

- **Assumption visibility:** no result without assumptions attached.
- **Provenance:** every input, transformation, model, result, and chart carries source lineage.
- **Uncertainty:** calibrated intervals, scenario ranges, and missing-data warnings appear before conclusions.
- **Scope:** clearly distinguish exploratory models, validated models, and externally grounded analyses.
- **Human gatekeeping:** public/high-stakes scenarios require explicit review.
- **Privacy and ethics:** prohibit modeling identifiable individuals unless data rights, purpose, and review are explicit.
- **Adversarial robustness:** treat external data/code/models as untrusted until scanned, sandboxed, and provenance-checked.
- **Reproducibility:** retained manifests, images/containers, seeds, outputs, and failure records.
- **Cost accounting:** report compute, storage, and inference consumption per experiment.


## Delivery sequence

1. **Epic 22: Simulation Kernel and Experiment Ledger**
2. **Epic 28: Distributed Experiment Scheduling**
3. **Epic 29: Minimal Simulation Visual Analytics Workbench**
4. **Epic 26: Graph Dynamics and Semantic Physics Laboratory**
5. **Epic 27: Calibration, Uncertainty, and Counterfactual Analysis**
6. **Epic 23: Spatial-Temporal Data Fabric**
7. **Epic 24: Weather and Environmental Scenario Modeling**
8. **Epic 25: Agent-Based Behavioral Modeling**
9. **Epic 30: Bounded Autonomous Experiment Loops**
10. **Simulation safety/governance throughout**

This sequence puts your distinctive work—the graph/ACO dynamics—before weather or broad behavioral modeling, because it already connects to the corpus, graph, and retrieval system built in Phases 1–3. It also means the platform becomes useful as a rigorous testing environment before acquiring the complexity of large spatiotemporal datasets.

## Phase-four exit test

Phase 4 is complete when you can take a real research question—such as whether your ACO/semantic-gravity traversal improves evidence retrieval or cluster stability over conventional graph/ranking baselines—and produce a full, inspectable simulation research packet:

- a pinned scenario and simulation manifest;
- a clear hypothesis, baselines, intervention, metrics, and stopping conditions;
- versioned input graph/corpus data with provenance;
- parameter sweeps and seed ensembles;
- comparison against at least one established baseline;
- sensitivity and uncertainty analysis;
- a visualization workspace showing graph/time evolution and evidence traces;
- resource use across your four machines;
- retained failures, outliers, and inconclusive results;
- explicit limits on what the results establish;
- and, if useful, an agent-generated next experiment proposal that remains within a human-approved budget and objective.

At that point, the platform is no longer merely a knowledge system or research index. It is a **reproducible computational laboratory** for investigating your own ideas, models, and complex-system hypotheses.
<span style="display:none">[^13_10][^13_11][^13_12][^13_13][^13_14][^13_15][^13_16]</span>

<div align="center">⁂</div>

[^13_1]: https://docs.ogc.org/cs/25-005/25-005.html

[^13_2]: https://cmr.earthdata.nasa.gov/stac/docs/index.html

[^13_3]: https://github.com/modelica/fmi-standard

[^13_4]: https://github.com/radiantearth/stac-api-spec

[^13_5]: https://github.com/modelica/fmi-standard/blob/main/docs/4___co-simulation.adoc

[^13_6]: https://openmodelica.org/doc/OpenModelicaUsersGuide/latest/fmitlm.html

[^13_7]: https://www.netlogo.org/references/

[^13_8]: https://www.aeeejournal.org/UserFiles/file/AETR_2023_044RR%20Final.pdf

[^13_9]: https://www.perplexity.ai/search/0c80ab22-a903-436b-94bf-4b026a660698

[^13_10]: https://openmodelica.org/doc/OpenModelicaUsersGuide/v1.21.0/fmitlm.html

[^13_11]: https://openmodelica.org/doc/OpenModelicaUsersGuide/OpenModelicaUsersGuide-1.21.pdf

[^13_12]: https://modelica.github.io/fmi-guides/main/fmi-guide/

[^13_13]: https://www.earthdata.nasa.gov/s3fs-public/2023-07/ESDS-RFC-044 SpatioTemporal Asset Catalogs v1.0-001.pdf

[^13_14]: https://openmodelica.org/images/M_images/OpenModelicaWorkshop_2020/OpenModelica%20Development%20on%20FMI%20\&%20SSP.pdf

[^13_15]: https://github.com/modelica/fmi-standard/blob/main/docs/4_2_co-simulation_api.adoc

[^13_16]: https://github.com/modelica/fmi-standard/blob/main/docs/1___overview.adoc
