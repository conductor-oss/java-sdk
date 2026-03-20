# Distributed Logging in Java Using Conductor :  Multi-Service Log Collection and Correlation

A Java Conductor workflow example for distributed logging .  collecting logs from multiple services in parallel via FORK/JOIN, then correlating them by trace ID into a unified timeline for debugging distributed transactions.

## The Problem

A distributed transaction spans three services, and you need to see what happened across all of them for a given request. Logs must be collected from each service (different log formats, different storage locations), then correlated by trace ID into a single chronological view. Without correlation, debugging a failed request means manually searching logs in three different systems and mentally stitching them together.

Without orchestration, log correlation is a manual process using grep across multiple log stores. Collection from different services runs serially instead of in parallel, slowing down incident response. When one service's log store is temporarily unavailable, the entire investigation stalls.

## The Solution

**You just write the log collection queries and trace correlation logic. Conductor handles parallel log collection across services, retries when individual log stores are slow, and timing data showing how long each service's collection took.**

Conductor's FORK/JOIN collects logs from all three services in parallel .  if one service's log store is slow, the others don't wait. A correlation worker then stitches the logs together by trace ID into a unified timeline. Every collection and correlation run is tracked with timing ,  you can see which services responded and how long each took. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three per-service collectors (CollectSvc1Worker, CollectSvc2Worker, CollectSvc3Worker) run in parallel via FORK/JOIN to gather logs from each microservice, then a CorrelateWorker stitches them into a unified timeline by trace ID.

| Worker | Task | What It Does |
|---|---|---|
| **CollectSvc1Worker** | `dg_collect_svc1` | Collects logs from service 1 for a given trace ID, returning the log count |
| **CollectSvc2Worker** | `dg_collect_svc2` | Collects logs from service 2 for a given trace ID, returning the log count |
| **CollectSvc3Worker** | `dg_collect_svc3` | Collects logs from service 3 for a given trace ID, returning the log count |
| **CorrelateWorker** | `dg_correlate` | Correlates logs from all three services by trace ID into a unified timeline, identifying the request flow across services |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
FORK_JOIN
    ├── dg_collect_svc1
    ├── dg_collect_svc2
    └── dg_collect_svc3
    │
    ▼
JOIN (wait for all branches)
dg_correlate
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
java -jar target/distributed-logging-1.0.0.jar
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
java -jar target/distributed-logging-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow distributed_logging_415 \
  --version 1 \
  --input '{"traceId": "TEST-001", "timeRange": "2026-01-01T00:00:00Z"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w distributed_logging_415 -s COMPLETED -c 5
```

## How to Extend

Each worker handles one logging concern .  connect the service log collectors to Elasticsearch or CloudWatch Logs, the correlator to stitch entries by trace ID, and the parallel-collect-then-correlate workflow stays the same.

- **CollectSvc1Worker** (`dg_collect_svc1`): query Elasticsearch, CloudWatch Logs, or Splunk for service 1's logs filtered by trace ID and time range
- **CollectSvc2Worker** (`dg_collect_svc2`): query the second service's log store .  potentially a different system or region
- **CollectSvc3Worker** (`dg_collect_svc3`): query the third service's log store .  may use a different log format requiring parsing

Point each collector at your real log backends (Elasticsearch, CloudWatch, Splunk), and the parallel collection and correlation pipeline runs unchanged.

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
distributed-logging/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/distributedlogging/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DistributedLoggingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectSvc1Worker.java
│       ├── CollectSvc2Worker.java
│       ├── CollectSvc3Worker.java
│       └── CorrelateWorker.java
└── src/test/java/distributedlogging/workers/
    └── CorrelateWorkerTest.java        # 2 tests
```
