# Program Evaluation in Java with Conductor

A Java Conductor workflow example demonstrating Program Evaluation. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

Your nonprofit needs to evaluate a community outreach program to decide whether to expand, modify, or sunset it. The evaluation team must define the KPIs and outcome metrics, collect data across reach, outcomes, cost, and satisfaction dimensions, analyze program performance by scoring each metric, benchmark the results against sector averages, and generate actionable recommendations. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the data gathering, outcome measurement, effectiveness analysis, and recommendation generation logic. Conductor handles data gathering retries, scoring sequencing, and evaluation audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Data gathering, metric analysis, effectiveness scoring, and recommendation generation workers each assess one dimension of program performance.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeWorker** | `pev_analyze` | Scores program performance across reach, outcomes, efficiency, and satisfaction dimensions |
| **BenchmarkWorker** | `pev_benchmark` | Compares the program's scores against sector averages and top-quartile thresholds, returning a ranking |
| **CollectWorker** | `pev_collect` | Collects evaluation data for the specified period: reach count, outcome percentage, cost per unit, and satisfaction score |
| **DefineMetricsWorker** | `pev_define_metrics` | Defines the evaluation framework with KPIs: reach, outcomes, cost-effectiveness, and satisfaction |
| **RecommendWorker** | `pev_recommend` | Generates actionable recommendations (e.g., scale reach, optimize costs) based on the overall evaluation score and ranking |

Workers simulate nonprofit operations .  donor processing, campaign management, reporting ,  with realistic outputs. Replace with real CRM and payment integrations and the workflow stays the same.

### The Workflow

```
pev_define_metrics
    │
    ▼
pev_collect
    │
    ▼
pev_analyze
    │
    ▼
pev_benchmark
    │
    ▼
pev_recommend

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
java -jar target/program-evaluation-1.0.0.jar

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
java -jar target/program-evaluation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow program_evaluation_757 \
  --version 1 \
  --input '{"programName": "test", "evaluationPeriod": "sample-evaluationPeriod"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w program_evaluation_757 -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real evaluation tools .  your program database for outcomes data, your survey platform for participant feedback, your analytics engine for effectiveness scoring, and the workflow runs identically in production.

- **DefineMetricsWorker** (`pev_define_metrics`): load the evaluation framework from your program database or Salesforce NPSP program objects, configuring which KPIs apply to this program type
- **CollectWorker** (`pev_collect`): query outcome data from your case management system (Apricot, Penelope) and financial data from your accounting platform (Sage Intacct, QuickBooks)
- **AnalyzeWorker** (`pev_analyze`): run scoring algorithms against the collected data using your analytics engine or a data science pipeline
- **BenchmarkWorker** (`pev_benchmark`): compare results against sector benchmark databases (e.g., GuideStar/Candid data, NTEN benchmarks) via their APIs
- **RecommendWorker** (`pev_recommend`): generate the evaluation report with recommendations and store it in your board portal or Salesforce Files for stakeholder review

Update evaluation frameworks or scoring methodologies and the pipeline adjusts without restructuring.

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
program-evaluation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/programevaluation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ProgramEvaluationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeWorker.java
│       ├── BenchmarkWorker.java
│       ├── CollectWorker.java
│       ├── DefineMetricsWorker.java
│       └── RecommendWorker.java
└── src/test/java/programevaluation/workers/
    ├── DefineMetricsWorkerTest.java        # 1 tests
    └── RecommendWorkerTest.java        # 1 tests

```
