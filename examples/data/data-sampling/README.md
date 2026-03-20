# Data Sampling in Java Using Conductor :  Sample Drawing, Quality Checks, and Conditional Approval Routing

A Java Conductor workflow example for sample-based data quality gating: loading a dataset, drawing a representative sample at a configurable rate, running quality checks against the sample, and using a `SWITCH` to route the dataset to approval (if quality meets the threshold) or flag it for manual review (if quality falls short). Uses [Conductor](https://github.## The Problem

You have a large incoming dataset, a vendor file, an ETL output, a batch import, and you need to decide whether it's good enough to accept before loading it into your production systems. Checking every record is too expensive, so you sample. That means drawing a statistically representative subset at a configurable sample rate, running quality checks on the sample (completeness, format validity, consistency), and making a pass/fail decision based on a threshold. Datasets that pass get approved for loading; datasets that fail get routed to a review queue with a list of specific issues found.

Without orchestration, you'd write a single script that samples, checks, and decides inline. There's no visibility into why a dataset was rejected. Was it the sample quality or a bug in the check logic? If the quality check fails transiently, you'd manually re-run the entire pipeline. Changing the sample rate or threshold means modifying code, and there's no audit trail of which datasets were approved, which were flagged, and what their quality scores were.

## The Solution

**You just write the dataset loading, sample drawing, quality checking, approval, and review workers. Conductor handles conditional routing via SWITCH based on quality scores, retries on transient check failures, and a complete audit trail of every dataset's sample score and routing decision.**

Each stage is a simple, independent worker. The loader reads the incoming dataset. The sampler draws a deterministic subset at the configured sample rate. The quality checker scores the sample against completeness and validity rules. Conductor's `SWITCH` task then routes based on the result: datasets meeting the quality threshold go to the approval worker, while those falling short go to the review flagger with a list of specific issues. Conductor tracks the quality score, sample size, and routing decision for every dataset, retries if a check fails transiently, and provides a complete audit trail. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers implement sample-based quality gating: loading the dataset, drawing a representative sample at a configurable rate, running quality checks, and then routing to either approval or manual review based on the quality score via a SWITCH.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveDatasetWorker** | `sm_approve_dataset` | Approves a dataset that passed quality checks. |
| **DrawSampleWorker** | `sm_draw_sample` | Draws a deterministic sample from the dataset. |
| **FlagForReviewWorker** | `sm_flag_for_review` | Flags a dataset for review when quality checks fail. |
| **LoadDatasetWorker** | `sm_load_dataset` | Loads a dataset from the input records. |
| **RunQualityChecksWorker** | `sm_run_quality_checks` | Runs quality checks on the sampled data. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
sm_load_dataset
    │
    ▼
sm_draw_sample
    │
    ▼
sm_run_quality_checks
    │
    ▼
SWITCH (decision_switch_ref)
    ├── pass: sm_approve_dataset
    └── default: sm_flag_for_review
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
java -jar target/data-sampling-1.0.0.jar
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
java -jar target/data-sampling-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_sampling_wf \
  --version 1 \
  --input '{"records": "test-value", "sampleRate": "test-value", "threshold": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_sampling_wf -s COMPLETED -c 5
```

## How to Extend

Implement stratified or reservoir sampling, add domain-specific quality rules and anomaly detection, and connect approvals to your data catalog, the sample-based quality gating workflow runs unchanged.

- **LoadDatasetWorker** → read datasets from S3, SFTP, database exports, or streaming sources
- **DrawSampleWorker** → implement stratified sampling (proportional representation by category), reservoir sampling for streaming data, or systematic sampling with random start
- **RunQualityChecksWorker** → add domain-specific quality rules (statistical distribution checks, anomaly detection, schema drift detection, comparison against historical baselines)
- **ApproveDatasetWorker** → trigger downstream loading pipelines, update a data catalog with approval status, or send a Slack notification to the data team
- **FlagForReviewWorker** → create a Jira ticket, send an alert to the data quality team, or queue the dataset for manual inspection in a review dashboard

Adjusting the sample rate, threshold, or quality check rules inside any worker does not alter the sampling-and-routing workflow, provided each returns its quality score and pass/fail decision consistently.

**Add new routing outcomes** by adding cases to the `SWITCH` in `workflow.json`, for example, a "warning" path that loads the dataset but sends a notification, or a "quarantine" path that holds the dataset for 24 hours before re-checking.

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
data-sampling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/datasampling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataSamplingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveDatasetWorker.java
│       ├── DrawSampleWorker.java
│       ├── FlagForReviewWorker.java
│       ├── LoadDatasetWorker.java
│       └── RunQualityChecksWorker.java
└── src/test/java/datasampling/workers/
    ├── ApproveDatasetWorkerTest.java        # 8 tests
    ├── DrawSampleWorkerTest.java        # 9 tests
    ├── FlagForReviewWorkerTest.java        # 9 tests
    ├── LoadDatasetWorkerTest.java        # 8 tests
    └── RunQualityChecksWorkerTest.java        # 10 tests
```
