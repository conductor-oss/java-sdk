# Data Quality Checks in Java Using Conductor: Parallel Completeness, Accuracy, and Consistency Scoring

The executive dashboard shows 15% revenue growth this quarter. The CEO quotes it in the board meeting. Then a data engineer discovers that the pipeline has been double-counting records from one integration source for six weeks. duplicate IDs that slipped in when a vendor changed their API response format. The real growth is 7%. On top of that, 300 customer records have null email fields breaking the marketing segmentation, and half the timestamps from the EU data center are in UTC while the other half are in CET, inflating "active users" every evening. Nobody caught it because there were no automated quality checks. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate parallel data quality checks, completeness, accuracy, and consistency, with FORK_JOIN, automatic retries, and a graded quality report.

## The Problem

Before data enters your warehouse, analytics pipeline, or ML training set, you need to know if it's any good. That means checking three independent dimensions: completeness (are required fields like name, email, and ID populated?), accuracy (do emails have valid format? do status values match the allowed set?), and consistency (are IDs unique? do cross-field relationships hold?). These checks are independent of each other and can run simultaneously, but the quality report needs all three scores before it can compute an overall grade.

Without orchestration, you'd run all three checks sequentially in a single method, tripling the total check time. If you parallelize with thread pools, you'd manage `Future` objects, handle partial failures, and write join logic manually. Adding a new quality dimension (timeliness, uniqueness, validity) means modifying the parallelism and aggregation code every time.

## The Solution

**You just write the completeness, accuracy, consistency, and report generation workers. Conductor handles parallel check execution via FORK_JOIN, per-check retries, and automatic join-then-report sequencing so scores are always complete before grading.**

Each quality check is a simple, independent worker. The completeness checker counts filled required fields across all records. The accuracy checker validates email formats and status value enums. The consistency checker looks for duplicate IDs and cross-field contradictions. Conductor's `FORK_JOIN` runs all three checks simultaneously against the same dataset, waits for all to complete, and then the report generator computes a weighted overall score and letter grade. If one check fails, Conductor retries just that check. You get all of that for free, without writing a single line of thread pool or join logic.

### What You Write: Workers

Five workers run the quality assessment: loading records, then checking completeness, accuracy, and consistency in parallel via FORK_JOIN, and finally generating a quality report with scores and an overall letter grade.

| Worker | Task | What It Does |
|---|---|---|
| `LoadDataWorker` | `qc_load_data` | Accepts incoming records and passes them through with a count for downstream checks |
| `CheckCompletenessWorker` | `qc_check_completeness` | Checks four required fields (id, name, email, status) across all records; empty strings count as missing |
| `CheckAccuracyWorker` | `qc_check_accuracy` | Validates email format (must contain @ and .) and status values (must be active/inactive/pending) |
| `CheckConsistencyWorker` | `qc_check_consistency` | Detects duplicate IDs by comparing unique ID count to total ID count |
| `GenerateReportWorker` | `qc_generate_report` | Averages the three check scores and assigns a letter grade (A/B/C/D) |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks, the pipeline structure and error handling stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Parallel execution** | FORK_JOIN runs multiple tasks simultaneously and waits for all to complete |

### The Workflow

```
qc_load_data
    │
    ▼
FORK_JOIN
    ├── qc_check_completeness
    ├── qc_check_accuracy
    └── qc_check_consistency
    │
    ▼
JOIN (wait for all branches)
qc_generate_report
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
java -jar target/data-quality-checks-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

### Sample Output

```
=== Data Quality Checks Workflow Demo ===

Step 1: Registering task definitions...
  Registered: qc_load_data, qc_check_completeness, qc_check_accuracy, qc_check_consistency, qc_generate_report

Step 2: Registering workflow 'data_quality_checks'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...

  [load] Loaded 6 records for quality checks
  [completeness] 23/24 fields filled -> score: 96%
  [accuracy] 5/6 records accurate -> score: 83%
  [consistency] Unique IDs: 6/6 -> score: 100%
  [report] Quality Report: completeness=96%, accuracy=83%, consistency=100% -> overall=93% (A)

  Workflow ID: 3fa85f64-5542-4562-b3fc-2c963f66afa6

Step 5: Waiting for completion...
  Status: COMPLETED
  Output: {totalRecords=6, completenessScore=0.96, accuracyScore=0.83, consistencyScore=1.0, overallScore=0.93, grade=A}

Result: PASSED
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/data-quality-checks-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_quality_checks \
  --version 1 \
  --input '{"records": [{"id":1,"name":"Alice","email":"alice@example.com","status":"active"},{"id":2,"name":"Bob","email":"invalid-email","status":"active"},{"id":3,"name":"","email":"charlie@example.com","status":"pending"}]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_quality_checks -s COMPLETED -c 5
```

## How to Extend

Add domain-specific validation rules, integrate reference data lookups for accuracy checks, and connect the report to a quality dashboard, the parallel quality assessment workflow runs unchanged.

- **`LoadDataWorker`**: Read records from a real data source (database, data lake, API response) to check quality before downstream processing.

- **`CheckCompletenessWorker`**: Configure required fields per dataset type; compute null rates, empty string percentages, and missing value distributions.

- **`CheckAccuracyWorker`**: Validate against reference data (zip code databases, currency code lists), use regex for format validation, or call verification APIs (email verification, address validation).

- **`CheckConsistencyWorker`**: Check cross-field rules (if status is "shipped", shipDate must be set), referential integrity, and temporal consistency (startDate before endDate).

- **`GenerateReportWorker`**: Produce quality dashboards, trigger alerts when scores drop below thresholds, or block downstream pipeline execution on failing grades.

Adding reference data lookups or stricter validation rules inside any check worker does not affect the parallel quality assessment workflow, provided each returns its score in the expected format.

**Add new quality dimensions** by adding branches to the `FORK_JOIN` in `workflow.json`, for example, a timeliness check (is data arriving on schedule?), a uniqueness check, or a freshness check (how old is the newest record?).

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
data-quality-checks/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dataqualitychecks/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataQualityChecksExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckAccuracyWorker.java
│       ├── CheckCompletenessWorker.java
│       ├── CheckConsistencyWorker.java
│       ├── GenerateReportWorker.java
│       └── LoadDataWorker.java
└── src/test/java/dataqualitychecks/workers/
    ├── CheckAccuracyWorkerTest.java
    ├── CheckCompletenessWorkerTest.java
    ├── CheckConsistencyWorkerTest.java
    ├── GenerateReportWorkerTest.java
    └── LoadDataWorkerTest.java
```
