# Log Aggregation in Java with Conductor :  Collect, Parse, Enrich, Store

Aggregate logs: collect raw logs, parse them into structured format, enrich with metadata, and store in the log store. Pattern: collect -> parse -> enrich -> store. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Raw Logs Are Useless Without Processing

A production system generates gigabytes of logs daily across dozens of services. Raw log lines like `2025-01-15T14:32:01Z INFO [payment-svc] Payment processed for order ORD-12345` are useless in raw form .  they need to be collected from all sources (files, stdout, syslog), parsed into structured fields (timestamp, level, service, message, order ID), enriched with context (deployment version, environment, geo-IP of the request), and stored in a searchable system (Elasticsearch, Loki, CloudWatch).

Each step transforms the data: collection handles different log formats and transports. Parsing extracts structured fields using format-specific patterns (JSON, logfmt, regex). Enrichment adds context that isn't in the log line itself. Storage indexes the enriched data for search and analysis. If the storage step fails, parsed and enriched logs should be buffered .  not lost.

## The Solution

**You write the parsing and enrichment logic. Conductor handles the collect-parse-enrich-store pipeline, retries on storage failures, and processing throughput tracking.**

`CollectLogsWorker` gathers log entries from the configured sources and time range .  application log files, container stdout, syslog streams, and cloud provider log services. `ParseLogsWorker` transforms raw log lines into structured records ,  extracting timestamp, level, service name, message, and any embedded fields using format-appropriate parsers. `EnrichLogsWorker` adds context metadata ,  deployment version, environment name, geo-IP lookup for request IPs, and trace correlation IDs. `StoreLogsWorker` indexes the enriched records in the log store for search, alerting, and dashboarding. Conductor pipelines these four steps and records processing throughput and error rates.

### What You Write: Workers

Four workers process the log pipeline. Collecting raw logs, parsing into structured records, enriching with metadata, and storing in a searchable index.

| Worker | Task | What It Does |
|---|---|---|
| **CollectLogs** | `la_collect_logs` | Collects raw logs from the specified sources for the given time range. |
| **EnrichLogs** | `la_enrich_logs` | Enriches parsed logs with additional metadata fields. |
| **ParseLogs** | `la_parse_logs` | Parses raw logs into a structured format. |
| **StoreLogs** | `la_store_logs` | Stores enriched logs in the log store index. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

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

## Example Output

```
=== Example 412: Log Aggregatio ===

Step 1: Registering task definitions...
  Registered: la_collect_logs, la_parse_logs, la_enrich_logs, la_store_logs

Step 2: Registering workflow 'log_aggregation_412'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  Status: COMPLETED

Result: PASSED
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
  --input '{"sources": "sample-sources", "api-gateway": "sample-api-gateway", "auth-service": "sample-auth-service", "timeRange": "2025-01-15T10:00:00Z", "last-1h": "sample-last-1h", "logLevel": "sample-logLevel"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w log_aggregation_412 -s COMPLETED -c 5
```

## How to Extend

Each worker handles one log processing step .  replace the simulated calls with Fluent Bit for collection, Grok patterns for parsing, or Elasticsearch Bulk API for storage, and the aggregation workflow runs unchanged.

- **CollectLogs** (`la_collect_logs`): use Fluent Bit or Vector for log collection from Kubernetes pods, AWS CloudWatch Logs, or syslog endpoints with backpressure handling
- **ParseLogs** (`la_parse_logs`): implement Grok patterns for common log formats (Apache, Nginx, application logs), JSON parsing for structured logs, and multiline log handling for stack traces
- **EnrichLogs** (`la_enrich_logs`): add metadata like geo-IP lookups, service name resolution from Kubernetes labels, deployment version tagging, and request correlation IDs for distributed tracing
- **StoreLogs** (`la_store_logs`): write to Elasticsearch via the Bulk API, Grafana Loki via the push API, or AWS CloudWatch Logs for searchable, indexed log storage with retention policies

Swap in Fluentd and Elasticsearch for real log processing; the aggregation pipeline keeps the same data flow contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

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
│       ├── CollectLogs.java
│       ├── EnrichLogs.java
│       ├── ParseLogs.java
│       └── StoreLogs.java
└── src/test/java/logaggregation/workers/
    ├── CollectLogsTest.java        # 8 tests
    └── ParseLogsTest.java        # 8 tests
```
