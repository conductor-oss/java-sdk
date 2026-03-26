# Report Generation

A finance team needs a monthly revenue report that pulls data from the transactions database, aggregates by product line, formats the numbers, and renders the output as a structured document. The report generation takes 20 minutes and used to be a single script; when it failed at the formatting step, the entire 15-minute data pull had to restart.

## Pipeline

```
[rg_query_data]
     |
     v
[rg_aggregate_results]
     |
     v
[rg_format_report]
     |
     v
[rg_distribute_report]
```

**Workflow inputs:** `reportType`, `dateRange`, `recipients`

## Workers

**AggregateResultsWorker** (task: `rg_aggregate_results`)

Aggregates raw data into summary metrics for reporting.

- Formats output strings
- Writes `aggregated`, `aggregationCount`

**DistributeReportWorker** (task: `rg_distribute_report`)

Distributes the generated report to recipients via email or Slack.

- Formats output strings
- Sets `status` = `"all_delivered"`
- Writes `recipientCount`, `status`, `deliveries`

**FormatReportWorker** (task: `rg_format_report`)

Formats aggregated data into a report document.

- Sets `format` = `"PDF"`
- Writes `report`, `format`, `reportUrl`, `pageCount`

**QueryDataWorker** (task: `rg_query_data`)

Queries raw data for report generation.

- Writes `data`, `recordCount`

---

**22 tests** | Workflow: `report_generation` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
