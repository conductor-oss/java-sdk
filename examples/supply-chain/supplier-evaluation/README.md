# Supplier Evaluation in Java with Conductor :  Performance Data Collection, Scoring, Ranking, and Quarterly Reporting

A Java Conductor workflow example for supplier evaluation .  collecting performance data across all raw-materials suppliers for a quarterly review period (e.g., Q4 2024), scoring each supplier on delivery, quality, cost, and responsiveness metrics, ranking suppliers against each other within the category, and generating the quarterly supplier performance report. Uses [Conductor](https://github.

## The Problem

You need to evaluate your raw-materials suppliers at the end of each quarter. Performance data must be collected from multiple sources .  on-time delivery rates from the TMS, quality rejection rates from the QMS, cost variance from the ERP, and responsiveness scores from buyer surveys. Each supplier must be scored on a consistent rubric. Suppliers must be ranked within their category so procurement knows which to grow, maintain, or phase out. The final report must be ready for the quarterly business review.

Without orchestration, supplier data lives in four different systems. A procurement analyst manually pulls reports from each, copies numbers into a spreadsheet, and applies scoring formulas that differ from quarter to quarter because the spreadsheet template keeps changing. Rankings are subjective because some metrics are stale (last quarter's quality data) while others are current. The report is always late because data collection alone takes three days of manual work.

## The Solution

**You just write the evaluation workers. Performance data collection, scoring, ranking, and quarterly reporting. Conductor handles data source retries, scoring sequencing, and versioned quarterly records for trend analysis.**

Each stage of the supplier evaluation pipeline is a simple, independent worker .  a plain Java class that does one thing. Conductor sequences them so all performance data is collected before scoring begins, scoring completes before ranking, and rankings feed the final report. If the quality data pull fails (QMS timeout), Conductor retries without re-pulling delivery data that was already collected. Every data snapshot, score calculation, ranking decision, and report generation is recorded for trend analysis across quarters.

### What You Write: Workers

Four workers power the quarterly review: CollectDataWorker pulls delivery, quality, cost, and responsiveness metrics, ScoreWorker applies a consistent rubric, RankWorker orders suppliers by composite score, and ReportWorker generates the performance summary.

| Worker | Task | What It Does |
|---|---|---|
| **CollectDataWorker** | `spe_collect_data` | Collects supplier performance data .  on-time delivery, quality rejection rates, cost variance, and responsiveness. |
| **RankWorker** | `spe_rank` | Ranks suppliers against each other within the category based on composite scores. |
| **ReportWorker** | `spe_report` | Generates the quarterly supplier performance report for the business review. |
| **ScoreWorker** | `spe_score` | Scores each supplier on delivery, quality, cost, and responsiveness using a consistent rubric. |

Workers simulate supply chain operations .  inventory checks, shipment tracking, supplier coordination ,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

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
java -jar target/supplier-evaluation-1.0.0.jar

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
java -jar target/supplier-evaluation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow spe_supplier_evaluation \
  --version 1 \
  --input '{"category": "general", "period": "sample-period"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w spe_supplier_evaluation -s COMPLETED -c 5

```

## How to Extend

Connect CollectDataWorker to your TMS, QMS, and ERP for delivery, quality, and cost metrics, and ReportWorker to your BI platform for the quarterly business review. The workflow definition stays exactly the same.

- **CollectDataWorker** (`spe_collect_data`): pull on-time delivery rates from your TMS, quality rejection rates from your QMS, cost variance from the ERP, and responsiveness scores from buyer survey tools
- **ScoreWorker** (`spe_score`): apply weighted scoring (e.g., delivery 30%, quality 30%, cost 25%, responsiveness 15%) using consistent formulas stored in your supplier management configuration
- **RankWorker** (`spe_rank`): sort suppliers within their category by composite score, flag suppliers below minimum thresholds for corrective action plans, and identify top performers for volume growth
- **ReportWorker** (`spe_report`): generate the quarterly supplier scorecard report (PDF or dashboard) with trend charts, upload to your procurement portal or GRC platform for stakeholder review

Connect any worker to your TMS, QMS, or ERP data feeds while preserving output fields, and the evaluation pipeline stays unchanged.

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
supplier-evaluation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/supplierevaluation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SupplierEvaluationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectDataWorker.java
│       ├── RankWorker.java
│       ├── ReportWorker.java
│       └── ScoreWorker.java
└── src/test/java/supplierevaluation/workers/
    ├── CollectDataWorkerTest.java        # 2 tests
    ├── RankWorkerTest.java        # 2 tests
    ├── ReportWorkerTest.java        # 2 tests
    └── ScoreWorkerTest.java        # 2 tests

```
