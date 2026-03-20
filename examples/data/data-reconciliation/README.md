# Data Reconciliation in Java Using Conductor :  Cross-System Comparison and Discrepancy Reporting

A Java Conductor workflow example for data reconciliation. fetching records from two independent sources (e.g., billing system and fulfillment system), comparing them by a configurable key field to find matches, mismatches, and records missing from either side, and generating a discrepancy report with reconciliation rate. Uses [Conductor](https://github.## The Problem

You have the same data in two systems. Orders in billing and orders in fulfillment, transactions in the ledger and transactions in the payment gateway, inventory in the warehouse system and inventory in the ERP. These systems should agree, but they drift. You need to fetch records from both sources, join them by a key field (order ID, transaction ID), identify records that match perfectly, records that exist in both but have different values (amount mismatches, status discrepancies), and records that exist in one system but not the other. The result is a reconciliation report showing exactly where the systems disagree.

Without orchestration, you'd write a script that connects to both databases, pulls records, builds hash maps for comparison, and prints results. If one data source is slow or temporarily unavailable, the script fails entirely. If the process crashes after fetching source A but before fetching source B, you'd refetch both. There's no record of reconciliation rate trends over time, and adding a third data source means rewriting the comparison logic.

## The Solution

**You just write the source fetching, record comparison, and discrepancy reporting workers. Conductor handles fetch-compare-report sequencing, retries when either data source is temporarily unavailable, and full tracking of match and mismatch counts.**

Each stage of the reconciliation is a simple, independent worker. Source A and Source B fetch workers each connect to their respective system and return records. The comparator joins records by the configured key field and classifies each as matched, mismatched, missing-in-A, or missing-in-B. The report generator computes the reconciliation rate and lists every discrepancy. Conductor executes them in sequence, retries if a data source is temporarily unavailable, and tracks exactly how many records were fetched, matched, and mismatched. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers handle cross-system reconciliation: fetching records from source A (e.g., billing), fetching from source B (e.g., fulfillment), comparing them by key field to find matches and discrepancies, and generating a reconciliation report.

| Worker | Task | What It Does |
|---|---|---|
| **CompareRecordsWorker** | `rc_compare_records` | Compares records from two sources and identifies matches, mismatches, and missing records. |
| **FetchSourceAWorker** | `rc_fetch_source_a` | Fetches records from source A (billing system). |
| **FetchSourceBWorker** | `rc_fetch_source_b` | Fetches records from source B (fulfillment system). |
| **GenerateDiscrepancyReportWorker** | `rc_generate_discrepancy_report` | Generates a discrepancy report from comparison results. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
rc_fetch_source_a
    │
    ▼
rc_fetch_source_b
    │
    ▼
rc_compare_records
    │
    ▼
rc_generate_discrepancy_report
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
java -jar target/data-reconciliation-1.0.0.jar
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
java -jar target/data-reconciliation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_reconciliation \
  --version 1 \
  --input '{"sourceA": "test-value", "sourceB": "test-value", "keyField": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_reconciliation -s COMPLETED -c 5
```

## How to Extend

Connect the fetch workers to your real billing and fulfillment databases via JDBC, add fuzzy matching for amount tolerances, and the cross-system reconciliation workflow runs unchanged.

- **FetchSourceAWorker** → query your billing system, ledger, or primary database via JDBC or API
- **FetchSourceBWorker** → query your fulfillment system, payment gateway, or replica database
- **CompareRecordsWorker** → implement fuzzy matching for amount tolerances (e.g., within $0.01), date range matching, or multi-field composite key comparison
- **GenerateDiscrepancyReportWorker** → write reports to a reconciliation dashboard, create Jira tickets for discrepancies above a threshold, or trigger auto-correction workflows for known mismatch patterns

Connecting the fetch workers to real billing and fulfillment databases requires no workflow modifications, provided each returns records in the expected key-value format.

**Add new capabilities** by modifying `workflow.json`, for example, make the two fetch steps run in parallel with `FORK_JOIN` for faster execution, add a third source for three-way reconciliation, or add an auto-resolution step that fixes known discrepancy types automatically.

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
data-reconciliation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/datareconciliation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataReconciliationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CompareRecordsWorker.java
│       ├── FetchSourceAWorker.java
│       ├── FetchSourceBWorker.java
│       └── GenerateDiscrepancyReportWorker.java
└── src/test/java/datareconciliation/workers/
    ├── CompareRecordsWorkerTest.java        # 5 tests
    ├── FetchSourceAWorkerTest.java        # 3 tests
    ├── FetchSourceBWorkerTest.java        # 3 tests
    └── GenerateDiscrepancyReportWorkerTest.java        # 4 tests
```
