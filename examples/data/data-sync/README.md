# Data Sync in Java Using Conductor :  Bidirectional Change Detection, Conflict Resolution, and Consistency Verification

A Java Conductor workflow example for bidirectional data synchronization: detecting changes in two systems since last sync, resolving conflicts when both systems modified the same record (using configurable strategies like "latest wins"), applying the resolved updates to both systems, and verifying that the two systems are consistent after sync. Uses [Conductor](https://github.## The Problem

You have data in two systems that need to stay in sync: a CRM and an ERP, a mobile app's local database and the cloud backend, a primary database and a partner system. Changes happen on both sides between sync cycles. You need to detect what changed in each system, identify conflicts where both systems modified the same record, resolve those conflicts using a configurable strategy (latest timestamp wins, source-of-truth priority, manual review), apply the merged updates to both systems, and verify consistency after the sync completes. If the apply step fails after updating system A but before updating system B, the systems are now out of sync, worse than before.

Without orchestration, you'd write a sync script that queries both systems, diffs the results, picks winners for conflicts, and writes updates. If the write to system B fails, system A has already been updated and there's no automatic rollback or retry. There's no record of which conflicts were found, how they were resolved, or whether the final state is consistent. Adding a conflict resolution strategy or a third system means rewriting deeply coupled code.

## The Solution

**You just write the change detection, conflict resolution, update application, and consistency verification workers. Conductor handles strict detect-resolve-apply-verify ordering, retries when either system is temporarily unavailable, and a full audit trail of changes detected, conflicts resolved, and updates applied.**

Each stage of the sync pipeline is a simple, independent worker. The change detector queries both systems and identifies inserts, updates, and deletes since last sync, flagging records modified in both as conflicts. The conflict resolver applies the configured strategy (latest_wins, system_a_priority, merge) to produce a unified set of updates. The applier writes the resolved changes to both systems. The consistency verifier confirms both systems match after sync. Conductor executes them in strict sequence, ensures updates only apply after conflict resolution, retries if a system is temporarily unavailable, and provides a complete audit trail of changes detected, conflicts resolved, and updates applied. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers manage bidirectional sync: detecting changes in both systems since the last sync, resolving conflicts using a configurable strategy like latest-wins, applying the merged updates to both systems, and verifying post-sync consistency.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyUpdatesWorker** | `sy_apply_updates` | Applies resolved updates to both systems. |
| **DetectChangesWorker** | `sy_detect_changes` | Detects changes in both systems and identifies conflicts. |
| **ResolveConflictsWorker** | `sy_resolve_conflicts` | Resolves conflicts using the specified strategy (e.g, latest_wins). |
| **VerifyConsistencyWorker** | `sy_verify_consistency` | Verifies that both systems are consistent after sync. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
sy_detect_changes
    │
    ▼
sy_resolve_conflicts
    │
    ▼
sy_apply_updates
    │
    ▼
sy_verify_consistency
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
java -jar target/data-sync-1.0.0.jar
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
java -jar target/data-sync-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_sync \
  --version 1 \
  --input '{"systemA": "test-value", "systemB": "test-value", "syncMode": "test-value", "conflictStrategy": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_sync -s COMPLETED -c 5
```

## How to Extend

Connect the change detector to real CDC streams via Debezium, write to CRM APIs like Salesforce with idempotent upserts, and the bidirectional sync workflow runs unchanged.

- **DetectChangesWorker** → query real change logs (CDC streams, audit tables, `updated_at` timestamps) or use change data capture tools (Debezium, DynamoDB Streams)
- **ResolveConflictsWorker** → implement additional strategies beyond "latest wins" (field-level merge, source-of-truth priority, human review via WAIT task for unresolvable conflicts)
- **ApplyUpdatesWorker** → write to real databases, CRM APIs (Salesforce, HubSpot), or ERP systems with idempotent upserts to handle retries safely
- **VerifyConsistencyWorker** → run record count comparisons, checksum verification, or sample-based data matching between the two systems

Connecting to real CRM and ERP systems or adding a new conflict resolution strategy requires no workflow modifications, as long as each worker returns the expected change and resolution structures.

**Add new capabilities** by modifying `workflow.json`, for example, a pre-sync backup step, a notification step that alerts when conflicts exceed a threshold, or support for three-way sync by adding a third change detection and apply step.

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
data-sync/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/datasync/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataSyncExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyUpdatesWorker.java
│       ├── DetectChangesWorker.java
│       ├── ResolveConflictsWorker.java
│       └── VerifyConsistencyWorker.java
└── src/test/java/datasync/workers/
    ├── ApplyUpdatesWorkerTest.java        # 3 tests
    ├── DetectChangesWorkerTest.java        # 5 tests
    ├── ResolveConflictsWorkerTest.java        # 4 tests
    └── VerifyConsistencyWorkerTest.java        # 5 tests
```
