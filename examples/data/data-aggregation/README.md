# Data Aggregation in Java Using Conductor: Group-By, Statistical Computation, and Report Generation

The VP of Sales opens the regional revenue dashboard Monday morning and it takes 45 seconds to load. The dashboard runs a query that scans 12 million raw transaction rows, groups them by region, and computes sum/average/min/max: live, on every page load. By Wednesday, when the table has grown by another 2 million rows, the query times out entirely. So someone adds a materialized view, but it's stale by the time the next sales call happens. The real problem is that aggregation is happening at read time against raw data, instead of being computed once, stored, and served instantly. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You have a dataset of records. Sales transactions, sensor readings, user events, and you need to answer questions like "what's the average revenue per region?" or "what's the total count per department?" That means loading the dataset, grouping rows by a specified dimension (region, category, department), computing statistical aggregates (count, sum, average, min, max) on a numeric field for each group, formatting the results into a readable report, and delivering the output. Each step depends on the previous one: you can't compute aggregates without groups, and you can't group without loaded data.

Without orchestration, you'd write a single method that reads data, does the grouping with `Collectors.groupingBy`, computes stats inline, formats output, and returns everything. If the data source times out, you'd add manual retry logic. If the process crashes after expensive computation but before emitting results, that work is lost. Changing the grouping dimension or adding a new aggregate function (median, percentiles) means touching deeply coupled code with no visibility into which step is slow.

## The Solution

**You just write the data loading, grouping, aggregation, formatting, and emission workers. Conductor handles the load-group-aggregate-format-emit sequence, retries on data source failures, and detailed observability into record counts at every stage.**

Each stage of the aggregation pipeline is a simple, independent worker. The loader reads records from the data source. The grouper partitions records by the specified dimension field. The aggregator computes count, sum, average, min, and max for each group on the specified numeric field. The formatter turns raw aggregates into human-readable report lines. The emitter delivers the final output. Conductor executes them in sequence, passes grouped data between steps, retries if a data source query fails, and resumes from exactly where it left off if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers cover the full aggregation lifecycle: loading records, grouping by a configurable dimension, computing statistical aggregates (count, sum, average, min, max), formatting results, and emitting the final report.

| Worker | Task | What It Does |
|---|---|---|
| **ComputeAggregatesWorker** | `agg_compute_aggregates` | Computes aggregate statistics (count, sum, avg, min, max) for each group on a specified numeric field. |
| **EmitResultsWorker** | `agg_emit_results` | Emits the final aggregation summary. |
| **FormatReportWorker** | `agg_format_report` | Formats aggregate results into human-readable report lines. |
| **GroupByDimensionWorker** | `agg_group_by_dimension` | Groups records by a specified dimension field. |
| **LoadDataWorker** | `agg_load_data` | Loads input records and passes them through with a count. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks, the pipeline structure and error handling stay the same.

### The Workflow

```
agg_load_data
    │
    ▼
agg_group_by_dimension
    │
    ▼
agg_compute_aggregates
    │
    ▼
agg_format_report
    │
    ▼
agg_emit_results

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
java -jar target/data-aggregation-1.0.0.jar

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
java -jar target/data-aggregation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_aggregation_wf \
  --version 1 \
  --input '{"records": [{"region": "east", "amount": 100}, {"region": "west", "amount": 200}, {"region": "east", "amount": 150}], "groupBy": "region", "aggregateField": "amount"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_aggregation_wf -s COMPLETED -c 5

```

## How to Extend

Connect the loader to a real data source like PostgreSQL or Elasticsearch, add advanced statistics in the aggregator, and the group-by pipeline runs unchanged.

- **LoadDataWorker** → read records from a real data source (PostgreSQL query, S3 CSV file, Elasticsearch index, Kafka topic)
- **GroupByDimensionWorker** → support hierarchical grouping (region > city), multi-field grouping, or time-based bucketing (by hour, day, month)
- **ComputeAggregatesWorker** → add advanced statistics like median, percentiles, standard deviation, or weighted averages
- **FormatReportWorker** → generate output in different formats (HTML tables, PDF, Excel via Apache POI, Markdown)
- **EmitResultsWorker** → write results to a data warehouse, push to a BI tool API, or send as an email attachment

Any worker can be swapped out, a new data source, a different grouping strategy, or additional statistics., without altering the rest of the pipeline, as long as the output fields remain consistent.

**Add new stages** by inserting tasks in `workflow.json`, for example, a filtering step before grouping, an outlier detection pass after aggregation, or a comparison step that computes deltas against a previous period's aggregates.

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
data-aggregation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dataaggregation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataAggregationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ComputeAggregatesWorker.java
│       ├── EmitResultsWorker.java
│       ├── FormatReportWorker.java
│       ├── GroupByDimensionWorker.java
│       └── LoadDataWorker.java
└── src/test/java/dataaggregation/workers/
    ├── ComputeAggregatesWorkerTest.java        # 8 tests
    ├── EmitResultsWorkerTest.java        # 8 tests
    ├── FormatReportWorkerTest.java        # 8 tests
    ├── GroupByDimensionWorkerTest.java        # 8 tests
    └── LoadDataWorkerTest.java        # 8 tests

```
