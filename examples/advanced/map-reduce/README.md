# Map-Reduce in Java Using Conductor: Split Documents, Search in Parallel, Reduce Results

You have 10 million log entries and need to find every occurrence of "ERROR" with context. A single-threaded scan takes four hours. You split the corpus into partitions and spin up parallel workers; but partition 2 hits a corrupt record and crashes, so you restart the entire four-hour job. Even when it works, you have no visibility into which partition found the most hits, how long each took, or whether the merge step deduplicated correctly. This example implements map-reduce with Conductor: split a document corpus into partitions, search each in parallel via `FORK_JOIN`, retry any failed partition independently, and reduce the per-partition results into a unified ranked output. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Searching Large Document Sets Sequentially Is Too Slow

Searching 10 million documents for a term takes minutes when done sequentially. Map-reduce splits the corpus into partitions, searches each partition independently and in parallel, then merges the results. The speedup is linear with the number of partitions; but coordinating parallel search workers, waiting for all to complete, handling a mapper that crashes on a corrupt partition, and merging partial result sets into a single ranked output is complex to build from scratch.

If one partition's search worker fails, you need to retry just that partition without restarting the entire search. And you need to see how long each partition took, which ones had the most hits, and whether the reduce step merged them correctly.

## The Solution

**You write the map and reduce logic. Conductor handles parallel fan-out, per-partition retries, and result merging.**

`MprSplitInputWorker` partitions the document corpus into chunks. A `FORK_JOIN` fans out the map phase. `MprMap1Worker`, `MprMap2Worker`, and `MprMap3Worker` each search their assigned partition for the search term in parallel and return matching documents with scores. The `JOIN` waits for all three mappers to finish. `MprReduceWorker` merges the per-partition results into a single combined output. `MprOutputWorker` formats and delivers the final result. Conductor handles the parallel fan-out, retries any failed mapper independently, and tracks the hit count and timing for each partition.

### What You Write: Workers

Six workers implement the map-reduce pattern: corpus splitting, three parallel partition mappers, result reduction, and final output formatting, each scoped to one phase of the search pipeline.

| Worker | Task | What It Does |
|---|---|---|
| **MprMap1Worker** | `mpr_map_1` | Scans the first document partition for the search term and returns per-document match counts |
| **MprMap2Worker** | `mpr_map_2` | Scans the second document partition for the search term and returns per-document match counts |
| **MprMap3Worker** | `mpr_map_3` | Scans the third document partition for the search term and returns per-document match counts |
| **MprOutputWorker** | `mpr_output` | Produces the final summary with the search term and total occurrences across all documents |
| **MprReduceWorker** | `mpr_reduce` | Combines map results from all partitions, ranking documents by match count and summing totals |
| **MprSplitInputWorker** | `mpr_split_input` | Splits the document corpus into three partitions for parallel map processing |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations, the pattern and Conductor orchestration stay the same.

### The Workflow

```
mpr_split_input
    │
    ▼
FORK_JOIN
    ├── mpr_map_1
    ├── mpr_map_2
    └── mpr_map_3
    │
    ▼
JOIN (wait for all branches)
mpr_reduce
    │
    ▼
mpr_output

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
java -jar target/map-reduce-1.0.0.jar

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
java -jar target/map-reduce-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow mpr_map_reduce \
  --version 1 \
  --input '{"documents": ["doc1", "doc2", "doc3"], "searchTerm": "error"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w mpr_map_reduce -s COMPLETED -c 5

```

## How to Extend

Each worker handles one map-reduce phase. Replace the demo document searches with real Elasticsearch or Lucene queries and the parallel split-map-reduce pipeline runs unchanged.

- **MprSplitInputWorker** (`mpr_split_input`): read real document sets from S3, HDFS, or Elasticsearch and partition them by shard, date range, or document ID range
- **MprMap*Workers** (`mpr_map_1/2/3`). Execute real search using Lucene, Elasticsearch `_search` API, or full-text PostgreSQL queries against each document partition
- **MprReduceWorker** (`mpr_reduce`): merge and rank results using TF-IDF scoring, BM25, or custom relevance algorithms, deduplicating across partitions

The per-partition result contract stays fixed. Swap the demo document search for a real Elasticsearch or Solr query and the reduce-merge pipeline runs unchanged.

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
map-reduce/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/mapreduce/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MapReduceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MprMap1Worker.java
│       ├── MprMap2Worker.java
│       ├── MprMap3Worker.java
│       ├── MprOutputWorker.java
│       ├── MprReduceWorker.java
│       └── MprSplitInputWorker.java
└── src/test/java/mapreduce/workers/
    ├── MprMap1WorkerTest.java        # 4 tests
    ├── MprMap2WorkerTest.java        # 4 tests
    ├── MprMap3WorkerTest.java        # 4 tests
    ├── MprOutputWorkerTest.java        # 4 tests
    ├── MprReduceWorkerTest.java        # 4 tests
    └── MprSplitInputWorkerTest.java        # 4 tests

```
