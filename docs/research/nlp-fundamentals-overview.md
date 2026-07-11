---
title: NLP Fundamentals Overview
slug: nlp-fundamentals-overview
created: 2026-07-11
source: docs/inbox/2026.07.11.14.35.24.md
kind: research
labels: ['nlp', 'fundamentals', 'rag', 'embeddings', 'transformers']
---

# Summary

A refresher on NLP tasks, representations, evaluation, and the shift from classical pipelines to transformers and retrieval-augmented generation.

# Give me a refresher in NLP

Natural language processing (NLP) is the discipline of turning human language—written or spoken—into structured representations that software can search, reason over, classify, generate, or act upon. Common NLP tasks include classification, question answering, generation, named-entity recognition, translation, and summarization.[^2_1][^2_2]

## The core problem

Language is not merely text. It is **contextual, ambiguous, compositional, and grounded in a real-world situation**.

Consider:

> “Dispatch a unit to the bank.”

“Bank” could mean a financial institution or riverbank; “unit” depends on the operating domain; “dispatch” might be a request, a description, or an instruction. A useful NLP system must preserve the original input and attach interpretations with confidence, provenance, offsets, and model version—not replace the text with one supposedly certain meaning.

## The classic pipeline

Before neural models, NLP was commonly organized as a staged pipeline:

1. **Normalization** — Unicode handling, case-folding where appropriate, whitespace cleanup, spelling or format normalization.
2. **Segmentation** — Split documents into sentences and sentences into tokens.
3. **Morphology** — Identify word forms: “responded,” “responding,” and “responds” share a lemma, “respond.”
4. **Part-of-speech tagging** — Label grammatical roles such as noun, verb, adjective, and adposition.
5. **Syntactic parsing** — Infer phrase structure or dependency relations, such as the subject and object of “Medic 4 transported the patient.”
6. **Named-entity recognition (NER)** — Find typed spans such as people, places, organizations, times, incidents, resources, and addresses.
7. **Relation/event extraction** — Produce facts: `(:transported medic-4 patient-23 hospital-a)` or an event frame with roles.
8. **Coreference resolution** — Determine whether “it,” “they,” or “the vehicle” refers to an earlier entity.

These techniques remain useful, especially when you need observable intermediate artifacts, constrained output, local inference, and rule/model hybrids. The Clojure ecosystem includes JVM access to Apache OpenNLP through `clojure-opennlp`, and `core.nlp` was designed around making conventional NLP operations idiomatic in Clojure.[^2_3][^2_4]

## Representations

A model cannot directly operate on text; it needs a representation.


| Representation | Basic idea | Best use | Main limitation |
| :-- | :-- | :-- | :-- |
| Tokens | Text split into units such as words or subwords | Rules, indexing, parsing | Meaning is not represented |
| Bag of words / TF-IDF | Document as token counts, weighted by rarity | Search, spam/topic classification baselines | Ignores word order and context |
| N-grams | Counts of adjacent token sequences | Short-text classification, lexical signals | Sparse; weak generalization |
| Static embeddings | Each word receives one learned vector | Similarity, lightweight models | “Bank” has one vector across meanings |
| Contextual embeddings | Each token/document vector depends on context | Retrieval, semantic classification, clustering | Costlier and less directly interpretable |
| Language-model tokens | Subword units predicted from surrounding or prior text | Generation, extraction, reasoning-like workflows | Can be brittle or hallucinate |

For vector representations, semantic similarity is often estimated with cosine similarity:

$$
\operatorname{cosine}(a,b) =
\frac{a \cdot b}{\lVert a\rVert \lVert b\rVert}
$$

Vectors pointing in similar directions are treated as semantically related, which enables semantic retrieval: a query about “road closures” can find passages about “traffic blocked” even without lexical overlap.

## Statistical to transformer NLP

The field’s practical evolution is roughly:

- **Rules and grammars:** precise and explainable, but expensive to author and brittle against linguistic variation.
- **Feature-based ML:** manually engineered signals such as TF-IDF, capitalization, POS tags, plus models such as logistic regression or CRFs.
- **Word embeddings and sequence models:** learned features, then RNNs/LSTMs for sequences; better context handling but difficult long-range dependencies.
- **Transformers:** attention connects each token to relevant context, supporting pretrained models reused for many downstream tasks.
- **Foundation-model workflows:** pretrained language models used through prompting, constrained structured outputs, retrieval augmentation, fine-tuning, or tool calling.

Modern NLP curricula typically cover the path from one-hot/static word embeddings through RNNs and attention to Transformers, pretrained models, transfer learning, and tasks such as classification, NER, QA, generation, translation, and summarization.[^2_1]

## Key model objectives

Different tasks map input language to different output forms:


| Task | Input -> output | Example |
| :-- | :-- | :-- |
| Classification | Document -> label(s) | Incident note -> `:medical` |
| Sequence labeling | Token sequence -> token labels | “Minneapolis” -> `:location` |
| Extraction | Document -> schema | Report -> `{ :location ..., :hazards [...] }` |
| Retrieval | Query -> ranked documents/chunks | “bridge closure” -> relevant reports |
| Summarization | Long text -> shorter text | Shift log -> briefing |
| Question answering | Question + context -> answer | “Which unit arrived first?” |
| Translation | Language A -> language B | English -> Somali |
| Generation | Prompt/context -> new text | Draft an incident update |

A language model itself is principally trained to predict a next token—or, depending on its architecture, missing/related tokens. It becomes useful for many tasks because that training forces it to model many regularities of language; task performance still depends heavily on instructions, grounding data, evaluation, and output constraints.

## Retrieval-augmented generation

**RAG** grounds a generative model in selected external information:

1. Split source material into semantically coherent chunks.
2. Encode chunks as vectors and store them with metadata.
3. Embed the query and retrieve the most relevant chunks, usually combining vector and keyword search.
4. Give only retrieved evidence to the language model.
5. Require citations or source IDs in the generated answer.
6. Validate whether the answer is supported by the retrieved material.

In systems terms, RAG is a read model: the vector index accelerates approximate semantic lookup, but canonical documents and their access controls remain the source of truth. Retrieval reduces—but does not eliminate—unsupported output, so evaluate retrieval quality independently from answer quality.

## Evaluation

Do not evaluate “the NLP system” as a single blob. Evaluate the properties that matter at each boundary.

- **Precision:** of predicted items, how many were correct?
- **Recall:** of real items, how many did the system find?
- **F1:** harmonic balance of precision and recall: $F1 = 2PR/(P+R)$.
- **Exact match:** output matches the expected output exactly; appropriate for constrained extraction.
- **Ranking metrics:** Recall@k, MRR, and nDCG for retrieval.
- **Calibration:** whether a stated 80% confidence is correct approximately 80% of the time.
- **Human/domain review:** necessary for usefulness, harmful failure modes, ambiguity, and language coverage.

For entity extraction in emergency-response-style data, a false negative for a hazardous-material reference may be much more costly than a false positive. Choose thresholds and metrics from the decision’s cost structure, not a generic leaderboard score.

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

## Mental model

Think of NLP as four separate capabilities:

- **Understand surface form:** tokens, syntax, entities, text spans.
- **Represent meaning approximately:** embeddings, classifiers, language models.
- **Ground against trusted state:** documents, databases, knowledge graphs, tools, policies.
- **Constrain and verify outputs:** schemas, rules, confidence checks, citations, and humans for consequential decisions.

The mistake to avoid is treating fluent output as verified understanding. In a robust system, language-model output is a probabilistic proposal; your data model, retrieval layer, rules, and evaluation suite decide whether it becomes an accepted fact.
<span style="display:none">[^2_10][^2_11][^2_12][^2_13][^2_6][^2_7][^2_8][^2_9]</span>

<div align="center">⁂</div>

[^2_1]: https://app.aiplus.training/courses/advanced-NLP-2-modern-NLP-in-depth-from-theory-to-action

[^2_2]: https://www.deeplearning.ai/resources/natural-language-processing

[^2_3]: https://github.com/dakrone/clojure-opennlp

[^2_4]: https://arnaudsj.github.io/core.nlp/

[^2_5]: https://clojureverse.org/t/nlp-in-clojure-session-1-summary-recording/9347

[^2_6]: https://www.anilthomas.co/challenge-page/7c2b9bdf-f9e9-43db-be7f-d379ebee47fe

[^2_7]: https://github.com/Gladiator07/Natural-Language-Processing

[^2_8]: https://www.nlplanet.org/course-practical-nlp/00-ai-and-ml-refresher

[^2_9]: https://www.youtube.com/watch?v=xrtn9j-fJdk

[^2_10]: https://cs.usm.maine.edu/~behrooz.mansouri/courses/Slides_NLP_23/Natural%20Language%20Processing%20--%20Session%201%20-%20Introduction.pdf

[^2_11]: https://humanneuromanagement.com/training/nlp-refresher-training/

[^2_12]: https://cs.usm.maine.edu/~behrooz.mansouri/courses/Slides_NLP_23/Natural%20Language%20Processing%20--%20Session%202%20-%20Python%20Refresher.pdf

[^2_13]: https://die-nlp-ausbildung.de/der-nlp-refresher/
