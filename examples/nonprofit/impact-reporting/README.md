# Impact Reporting in Java with Conductor

## The Problem

Your nonprofit needs to produce its annual impact report for donors and board members. The reporting team must collect raw data across all programs (beneficiaries served, events held, volunteer hours), aggregate the totals into organization-wide metrics, analyze year-over-year growth and cost-effectiveness, format the report with charts, metrics, and narrative sections, and publish the final report as a downloadable document. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the data collection, metric calculation, narrative generation, and report delivery logic. Conductor handles data collection retries, outcome measurement, and impact audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Data collection, outcome measurement, narrative generation, and report distribution workers each contribute one layer to demonstrating program impact.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `ipr_aggregate` | Aggregates program data into organization-wide totals: beneficiaries served, meals provided, volunteer hours |
| **AnalyzeWorker** | `ipr_analyze` | Analyzes impact metrics including year-over-year growth, cost per beneficiary, and satisfaction rate |
| **CollectDataWorker** | `ipr_collect_data` | Collects raw data for the program and report year: beneficiary count, event count, and volunteer count |
| **FormatWorker** | `ipr_format` | Formats the impact report with sections, charts, and narrative for stakeholder presentation |
| **PublishWorker** | `ipr_publish` | Publishes the final impact report to a public URL as a downloadable document |

### The Workflow

```
ipr_collect_data
 │
 ▼
ipr_aggregate
 │
 ▼
ipr_analyze
 │
 ▼
ipr_format
 │
 ▼
ipr_publish

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
