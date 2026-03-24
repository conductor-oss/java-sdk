# Data Sampling in Java Using Conductor : Sample Drawing, Quality Checks, and Conditional Approval Routing

## The Problem

You have a large incoming dataset, a vendor file, an ETL output, a batch import, and you need to decide whether it's good enough to accept before loading it into your production systems. Checking every record is too expensive, so you sample. That means drawing a statistically representative subset at a configurable sample rate, running quality checks on the sample (completeness, format validity, consistency), and making a pass/fail decision based on a threshold. Datasets that pass get approved for loading; datasets that fail get routed to a review queue with a list of specific issues found.

Without orchestration, you'd write a single script that samples, checks, and decides inline. There's no visibility into why a dataset was rejected. Was it the sample quality or a bug in the check logic? If the quality check fails transiently, you'd manually re-run the entire pipeline. Changing the sample rate or threshold means modifying code, and there's no audit trail of which datasets were approved, which were flagged, and what their quality scores were.

## The Solution

**You just write the dataset loading, sample drawing, quality checking, approval, and review workers. Conductor handles conditional routing via SWITCH based on quality scores, retries on transient check failures, and a complete audit trail of every dataset's sample score and routing decision.**

Each stage is a simple, independent worker. The loader reads the incoming dataset. The sampler draws a deterministic subset at the configured sample rate. The quality checker scores the sample against completeness and validity rules. Conductor's `SWITCH` task then routes based on the result: datasets meeting the quality threshold go to the approval worker, while those falling short go to the review flagger with a list of specific issues. Conductor tracks the quality score, sample size, and routing decision for every dataset, retries if a check fails transiently, and provides a complete audit trail.

### What You Write: Workers

Five workers implement sample-based quality gating: loading the dataset, drawing a representative sample at a configurable rate, running quality checks, and then routing to either approval or manual review based on the quality score via a SWITCH.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveDatasetWorker** | `sm_approve_dataset` | Approves a dataset that passed quality checks. |
| **DrawSampleWorker** | `sm_draw_sample` | Draws a deterministic sample from the dataset. |
| **FlagForReviewWorker** | `sm_flag_for_review` | Flags a dataset for review when quality checks fail. |
| **LoadDatasetWorker** | `sm_load_dataset` | Loads a dataset from the input records. |
| **RunQualityChecksWorker** | `sm_run_quality_checks` | Runs quality checks on the sampled data. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
sm_load_dataset
 │
 ▼
sm_draw_sample
 │
 ▼
sm_run_quality_checks
 │
 ▼
SWITCH (decision_switch_ref)
 ├── pass: sm_approve_dataset
 └── default: sm_flag_for_review

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
