---
title: NLP and Code Intelligence Deep Research
slug: nlp-and-code-intelligence-deep-research
created: 2026-07-11
kind: research
source: docs/notes/research/nlp-fundamentals-and-classifiers.md, docs/notes/research/clojure-nlp-stack.md, docs/notes/design/phase-1-corpus-archaeology.md, docs/notes/design/phase-2-code-comprehension.md
---

# NLP and Code Intelligence Deep Research

This note synthesizes external research on NLP fundamentals, Clojure NLP tooling, polyglot parsing, Clojure semantic analysis, and code-comprehension methods. It evaluates the claims in the existing design documents and identifies gaps, open questions, and concrete recommendations for the knowledge-platform architecture.

## Research Questions

1. Which text-classifier architecture is appropriate for different tasks in a knowledge-management platform: lexical, embedding-based, fine-tuned transformer, or LLM-as-classifier?
2. What is the current maintenance status and capability of Clojure NLP tools (clojure-opennlp, core.nlp, libpython-clj, Stanford CoreNLP)?
3. Can Tree-sitter serve as a robust, polyglot parser substrate for code intelligence?
4. What semantic facts does clj-kondo expose, and how reliable is it for building namespace/reference/dependency graphs?
5. What does research say about multi-view clustering, co-change analysis, and AST-based feature extraction for identifying architectural boundaries?

## Summary of Findings

### 1. Text Classification Pipelines

A recent survey (Li et al., 2020) traces text classification from TF-IDF and linear models through deep learning and transformers. The consensus is that the right tool depends on data volume, label stability, and operational budget:

| Approach | When it is appropriate | Caveats |
|---|---|---|
| **Lexical (TF-IDF / n-grams + linear)** | Fast, inspectable baseline; competitive when labels correlate strongly with vocabulary (Li et al., 2020) | Fails when word order, synonymy, or domain transfer matter |
| **Embedding (sentence transformer + logistic regression / SetFit)** | Few-shot settings, small domain corpora, or when a pretrained encoder can be reused without full fine-tuning (Tunstall et al., 2022) | Still requires representative examples and label definitions |
| **Fine-tuned transformer (BERT/DistilBERT)** | Canonical choice when hundreds to thousands of high-quality labels exist (Hugging Face, 2026) | Higher compute, longer training, harder to debug |
| **LLM-as-classifier** | Complex policy, changing taxonomy, or zero/few-shot tasks where prompt engineering is cheaper than labeling (Valdes Gonzalez, 2026) | Can be 1–2 orders of magnitude more expensive and slower than fine-tuned encoders for fixed-label classification (Valdes Gonzalez, 2026) |

A recent production-oriented benchmark (Valdes Gonzalez, 2026) explicitly warns against indiscriminate LLM use for structured text classification: fine-tuned encoder models achieved competitive or superior macro F1 on IMDB, SST-2, AG News, and DBPedia while operating at one to two orders of magnitude lower latency and cost. Hugging Face documentation also frames the canonical workflow as: start with a baseline, inspect errors, improve data, and only then scale model size. This is consistent with the design note’s “improve data before tuning models” rule.

### 2. Clojure NLP Tooling

| Tool | Status | Capabilities | Limitations |
|---|---|---|---|
| **clojure-opennlp** | Mature, stable; 756 stars, 313 commits, version 0.5.0 (dakrone, 2026) | Wrappers for Apache OpenNLP tokenization, sentence splitting, POS tagging, NER, chunking, parsing, document categorization | Based on older statistical models; not actively modernized; treebank parsing is memory intensive |
| **core.nlp** | Experimental; 15 stars, 16 commits (arnaudsj, 2026) | Aims to provide idiomatic, pluggable Clojure NLP API | Self-described as “in its infancy” and not production-ready |
| **libpython-clj** | Actively maintained; 1.2k stars, 677 commits, JDK 17 / Apple Silicon support (clj-python, 2026) | Deep JVM/Python interop; can use spaCy, transformers, NumPy, etc. from Clojure | Requires a Python runtime and dependency alignment |
| **Stanford CoreNLP** | Actively maintained; version 4.5.10, supports 8 languages (Stanford NLP, 2026) | Comprehensive linguistic pipeline: tokenization, POS, NER, dependency/constituency parsing, coreference, sentiment, OpenIE | Heavy model loading; full GPL v3; JVM integration via interop |
| **spaCy (via libpython-clj)** | Production-grade; tokenization, POS, morphology, dependency parsing, NER, entity linking, rule-based matching (Explosion, 2026) | Modern pretrained pipelines, transformer integration, efficient Cython core | Runs outside the JVM |

The existing design note’s recommendation to use **libpython-clj + spaCy** for modern NLP and **OpenNLP/CoreNLP** for JVM-native deployment is well supported. **core.nlp should not be relied on** for production work.

### 3. Tree-sitter as a Polyglot Parser Substrate

Tree-sitter is an incremental parser generator and runtime library designed for editor tooling. Its stated goals are to be general enough for any programming language, fast enough to parse on every keystroke, robust enough to produce useful trees even in the presence of syntax errors, and dependency-free (C11 runtime) (Tree-sitter, 2026). It has official parsers for Bash, C, C++, C#, Go, Haskell, HTML, Java, JavaScript, JSON, Julia, OCaml, PHP, Python, Ruby, Rust, Scala, TypeScript, and many others, plus a large community parser ecosystem.

Strengths for this project:
- Fast, incremental reparsing enables interactive workbench experiences.
- Error tolerance means partial or malformed files still produce trees.
- Language bindings exist for many host languages, including Java (JDK 22+) and Rust, making JVM integration feasible.
- The supported language list covers nearly every language named in Phase 2 except Clojure/ClojureScript; those will need a separate parser (or clj-kondo/rewrite-clj).

Limitations:
- Grammar quality and coverage vary by language; some community grammars are incomplete or stale.
- Tree-sitter produces a **concrete syntax tree**, not a semantic graph. It does not resolve symbols, imports, or types.
- Structural similarity is not proof of domain similarity (Alon et al., 2018), so the design note’s warning that “AST similarity is not proof of equivalent behavior” is correct.

### 4. clj-kondo as a Semantic Analysis Source

clj-kondo exposes a rich analysis dataset when configured with `{:analysis true}`. The documented output includes (clj-kondo, 2026):

- `:namespace-definitions` and `:namespace-usages` (with aliases, from/to namespaces, and source locations)
- `:var-definitions` and `:var-usages` (with arities, whether private/macro/deprecated, docstrings, source locations)
- `:locals` and `:local-usages`
- `:keywords` (including namespaced and auto-resolved keywords)
- `:protocol-impls`
- `:symbols` in quoted forms or EDN
- `:java-class-definitions`, `:java-class-usages`, `:java-member-definitions`, `:instance-invocations`

Reliability:
- clj-kondo is a **static analyzer**; it does not execute macros or evaluate code. This means macro-heavy or dynamically generated code is explicitly incomplete, matching the design note’s intent to “treat macro-heavy or dynamically resolved behavior as explicitly incomplete.”
- It supports custom hooks and configurations for third-party macros, but these require manual setup.
- For the well-defined static core of a Clojure project, the namespace, var, and reference graphs it produces are reliable and sufficient for dependency analysis and boundary exploration.

### 5. Code Comprehension and Architecture Recovery

The design’s multi-view approach for architectural boundary inference is well grounded in recent research:

- **Multi-view architecture recovery.** SARIF (Software Architecture Recovery with Information Fusion) fuses three signals—dependencies, code text, and folder structure—and was shown to be 36.1% more accurate than the best previous single- or dual-view technique on projects with ground-truth architectures (Zhang et al., 2023). This strongly supports the design’s call to use dependency topology, semantic vocabulary, and file structure as independent coordinate systems.
- **Co-change analysis.** Co-change clusters are a long-established signal for modular structure. ModularityCheck uses co-change clusters from version control to assess package modularity and expose dominant-decomposition problems (Silva et al., 2015). However, co-change is not identical to conceptual cohesion: Jiang et al. (2021) found that 80–90% of co-changed function pairs either invoke the same functions, access the same variables, or contain similar statements, but also that co-change can be driven by refactorings, copy-paste, or build-system coupling. The design note’s caution that “co-change does not mean conceptual cohesion” is therefore justified.
- **AST-based feature extraction.** The code2vec work (Alon et al., 2018) showed that path-based representations of ASTs can predict program properties (variable names, method names, types) across multiple languages. This supports using structural motifs as one signal among many, but not as a standalone architecture oracle.

## Evaluation of Design Claims

### Supported Claims

- **Hybrid retrieval (lexical + semantic + structural) is the right substrate.** The text-classification and architecture-recovery literature both show that combining independent signals outperforms any single signal.
- **Tree-sitter as the default CST/AST substrate.** Tree-sitter’s language coverage, speed, and error tolerance make it suitable for the syntax-forest epic.
- **clj-kondo as the primary Clojure semantic source.** The analysis dataset it exports is exactly the namespace/var/reference/dependency material the design requires.
- **Multi-view clustering for boundary inference.** SARIF provides direct evidence that fusing dependency, text, and folder-structure signals improves recovery accuracy.
- **Explicitly marking macro/dynamic resolution as incomplete.** clj-kondo’s static nature makes this a necessity, and the design correctly treats it as an uncertainty rather than a failure.

### Qualified or Contradicted Claims

- **LLM-as-classifier as the default for “few labels, complex policy, changing taxonomy.”** The use case is valid, but recent production benchmarks (Valdes Gonzalez, 2026) show that fine-tuned encoders are generally preferred for fixed-label structured classification. LLMs should be a complementary tool, not the default.
- **“Modern recommendation: Clojure + libpython-clj + spaCy.”** This is supported, but the design should also recommend starting with lexical and lightweight embedding baselines before reaching for spaCy or LLMs.
- **Tree-sitter coverage for all Phase 2 languages.** Coverage is broad but uneven; a per-language parser-quality matrix and telemetry are required, especially for languages like Clojure that lack a first-class Tree-sitter grammar.
- **“AST n-gram vocabulary per language family.”** This is a good idea, but the design does not specify how to calibrate or evaluate these features. Without evaluation, structural signals can produce plausible but wrong clusters.

### Uncertain Claims

- **The contradiction pipeline.** The design proposes extracting claims, normalizing them into subject/predicate/object schemas, and detecting contradictions. While individual steps (entity extraction, relation extraction) are well studied, end-to-end contradiction detection in unstructured notes is still an open research problem and will require substantial human review.
- **The exact hybrid-ranker weights.** The design gives a sample score breakdown but no principled method for tuning the lexical/semantic/structure/temporal weights. This needs a benchmark and A/B evaluation.
- **Cross-language concept-to-code grounding.** The design lists many evidence signals (lexical overlap, embeddings, docstrings, namespace naming, commit proximity) but does not specify how to combine them or how to evaluate precision/recall for a given codebase.

## Gaps and Missing Considerations

1. **No NLP-classifier benchmark.** The design proposes classifiers for redundancy, tension, and routing, but only Phase 2 has a concrete evaluation dataset. Phase 1 needs an equivalent benchmark (e.g., 50 manually labeled note pairs with duplicate/contradiction/continuation labels).
2. **Long-document handling.** Embeddings and LLMs have context-window limits. There is no strategy for chunking, hierarchical retrieval, or cross-chunk aggregation for long notes or source files.
3. **Cost and latency budget.** The design does not specify acceptable cost or latency for LLM classification, which matters for a production system.
4. **Language detection and non-English support.** The design is English-centric; the Clojure/OpenNLP/spaCy stack has varying multilingual support.
5. **Parser-quality telemetry.** The Phase 2 design lists parser coverage and error rate as metrics but does not say how to collect them per language.
6. **Macro/dynamic-code fallback.** The design says to treat these as incomplete but does not specify a concrete fallback (e.g., manual annotation, hook library, runtime tracing).
7. **Ground-truth architecture boundaries.** The design relies on human review but does not describe how to obtain or curate accepted boundaries for evaluation.

## Open Questions

1. For the specific domain of personal notes and mixed code/config corpora, what is the optimal mix of lexical, embedding, fine-tuned, and LLM classifiers?
2. How will the system evaluate and tune the hybrid retrieval ranker in Phase 1?
3. What is the minimal set of Tree-sitter grammars needed to cover the target codebases, and what is the fallback for Clojure/ClojureScript?
4. How should the system handle macros and dynamic code that clj-kondo cannot resolve—through hooks, manual annotation, or runtime instrumentation?
5. What is the ground-truth dataset for architecture boundaries, and how will human reviewers produce and maintain it?
6. How will the system distinguish co-change that reflects cohesion from co-change driven by refactoring, build coupling, or copy-paste?
7. What is the cost and latency budget for LLM-based classification, extraction, and contradiction detection?

## Recommendations for the Design

1. **Adopt a tiered classification strategy.** Start every text-classification task with a lexical baseline (TF-IDF + logistic regression) and a lightweight embedding baseline (SetFit or sentence-transformer + logistic regression). Promote to fine-tuned BERT only if the baselines fail on a fixed test set. Reserve LLM-as-classifier for truly dynamic taxonomies or complex policies with explicit cost accounting.
2. **Use libpython-clj + spaCy for production NLP, but keep a JVM-native path.** clojure-opennlp or CoreNLP can serve deployments where adding a Python runtime is undesirable. Do not use core.nlp.
3. **Adopt Tree-sitter for the syntax forest, but with a quality matrix.** Build a per-language table of grammar maturity, known error patterns, and parse-error telemetry. Treat Clojure/ClojureScript separately (clj-kondo/rewrite-clj) unless a high-quality Tree-sitter grammar is adopted.
4. **Build on clj-kondo analysis data.** Use the exported namespace, var, keyword, and Java-interop facts as the core Clojure semantic layer. Explicitly tag unresolved or macro-expanded edges as `:unknown` or `:incomplete` rather than omitting them.
5. **Implement multi-view boundary inference following SARIF.** Use dependency, text, and folder-structure signals as the primary views. Add co-change and AST structural signals as secondary, weak evidence. Require human review before promoting any boundary hypothesis to an accepted relation.
6. **Create evaluation benchmarks before building UI polish.** For Phase 1, curate a labeled set of note pairs for duplicate/contradiction/continuation. For Phase 2, implement the existing 100-item benchmark and add parser-error/semantic-unknown rates as first-class metrics.
7. **Add explicit uncertainty propagation.** Every model output, parser result, and inferred relationship should carry provenance, confidence, model/version, and an abstention path. The design’s score-breakdown example should be extended to include confidence calibration and human-review status.

## References

- Alon, U., Zilberstein, M., Levy, O., & Yahav, E. (2018). A General Path-Based Representation for Predicting Program Properties. *arXiv:1803.09544*. https://arxiv.org/abs/1803.09544
- arnaudsj. (2026). *core.nlp*. GitHub. https://github.com/arnaudsj/core.nlp
- clj-kondo. (2026). *Analysis data and tools*. GitHub. https://github.com/clj-kondo/clj-kondo/tree/master/analysis
- clj-python. (2026). *libpython-clj*. GitHub. https://github.com/clj-python/libpython-clj
- dakrone. (2026). *clojure-opennlp*. GitHub. https://github.com/dakrone/clojure-opennlp
- Explosion. (2026). *Linguistic Features*. spaCy Usage Documentation. https://spacy.io/usage/linguistic-features
- Hugging Face. (2026). *Text classification*. Transformers documentation. https://huggingface.co/docs/transformers/tasks/sequence_classification
- Jiang, Z., Zhong, H., & Meng, N. (2021). Investigating and Recommending Co-Changed Entities for JavaScript Programs. *arXiv:2102.07877*. https://arxiv.org/abs/2102.07877
- Li, Q., Peng, H., Li, J., Xia, C., Yang, R., Sun, L., Yu, P. S., & He, L. (2020). A Survey on Text Classification: From Shallow to Deep Learning. *arXiv:2008.00364*. https://arxiv.org/abs/2008.00364
- Nadim, M., Mondal, M., Roy, C. K., & Schneider, K. (2022). Evaluating the Performance of Clone Detection Tools in Detecting Cloned Co-change Candidates. *arXiv:2201.07996*. https://arxiv.org/abs/2201.07996
- Silva, L., Felix, D., Valente, M. T., & Maia, M. (2015). ModularityCheck: A Tool for Assessing Modularity using Co-Change Clusters. *arXiv:1506.05754*. https://arxiv.org/abs/1506.05754
- Stanford NLP. (2026). *CoreNLP*. https://stanfordnlp.github.io/CoreNLP/
- Tree-sitter. (2026). *Introduction*. https://tree-sitter.github.io/tree-sitter/
- Tree-sitter. (2026). *tree-sitter/tree-sitter*. GitHub. https://github.com/tree-sitter/tree-sitter
- Tunstall, L., Reimers, N., Jo, U. E. S., Bates, L., Korat, D., Wasserblat, M., & Pereg, O. (2022). Efficient Few-Shot Learning Without Prompts. *arXiv:2209.11055*. https://arxiv.org/abs/2209.11055
- Valdes Gonzalez, A. A. (2026). Cost-Aware Model Selection for Text Classification: Multi-Objective Trade-offs Between Fine-Tuned Encoders and LLM Prompting in Production. *arXiv:2602.06370*. https://arxiv.org/abs/2602.06370
- Zhang, Y., Xu, Z., Liu, C., Chen, H., Sun, J., Qiu, D., & Liu, Y. (2023). Software Architecture Recovery with Information Fusion. *arXiv:2311.04643*. https://arxiv.org/abs/2311.04643
