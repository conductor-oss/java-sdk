# Impact Reporting in Java with Conductor

A Java Conductor workflow example demonstrating Impact Reporting. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

Your nonprofit needs to produce its annual impact report for donors and board members. The reporting team must collect raw data across all programs (beneficiaries served, events held, volunteer hours), aggregate the totals into organization-wide metrics, analyze year-over-year growth and cost-effectiveness, format the report with charts, metrics, and narrative sections, and publish the final report as a downloadable document. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

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

Workers simulate nonprofit operations .  donor processing, campaign management, reporting ,  with realistic outputs. Replace with real CRM and payment integrations and the workflow stays the same.

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
java -jar target/impact-reporting-1.0.0.jar
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
java -jar target/impact-reporting-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow impact_reporting_756 \
  --version 1 \
  --input '{"programName": "test", "reportYear": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w impact_reporting_756 -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real impact tools .  your program database for outcomes data, your analytics platform for metric aggregation, your reporting engine for donor-ready documents, and the workflow runs identically in production.

- **CollectDataWorker** (`ipr_collect_data`): query program data from Salesforce NPSP, Bloomerang, or your program database, pulling beneficiary counts, event attendance, and volunteer hours
- **AggregateWorker** (`ipr_aggregate`): run SQL aggregations against your data warehouse or use the Salesforce Reports API to compute organization-wide totals
- **AnalyzeWorker** (`ipr_analyze`): compute year-over-year trends and cost-per-beneficiary metrics using your analytics platform or a data science pipeline
- **FormatWorker** (`ipr_format`): generate the formatted report using a template engine (JasperReports, Apache POI) with charts from a visualization library
- **PublishWorker** (`ipr_publish`): upload the report PDF to your website CMS, Salesforce Files, or S3 and distribute the link via email through SendGrid or Mailchimp

Swap data sources or narrative generators and the reporting pipeline structure remains the same.

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
impact-reporting/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/impactreporting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ImpactReportingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWorker.java
│       ├── AnalyzeWorker.java
│       ├── CollectDataWorker.java
│       ├── FormatWorker.java
│       └── PublishWorker.java
└── src/test/java/impactreporting/workers/
    ├── CollectDataWorkerTest.java        # 1 tests
    └── PublishWorkerTest.java        # 1 tests
```
