# Data Partitioning in Java Using Conductor :  Split, Parallel Process, and Merge

A Java Conductor workflow example for data partitioning. splitting a dataset into two partitions based on a configurable partition key, processing both partitions simultaneously using `FORK_JOIN` parallelism, and merging the results back into a unified dataset. Uses [Conductor](https://github.

## The Problem

You have a large dataset that's too slow to process sequentially, but you can split it into independent partitions and process them in parallel. That means dividing records based on a partition key (region, category, hash), running the same processing logic against each partition simultaneously, and combining the results back into a single output. If one partition fails (bad data, timeout), the other partition's work shouldn't be lost.

Without orchestration, you'd manage `ExecutorService` thread pools manually, submit partition processing as `Callable` tasks, handle `Future` results, and write your own merge logic. If the process crashes after partition A finishes but before partition B completes, you'd re-process both. There's no visibility into which partition is running, how many records each processed, or where a failure occurred.

## The Solution

**You just write the data splitting, partition processing, and result merging workers. Conductor handles parallel partition processing via FORK_JOIN, independent per-partition retries, and crash recovery that resumes only the incomplete partition.**

Each concern is a simple, independent worker. The splitter divides the dataset into two partitions. The partition workers each process their half of the data independently. The merger combines results from both partitions into a unified output with counts from each. Conductor's `FORK_JOIN` runs both partitions simultaneously, waits for both to complete, and then triggers the merge. If one partition fails, Conductor retries just that partition. If the process crashes after one partition finishes, Conductor resumes only the incomplete one. You get all of that, without writing a single line of thread pool or synchronization code.

### What You Write: Workers

Four workers implement the split-process-merge pattern: dividing the dataset into two partitions by a configurable key, processing both partitions simultaneously via FORK_JOIN, and merging results into a unified output.

| Worker | Task | What It Does |
|---|---|---|
| **MergeResultsWorker** | `par_merge_results` | Merges results from both partitions into a single combined output. |
| **ProcessPartitionAWorker** | `par_process_partition_a` | Processes partition A by adding processed:true and partition:"A" to each record. |
| **ProcessPartitionBWorker** | `par_process_partition_b` | Processes partition B by adding processed:true and partition:"B" to each record. |
| **SplitDataWorker** | `par_split_data` | Split Data. Computes and returns partition a, partition b, total count |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
par_split_data
    │
    ▼
FORK_JOIN
    ├── par_process_partition_a
    └── par_process_partition_b
    │
    ▼
JOIN (wait for all branches)
par_merge_results

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
java -jar target/data-partitioning-1.0.0.jar

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
java -jar target/data-partitioning-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_partitioning_wf \
  --version 1 \
  --input '{"records": "sample-records", "partitionKey": "sample-partitionKey"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_partitioning_wf -s COMPLETED -c 5

```

## How to Extend

Implement real partitioning strategies (hash-based, range-based, or key-based by region), replace the processing workers with your per-record logic, and the parallel split-process-merge workflow runs unchanged.

- **SplitDataWorker** → implement real partitioning strategies (hash-based, range-based, round-robin, or key-based splitting by region/tenant/category)
- **ProcessPartitionA/BWorkers** → replace with your actual per-record processing logic (database writes, API calls, ML inference, file transformations)
- **MergeResultsWorker** → combine partition outputs with deduplication, sorting, or aggregation depending on your use case

Implementing hash-based or range-based splitting in the splitter, or replacing the processing logic entirely, leaves the parallel workflow unchanged as long as partition outputs follow the expected structure.

**Scale beyond two partitions** by adding more branches to the `FORK_JOIN` in `workflow.json`, or switch to `FORK_JOIN_DYNAMIC` for runtime-determined partition counts. Add a validation step after merge to verify no records were lost during split-process-merge.

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
data-partitioning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/datapartitioning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataPartitioningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MergeResultsWorker.java
│       ├── ProcessPartitionAWorker.java
│       ├── ProcessPartitionBWorker.java
│       └── SplitDataWorker.java
└── src/test/java/datapartitioning/workers/
    ├── MergeResultsWorkerTest.java        # 9 tests
    ├── ProcessPartitionAWorkerTest.java        # 9 tests
    ├── ProcessPartitionBWorkerTest.java        # 9 tests
    └── SplitDataWorkerTest.java        # 9 tests

```
