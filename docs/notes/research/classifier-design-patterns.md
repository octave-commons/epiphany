---
title: Classifier Design Patterns
slug: classifier-design-patterns
created: 2026-07-11
source: docs/inbox/2026.07.11.14.35.24.md
kind: research
labels: ['classification', 'machine-learning', 'patterns', 'nlp']
---

# Summary

Current classifier design patterns: lexical, embedding, fine-tuned transformer, and LLM-as-classifier, with workflow and evaluation guidance.

# how are people building classifiers these days?

People increasingly build classifiers as **data-and-evaluation systems**, not as a one-time “pick an algorithm and train it” exercise. The model choice follows the data modality, label volume, latency/cost constraints, and the consequences of errors; for text, the usual options now range from a TF-IDF baseline to embeddings plus a small classifier, fine-tuned transformers, or an LLM constrained to a label schema.[^3_1][^3_2][^3_3]

## First, define the decision

A classifier maps an input $x$ to one or more labels $y$, often along with a calibrated probability or score:

$$
f(x) \rightarrow \{(label, probability)\}
$$

The key design work happens before training:

- **Specify labels operationally.** “Urgent,” for example, should have decision criteria, inclusion/exclusion rules, and edge-case examples.
- **Choose the label shape.** A single-label classifier chooses exactly one class; multi-class chooses one among several classes; multi-label may assign several independent tags; hierarchical labels allow outputs such as `:incident > :medical > :cardiac`.
- **Specify action thresholds.** “Auto-route when confidence is high; otherwise send to review” is usually a more realistic product contract than “always predict a label.”
- **Preserve an `:unknown`, `:other`, or abstention path.** Real inputs eventually fall outside the taxonomy.


## The current toolbox

| Situation | Common approach | Why it remains useful |
| :-- | :-- | :-- |
| Tabular or structured records | Gradient-boosted trees such as XGBoost or LightGBM, with logistic regression as a baseline | Boosted trees are a standard first choice for structured/tabular classification; they capture nonlinear interactions with relatively modest data. [^3_1] |
| Small or medium text dataset; need transparency | TF-IDF or n-grams plus logistic regression / linear SVM | Fast, cheap, inspectable, and often surprisingly competitive when labels correlate with vocabulary. Logistic regression gives probabilities; linear model weights reveal influential terms. [^3_4][^3_5] |
| Limited labeled examples | Sentence embeddings plus logistic regression, nearest-neighbor, or SetFit | Reuses semantic knowledge in a pretrained embedding model; SetFit fine-tunes a sentence transformer and then trains a classification head. [^3_6][^3_7] |
| Lots of high-quality domain labels; accuracy matters | Fine-tune a pretrained encoder such as BERT/DistilBERT | Fine-tuning continues training pretrained weights on task/domain data and is the canonical transformer workflow for sequence classification. [^3_2][^3_3] |
| Few labels, complicated policy, rapidly changing taxonomy | LLM classification with JSON/schema-constrained output, often with retrieval | Fast iteration and good semantic handling, but needs a strong evaluation set, versioning, and validation because output may vary. |
| Images, audio, multimodal inputs | Fine-tune an appropriate vision, audio, or multimodal model | The representation must match the modality; generic text approaches do not transfer automatically. [^3_1] |

The unglamorous baseline matters. If TF-IDF plus logistic regression meets the error, latency, privacy, and explainability requirements, it can be the right production solution—not merely a prototype.

## Text classifier patterns

### Lexical classifier

Represent each document as sparse features: token, word n-gram, character n-gram, document length, source, or structured metadata. Train a linear classifier on those features.

This works well for categories with stable language, such as routing support tickets, spam signals, product categories, moderation keywords, or command types. It struggles with paraphrase and new wording: “my card won’t work” and “payment method declined” may mean the same thing but share little vocabulary.

### Embedding classifier

Embed each text into a dense vector, then learn a small boundary over the vector space:

```text
text -> embedding vector -> logistic regression -> labels/scores
```

This is a highly practical modern default when you have tens to a few thousand examples per category, semantic variation matters, and you want inexpensive retraining. Few-shot methods such as SetFit explicitly use contrastive adaptation of a sentence-transformer followed by a classification head, targeting good results with little labeled data.[^3_6][^3_7]

### Fine-tuned transformer

Use a pretrained encoder, attach a classification head, and train it on labeled examples:

```text
text -> tokenizer -> pretrained transformer -> classifier head -> labels
```

Fine-tuning is not training from scratch: it starts from pretrained weights and updates them using task-specific data, normally requiring far less compute, time, and data than pretraining.  It is useful when language nuance is central, you have a stable labeled corpus, and inference infrastructure is justified.[^3_3]

### LLM-as-classifier

Prompt a language model with label definitions, examples, and an output schema:

```json
{
  "labels": ["medical", "fire", "traffic", "other"],
  "confidence": 0.0,
  "rationale": "short evidence-grounded reason"
}
```

Use it for bootstrapping labels, ambiguous long-form inputs, evolving taxonomies, or cases requiring a little contextual interpretation. Do not let its prose explanation substitute for correctness: validate the JSON/schema, log the exact prompt and model version, and use confidence thresholds plus a review queue.

## The actual workflow

1. **Collect representative examples.** Include mundane cases, rare but expensive failures, adversarial inputs, historical drift, different sources, and out-of-scope examples.
2. **Write a labeling guide.** Define every class, provide positive and negative examples, and decide how annotators handle uncertainty and multiple applicable labels.
3. **Create a fixed test set early.** Split by a meaningful boundary—time, customer, incident, source system, or document family—to avoid leakage from near-duplicate train/test records.
4. **Train two baselines.** For text, start with TF-IDF + logistic regression and embedding + logistic regression; compare them before committing to larger models.
5. **Inspect errors.** Cluster false positives and false negatives by cause: ambiguous labels, missing context, contradictory data, new vocabulary, long inputs, or a flawed taxonomy.
6. **Improve the data before tuning endlessly.** Add discriminating examples, merge indistinguishable classes, split overloaded classes, or add structured context.
7. **Calibrate scores and set policy thresholds.** The highest-scoring label is not always sufficiently certain for automation.
8. **Deploy with observability.** Store model/prompt version, feature or embedding version, score, selected label, input hash, and eventual human correction.
9. **Continuously evaluate drift.** Monitor label distribution, confidence distribution, review overrides, and task-specific precision/recall over time.

## Evaluation that matters

Accuracy can be misleading when classes are imbalanced. If 95% of incidents are routine, a classifier that always predicts “routine” is 95% accurate and operationally useless.

Use:

- **Per-class precision:** predicted `:hazmat` cases that were actually hazmat.
- **Per-class recall:** actual hazmat cases detected by the system.
- **F1:** a balance of precision and recall, $F1 = 2PR/(P+R)$.
- **Confusion matrix:** shows exactly which categories are being confused.
- **Precision at automation threshold:** among cases automatically acted on, how often is the system right?
- **Coverage:** what fraction receives an automatic decision rather than abstention/review.
- **Slice metrics:** evaluate by source, document length, language, time period, or sensitive operational subgroup.

For a routing system, a productive objective might be: maximize automated coverage while maintaining at least 99% precision on auto-routing. That explicitly preserves a human-review path rather than hiding uncertainty behind a forced label.

## What is changing

The main shift is not that classical classifiers disappeared. It is that pretrained representations dramatically lower the amount of bespoke feature engineering needed:

- **Classical models** still dominate many constrained, explainable, structured-data, and low-latency situations.
- **Embeddings** make semantic classifiers feasible with small datasets and inexpensive heads.
- **Fine-tuned models** are used when a task is stable enough to justify training and deployment.
- **LLMs** are often used to prototype taxonomies, generate candidate labels, handle hard tails, and assist annotators—not necessarily as the final high-volume classifier.
- **Hybrid systems** are common: deterministic rules catch policy-critical cases, a statistical or embedding model handles ordinary inputs, and uncertain cases go to an LLM or human review.


## Clojure-friendly boundary

For a Clojure system, make classification an explicitly versioned effect and keep policy pure:

```clojure
(defn classify-request [doc]
  {:document/id (:id doc)
   :text (:body doc)
   :labels [:medical :fire :traffic :other]
   :model/version "incident-embed-v12"})

(defn decide-route [{:keys [scores]}]
  (let [[label score] (apply max-key second scores)]
    (if (>= score 0.98)
      {:decision :auto-route :label label :confidence score}
      {:decision :review :confidence score})))
```

A Python/JVM/remote model adapter can produce the `:scores`; Clojure then validates the response, applies threshold policy, records provenance, emits domain events, and stores human corrections as future labeled training data. The model should be replaceable without changing the decision ledger or domain model.
<span style="display:none">[^3_10][^3_11][^3_12][^3_13][^3_14][^3_15][^3_16][^3_17][^3_18][^3_19][^3_20][^3_21][^3_22][^3_23][^3_24][^3_25][^3_8][^3_9]</span>

<div align="center">⁂</div>

[^3_1]: https://www.gyansetu.in/data-science/classification-in-machine-learning/

[^3_2]: https://huggingface.co/docs/transformers/en/tasks/sequence_classification

[^3_3]: https://huggingface.co/docs/transformers/en/training

[^3_4]: https://www.datacamp.com/blog/classification-machine-learning

[^3_5]: https://stackoverflow.com/questions/48401148/document-classification-with-scikit-learn-most-efficient-way-to-get-the-words

[^3_6]: https://huggingface.co/Mohamedsheded33/SetFit-few-shot-classification-sst2/blob/main/README.md

[^3_7]: https://github.com/huggingface/setfit

[^3_8]: https://www.cs.princeton.edu/courses/archive/fall04/cos557/Articles/schapire-slides.pdf

[^3_9]: https://www.bio-conferences.org/articles/bioconf/pdf/2024/16/bioconf_iscku2024_00133.pdf

[^3_10]: https://www.geeksforgeeks.org/machine-learning/top-machine-learning-algorithms-for-classification/

[^3_11]: https://www.quantitative-biology.ca/machine-learning-and-classification.html

[^3_12]: https://towardsdatascience.com/top-machine-learning-algorithms-for-classification-2197870ff501/

[^3_13]: https://www.mathworks.com/campaigns/offers/next/choosing-the-best-machine-learning-classification-model-and-avoiding-overfitting.html

[^3_14]: https://www.tutorialspoint.com/machine_learning/machine_learning_classification_algorithms.htm

[^3_15]: https://builtin.com/data-science/supervised-machine-learning-classification

[^3_16]: https://huggingface.co/transformers/v4.1.1/notebooks.html

[^3_17]: https://github.com/SMMousaviSP/huggingface_transformers_tutorial

[^3_18]: https://www.scribd.com/document/1001494748/Notes-on-Module-3-part-1-bow-tfidf-docx

[^3_19]: https://7c0h.com/blog/new/fine_tuned_text_classif.html

[^3_20]: https://github.com/huggingface/transformers/tree/main/examples/pytorch/text-classification

[^3_21]: https://github.com/philschmid/deep-learning-habana-huggingface/blob/master/fine-tuning-transformers/text-classification.ipynb

[^3_22]: https://huggingface.co/transformers/v3.2.0/custom_datasets.html

[^3_23]: https://medium.com/@rajratangulab.more/fine-tuning-bert-for-text-classification-using-hugging-face-transformers-685c132d185d

[^3_24]: https://github.com/geraltofrivia/fewshot-textclassification

[^3_25]: https://docs.v1.argilla.io/en/v1.18.0/tutorials/notebooks/training-textclassification-setfit-fewshot.html
