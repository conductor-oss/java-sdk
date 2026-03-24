# Sentiment Analysis

A customer success team wants to triage support tickets by sentiment before a human reads them. Each ticket needs text preprocessing (lowercasing, punctuation removal), sentiment scoring on a -1.0 to +1.0 scale, and classification into negative/neutral/positive buckets so the angriest tickets get routed first.

## Pipeline

```
[snt_preprocess]
     |
     v
[snt_analyze]
     |
     v
[snt_classify]
     |
     v
[snt_aggregate]
```

**Workflow inputs:** `texts`, `source`

## Workers

**AggregateWorker** (task: `snt_aggregate`)

- Filters with predicates
- Reads `source`. Writes `overallSentiment`, `distribution`, `source`

**AnalyzeWorker** (task: `snt_analyze`)

- Writes `sentiments`

**ClassifyWorker** (task: `snt_classify`)

- Uses `math.abs()`
- Writes `classifications`

**PreprocessWorker** (task: `snt_preprocess`)

- Writes `cleanedTexts`, `processedCount`

---

**8 tests** | Workflow: `snt_sentiment_analysis` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
