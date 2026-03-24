# Travel Analytics in Java with Conductor

Travel analytics: collect, aggregate, analyze, report.

## The Problem

You need to generate a travel analytics report for a department and time period. Collecting all booking, expense, and reimbursement data, aggregating spending across categories (flights, hotels, car rentals, meals), analyzing trends to identify cost-saving opportunities (preferred vendor compliance, advance booking rates, policy exception frequency), and producing a report for management. Each transformation depends on the previous one's output.

If aggregation miscounts hotel expenses as meals, the category breakdown is wrong and management makes decisions based on incorrect data. If the analysis step finds cost-saving opportunities but the report generation fails, those insights never reach the people who can act on them. Without orchestration, you'd build a batch analytics script that mixes data collection queries, aggregation logic, trend analysis, and report formatting. Making it impossible to add new data sources, test analysis algorithms independently, or schedule reports on different cadences for different departments.

## The Solution

**You just write the data collection, spending aggregation, trend analysis, and report generation logic. Conductor handles data aggregation retries, trend analysis, and travel spend audit trails.**

CollectWorker gathers travel data for the specified department and period. Bookings, expenses, reimbursements, and policy exceptions. AggregateWorker groups the data by category (airfare, lodging, ground transport, meals) and computes totals, averages, and per-trip costs. AnalyzeWorker identifies cost-saving opportunities by comparing actual spending against negotiated rates, measuring advance booking compliance, and flagging departments with high exception rates. ReportWorker generates the analytics dashboard with charts, trend lines, and actionable recommendations for management. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

Data aggregation, spend analysis, trend identification, and report generation workers each process one dimension of corporate travel intelligence.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `tan_aggregate` | Aggregated bookings across categories |
| **AnalyzeWorker** | `tan_analyze` | Identified 3 cost-saving opportunities |
| **CollectWorker** | `tan_collect` | Collects and validates and computes raw data |
| **ReportWorker** | `tan_report` | Analytics dashboard updated with new insights |

### The Workflow

```
tan_collect
 │
 ▼
tan_aggregate
 │
 ▼
tan_analyze
 │
 ▼
tan_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
