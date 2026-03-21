# Data Deduplication in Java Using Conductor :  Key Computation, Duplicate Detection, and Record Merging

A Java Conductor workflow example for data deduplication: loading records, computing dedup keys from configurable match fields (normalized and lowercased), grouping records by key to find duplicates, merging duplicate groups into a single canonical record, and emitting the clean, deduplicated dataset. Uses [Conductor](https://github.

## The Problem

Your dataset has duplicate records, the same customer entered twice with slightly different casing, the same order submitted by two systems, the same product listed with trailing whitespace in the name. You need to find and eliminate these duplicates before the data goes into your analytics pipeline, CRM, or data warehouse. That means computing dedup keys from configurable match fields (lowercased, trimmed for consistency), grouping records that share the same key, deciding which record to keep when duplicates are found (first seen, most complete, most recent), and emitting a clean dataset with a summary of how many duplicates were removed.

Without orchestration, you'd write a single method that loads records, builds a `HashMap` for grouping, picks winners inline, and outputs results. If the key computation logic changes (adding fuzzy matching or phonetic encoding), you'd rewrite the entire pipeline. If the process crashes after finding duplicates but before merging, you'd lose that work. There's no audit trail showing how many duplicates were found, which groups were merged, or what the dedup ratio was.

## The Solution

**You just write the record loading, key computation, duplicate detection, group merging, and emission workers. Conductor handles the load-key-detect-merge-emit pipeline, per-step retries, and full tracking of how many duplicates were found and removed at each stage.**

Each stage of the deduplication pipeline is a simple, independent worker. The loader reads records from the data source. The key computer generates a dedup key for each record by normalizing and concatenating the configured match fields. The duplicate finder groups records by key and identifies groups with more than one member. The merger selects a canonical record from each duplicate group. The emitter outputs the deduplicated dataset with a summary of original count, duplicates found, and final count. Conductor executes them in sequence, passes keyed records between steps, retries if a step fails, and tracks exactly how many duplicates were found and merged. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers form the deduplication pipeline: loading records, computing normalized dedup keys from configurable match fields, detecting duplicate groups, merging groups into canonical records, and emitting the clean dataset.

| Worker | Task | What It Does |
|---|---|---|
| **ComputeKeysWorker** | `dp_compute_keys` | Computes keys |
| **EmitDedupedWorker** | `dp_emit_deduped` | Emits the final deduplicated result with a summary. |
| **FindDuplicatesWorker** | `dp_find_duplicates` | Groups keyed records by dedupKey and identifies duplicate groups. |
| **LoadRecordsWorker** | `dp_load_records` | Loads records for deduplication and passes them through with a count. |
| **MergeGroupsWorker** | `dp_merge_groups` | Merges duplicate groups by picking the first record from each group and removing dedupKey. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
dp_load_records
    │
    ▼
dp_compute_keys
    │
    ▼
dp_find_duplicates
    │
    ▼
dp_merge_groups
    │
    ▼
dp_emit_deduped

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
java -jar target/data-dedup-1.0.0.jar

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
java -jar target/data-dedup-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_dedup \
  --version 1 \
  --input '{"records": "sample-records", "matchFields": "sample-matchFields"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_dedup -s COMPLETED -c 5

```

## How to Extend

Add fuzzy matching with Levenshtein distance or phonetic encoding in the key worker, use locality-sensitive hashing for large-scale detection, and the deduplication workflow runs unchanged.

- **LoadRecordsWorker** → read records from a real data source (database query, S3 CSV, Kafka topic, API endpoint)
- **ComputeKeysWorker** → add fuzzy matching (Levenshtein distance), phonetic encoding (Soundex, Metaphone), or ML-based similarity scoring for near-duplicate detection
- **FindDuplicatesWorker** → use locality-sensitive hashing (LSH) or MinHash for efficient duplicate detection at scale across millions of records
- **MergeGroupsWorker** → implement smarter merge strategies (most recent wins, most complete record, field-level merge from multiple duplicates)
- **EmitDedupedWorker** → write the clean dataset to a database, publish to a downstream topic, or trigger a notification with the dedup summary

Replacing the key computation with fuzzy matching or the merge strategy with a most-recent-wins policy leaves the pipeline unchanged, as long as each worker outputs the expected keyed-record and merge-result structures.

**Add new stages** by inserting tasks in `workflow.json`, for example, a human review step for uncertain matches, a quarantine queue for records that need manual dedup decisions, or a feedback loop that improves matching rules based on past merge decisions.

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
data-dedup/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/datadedup/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataDedupExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ComputeKeysWorker.java
│       ├── EmitDedupedWorker.java
│       ├── FindDuplicatesWorker.java
│       ├── LoadRecordsWorker.java
│       └── MergeGroupsWorker.java
└── src/test/java/datadedup/workers/
    ├── ComputeKeysWorkerTest.java        # 9 tests
    ├── EmitDedupedWorkerTest.java        # 8 tests
    ├── FindDuplicatesWorkerTest.java        # 9 tests
    ├── LoadRecordsWorkerTest.java        # 8 tests
    └── MergeGroupsWorkerTest.java        # 8 tests

```
