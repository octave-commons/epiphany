---
title: NLP Fundamentals and Text Classifier Patterns
slug: nlp-fundamentals-and-classifiers
created: 2026-07-11
source: docs/inbox/clojure Natural Language Processing.md
kind: research
---

# NLP Fundamentals and Text Classifier Patterns

Natural language processing (NLP) turns human language into structured representations that software can search, reason over, classify, generate, or act upon. Common tasks include classification, question answering, generation, named-entity recognition, translation, and summarization.

## The core problem

Language is contextual, ambiguous, compositional, and grounded in real-world situations. A useful NLP system must preserve the original input and attach interpretations with confidence, provenance, offsets, and model version rather than replacing the text with one supposedly certain meaning.

## The classic pipeline

1. **Normalization** — Unicode handling, case-folding, whitespace cleanup, format normalization.
2. **Segmentation** — documents into sentences and sentences into tokens.
3. **Morphology** — identify word forms sharing a lemma.
4. **Part-of-speech tagging** — label grammatical roles.
5. **Syntactic parsing** — phrase structure or dependency relations.
6. **Named-entity recognition (NER)** — typed spans such as people, places, organizations, incidents, resources, addresses.
7. **Relation/event extraction** — facts and event frames with roles.
8. **Coreference resolution** — resolve pronouns and references.

## Representations

| Representation | Basic idea | Best use | Main limitation |
|---|---|---|---|
| Tokens | Text split into units | Rules, indexing, parsing | Meaning is not represented |
| Bag of words / TF-IDF | Token counts weighted by rarity | Search, classification baselines | Ignores word order and context |
| N-grams | Counts of adjacent token sequences | Short-text classification | Sparse; weak generalization |
| Static embeddings | Each word receives one learned vector | Similarity, lightweight models | One vector across meanings |
| Contextual embeddings | Each token/document vector depends on context | Retrieval, semantic classification | Costlier and less interpretable |
| Language-model tokens | Subword units predicted from context | Generation, extraction, reasoning | Can be brittle or hallucinate |

Cosine similarity is commonly used to estimate semantic similarity between vectors:

```text
cosine(a, b) = (a · b) / (||a|| ||b||)
```

## Statistical to transformer NLP

The field’s practical evolution:

1. Rules and grammars — precise but brittle.
2. Feature-based ML — TF-IDF, POS tags, logistic regression, CRFs.
3. Word embeddings and sequence models — RNNs/LSTMs for better context.
4. Transformers — attention connects tokens to relevant context.
5. Foundation-model workflows — prompting, constrained outputs, retrieval augmentation, fine-tuning, tool calling.

## Key model objectives

| Task | Input -> output | Example |
|---|---|---|
| Classification | Document -> label(s) | Incident note -> `:medical` |
| Sequence labeling | Token sequence -> token labels | “Minneapolis” -> `:location` |
| Extraction | Document -> schema | Report -> `{:location ..., :hazards [...]}` |
| Retrieval | Query -> ranked documents/chunks | “bridge closure” -> relevant reports |
| Summarization | Long text -> shorter text | Shift log -> briefing |
| Question answering | Question + context -> answer | “Which unit arrived first?” |
| Translation | Language A -> language B | English -> Somali |
| Generation | Prompt/context -> new text | Draft an incident update |

## Retrieval-augmented generation (RAG)

RAG grounds a generative model in selected external information:

1. Split source material into semantically coherent chunks.
2. Encode chunks as vectors and store them with metadata.
3. Embed the query and retrieve the most relevant chunks (vector + keyword).
4. Give only retrieved evidence to the language model.
5. Require citations or source IDs in the generated answer.
6. Validate whether the answer is supported by the retrieved material.

RAG is a read model: the vector index accelerates approximate semantic lookup, but canonical documents and access controls remain the source of truth.

## Evaluation

Evaluate properties at each boundary:

- **Precision**
- **Recall**
- **F1 = 2PR / (P + R)**
- **Exact match** for constrained extraction
- **Ranking metrics** — Recall@k, MRR, nDCG
- **Calibration** — whether stated confidence matches accuracy
- **Human/domain review** for usefulness, failure modes, ambiguity, and language coverage

## Text classifier patterns

A classifier maps input `x` to one or more labels `y` with probabilities or scores.

Key design work before training:

- Specify labels operationally.
- Choose label shape (single-label, multi-class, multi-label, hierarchical).
- Specify action thresholds and abstention paths.

### Current toolbox

| Situation | Common approach | Why it remains useful |
|---|---|---|
| Tabular/structured records | Gradient-boosted trees (XGBoost, LightGBM) | Standard first choice for structured/tabular classification |
| Small/medium text; need transparency | TF-IDF or n-grams + logistic regression / linear SVM | Fast, cheap, inspectable, competitive when labels correlate with vocabulary |
| Limited labeled examples | Sentence embeddings + logistic regression / nearest-neighbor / SetFit | Reuses semantic knowledge in pretrained embeddings |
| Lots of high-quality domain labels | Fine-tune BERT/DistilBERT | Canonical transformer workflow for sequence classification |
| Few labels, complex policy, changing taxonomy | LLM classification with JSON/schema-constrained output | Fast iteration, but needs evaluation and validation |
| Images, audio, multimodal | Fine-tune appropriate vision/audio/multimodal model | Representation must match modality |

### Patterns

1. **Lexical classifier** — sparse token/n-gram features plus linear classifier.
2. **Embedding classifier** — text -> embedding vector -> small classifier.
3. **Fine-tuned transformer** — pretrained encoder + classification head.
4. **LLM-as-classifier** — prompt with label definitions, examples, and output schema.

### Actual workflow

1. Collect representative examples.
2. Write a labeling guide.
3. Create a fixed test set early.
4. Train two baselines (TF-IDF + logistic regression and embedding + logistic regression).
5. Inspect errors and cluster by cause.
6. Improve data before tuning models.
7. Calibrate scores and set policy thresholds.
8. Deploy with observability.
9. Continuously evaluate drift.

### Evaluation that matters

- Per-class precision and recall
- F1
- Confusion matrix
- Precision at automation threshold
- Coverage
- Slice metrics by source, length, language, time period, or operational subgroup

A productive objective might be: maximize automated coverage while maintaining at least 99% precision on auto-routing, preserving a human-review path rather than hiding uncertainty behind a forced label.

## References

- [Advanced NLP course](https://app.aiplus.training/courses/advanced-NLP-2-modern-NLP-in-depth-from-theory-to-action)
- [deeplearning.ai NLP resources](https://www.deeplearning.ai/resources/natural-language-processing)
- [Hugging Face sequence classification](https://huggingface.co/docs/transformers/en/tasks/sequence_classification)
- [Hugging Face training](https://huggingface.co/docs/transformers/en/training)
- [SetFit](https://github.com/huggingface/setfit)
- [Mohamedsheded33 SetFit example](https://huggingface.co/Mohamedsheded33/SetFit-few-shot-classification-sst2/blob/main/README.md)
- [DataCamp classification blog](https://www.datacamp.com/blog/classification-machine-learning)
- [scikit-learn document classification](https://stackoverflow.com/questions/48401148/document-classification-with-scikit-learn-most-efficient-way-to-get-the-words)
