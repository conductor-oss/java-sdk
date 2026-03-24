# Report Generation in Java Using Conductor : Data Querying, Aggregation, Formatting, and Distribution

## The Problem

Every Monday morning, the sales team needs a weekly revenue report. Every month-end, finance needs a P&L summary. Every quarter, the board needs a KPI dashboard export. Each report follows the same pattern: query the right data for the right date range, aggregate it into the metrics that audience cares about, format it into a presentable document (PDF, Excel, HTML), and deliver it to the right people. But the data query for a revenue report is different from a P&L query. The aggregation logic (sum revenue by region vs. compute gross margin by product line) depends on the report type. Formatting depends on the audience. And distribution might be email for finance, Slack for engineering, and a shared drive for the board.

Without orchestration, you'd write a cron job that queries, aggregates, formats, and emails in one script per report type. If the email server is down after you've already spent 5 minutes querying and formatting, the entire report generation fails with no retry. There's no record of how many records were queried, what the aggregated metrics looked like before formatting, or whether distribution actually succeeded. Adding a new report type means duplicating the entire script with slight modifications.

## The Solution

**You just write the data querying, aggregation, report formatting, and distribution workers. Conductor handles the query-aggregate-format-distribute sequence, retries when email servers or data sources are temporarily unavailable, and tracking of record counts and delivery status at every stage.**

Each stage of the report pipeline is a simple, independent worker. The data querier fetches raw records for the specified report type and date range, returning the data along with a record count. The aggregator computes summary metrics appropriate for the report type. Totals, averages, breakdowns by dimension, period-over-period comparisons, and counts the number of aggregations performed. The formatter renders the aggregated data into a report document (PDF, Excel, HTML) and produces a downloadable URL. The distributor delivers the finished report to the recipient list via the configured channel (email, Slack, shared drive). Conductor executes them in strict sequence, passes the evolving report between stages, retries if the email server is temporarily unavailable, and tracks record counts, aggregation counts, and delivery status at every stage.

### What You Write: Workers

Four workers implement the report pipeline: querying raw data for a report type and date range, aggregating results into summary metrics, formatting into a downloadable document (PDF, Excel, HTML), and distributing to recipients via email or Slack.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateResultsWorker** | `rg_aggregate_results` | Aggregates raw data into summary metrics for reporting. |
| **DistributeReportWorker** | `rg_distribute_report` | Distributes the generated report to recipients via email or Slack. |
| **FormatReportWorker** | `rg_format_report` | Formats aggregated data into a report document. |
| **QueryDataWorker** | `rg_query_data` | Queries raw data for report generation. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
rg_query_data
 │
 ▼
rg_aggregate_results
 │
 ▼
rg_format_report
 │
 ▼
rg_distribute_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
