# User Behavior Analytics in Java Using Conductor :  Event Collection, Sessionization, Pattern Analysis, and Anomaly Flagging

A Java Conductor workflow example for user behavior analytics .  collecting user events, sessionizing them into coherent sessions, analyzing behavioral patterns, and flagging anomalies that may indicate fraud or account compromise.

## The Problem

You need to analyze user behavior for security and product insights. Raw events (logins, page views, transactions) must be collected, grouped into sessions (a sequence of events from the same user within a time window), analyzed for behavioral patterns (typical session length, common navigation paths, usual transaction amounts), and flagged when behavior deviates from the baseline (login from a new country, unusual transaction velocity, impossible travel).

Without orchestration, user behavior analytics is either a batch job in a data warehouse (delayed by hours) or a streaming pipeline that's complex to build and maintain. Sessionization, pattern analysis, and anomaly detection are separate systems that don't share context. By the time an anomaly is detected, the fraudulent transaction has already completed.

## The Solution

**You just write the sessionization logic and behavioral anomaly rules. Conductor handles the collect-sessionize-analyze-flag sequence, retries when event streams or analytics services are temporarily down, and a complete audit of every analysis run with session counts, risk scores, and anomaly flags.**

Each analytics step is an independent worker .  event collection, sessionization, pattern analysis, and anomaly flagging. Conductor runs them in sequence: collect events, group into sessions, analyze patterns, then flag anomalies. Every analysis run is tracked with session counts, pattern metrics, and anomaly flags. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

CollectEventsWorker ingests raw user events, SessionizeWorker groups them into coherent sessions by time proximity, AnalyzePatternsWorker computes risk scores and baseline deviations, and FlagAnomaliesWorker triggers security alerts when behavior exceeds configured thresholds.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzePatternsWorker** | `uba_analyze_patterns` | Analyzes behavioral patterns across sessions, computing a risk score, baseline deviation, and identifying anomalies |
| **CollectEventsWorker** | `uba_collect_events` | Collects user events (logins, page views, API calls, downloads) for a given user, returning event count and types |
| **FlagAnomaliesWorker** | `uba_flag_anomalies` | Flags users whose risk score exceeds the configured threshold and triggers an alert for security review |
| **SessionizeWorker** | `uba_sessionize` | Groups raw events into user sessions by time proximity, returning session count and average duration |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
uba_collect_events
    │
    ▼
uba_sessionize
    │
    ▼
uba_analyze_patterns
    │
    ▼
uba_flag_anomalies
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
java -jar target/user-behavior-analytics-1.0.0.jar
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
java -jar target/user-behavior-analytics-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow user_behavior_analytics_429 \
  --version 1 \
  --input '{"userId": "TEST-001", "timeRange": "2026-01-01T00:00:00Z", "riskThreshold": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w user_behavior_analytics_429 -s COMPLETED -c 5
```

## How to Extend

Each worker manages one analytics phase .  connect the event collector to your clickstream pipeline (Segment, Snowplow), the anomaly flagger to your fraud detection rules, and the collect-sessionize-analyze-flag workflow stays the same.

- **AnalyzePatternsWorker** (`uba_analyze_patterns`): compute behavioral baselines .  session duration distributions, navigation path frequencies, transaction velocity
- **CollectEventsWorker** (`uba_collect_events`): pull events from Kafka/Kinesis event streams, Segment, or your application's event log
- **FlagAnomaliesWorker** (`uba_flag_anomalies`): detect anomalies against baselines using statistical methods or ML models, trigger fraud review workflows or account locks

Point the event collector at your Kafka stream and the anomaly flagger at your fraud detection rules, and the analytics pipeline adapts to production seamlessly.

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
user-behavior-analytics/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/userbehavioranalytics/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── UserBehaviorAnalyticsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzePatternsWorker.java
│       ├── CollectEventsWorker.java
│       ├── FlagAnomaliesWorker.java
│       └── SessionizeWorker.java
└── src/test/java/userbehavioranalytics/workers/
    └── FlagAnomaliesWorkerTest.java        # 2 tests
```
