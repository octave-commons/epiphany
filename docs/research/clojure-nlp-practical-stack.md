---
title: Clojure NLP Practical Stack
slug: clojure-nlp-practical-stack
created: 2026-07-11
source: docs/inbox/2026.07.11.14.35.24.md
kind: research
labels: ['nlp', 'clojure', 'practical-stack', 'opennlp', 'spacy']
---

# Summary

Practical Clojure NLP options, from OpenNLP and CoreNLP to spaCy via libpython-clj, plus a snippet on keeping model calls at the boundary and transformations pure.

# clojure Natural Language Processing

Clojure is a strong **orchestration language for NLP**: use its immutable data transformations and JVM/Python interoperability to compose pipelines, while delegating model-heavy tasks to mature Java or Python libraries. Existing options include OpenNLP bindings, Stanford CoreNLP wrappers, and spaCy through Python interop.[^1_1][^1_2][^1_3]

## Practical stack choices

| Need | Recommended approach | Why |
| :-- | :-- | :-- |
| Tokenization, sentence splitting, POS tagging, NER | Java OpenNLP via `clojure-opennlp` | Direct JVM integration; useful for conventional statistical NLP pipelines. [^1_1] |
| Parsing, dependencies, coreference | Stanford CoreNLP through JVM interop | Broad classical NLP pipeline, though model loading and runtime can be substantial. [^1_4] |
| Modern production NLP | spaCy via `libpython-clj` | Access to spaCy’s pretrained pipelines while retaining Clojure for dataflow and application logic. [^1_2] |
| Small, composable text transformations | `core.nlp` ideas or plain Clojure functions | Fits Clojure’s sequence/transducer style for normalization, tokenization, and feature extraction. [^1_3] |
| LLM embeddings, extraction, retrieval | Call an embedding/LLM service or JVM/Python model runtime | Clojure handles document flow, state, caching, schemas, and retrieval well; model inference stays external. |

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

(term-frequencies
  (tokenize "Clojure makes text pipelines pleasant; Clojure is composable."))
;; => {"clojure" 2, "makes" 1, ...}
```

This style is especially useful for deterministic stages: Unicode cleanup, language-specific normalization, stopword filtering, n-gram generation, document metadata enrichment, and converting inference results into domain facts.

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

For your interest in Datalog and systems architecture, a useful pattern is:

- Store each document, chunk, span, entity, relation, and inference output as immutable facts.
- Give model outputs provenance: model version, prompt or configuration, source offsets, timestamp, confidence, and input-content hash.
- Use Datalog to query derived knowledge, such as “incidents mentioning a location, resource type, and urgency signal.”
- Keep the model adapter impure and narrow; make all downstream validation, transformations, and business rules pure.

That structure helps with reprocessing after a model change, auditability, replay, and testing—qualities that matter more than raw model choice in serious systems.

## Modern recommendation

For a new Clojure NLP project, I would generally choose **Clojure + `libpython-clj` + spaCy** for linguistic annotations, or **Clojure + an embeddings/LLM API** for semantic search, classification, and extraction. A Clojure community session specifically demonstrated spaCy being used from Clojure through `libpython-clj`, illustrating the pragmatic interop route.[^1_2]

Use OpenNLP or CoreNLP when you need local JVM-native deployment, conventional linguistic annotations, or a minimal external-runtime footprint; `clojure-opennlp` exists as a Clojure interface to Apache OpenNLP.  `core.nlp` is also worth studying for API design, since it aims to make NLP idiomatic within Clojure and demonstrates a simple `tokenize` interface.[^1_3][^1_1]
<span style="display:none">[^1_10][^1_5][^1_6][^1_7][^1_8][^1_9]</span>

<div align="center">⁂</div>

[^1_1]: https://github.com/dakrone/clojure-opennlp

[^1_2]: https://clojureverse.org/t/nlp-in-clojure-session-1-summary-recording/9347

[^1_3]: https://arnaudsj.github.io/core.nlp/

[^1_4]: https://www.reddit.com/r/Clojure/comments/juumit/nlp_and_clojure/

[^1_5]: https://www.youtube.com/watch?v=u9bmA1r2H1s

[^1_6]: https://en.wikipedia.org/wiki/Clojure

[^1_7]: https://www.youtube.com/watch?v=X3Ilr74o6KQ

[^1_8]: https://github.com/Jah524/prism

[^1_9]: https://github.com/plandes/clj-nlp-parse

[^1_10]: https://markwatson.com/opencontent/clojureai.pdf
---
## A Clojure architecture

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

Then make the impure adapter responsible for calling spaCy, OpenNLP, or an LLM, while schema validation, deduplication, domain rules, and fact derivation remain pure. spaCy can be used from Clojure using Python interop through `libpython-clj`; that is a practical route when Clojure owns application architecture but Python owns a mature NLP runtime.[^2_5]

[^2_5]: https://clojureverse.org/t/nlp-in-clojure-session-1-summary-recording/9347
