# Dynamic Fork in Java with Conductor

You need to process N items in parallel, but N is only known at runtime. a user submits 3 URLs today and 300 tomorrow. Hardcoding 5 parallel branches means you can't scale to 50 or handle 1 without wasted resources. Rolling your own thread pool means managing `CompletableFuture` objects, writing barrier logic, and losing all progress if the process crashes after finishing 8 of 10 fetches. This example uses [Conductor](https://github.com/conductor-oss/conductor) `FORK_JOIN_DYNAMIC` to fan out to exactly N parallel branches at runtime, join when all complete, and aggregate the results, with per-branch retries and crash recovery built in.

## The Problem

You need to fetch multiple URLs in parallel, but you don't know how many URLs there are until runtime. A user submits a list of 3, 10, or 100 URLs, and each one needs to be fetched concurrently. . Not sequentially. After all fetches complete, the results must be aggregated into a single response with total size and per-URL metadata. If one fetch fails, it shouldn't block the others, and you need to know exactly which URLs succeeded and which didn't.

Without orchestration, you'd spin up a thread pool, submit each URL as a callable, manage a CompletableFuture for each, write barrier logic to wait for all completions, and aggregate results manually. If the process crashes after fetching 8 of 10 URLs, you lose all progress and start over. Adding a new URL to the list means the same code handles 3 threads or 300, with no visibility into which fetches are in flight.

## The Solution

**You just write the task preparation, per-URL fetch, and aggregation workers. Conductor handles the dynamic fanout, parallel execution, and join.**

This example demonstrates Conductor's FORK_JOIN_DYNAMIC task, runtime-determined parallelism. The PrepareTasksWorker inspects the input URL list and generates a dynamic task definition for each URL (one `df_fetch_url` task per URL with a unique reference name). Conductor fans out to N parallel branches, where N is determined entirely at runtime. Each FetchUrlWorker branch fetches its assigned URL independently. A JOIN task waits for every branch to complete, then the AggregateWorker collects all results from the join output into a single summary with total count and cumulative size. If one URL times out, Conductor retries just that branch, the other fetches are unaffected.

### What You Write: Workers

Three workers implement the dynamic fanout: PrepareTasksWorker generates one task definition per URL at runtime, FetchUrlWorker fetches each URL independently in its own parallel branch, and AggregateWorker collects all results after the join.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **AggregateWorker** | `df_aggregate` | Aggregates results from all dynamic fork branches. Takes the joinOutput map (keyed by taskReferenceName) produced by ... | Simulated |
| **FetchUrlWorker** | `df_fetch_url` | Fetches a URL and returns metadata. In a real application, this would make an HTTP request. Here it returns determini... | Simulated |
| **PrepareTasksWorker** | `df_prepare_tasks` | Prepares the dynamic task list and input map for FORK_JOIN_DYNAMIC. Takes a list of URLs and generates: - dynamicTask... | Simulated |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic, the task pattern and Conductor orchestration remain unchanged.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Dynamic parallelism** | FORK_JOIN_DYNAMIC creates parallel branches at runtime based on input data |
| **Automatic joining** | JOIN waits for all dynamically forked tasks to complete |

### The Workflow

```
df_prepare_tasks
    │
    ▼
FORK_JOIN_DYNAMIC (parallel, determined at runtime)
    │
    ▼
JOIN (wait for all branches)
df_aggregate
```

## Example Output

```
=== Dynamic FORK: Parallel URL Fetching ===

Step 1: Registering task definitions...
  Registered: df_prepare_tasks, df_fetch_url, df_aggregate

Step 2: Registering workflow 'dynamic_fork_demo'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...

  [df_prepare_tasks] Preparing 3 dynamic tasks
  [df_fetch_url] Fetching https://example.com
  [df_fetch_url] Fetching https://openai.com
  [df_fetch_url] Fetching https://conductor.netflix.com
  [df_aggregate] Aggregated 3 results, totalSize=45600

  Workflow ID: 3fa85f64-5542-4562-b3fc-2c963f66afa6

Step 5: Waiting for completion...
  Status: COMPLETED
  Output: {results=Success, totalProcessed=3, totalSize=45600}

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
java -jar target/dynamic-fork-1.0.0.jar
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
java -jar target/dynamic-fork-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow dynamic_fork_demo \
  --version 1 \
  --input '{"urls": ["https://example.com", "https://openai.com", "https://conductor.netflix.com"]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w dynamic_fork_demo -s COMPLETED -c 5
```

## How to Extend

Replace the URL fetcher with your real HTTP client or API integration, and the runtime-determined parallel fanout workflow runs unchanged regardless of the input list size.

- **PrepareTasksWorker** (`df_prepare_tasks`): query a database or message queue for the list of URLs to fetch, filter by domain or priority, and generate the dynamic task definitions with per-URL configuration (headers, auth tokens, timeout overrides)
- **FetchUrlWorker** (`df_fetch_url`): make real HTTP requests using Apache HttpClient or OkHttp, follow redirects, capture response headers and body, handle content types (JSON, HTML, binary), and return structured metadata (status code, content-length, response time)
- **AggregateWorker** (`df_aggregate`): merge all fetch results into a report, compute statistics (success rate, average response time, total bytes transferred), flag failures for retry, and write the aggregated output to S3 or a database

Replacing simulated fetches with real HTTP requests does not change the dynamic fork workflow, as long as each branch worker returns the expected URL metadata and status fields.

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
dynamic-fork/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dynamicfork/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DynamicForkExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWorker.java
│       ├── FetchUrlWorker.java
│       └── PrepareTasksWorker.java
└── src/test/java/dynamicfork/workers/
    ├── AggregateWorkerTest.java        # 7 tests
    ├── FetchUrlWorkerTest.java        # 8 tests
    └── PrepareTasksWorkerTest.java        # 7 tests
```
