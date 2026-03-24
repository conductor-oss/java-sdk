# Implementing Optional Tasks in Java with Conductor : Required Processing with Non-Blocking Enrichment

## The Problem

Your pipeline has a required processing step and an optional enrichment step. The enrichment adds value (e.g., appending metadata, fetching supplementary data) but isn't critical. if it fails, the pipeline should continue with whatever data is available. The summarize step must handle both the enriched case and the degraded case where enrichment data is missing.

Without orchestration, optional tasks are implemented with try/catch blocks that swallow errors. The summarize step has no way to know whether enrichment was skipped due to a genuine failure or was never attempted. Testing the degraded path requires manually breaking the enrichment service.

## The Solution

**You just write the core processing and optional enrichment logic. Conductor handles marking tasks as optional, continuing the pipeline when optional tasks fail, and clear tracking of whether enrichment succeeded or was skipped for every execution.**

The required worker runs first. The optional enrichment worker is configured to continue on failure. Conductor marks it as optional. The summarize worker checks whether enrichment data is present and adapts its output accordingly. Every execution shows clearly whether enrichment succeeded or was skipped, and why.

### What You Write: Workers

RequiredWorker handles the core processing that must succeed, OptionalEnrichWorker adds supplementary data that can fail without blocking, and SummarizeWorker produces the final output while gracefully handling missing enrichment.

| Worker | Task | What It Does |
|---|---|---|
| **OptionalEnrichWorker** | `opt_optional_enrich` | Worker for opt_optional_enrich. enriches data with additional information. This task is marked as optional in the wo.. |
| **RequiredWorker** | `opt_required` | Worker for opt_required. processes the input data. Returns { result: "processed-{data}" } on success. |
| **SummarizeWorker** | `opt_summarize` | Worker for opt_summarize. summarizes the workflow results. Checks whether the enrichment data is available. If enric.. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
opt_required
 │
 ▼
opt_optional_enrich
 │
 ▼
opt_summarize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
