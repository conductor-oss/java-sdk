# Dataset Versioning in Java Using Conductor :  Snapshot, Tag, Diff, and Rollback

A Java Conductor workflow example for dataset versioning .  taking a point-in-time snapshot of a dataset, tagging it with a version name, storing the metadata, computing a diff against a previous version, and rolling back if the diff reveals problems. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Datasets Change, and You Need to Track How

A data pipeline updates your ML training dataset daily. Yesterday's model performed well, but today's retrained model has degraded accuracy. Was it the new data? Which rows changed? Can you roll back to yesterday's version and retrain? Without versioning, you're guessing .  there's no snapshot to compare against, no diff to show what changed, and no tag to identify which version produced the good model.

Dataset versioning means capturing a snapshot before each update, tagging it (e.g., `v2.3-daily-2024-01-15`), comparing it against the previous tag to see additions, deletions, and schema changes, and rolling back if the diff crosses a threshold .  too many deleted rows, unexpected column changes, or data quality regressions. Coordinating snapshot, tag, store, diff, and rollback as a reliable pipeline is where manual scripts fall apart.

## The Solution

**You write the snapshot and diff logic. Conductor handles the versioning pipeline, retries, and rollback coordination.**

`DvrSnapshotWorker` captures a point-in-time snapshot of the dataset and returns a snapshot ID. `DvrTagWorker` attaches a human-readable tag name (like `v1.0` or `prod-2024-01-15`) to the snapshot. `DvrStoreWorker` persists the snapshot metadata .  dataset ID, snapshot ID, and tag ,  to the version catalog. `DvrDiffWorker` compares the current tag against the previous tag and produces diff statistics (rows added, removed, changed) plus a flag indicating whether rollback is needed. `DvrRollbackWorker` reverts to the previous version if the diff signals a problem. Conductor records every snapshot ID, tag, and diff result so you have a complete version history.

### What You Write: Workers

Five workers manage the version lifecycle: snapshot capture, tag assignment, metadata storage, diff computation, and conditional rollback, each scoped to one versioning concern.

| Worker | Task | What It Does |
|---|---|---|
| **DvrDiffWorker** | `dvr_diff` | Computes a change summary (added/modified/deleted rows) and determines if rollback is needed |
| **DvrRollbackWorker** | `dvr_rollback` | Rolls back the dataset to the previous version if the diff flagged issues |
| **DvrSnapshotWorker** | `dvr_snapshot` | Creates a point-in-time snapshot of the dataset with row count and checksum |
| **DvrStoreWorker** | `dvr_store` | Persists the tagged version to versioned storage (e.g., S3) under the dataset/tag path |
| **DvrTagWorker** | `dvr_tag` | Applies a version tag (e.g., v1, v2) to the current snapshot for future reference |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
dvr_snapshot
    │
    ▼
dvr_tag
    │
    ▼
dvr_store
    │
    ▼
dvr_diff
    │
    ▼
dvr_rollback
```

## Example Output

```
=== Data Versioning Demo ===

Step 1: Registering task definitions...
  Registered: dvr_snapshot, dvr_tag, dvr_store, dvr_diff, dvr_rollback

Step 2: Registering workflow 'data_versioning_demo'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [diff] Processing
  [rollback] Processing
  [snapshot] Processing
  [store] Processing
  [tag] Processing

  Status: COMPLETED
  Output: {summary=..., diffStats=..., needsRollback=..., rolledBack=...}

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
java -jar target/data-versioning-1.0.0.jar
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
java -jar target/data-versioning-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_versioning_demo \
  --version 1 \
  --input '{"datasetId": "DS-TRANSACTIONS", "DS-TRANSACTIONS": "tagName", "tagName": "v2.3.0", "v2.3.0": "previousTag", "previousTag": "v2.2.0", "v2.2.0": "sample-v2.2.0"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_versioning_demo -s COMPLETED -c 5
```

## How to Extend

Each worker owns one versioning step .  replace the simulated snapshot and diff calls with real DVC or LakeFS APIs and the tag-diff-rollback pipeline runs unchanged.

- **DvrSnapshotWorker** (`dvr_snapshot`): take real snapshots using DVC (`dvc commit`), LakeFS branches, Delta Lake `DESCRIBE HISTORY`, or S3 versioned object copies
- **DvrDiffWorker** (`dvr_diff`): compute real diffs using DVC `diff`, LakeFS `diff` API, or SQL queries comparing row counts and checksums between versions
- **DvrRollbackWorker** (`dvr_rollback`): revert using DVC `checkout`, LakeFS `revert`, or restoring an S3 object version to make the previous dataset active again

The snapshot and diff output contracts stay fixed. Swap the simulated storage for real DVC, LakeFS, or Delta Lake and the tag-diff-rollback pipeline runs unchanged.

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
data-versioning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dataversioning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataVersioningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DvrDiffWorker.java
│       ├── DvrRollbackWorker.java
│       ├── DvrSnapshotWorker.java
│       ├── DvrStoreWorker.java
│       └── DvrTagWorker.java
└── src/test/java/dataversioning/workers/
    ├── DvrDiffWorkerTest.java        # 4 tests
    ├── DvrRollbackWorkerTest.java        # 4 tests
    ├── DvrSnapshotWorkerTest.java        # 4 tests
    ├── DvrStoreWorkerTest.java        # 4 tests
    └── DvrTagWorkerTest.java        # 4 tests
```
