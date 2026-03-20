# Log Aggregation in Java Using Conductor :  Collect, Parse, Enrich, and Store Logs

A Java Conductor workflow example for log aggregation .  collecting logs from multiple sources, parsing them into structured format, enriching with metadata (geo-IP, user context), and storing in a searchable log store.

## The Problem

Your applications produce logs in different formats (JSON, plain text, syslog) across different sources (files, stdout, cloud services). These logs need to be collected, parsed into a common structured format, enriched with contextual data (which deployment, which region, which customer), and stored in a searchable system for debugging and compliance.

Without orchestration, log aggregation is a Logstash/Fluentd pipeline that's configured declaratively but hard to debug. When parsing fails for a new log format, enrichment silently produces incomplete records. Adding a new log source means modifying a complex configuration file and hoping it doesn't break existing sources.

## The Solution

**You just write the log parsers and enrichment rules. Conductor handles the collect-parse-enrich-store pipeline, retries when log sources or storage backends are temporarily unavailable, and per-step counts and error rates for every run.**

Each log processing step is an independent worker .  collection, parsing, enrichment, and storage. Conductor runs them in sequence: collect raw logs, parse into structured format, enrich with metadata, then store. Every pipeline run is tracked with counts and error rates per step. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

The aggregation pipeline sequences CollectLogsWorker to ingest raw logs, ParseLogsWorker to extract structured fields, EnrichLogsWorker to add geo-IP and service context, and a StoreLogsWorker to persist enriched records in a searchable backend.

| Worker | Task | What It Does |
|---|---|---|
| **CollectLogsWorker** | `la_collect_logs` | Collects raw logs from configured sources within a time range, returning log count and format |
| **EnrichLogsWorker** | `la_enrich_logs` | Enriches parsed logs with contextual metadata (geo, service, traceId, userId), returning enriched count and size |
| **ParseLogsWorker** | `la_parse_logs` | Parses raw logs into structured JSON format, reporting parsed count and parse errors |
| **StoreLogsWorker** | `la_store_logs` | Writes enriched logs to the search index (e.g., Elasticsearch), returning the index name and document count |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
la_collect_logs
    │
    ▼
la_parse_logs
    │
    ▼
la_enrich_logs
    │
    ▼
la_store_logs
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
java -jar target/log-aggregation-1.0.0.jar
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
java -jar target/log-aggregation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow log_aggregation_412 \
  --version 1 \
  --input '{"sources": "test-value", "timeRange": "2026-01-01T00:00:00Z", "logLevel": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w log_aggregation_412 -s COMPLETED -c 5
```

## How to Extend

Each worker handles one pipeline stage .  connect the collector to Fluentd or Filebeat, the storage worker to Elasticsearch or S3, and the collect-parse-enrich-store workflow stays the same.

- **CollectLogsWorker** (`la_collect_logs`): pull logs from CloudWatch, S3, Kafka topics, syslog receivers, or file-based log collectors (Filebeat, Fluent Bit)
- **EnrichLogsWorker** (`la_enrich_logs`): add geo-IP data (MaxMind), user context from your identity service, deployment metadata from Kubernetes labels
- **ParseLogsWorker** (`la_parse_logs`): parse diverse log formats (JSON, Apache, nginx, custom) into a common schema using regex or Grok patterns

Connect to your log sources and Elasticsearch, and the aggregation pipeline runs in production with the same workflow definition.

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
log-aggregation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/logaggregation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LogAggregationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectLogsWorker.java
│       ├── EnrichLogsWorker.java
│       ├── ParseLogsWorker.java
│       └── StoreLogsWorker.java
└── src/test/java/logaggregation/workers/
    ├── CollectLogsWorkerTest.java        # 2 tests
    └── StoreLogsWorkerTest.java        # 2 tests
```
