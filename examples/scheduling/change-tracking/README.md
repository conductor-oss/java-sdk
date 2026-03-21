# Change Tracking in Java Using Conductor :  Detect, Diff, Classify, and Record Infrastructure Changes

A Java Conductor workflow example for tracking infrastructure changes .  detecting when a resource changes, computing the diff against its previous state, classifying the change type (configuration, scaling, deployment), and recording it for audit and rollback purposes.

## The Problem

You need to know when infrastructure changes .  a security group rule was modified, an instance type was changed, a deployment rolled out a new version. For each change, you need to compute what exactly changed (diff the before/after state), classify whether it was a configuration change, scaling event, or deployment, and record everything for compliance audits and rollback capability.

Without orchestration, change tracking is either absent (you discover changes after incidents) or incomplete (you detect changes but don't record the diff). Classification is manual, diffs are computed inconsistently, and there's no centralized audit trail connecting change detection to change details.

## The Solution

**You just write the change detection and diff computation logic. Conductor handles the detect-diff-classify-record pipeline, retries when version control APIs are temporarily down, and a centralized audit trail connecting every change to its diff and risk classification.**

Each tracking concern is an independent worker .  change detection, diff computation, classification, and recording. Conductor runs them in sequence: detect a change, compute the diff, classify it, then record it. Every change event is tracked with full context ,  you can see exactly what changed, when, and how it was classified. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers track infrastructure changes: DetectChangeWorker spots version differences, DiffWorker computes lines added and removed, ClassifyChangeWorker rates the risk level, and RecordChangeWorker writes the classified change to the audit trail for rollback capability.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyChangeWorker** | `chg_classify` | Classifies changes as minor/moderate/major with a risk level based on files changed and lines modified |
| **DetectChangeWorker** | `chg_detect_change` | Detects a change in a resource by comparing current vs, previous version identifiers |
| **DiffWorker** | `chg_diff` | Computes the diff between resource versions, returning lines added/removed, files changed, and a summary |
| **RecordChangeWorker** | `chg_record` | Records the classified change to the audit trail with a unique change ID for tracking and rollback |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
chg_detect_change
    │
    ▼
chg_diff
    │
    ▼
chg_classify
    │
    ▼
chg_record

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
java -jar target/change-tracking-1.0.0.jar

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
java -jar target/change-tracking-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow change_tracking_427 \
  --version 1 \
  --input '{"resourceType": "standard", "resourceId": "TEST-001", "changeSource": "api"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w change_tracking_427 -s COMPLETED -c 5

```

## How to Extend

Each worker covers one tracking step .  connect the change detector to AWS Config or CloudTrail, the recorder to your CMDB, and the detect-diff-classify-record workflow stays the same.

- **ClassifyChangeWorker** (`chg_classify`): categorize changes by type (security, scaling, deployment, configuration) and risk level using rules or ML
- **DetectChangeWorker** (`chg_detect_change`): watch AWS Config, Kubernetes events, Terraform state, or CloudTrail for infrastructure changes in real time
- **DiffWorker** (`chg_diff`): compute structured diffs using deep comparison of JSON/YAML configurations or API responses

Connect to your real version control and config management systems, and the change tracking workflow persists without modification.

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
change-tracking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/changetracking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ChangeTrackingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyChangeWorker.java
│       ├── DetectChangeWorker.java
│       ├── DiffWorker.java
│       └── RecordChangeWorker.java
└── src/test/java/changetracking/workers/
    └── ClassifyChangeWorkerTest.java        # 2 tests

```
