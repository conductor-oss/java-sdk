# Creating Workers in Java with Conductor: Three Common Worker Patterns

You've defined a workflow in JSON, registered it with Conductor, and hit "start"; but nothing happens. The workflow sits in RUNNING state forever because Conductor workflows don't execute code; workers do. And right now you have zero workers polling for tasks. This example builds three workers from scratch, a synchronous transform, a simulated API fetch, and an error-handling processor, so you can see the `Worker` interface in action and understand how Conductor delegates real work to your Java code.

## Learning to Write Conductor Workers

A Conductor worker is a Java class that implements the `Worker` interface: it receives a `Task`, does work, and returns a `TaskResult`. But there are different patterns for different situations. Synchronous workers transform data in-place. Async-style workers simulate external calls (API requests, database queries) that might be slow. Error-handling workers demonstrate how to set `FAILED` status with error messages so Conductor knows to retry or route to failure handling.

This example chains all three patterns in a single workflow so you can see how each type of worker looks, how Conductor passes data between them, and how error handling works.

## The Solution

**You just write the transform, fetch, and processing logic in each worker class. Conductor handles sequencing, retries, and error recovery.**

Three workers demonstrate three patterns, a transform worker that processes text synchronously, a fetch worker that simulates an external data call, and a process worker that shows proper error handling. Conductor chains them, demonstrating how outputs from one worker become inputs to the next.

### What You Write: Workers

This example demonstrates three common patterns for writing Conductor workers: stateless transformations, external API calls, and conditional logic.

| Worker | Task | What It Does |
|---|---|---|
| **SimpleTransformWorker** | `simple_transform` | Takes a text string and returns four fields: `upper` (uppercased), `lower` (lowercased), `length` (character count), and `original` (unchanged input). Pure logic, no side effects. |
| **FetchDataWorker** | `fetch_data` | Takes a `source` name and returns 3 deterministic records prefixed with the source name (e.g., `"my-api-record-1"`). Includes a 100ms sleep to simulate API latency. |
| **SafeProcessWorker** | `safe_process` | Takes a list of records and scores each one using a deterministic hash-based algorithm (PASS if score >= 50, FAIL otherwise). Wraps all logic in try/catch. On exception, returns `FAILED` status so Conductor can retry. |

### The Workflow

```
simple_transform
    │
    ▼
fetch_data
    │
    ▼
safe_process

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
java -jar target/creating-workers-1.0.0.jar

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
java -jar target/creating-workers-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow worker_demo_workflow \
  --version 1 \
  --input '{"text": "Hello Conductor", "source": "example-api"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w worker_demo_workflow -s COMPLETED -c 5

```

## How to Extend

- **SimpleTransformWorker** (`simple_transform`): add NLP processing, content sanitization, or format conversion (e.g., Markdown to HTML).
- **FetchDataWorker** (`fetch_data`): replace simulated records with a real REST API call, database query, or message queue read.
- **SafeProcessWorker** (`safe_process`): implement real scoring/validation logic (e.g., data quality checks, ML inference, business rule evaluation).

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
creating-workers/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/creatingworkers/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkerPatternsExample.java   # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FetchDataWorker.java
│       ├── SafeProcessWorker.java
│       └── SimpleTransformWorker.java
└── src/test/java/creatingworkers/workers/
    ├── FetchDataWorkerTest.java
    ├── SafeProcessWorkerTest.java
    └── SimpleTransformWorkerTest.java

```
