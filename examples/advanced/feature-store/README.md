# Feature Store Pipeline in Java Using Conductor : Compute, Validate, Register, Serve

## Features Rot Without a Pipeline

ML models depend on features. user_lifetime_value, avg_session_duration, days_since_last_purchase, that are computed from raw data. When a data scientist computes features in a notebook, they work for that one training run. But serving those same features in production requires computing them on a schedule, validating that distributions haven't drifted, registering the new version in a catalog so models know where to find them, and enabling low-latency serving for real-time inference.

Without a pipeline, feature computation lives in ad-hoc scripts, validation is skipped, and the registry is a spreadsheet. The model in production reads stale features because nobody updated the serving layer, or worse, serves features computed with a bug that validation would have caught.

## The Solution

**You write the feature computation and validation logic. Conductor handles the registry pipeline, retries, and serving coordination.**

`FstComputeFeaturesWorker` reads the source table, computes the feature group by entity key, and produces feature vectors with statistics (mean, stddev, null rates). `FstValidateWorker` checks the computed features against quality constraints. no null rates above threshold, distributions within expected ranges, schema matches the registered definition. `FstRegisterWorker` writes the validated features to the feature registry with a version number. `FstServeWorker` enables the registered feature group for online serving so models can query features by entity key in real time. Conductor ensures features are never served before validation passes, and records the version, validation results, and serving status for every run.

### What You Write: Workers

Four workers manage the feature lifecycle: computation from raw data, quality validation, registry registration, and online serving enablement, ensuring features are never served before validation passes.

### The Workflow

```
fst_compute_features
 │
 ▼
fst_validate
 │
 ▼
fst_register
 │
 ▼
fst_serve

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
