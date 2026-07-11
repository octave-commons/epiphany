---
title: Clojure NLP Stack Options
slug: clojure-nlp-stack
created: 2026-07-11
source: docs/inbox/clojure Natural Language Processing.md
kind: research
---

# Clojure NLP Stack Options

Clojure is a strong orchestration language for NLP: use its immutable data transformations and JVM/Python interoperability to compose pipelines, while delegating model-heavy tasks to mature Java or Python libraries.

## Practical stack choices

| Need | Recommended approach | Why |
|---|---|---|
| Tokenization, sentence splitting, POS tagging, NER | Java OpenNLP via `clojure-opennlp` | Direct JVM integration; useful for conventional statistical NLP pipelines |
| Parsing, dependencies, coreference | Stanford CoreNLP through JVM interop | Broad classical NLP pipeline, though model loading and runtime can be substantial |
| Modern production NLP | spaCy via `libpython-clj` | Access to spaCy’s pretrained pipelines while retaining Clojure for dataflow and application logic |
| Small, composable text transformations | `core.nlp` ideas or plain Clojure functions | Fits Clojure’s sequence/transducer style for normalization, tokenization, and feature extraction |
| LLM embeddings, extraction, retrieval | Call an embedding/LLM service or JVM/Python model runtime | Clojure handles document flow, state, caching, schemas, and retrieval; model inference stays external |

## Clojure-shaped NLP

For basic normalization and tokenization, start with plain functions rather than adopting a framework:

```clojure
(ns app.text
  (:require [clojure.string :as str]))

(defn normalize [s]
  (-> s
      str/lower-case
      (str/replace #"[^\p{L}\p{N}\s'-]" " ")
      (str/replace #"\s+" " ")
      str/trim))

(defn tokenize [s]
  (-> s normalize (str/split #"\s+")))

(defn term-frequencies [tokens]
  (frequencies tokens))
```

This style is useful for deterministic stages: Unicode cleanup, language-specific normalization, stopword filtering, n-gram generation, document metadata enrichment, and converting inference results into domain facts.

## Pipeline design

Treat NLP as a data pipeline with explicit boundaries:

```clojure
(defn enrich-document [doc]
  (let [tokens (tokenize (:body doc))]
    (assoc doc
           :text/normalized (normalize (:body doc))
           :text/tokens tokens
           :text/tf (frequencies tokens))))
```

## Useful patterns for knowledge systems

- Store each document, chunk, span, entity, relation, and inference output as immutable facts.
- Give model outputs provenance: model version, prompt or configuration, source offsets, timestamp, confidence, and input-content hash.
- Use Datalog to query derived knowledge.
- Keep the model adapter impure and narrow; make all downstream validation, transformations, and business rules pure.

This structure helps with reprocessing after a model change, auditability, replay, and testing.

## Modern recommendation

For a new Clojure NLP project, generally choose **Clojure + `libpython-clj` + spaCy** for linguistic annotations, or **Clojure + an embeddings/LLM API** for semantic search, classification, and extraction.

Use OpenNLP or CoreNLP when you need local JVM-native deployment, conventional linguistic annotations, or a minimal external-runtime footprint. `core.nlp` is worth studying for API design because it aims to make NLP idiomatic within Clojure.

## Clojure architecture boundary

Keep model calls at the boundary and retain pure, testable transformations inside:

```clojure
(defn normalize [text]
  (-> text
      clojure.string/trim
      (clojure.string/replace #"\s+" " ")))

(defn document->request [doc]
  {:id (:id doc)
   :text (normalize (:body doc))
   :schema {:entities [:person :location :resource :time]
            :relations [:assigned-to :located-at :responded-to]}})

(defn accept-extraction [doc result]
  (assoc doc
         :nlp/extraction result
         :nlp/model (:model result)
         :nlp/confidence (:confidence result)))
```

Then make the impure adapter responsible for calling spaCy, OpenNLP, or an LLM, while schema validation, deduplication, domain rules, and fact derivation remain pure.

## Mental model

Think of NLP as four separate capabilities:

1. **Understand surface form:** tokens, syntax, entities, text spans.
2. **Represent meaning approximately:** embeddings, classifiers, language models.
3. **Ground against trusted state:** documents, databases, knowledge graphs, tools, policies.
4. **Constrain and verify outputs:** schemas, rules, confidence checks, citations, and humans for consequential decisions.

The mistake to avoid is treating fluent output as verified understanding. In a robust system, language-model output is a probabilistic proposal; the data model, retrieval layer, rules, and evaluation suite decide whether it becomes an accepted fact.

## References

- [clojure-opennlp](https://github.com/dakrone/clojure-opennlp)
- [NLP in Clojure session 1](https://clojureverse.org/t/nlp-in-clojure-session-1-summary-recording/9347)
- [core.nlp](https://arnaudsj.github.io/core.nlp/)
- [Clojure NLP Reddit discussion](https://www.reddit.com/r/Clojure/comments/juumit/nlp_and_clojure/)
- [Clojure AI book (Watson)](https://markwatson.com/opencontent/clojureai.pdf)
