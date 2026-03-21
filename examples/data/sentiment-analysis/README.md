# Sentiment Analysis in Java with Conductor : Preprocess, Analyze, Classify, and Aggregate Customer Sentiment

## Understanding How Customers Feel at Scale

One angry email is anecdotal. A thousand support tickets with declining sentiment is a trend. Analyzing customer sentiment manually does not scale. you need to preprocess the text (remove noise, normalize formatting), score sentiment for each piece of text, classify it into actionable categories, and aggregate the results to spot trends. Individual scores are useful for routing (flag negative feedback for urgent follow-up), but the aggregate view reveals whether satisfaction is improving or declining over time.

This workflow processes a batch of customer texts through the sentiment pipeline. The preprocessor cleans and normalizes the text (removing HTML, fixing encoding, normalizing case). The analyzer scores each text for sentiment polarity and intensity. The classifier maps scores to categories (positive, negative, neutral) with confidence levels. The aggregator computes batch-level metrics. average sentiment, distribution across categories, and trend indicators.

## The Solution

**You just write the preprocessing, scoring, classification, and aggregation workers. Conductor handles the sentiment pipeline and batch data flow.**

Four workers form the sentiment pipeline. preprocessing, analysis, classification, and aggregation. The preprocessor cleans raw text. The analyzer scores sentiment. The classifier labels each text. The aggregator computes batch metrics and trends. Conductor sequences the four steps and passes cleaned text, scores, classifications, and aggregate metrics between them via JSONPath.

### What You Write: Workers

PreprocessWorker cleans and normalizes text, AnalyzeWorker scores sentiment polarity, ClassifyWorker labels each text as positive/negative/neutral, and AggregateWorker computes batch-level metrics and trend indicators.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `snt_aggregate` | Computes batch-level sentiment metrics: overall polarity, positive/negative/neutral distribution, and trend indicators. |
| **AnalyzeWorker** | `snt_analyze` | Scores each text for sentiment polarity and intensity, producing per-text sentiment values. |
| **ClassifyWorker** | `snt_classify` | Maps sentiment scores to labels (positive, negative, neutral) with confidence levels. |
| **PreprocessWorker** | `snt_preprocess` | Cleans and normalizes raw text: lowercasing, stopword removal, and encoding normalization. |

### The Workflow

```
snt_preprocess
 │
 ▼
snt_analyze
 │
 ▼
snt_classify
 │
 ▼
snt_aggregate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
