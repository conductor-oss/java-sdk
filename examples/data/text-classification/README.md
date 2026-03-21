# Text Classification in Java with Conductor

## Sorting Text into Categories Accurately

Incoming text. support tickets, emails, chat messages, needs to be routed to the right team or tagged with the right label. Manual tagging does not scale, and a single-step classifier misses nuance. Reliable classification requires preprocessing (cleaning noise, normalizing formatting), extracting meaningful features, predicting the category, and then scoring the prediction's confidence so low-confidence results can be flagged for human review.

This workflow classifies a single piece of text. The preprocessor cleans and normalizes the input. The feature extractor produces n-grams, keyword counts, and statistical features. The classifier predicts the most likely category from the feature vector. The confidence scorer evaluates the prediction's reliability by checking the margin between the top category and runner-up. Each step's output feeds the next. cleaned text feeds feature extraction, features feed classification, and the classification feeds confidence scoring.

## The Solution

**You just write the preprocessing, feature-extraction, classification, and confidence-scoring workers. Conductor handles the classification pipeline and feature flow.**

Each worker handles one CRM operation. Conductor manages the customer lifecycle pipeline, assignment routing, follow-up scheduling, and activity tracking.

### What You Write: Workers

PreprocessWorker cleans raw input, ExtractFeaturesWorker produces n-grams and keyword counts, ClassifyWorker predicts the category, and ConfidenceWorker flags low-confidence results for human review.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyWorker** | `txc_classify` | Predicts the most likely category from the feature vector (e.g., "technology"). |
| **ConfidenceWorker** | `txc_confidence` | Scores prediction confidence by computing the margin between the top and runner-up categories; flags low-confidence results. |
| **ExtractFeaturesWorker** | `txc_extract_features` | Extracts n-grams, keyword counts, and statistical features from the cleaned text. |
| **PreprocessWorker** | `txc_preprocess` | Cleans raw text by normalizing whitespace, lowercasing, and removing noise characters. |

### The Workflow

```
txc_preprocess
 │
 ▼
txc_extract_features
 │
 ▼
txc_classify
 │
 ▼
txc_confidence

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
