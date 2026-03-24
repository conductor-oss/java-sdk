# Feature Engineering in Java Using Conductor : Feature Extraction, Transformation, Normalization, and Validation

## The Problem

Before your ML model can train, raw data needs to become features. That means extracting useful signals from raw fields (parsing dates into day-of-week, computing ratios from absolute values), transforming them (log transforms for skewed distributions, polynomial features for capturing non-linear relationships), normalizing to a consistent [0,1] range so gradient-based models converge properly, and validating that no feature has null values or falls outside the expected range. Each step depends on the previous one: you can't normalize features that haven't been transformed, and you can't validate until normalization is complete.

Without orchestration, you'd build a monolithic feature pipeline that extracts, transforms, normalizes, and validates in one pass. If the normalization step reveals a data issue, you'd re-run everything. There's no record of what the raw features looked like before transformation. Adding a new derived feature means modifying tightly coupled code with no visibility into which step is slow or producing unexpected distributions.

## The Solution

**You just write the feature extraction, transformation, normalization, and validation workers. Conductor handles the sequential feature pipeline, per-step retries, and tracking of feature counts and statistics at every stage for experiment reproducibility.**

Each stage of the feature pipeline is a simple, independent worker. The extractor computes raw features from source data. The transformer adds derived features (log, polynomial, ratio). The normalizer scales all features to [0,1] using min-max normalization. The validator checks for null values and confirms all features are within the expected range. Conductor executes them in sequence, passes the evolving feature matrix between steps, retries if a step fails, and tracks feature counts and statistics at every stage.

### What You Write: Workers

Four workers handle the ML feature pipeline: extracting raw features from source data, applying log/polynomial/ratio transformations, normalizing values to [0,1] via min-max scaling, and validating the final feature set for nulls and range compliance.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractFeaturesWorker** | `fe_extract_features` | Extracts raw features from input records. |
| **NormalizeFeaturesWorker** | `fe_normalize_features` | Min-max normalizes features to [0,1] range. |
| **TransformFeaturesWorker** | `fe_transform_features` | Transforms features by adding derived features (log, polynomial, ratio). |
| **ValidateFeaturesWorker** | `fe_validate_features` | Validates normalized features are in [0,1] range with no nulls. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
fe_extract_features
 │
 ▼
fe_transform_features
 │
 ▼
fe_normalize_features
 │
 ▼
fe_validate_features

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
