# Data Quality Checks in Java Using Conductor: Parallel Completeness, Accuracy, and Consistency Scoring

The executive dashboard shows 15% revenue growth this quarter. The CEO quotes it in the board meeting. Then a data engineer discovers that the pipeline has been double-counting records from one integration source for six weeks. duplicate IDs that slipped in when a vendor changed their API response format. The real growth is 7%. On top of that, 300 customer records have null email fields breaking the marketing segmentation, and half the timestamps from the EU data center are in UTC while the other half are in CET, inflating "active users" every evening. Nobody caught it because there were no automated quality checks. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate parallel data quality checks, completeness, accuracy, and consistency, with FORK_JOIN, automatic retries, and a graded quality report.

## The Problem

Before data enters your warehouse, analytics pipeline, or ML training set, you need to know if it's any good. That means checking three independent dimensions: completeness (are required fields like name, email, and ID populated?), accuracy (do emails have valid format? do status values match the allowed set?), and consistency (are IDs unique? do cross-field relationships hold?). These checks are independent of each other and can run simultaneously, but the quality report needs all three scores before it can compute an overall grade.

Without orchestration, you'd run all three checks sequentially in a single method, tripling the total check time. If you parallelize with thread pools, you'd manage `Future` objects, handle partial failures, and write join logic manually. Adding a new quality dimension (timeliness, uniqueness, validity) means modifying the parallelism and aggregation code every time.

## The Solution

**You just write the completeness, accuracy, consistency, and report generation workers. Conductor handles parallel check execution via FORK_JOIN, per-check retries, and automatic join-then-report sequencing so scores are always complete before grading.**

Each quality check is a simple, independent worker. The completeness checker counts filled required fields across all records. The accuracy checker validates email formats and status value enums. The consistency checker looks for duplicate IDs and cross-field contradictions. Conductor's `FORK_JOIN` runs all three checks simultaneously against the same dataset, waits for all to complete, and then the report generator computes a weighted overall score and letter grade. If one check fails, Conductor retries just that check. You get all of that, without writing a single line of thread pool or join logic.

### What You Write: Workers

Five workers run the quality assessment: loading records, then checking completeness, accuracy, and consistency in parallel via FORK_JOIN, and finally generating a quality report with scores and an overall letter grade.

| Worker | Task | What It Does |
|---|---|---|
| `LoadDataWorker` | `qc_load_data` | Accepts incoming records and passes them through with a count for downstream checks |
| `CheckCompletenessWorker` | `qc_check_completeness` | Checks four required fields (id, name, email, status) across all records; empty strings count as missing |
| `CheckAccuracyWorker` | `qc_check_accuracy` | Validates email format (must contain @ and .) and status values (must be active/inactive/pending) |
| `CheckConsistencyWorker` | `qc_check_consistency` | Detects duplicate IDs by comparing unique ID count to total ID count |
| `GenerateReportWorker` | `qc_generate_report` | Averages the three check scores and assigns a letter grade (A/B/C/D) |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks, the pipeline structure and error handling stay the same.

### The Workflow

```
qc_load_data
 │
 ▼
FORK_JOIN
 ├── qc_check_completeness
 ├── qc_check_accuracy
 └── qc_check_consistency
 │
 ▼
JOIN (wait for all branches)
qc_generate_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
