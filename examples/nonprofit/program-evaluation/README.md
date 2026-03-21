# Program Evaluation in Java with Conductor

## The Problem

Your nonprofit needs to evaluate a community outreach program to decide whether to expand, modify, or sunset it. The evaluation team must define the KPIs and outcome metrics, collect data across reach, outcomes, cost, and satisfaction dimensions, analyze program performance by scoring each metric, benchmark the results against sector averages, and generate actionable recommendations. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the data gathering, outcome measurement, effectiveness analysis, and recommendation generation logic. Conductor handles data gathering retries, scoring sequencing, and evaluation audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Data gathering, metric analysis, effectiveness scoring, and recommendation generation workers each assess one dimension of program performance.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeWorker** | `pev_analyze` | Scores program performance across reach, outcomes, efficiency, and satisfaction dimensions |
| **BenchmarkWorker** | `pev_benchmark` | Compares the program's scores against sector averages and top-quartile thresholds, returning a ranking |
| **CollectWorker** | `pev_collect` | Collects evaluation data for the specified period: reach count, outcome percentage, cost per unit, and satisfaction score |
| **DefineMetricsWorker** | `pev_define_metrics` | Defines the evaluation framework with KPIs: reach, outcomes, cost-effectiveness, and satisfaction |
| **RecommendWorker** | `pev_recommend` | Generates actionable recommendations (e.g., scale reach, optimize costs) based on the overall evaluation score and ranking |

### The Workflow

```
pev_define_metrics
 │
 ▼
pev_collect
 │
 ▼
pev_analyze
 │
 ▼
pev_benchmark
 │
 ▼
pev_recommend

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
