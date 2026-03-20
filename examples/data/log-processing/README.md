# Log Processing in Java Using Conductor :  Ingestion, Structured Parsing, Pattern Extraction, and Metric Aggregation

A Java Conductor workflow example for log processing. ingesting raw log entries from a source within a time range, parsing each entry into structured fields (timestamp, level, service, message, isError flag), extracting recurring patterns to identify the most common log signatures, and aggregating metrics like error counts, warning counts, and per-service breakdowns. Uses [Conductor](https://github.## The Problem

Your services generate thousands of log entries per minute, each with a raw timestamp, severity level, service name, and message. Before you can answer questions like "which service is producing the most errors?" or "is there a recurring NullPointerException pattern?", you need to ingest the logs from the source for a specific time range, parse each raw entry into structured fields (normalizing `ts` to `timestamp`, `msg` to `message`, flagging ERROR-level entries), extract recurring patterns to identify the most frequent log signatures (repeated exceptions, timeout messages, connection errors), and aggregate metrics (total entries, error count, warning count, top patterns by frequency). Each step depends on the previous one: you can't extract patterns from unparsed logs, and you can't aggregate metrics without knowing which entries are errors.

Without orchestration, you'd write a single log analysis method that reads, parses, pattern-matches, and aggregates in one pass. If the pattern extraction logic changes, you'd re-run everything from ingestion. There's no record of how many entries were ingested vs: how many were successfully parsed, making it impossible to detect silent parsing failures. When a log source produces malformed entries, the entire analysis crashes with no retry, and you'd lose the structured entries you already parsed.

## The Solution

**You just write the log ingestion, structured parsing, pattern extraction, and metric aggregation workers. Conductor handles sequential log processing, retries when log source connections time out, and entry count tracking at every stage to detect silent parsing failures.**

Each stage of the log pipeline is a simple, independent worker. The ingester reads raw log entries from the specified source within the configured time range, applying any input filters. The parser normalizes each raw entry into a structured format: mapping `ts` to `timestamp`, `level` to severity, `msg` to `message`, and computing derived fields like `isError` for quick filtering, then counts errors and warnings. The pattern extractor scans parsed entries to identify recurring log signatures, ranking them by frequency to surface the top patterns. The metric aggregator combines parsed entries and extracted patterns into a summary: total entries, error and warning counts, and the most common patterns. Conductor executes them in sequence, passes the growing result set between stages, retries if ingestion fails due to a log source timeout, and tracks entry counts at every stage so you can see how many raw logs became structured entries became actionable patterns. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers form the log analysis pipeline: ingesting raw entries from a source, parsing them into structured fields with severity flags, extracting recurring patterns to surface top log signatures, and aggregating error counts and per-service metrics.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateMetricsWorker** | `lp_aggregate_metrics` | Aggregates metrics from parsed log entries. |
| **ExtractPatternsWorker** | `lp_extract_patterns` | Extracts recurring patterns from parsed log entries. |
| **IngestLogsWorker** | `lp_ingest_logs` | Ingests raw log entries from a specified source. |
| **ParseEntriesWorker** | `lp_parse_entries` | Parses raw log entries into structured format. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
lp_ingest_logs
    │
    ▼
lp_parse_entries
    │
    ▼
lp_extract_patterns
    │
    ▼
lp_aggregate_metrics
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
java -jar target/log-processing-1.0.0.jar
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
java -jar target/log-processing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow log_processing \
  --version 1 \
  --input '{"logSource": "test-value", "timeRange": "2026-01-01T00:00:00Z", "filters": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w log_processing -s COMPLETED -c 5
```

## How to Extend

Connect the ingester to a real log source like Elasticsearch or CloudWatch, add ML-based pattern detection, and push metrics to Datadog, the log analysis workflow runs unchanged.

- **IngestLogsWorker** → read from real log sources: Elasticsearch queries, CloudWatch Logs Insights, Splunk search API, Kafka topics, or S3-stored log files, with time range filtering and pagination for large result sets
- **ParseEntriesWorker** → parse real log formats: Apache/Nginx access logs, JSON-structured application logs, syslog entries, or custom formats using regex or Grok patterns, with configurable field mappings per log source
- **ExtractPatternsWorker** → use real pattern detection: log clustering algorithms (Drain, Spell, LenMa) that group similar messages into templates, stack trace deduplication, or regex-based pattern matching for known error signatures
- **AggregateMetricsWorker** → write metrics to real monitoring systems: push error rates and pattern frequencies to Prometheus, Datadog, or Grafana, trigger PagerDuty alerts when error counts exceed thresholds, or store time-series data for trend analysis

Replacing the parser with a real log format handler or the pattern extractor with an ML-based anomaly detector leaves the ingest-parse-extract-aggregate pipeline intact.

**Add new stages** by inserting tasks in `workflow.json`, for example, an anomaly detection step that flags unusual log volume spikes, a correlation step that links error patterns across services to identify cascading failures, or a notification step that sends a Slack digest of the top error patterns found in each processing run.

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
log-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/logprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LogProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateMetricsWorker.java
│       ├── ExtractPatternsWorker.java
│       ├── IngestLogsWorker.java
│       └── ParseEntriesWorker.java
└── src/test/java/logprocessing/workers/
    ├── AggregateMetricsWorkerTest.java        # 4 tests
    ├── ExtractPatternsWorkerTest.java        # 5 tests
    ├── IngestLogsWorkerTest.java        # 4 tests
    └── ParseEntriesWorkerTest.java        # 4 tests
```
