# Passing Output To Input in Java with Conductor

Shows all the ways to pass data between tasks. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to build a regional sales report in three stages: generate raw metrics and top products for a region and time period, enrich the report with computed growth rates and health insights, then summarize by combining the original metrics with the enriched data. The second step needs access to the first step's output (metrics object, individual revenue field, top products list). The third step needs data from both previous steps .  the original metrics from step 1 and the enriched insights and growth rate from step 2. Each step consumes data wired from different upstream tasks.

Without orchestration, you'd pass data between functions manually .  storing intermediate results in shared state, passing objects through method signatures, or writing to a database between steps. When the data shape changes in step 1, you have to trace through all downstream consumers to update them. There is no way to inspect what data each step received without adding debug logging at every boundary.

## The Solution

**You just write the report generation, enrichment, and summarization workers. Conductor handles all the data wiring between tasks via input template expressions.**

This example demonstrates all the ways Conductor wires data between tasks using `${taskRef.output}` expressions. GenerateReportWorker produces structured output (nested metrics object, top products array) for a given region and period. EnrichReportWorker receives data three different ways: the entire metrics object (`${report_ref.output.metrics}`), a specific nested field (`${report_ref.output.metrics.revenue}`), and the top products array, plus a workflow input value (`${workflow.input.region}`). SummarizeReportWorker pulls from two upstream tasks .  original metrics from the generate step and enriched insights plus growth rate from the enrich step. Conductor records every input/output mapping, so you can see exactly what data each task received and produced.

### What You Write: Workers

Three workers build a sales report pipeline: GenerateReportWorker produces regional metrics and top products, EnrichReportWorker adds growth rates and insights using data wired from multiple sources, and SummarizeReportWorker combines outputs from both previous steps.

| Worker | Task | What It Does |
|---|---|---|
| **EnrichReportWorker** | `enrich_report` | Enriches a report by computing growth rate and health insights. Receives the entire metrics object, individual fields... |
| **GenerateReportWorker** | `generate_report` | Generates a report with metrics and top products for a given region and period. Outputs structured data (nested objec... |
| **SummarizeReportWorker** | `summarize_report` | Summarizes a report using data from both previous tasks. Demonstrates accessing outputs from multiple upstream task r... |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
generate_report
    │
    ▼
enrich_report
    │
    ▼
summarize_report
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
java -jar target/passing-output-to-input-1.0.0.jar
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
java -jar target/passing-output-to-input-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_wiring_demo \
  --version 1 \
  --input '{"region": "test-value", "period": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_wiring_demo -s COMPLETED -c 5
```

## How to Extend

Replace the report generator with real database queries, add your own enrichment logic, and the multi-step data wiring workflow runs unchanged.

- **GenerateReportWorker** (`generate_report`): query your analytics database (BigQuery, Redshift, Snowflake) for revenue, orders, and top products by region and time period
- **EnrichReportWorker** (`enrich_report`): compute year-over-year growth rates, add market benchmarks from external data sources, and generate health insights (trending up/down, anomaly detection)
- **SummarizeReportWorker** (`summarize_report`): combine raw metrics and enriched insights into an executive summary with actionable recommendations, and distribute via email, Slack, or a dashboard API

Changing the report metrics or enrichment logic inside any worker does not affect the input template wiring between tasks, as long as referenced output fields remain present.

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
passing-output-to-input/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/passingoutput/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PassingOutputExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EnrichReportWorker.java
│       ├── GenerateReportWorker.java
│       └── SummarizeReportWorker.java
└── src/test/java/passingoutput/workers/
    ├── EnrichReportWorkerTest.java        # 6 tests
    ├── GenerateReportWorkerTest.java        # 5 tests
    └── SummarizeReportWorkerTest.java        # 6 tests
```
