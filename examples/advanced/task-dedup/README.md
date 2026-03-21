# Task Deduplication in Java Using Conductor :  Hash Input, Check Cache, Execute or Return Cached

A Java Conductor workflow example for task deduplication. hashing the task input to create a fingerprint, checking whether that fingerprint has been seen before, and routing via `SWITCH` to either execute the task for the first time or return the cached result from a previous execution. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Identical Inputs Should Produce Cached Results, Not Redundant Computation

A report generation request comes in with the same parameters as yesterday's run. same date range, same filters, same output format. Regenerating the report takes 15 minutes and produces identical output. If you could detect that the input hasn't changed, you'd return yesterday's result in milliseconds instead of burning compute.

Task deduplication means hashing the input to create a deterministic fingerprint, looking up that hash in a cache, and short-circuiting execution if a cached result exists. The `SWITCH` task makes the decision explicit. new inputs go to the execute path, seen inputs go to the cache-return path.

## The Solution

**You write the hashing and cache-check logic. Conductor handles the hit-or-miss routing, retries, and execution tracking.**

`HashInputWorker` computes a deterministic hash (SHA-256) of the task payload to create a fingerprint. `CheckSeenWorker` looks up the hash in the deduplication cache to determine if this exact input has been processed before. A `SWITCH` task routes based on the result: new inputs go to `ExecuteNewWorker` for full processing, while previously-seen inputs go to `ReturnCachedWorker` which returns the stored result immediately. Conductor records whether each execution was a cache hit or miss, and the hash used for deduplication.

### What You Write: Workers

Four workers implement the cache-or-compute pattern: input hashing, cache lookup, new-task execution, and cached-result retrieval, routing each request through a hit-or-miss decision.

| Worker | Task | What It Does |
|---|---|---|
| **CheckSeenWorker** | `tdd_check_seen` | Checks if a hash has been seen before to determine duplicate status. |
| **ExecuteNewWorker** | `tdd_execute_new` | Processes a new (non-duplicate) task and caches the result for future dedup. |
| **HashInputWorker** | `tdd_hash_input` | Hashes the input payload to produce a deterministic deduplication key. |
| **ReturnCachedWorker** | `tdd_return_cached` | Returns a previously cached result for a duplicate task. |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
tdd_hash_input
    │
    ▼
tdd_check_seen
    │
    ▼
SWITCH (tdd_switch_ref)
    ├── new: tdd_execute_new
    ├── dup: tdd_return_cached
    └── default: tdd_execute_new

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
java -jar target/task-dedup-1.0.0.jar

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
java -jar target/task-dedup-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tdd_task_dedup \
  --version 1 \
  --input '{"payload": {"key": "value"}, "cacheEnabled": true}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tdd_task_dedup -s COMPLETED -c 5

```

## How to Extend

Each worker handles one deduplication concern. replace the demo hash cache with a real Redis or DynamoDB lookup and the execute-or-return-cached routing runs unchanged.

- **HashInputWorker** (`tdd_hash_input`): compute real content hashes using SHA-256 (`MessageDigest`), or use MurmurHash3 for faster non-cryptographic fingerprinting of large payloads
- **CheckSeenWorker** (`tdd_check_seen`): look up hashes in a real cache: Redis `EXISTS`/`GET`, DynamoDB `getItem`, or a Bloom filter for space-efficient probabilistic dedup
- **ExecuteNewWorker** (`tdd_execute_new`): run the real computation (report generation, data transformation, API call) and store the result in the cache keyed by the input hash with a configurable TTL

The hash and cache-result contract stays fixed. Swap the in-memory cache for Redis or DynamoDB and the hit-or-miss routing runs unchanged.

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
task-dedup/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/taskdedup/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TaskDedupExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckSeenWorker.java
│       ├── ExecuteNewWorker.java
│       ├── HashInputWorker.java
│       └── ReturnCachedWorker.java
└── src/test/java/taskdedup/workers/
    ├── CheckSeenWorkerTest.java        # 8 tests
    ├── ExecuteNewWorkerTest.java        # 8 tests
    ├── HashInputWorkerTest.java        # 8 tests
    └── ReturnCachedWorkerTest.java        # 8 tests

```
