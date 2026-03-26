# Scheduled Reports

A periodic report needs generation and distribution. The pipeline queries data for the specified report type and date range, formats the report with the queried row count, and distributes it to the configured recipients.

## Workflow

```
sch_query_data ──> sch_format_report ──> sch_distribute_report
```

Workflow `scheduled_reports_402` accepts `reportType`, `dateRange`, and `recipients`. Times out after `60` seconds.

## Workers

**QueryDataWorker** (`sch_query_data`) -- queries data for the specified report type and date range.

**FormatReportWorker** (`sch_format_report`) -- formats the report from the queried data, reporting the row count.

**DistributeReportWorker** (`sch_distribute_report`) -- sends the formatted report to recipients.

## Workflow Output

The workflow produces `rowCount`, `reportUrl`, `delivered` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `scheduled_reports_402` defines 3 tasks with input parameters `reportType`, `dateRange`, `recipients` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "Generate scheduled reports: query data sources, format the report, and distribute to recipients.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

7 tests verify data querying, report formatting, and distribution to recipients.


## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
