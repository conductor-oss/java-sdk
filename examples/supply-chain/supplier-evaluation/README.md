# Supplier Evaluation in Java with Conductor : Performance Data Collection, Scoring, Ranking, and Quarterly Reporting

## The Problem

You need to evaluate your raw-materials suppliers at the end of each quarter. Performance data must be collected from multiple sources. on-time delivery rates from the TMS, quality rejection rates from the QMS, cost variance from the ERP, and responsiveness scores from buyer surveys. Each supplier must be scored on a consistent rubric. Suppliers must be ranked within their category so procurement knows which to grow, maintain, or phase out. The final report must be ready for the quarterly business review.

Without orchestration, supplier data lives in four different systems. A procurement analyst manually pulls reports from each, copies numbers into a spreadsheet, and applies scoring formulas that differ from quarter to quarter because the spreadsheet template keeps changing. Rankings are subjective because some metrics are stale (last quarter's quality data) while others are current. The report is always late because data collection alone takes three days of manual work.

## The Solution

**You just write the evaluation workers. Performance data collection, scoring, ranking, and quarterly reporting. Conductor handles data source retries, scoring sequencing, and versioned quarterly records for trend analysis.**

Each stage of the supplier evaluation pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so all performance data is collected before scoring begins, scoring completes before ranking, and rankings feed the final report. If the quality data pull fails (QMS timeout), Conductor retries without re-pulling delivery data that was already collected. Every data snapshot, score calculation, ranking decision, and report generation is recorded for trend analysis across quarters.

### What You Write: Workers

Four workers power the quarterly review: CollectDataWorker pulls delivery, quality, cost, and responsiveness metrics, ScoreWorker applies a consistent rubric, RankWorker orders suppliers by composite score, and ReportWorker generates the performance summary.

| Worker | Task | What It Does |
|---|---|---|
| **CollectDataWorker** | `spe_collect_data` | Collects supplier performance data. on-time delivery, quality rejection rates, cost variance, and responsiveness. |
| **RankWorker** | `spe_rank` | Ranks suppliers against each other within the category based on composite scores. |
| **ReportWorker** | `spe_report` | Generates the quarterly supplier performance report for the business review. |
| **ScoreWorker** | `spe_score` | Scores each supplier on delivery, quality, cost, and responsiveness using a consistent rubric. |

### The Workflow

```
spe_collect_data
 │
 ▼
spe_score
 │
 ▼
spe_rank
 │
 ▼
spe_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
