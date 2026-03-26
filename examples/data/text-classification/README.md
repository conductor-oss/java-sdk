# Text Classification

A content moderation system receives user-generated text that needs categorization. Each post requires preprocessing (tokenization, stop-word removal), feature extraction (word frequencies, text length metrics), classification into a category, and confidence scoring so low-confidence predictions get routed to human review.

## Pipeline

```
[txc_preprocess]
     |
     v
[txc_extract_features]
     |
     v
[txc_classify]
     |
     v
[txc_confidence]
```

**Workflow inputs:** `text`, `categories`

## Workers

**ClassifyWorker** (task: `txc_classify`)

- Writes `prediction`, `scores`

**ConfidenceWorker** (task: `txc_confidence`)

- Clamps with `math.max()`, formats output strings, filters with predicates, maps to primitives for aggregation
- Writes `confidence`, `margin`, `reliable`

**ExtractFeaturesWorker** (task: `txc_extract_features`)

- Clamps with `math.max()`, matches against regex, formats output strings
- Writes `features`

**PreprocessWorker** (task: `txc_preprocess`)

- Lowercases strings, trims whitespace
- Writes `cleanedText`, `tokenCount`

---

**8 tests** | Workflow: `txc_text_classification` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
