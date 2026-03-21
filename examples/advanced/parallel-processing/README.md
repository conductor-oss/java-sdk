# Parallel Chunk Processing in Java Using Conductor :  Split, Process in Parallel, Merge

A Java Conductor workflow example for parallel data processing. splitting a dataset into chunks based on a configurable chunk size, processing each chunk simultaneously via `FORK_JOIN`, and merging the per-chunk results into a single output. Uses [Conductor](https://github.

## Processing Large Datasets Sequentially Wastes Time

A 10 GB CSV file needs to be processed. parsing rows, applying transformations, computing aggregates. Doing it sequentially takes an hour. Splitting it into three chunks and processing them in parallel takes 20 minutes, but you need to manage the chunking logic, wait for all three to finish, handle a chunk that fails while the others succeed, and merge the partial results into a coherent whole.

Building parallel processing with raw threads means managing a thread pool, implementing a barrier to wait for all chunks, retrying failed chunks without redoing successful ones, and merging heterogeneous partial results. Each concern is simple alone, but combining them reliably is where the complexity lives.

## The Solution

**You write the chunking and per-partition logic. Conductor handles parallel execution, per-chunk retries, and result merging.**

`PprSplitWorkWorker` divides the dataset into chunks based on the configured chunk size. A `FORK_JOIN` processes all three chunks in parallel. `PprChunk1Worker`, `PprChunk2Worker`, and `PprChunk3Worker` each handle their assigned partition independently. The `JOIN` waits for all three to complete. `PprMergeWorker` combines the per-chunk outputs into a single merged result. Conductor handles the parallel fan-out, retries any failed chunk independently, and tracks per-chunk timing so you can identify slow partitions.

### What You Write: Workers

Five workers implement the split-process-merge pattern: data chunking, three parallel chunk processors, and a result merger, each handling one partition independently.

| Worker | Task | What It Does |
|---|---|---|
| **PprChunk1Worker** | `ppr_chunk_1` | Processes the first data chunk in parallel (e.g., doubles each value) and reports items processed |
| **PprChunk2Worker** | `ppr_chunk_2` | Processes the second data chunk in parallel and reports items processed |
| **PprChunk3Worker** | `ppr_chunk_3` | Processes the third data chunk in parallel and reports items processed |
| **PprMergeWorker** | `ppr_merge` | Combines results from all three parallel chunks into a single merged output |
| **PprSplitWorkWorker** | `ppr_split_work` | Divides the input data into three equal chunks for parallel processing |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
ppr_split_work
    │
    ▼
FORK_JOIN
    ├── ppr_chunk_1
    ├── ppr_chunk_2
    └── ppr_chunk_3
    │
    ▼
JOIN (wait for all branches)
ppr_merge

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
java -jar target/parallel-processing-1.0.0.jar

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
java -jar target/parallel-processing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ppr_parallel_processing \
  --version 1 \
  --input '{"dataset": {"key": "value"}, "chunkSize": 10}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ppr_parallel_processing -s COMPLETED -c 5

```

## How to Extend

Each worker processes one data chunk independently. replace the simulated transformations with real ETL or batch processing logic and the parallel split-process-merge pipeline runs unchanged.

- **PprSplitWorkWorker** (`ppr_split_work`): implement real chunking: split files by byte offset, partition database tables by ID range, or use Spark's partitioning to create balanced chunks
- **PprChunk*Workers** (`ppr_chunk_1/2/3`). run real per-chunk processing: ETL transformations, image resizing batches, or ML inference on data partitions using your compute framework of choice
- **PprMergeWorker** (`ppr_merge`): merge real outputs: concatenate processed CSV files, union database result sets, or aggregate per-chunk metrics into global totals

The per-chunk result contract stays fixed. Swap the simulated transformation for real Spark or pandas processing and the split-merge pipeline runs unchanged.

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
parallel-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/parallelprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ParallelProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PprChunk1Worker.java
│       ├── PprChunk2Worker.java
│       ├── PprChunk3Worker.java
│       ├── PprMergeWorker.java
│       └── PprSplitWorkWorker.java
└── src/test/java/parallelprocessing/workers/
    ├── PprChunk1WorkerTest.java        # 4 tests
    ├── PprChunk2WorkerTest.java        # 4 tests
    ├── PprChunk3WorkerTest.java        # 4 tests
    ├── PprMergeWorkerTest.java        # 4 tests
    └── PprSplitWorkWorkerTest.java        # 4 tests

```
