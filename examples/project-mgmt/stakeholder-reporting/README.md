# Stakeholder Reporting in Java with Conductor :  Collect Updates, Aggregate, Format, and Distribute

A Java Conductor workflow example for automated stakeholder reporting .  collecting project updates from multiple sources, aggregating them into a coherent summary, formatting for executive consumption, and distributing to the right stakeholders. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to produce regular stakeholder reports for a project. Every reporting period, someone has to gather updates from engineering (sprint velocity, blockers), finance (burn rate, forecast), and program management (milestone status, risks). Those raw updates need to be aggregated into a single coherent summary, formatted into a professional report (PDF, slide deck, or dashboard), and then distributed to the right audience .  executives get the high-level view, team leads get the details.

Without orchestration, this becomes a manual, error-prone process. Someone writes a script that queries Jira, pulls from Sheets, formats a PDF, and sends emails .  all in one monolithic block. If the Jira API times out, the whole report fails. If the email step breaks, you don't know whether the report was generated. Nobody can tell which reporting period was last completed successfully.

## The Solution

**You just write the update collection, data aggregation, report formatting, and stakeholder distribution logic. Conductor handles data aggregation retries, report formatting, and distribution audit trails.**

Each step in the reporting pipeline is a simple, independent worker .  one collects raw updates, one aggregates them into a summary, one formats the report, one distributes it. Conductor takes care of executing them in sequence, retrying if a data source is temporarily unavailable, tracking every report generation with full audit history, and resuming if the process crashes mid-generation. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Data aggregation, insight generation, report formatting, and distribution workers each handle one phase of keeping stakeholders informed.

| Worker | Task | What It Does |
|---|---|---|
| **CollectUpdatesWorker** | `shr_collect_updates` | Gathers raw project updates (sprint progress, budget status, milestone completion) for the given reporting period |
| **AggregateWorker** | `shr_aggregate` | Consolidates raw updates into a structured summary with key metrics, blockers, and highlights |
| **FormatWorker** | `shr_format` | Transforms the aggregated summary into a formatted report (executive brief, detailed breakdown) |
| **DistributeWorker** | `shr_distribute` | Delivers the finished report to stakeholders via email, Slack, or dashboard publication |

Workers simulate project management operations .  task creation, status updates, notifications ,  with realistic outputs. Replace with real Jira/Asana/Linear integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
shr_collect_updates
    │
    ▼
shr_aggregate
    │
    ▼
shr_format
    │
    ▼
shr_distribute
```

## Example Output

```
=== Example stakeholder-reporting: Stakeholder Reporting ===

Step 1: Registering task definitions...
  Registered: shr_collect_updates, shr_aggregate, shr_format, shr_distribute

Step 2: Registering workflow 'stakeholder_reporting_stakeholder-reporting'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [aggregate] Aggregating team updates
  [collect] Gathering updates for
  [distribute] Report sent to stakeholders for
  [format] Formatting report

  Status: COMPLETED
  Output: {summary=..., updates=..., distributed=..., report=...}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/stakeholder-reporting-1.0.0.jar
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
java -jar target/stakeholder-reporting-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow stakeholder_reporting_stakeholder-reporting \
  --version 1 \
  --input '{"projectId": "PROJ-42", "PROJ-42": "reportPeriod", "reportPeriod": "2026-W10", "2026-W10": "sample-2026-W10"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w stakeholder_reporting_stakeholder-reporting -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real reporting stack. Jira and your finance system for update collection, your BI tools for formatting, email or Slack for distribution, and the workflow runs identically in production.

- **CollectUpdatesWorker** (`shr_collect_updates`): query Jira for sprint metrics, pull budget data from Google Sheets or SAP, and fetch milestone status from your PM tool
- **AggregateWorker** (`shr_aggregate`): implement weighted scoring for project health, trend analysis across reporting periods, and automatic blocker escalation flags
- **FormatWorker** (`shr_format`): generate PDF reports with Apache PDFBox, create slide decks via Google Slides API, or render HTML dashboards
- **DistributeWorker** (`shr_distribute`): send via SendGrid/SES for email, post to Slack channels, or publish to Confluence/SharePoint

Swap your data sources or report templates and the reporting pipeline remains structurally identical.

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
stakeholder-reporting-stakeholder-reporting/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/stakeholderreporting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── StakeholderReportingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWorker.java
│       ├── CollectUpdatesWorker.java
│       ├── DistributeWorker.java
│       └── FormatWorker.java
└── src/test/java/stakeholderreporting/workers/
    ├── AggregateWorkerTest.java        # 2 tests
    ├── CollectUpdatesWorkerTest.java        # 2 tests
    ├── DistributeWorkerTest.java        # 2 tests
    └── FormatWorkerTest.java        # 2 tests
```
