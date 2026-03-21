# Report Generation in Java Using Conductor :  Data Querying, Aggregation, Formatting, and Distribution

A Java Conductor workflow example for automated report generation. querying raw data for a specific report type and date range, aggregating the results into summary metrics, formatting the aggregated data into a report document with a downloadable URL, and distributing the finished report to a recipient list via email or Slack. Uses [Conductor](https://github.

## The Problem

Every Monday morning, the sales team needs a weekly revenue report. Every month-end, finance needs a P&L summary. Every quarter, the board needs a KPI dashboard export. Each report follows the same pattern: query the right data for the right date range, aggregate it into the metrics that audience cares about, format it into a presentable document (PDF, Excel, HTML), and deliver it to the right people. But the data query for a revenue report is different from a P&L query. The aggregation logic (sum revenue by region vs. compute gross margin by product line) depends on the report type. Formatting depends on the audience. And distribution might be email for finance, Slack for engineering, and a shared drive for the board.

Without orchestration, you'd write a cron job that queries, aggregates, formats, and emails in one script per report type. If the email server is down after you've already spent 5 minutes querying and formatting, the entire report generation fails with no retry. There's no record of how many records were queried, what the aggregated metrics looked like before formatting, or whether distribution actually succeeded. Adding a new report type means duplicating the entire script with slight modifications.

## The Solution

**You just write the data querying, aggregation, report formatting, and distribution workers. Conductor handles the query-aggregate-format-distribute sequence, retries when email servers or data sources are temporarily unavailable, and tracking of record counts and delivery status at every stage.**

Each stage of the report pipeline is a simple, independent worker. The data querier fetches raw records for the specified report type and date range, returning the data along with a record count. The aggregator computes summary metrics appropriate for the report type. Totals, averages, breakdowns by dimension, period-over-period comparisons, and counts the number of aggregations performed. The formatter renders the aggregated data into a report document (PDF, Excel, HTML) and produces a downloadable URL. The distributor delivers the finished report to the recipient list via the configured channel (email, Slack, shared drive). Conductor executes them in strict sequence, passes the evolving report between stages, retries if the email server is temporarily unavailable, and tracks record counts, aggregation counts, and delivery status at every stage. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers implement the report pipeline: querying raw data for a report type and date range, aggregating results into summary metrics, formatting into a downloadable document (PDF, Excel, HTML), and distributing to recipients via email or Slack.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateResultsWorker** | `rg_aggregate_results` | Aggregates raw data into summary metrics for reporting. |
| **DistributeReportWorker** | `rg_distribute_report` | Distributes the generated report to recipients via email or Slack. |
| **FormatReportWorker** | `rg_format_report` | Formats aggregated data into a report document. |
| **QueryDataWorker** | `rg_query_data` | Queries raw data for report generation. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

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

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build

```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build

```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/report-generation-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/report-generation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow report_generation \
  --version 1 \
  --input '{"reportType": "standard", "dateRange": "2026-01-01T00:00:00Z", "recipients": "sample-recipients"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w report_generation -s COMPLETED -c 5

```

## How to Extend

Query real data from PostgreSQL or BigQuery, format with Apache POI or JasperReports, and distribute via email or Slack, the report generation workflow runs unchanged.

- **QueryDataWorker** → query real data sources: SQL databases (PostgreSQL, MySQL, BigQuery), analytics platforms (Mixpanel, Amplitude), CRM systems (Salesforce reports API), or data warehouses (Snowflake, Redshift) with parameterized queries by report type and date range
- **AggregateResultsWorker** → implement real aggregation logic: revenue rollups by region/product/channel, cohort analysis, period-over-period growth calculations, funnel conversion rates, or custom KPI formulas defined per report type
- **FormatReportWorker** → generate real report documents: Apache POI for Excel workbooks with charts, iText/PDFBox for PDF reports with headers and tables, Thymeleaf for HTML email reports, or JasperReports for complex templated layouts
- **DistributeReportWorker** → deliver via real channels: SendGrid/SES for email attachments, Slack API for channel posts with file uploads, Google Drive API for shared folder uploads, or S3 presigned URLs for secure download links

Pointing the data querier at a real database or switching the formatter to PDF generation requires no workflow modifications, as long as each worker outputs the expected record counts and report URLs.

**Add new stages** by inserting tasks in `workflow.json`, for example, a caching step that skips re-querying if the data hasn't changed since the last report, an approval step (using Conductor's WAIT task) that holds distribution until a manager reviews the report, or a scheduling step that triggers the workflow on a cron schedule for recurring reports.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>

```

## Project Structure

```
report-generation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/reportgeneration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ReportGenerationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateResultsWorker.java
│       ├── DistributeReportWorker.java
│       ├── FormatReportWorker.java
│       └── QueryDataWorker.java
└── src/test/java/reportgeneration/workers/
    ├── AggregateResultsWorkerTest.java        # 5 tests
    ├── DistributeReportWorkerTest.java        # 6 tests
    ├── FormatReportWorkerTest.java        # 6 tests
    └── QueryDataWorkerTest.java        # 5 tests

```
