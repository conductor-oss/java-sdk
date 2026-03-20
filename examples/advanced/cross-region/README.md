# Cross-Region Data Replication in Java Using Conductor :  Replicate, Sync, Verify Consistency

A Java Conductor workflow example for cross-region data replication .  copying a dataset from a primary region to a replica region, synchronizing the data and computing checksums, and verifying consistency between the two regions. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Keeping Data Consistent Across Regions

Multi-region architectures improve latency and availability, but replicating data from us-east-1 to eu-west-1 is not a simple copy. You need to initiate the replication, wait for the data to sync, compute checksums on both sides, and verify they match. If the checksums diverge .  because a write landed on the primary during sync, or a network partition caused partial replication ,  you need to know immediately, not discover it hours later when a user in Europe sees stale data.

Manually coordinating replication means writing polling loops to check sync status, implementing checksum verification across regions with different API endpoints, handling transient failures on cross-region network calls, and logging enough context to diagnose consistency drift. Each step depends on the previous one .  you can't verify consistency before sync completes, and sync requires a valid replication ID from the initial copy.

## The Solution

**You write the replication and consistency checks. Conductor handles sequencing, retries, and cross-region audit trails.**

`XrReplicateWorker` initiates the data replication from the primary region to the replica region and returns a replication ID. `XrSyncWorker` waits for synchronization to complete and computes checksums for both the primary and replica copies. `XrVerifyConsistencyWorker` compares the checksums .  if they match, the regions are consistent; if they diverge, the workflow output flags the inconsistency. Conductor sequences these steps, retries any that fail due to cross-region network issues, and records the replication ID, checksums, and consistency verdict for every run.

### What You Write: Workers

Three workers own the replication lifecycle: data copying between regions, checksum synchronization, and consistency verification, each targeting one phase of cross-region durability.

| Worker | Task | What It Does |
|---|---|---|
| **XrReplicateWorker** | `xr_replicate` | Copies data from the primary region to the replica region, tracking bytes transferred |
| **XrSyncWorker** | `xr_sync` | Synchronizes primary and replica by computing checksums and measuring replication lag |
| **XrVerifyConsistencyWorker** | `xr_verify_consistency` | Compares primary and replica checksums to confirm cross-region data consistency |

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
xr_replicate
    │
    ▼
xr_sync
    │
    ▼
xr_verify_consistency
```

## Example Output

```
=== Cross-Region Demo ===

Step 1: Registering task definitions...
  Registered: xr_replicate, xr_sync, xr_verify_consistency

Step 2: Registering workflow 'cross_region_demo'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [replicate] Processing
  [sync] Processing
  [verify] Processing

  Status: COMPLETED
  Output: {replicated=..., replicationId=..., bytesTransferred=..., synced=...}

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
java -jar target/cross-region-1.0.0.jar
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
java -jar target/cross-region-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cross_region_demo \
  --version 1 \
  --input '{"primaryRegion": "us-east-1", "us-east-1": "replicaRegion", "replicaRegion": "eu-west-1", "eu-west-1": "datasetId", "datasetId": "DS-USERS-2024", "DS-USERS-2024": "sample-DS-USERS-2024"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cross_region_demo -s COMPLETED -c 5
```

## How to Extend

Each worker manages one replication concern .  replace the simulated cross-region copies with real S3 replication or DynamoDB Global Tables APIs and the sync-and-verify pipeline runs unchanged.

- **XrReplicateWorker** (`xr_replicate`): call AWS S3 Cross-Region Replication API, DynamoDB Global Tables, or `pg_dump`/`pg_restore` across PostgreSQL instances in different regions
- **XrSyncWorker** (`xr_sync`): poll the replication status endpoint (e.g., `describeTable` for DynamoDB, S3 replication metrics) and compute MD5/SHA-256 checksums on both sides
- **XrVerifyConsistencyWorker** (`xr_verify_consistency`): query both regions and compare row counts, checksums, or last-modified timestamps; alert via PagerDuty or Slack if drift is detected

The replication and checksum output contract stays fixed. Swap the simulated region APIs for real DynamoDB Global Tables or S3 Cross-Region Replication and the consistency pipeline runs unchanged.

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
cross-region/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/crossregion/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CrossRegionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── XrReplicateWorker.java
│       ├── XrSyncWorker.java
│       └── XrVerifyConsistencyWorker.java
└── src/test/java/crossregion/workers/
    ├── XrReplicateWorkerTest.java        # 4 tests
    ├── XrSyncWorkerTest.java        # 4 tests
    └── XrVerifyConsistencyWorkerTest.java        # 4 tests
```
