# Chaining HTTP Tasks in Java with Conductor

Chain HTTP system tasks for API orchestration. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to call multiple external APIs in sequence, where each call depends on the previous one's response. A prepare step builds the request parameters, an HTTP system task calls the first API, the response feeds into the next HTTP call, and finally a worker processes the combined results. If any API call fails or returns an error, the chain must handle the failure without leaving data in an inconsistent state.

Without orchestration, you'd write nested HTTP client calls with try/catch blocks around each one, manually passing response data between calls, and implementing retry logic for transient API failures. Error handling becomes deeply nested, and there is no record of which API calls succeeded before the chain broke.

## The Solution

**You just write the request preparation and response processing workers. Conductor handles the HTTP calls, retries, and chaining.**

This example demonstrates Conductor's HTTP system tasks chained together for multi-step API orchestration. A PrepareRequest worker builds the request context, HTTP system tasks make the actual API calls (no worker code needed for these), and a ProcessResponse worker handles the final result. Conductor manages retries for each HTTP call independently, tracks every request/response pair, and resumes from the failed call if the process crashes mid-chain.

### What You Write: Workers

Two workers bookend the HTTP chain: PrepareRequestWorker builds the request context before the system HTTP tasks, and ProcessResponseWorker handles the final API result after the chain completes.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareRequestWorker** | `http_prepare_request` | Worker that prepares an HTTP request by extracting the search term from the query input. Runs before the HTTP system ... |
| **ProcessResponseWorker** | `http_process_response` | Worker that processes the HTTP response from the API call. Receives the status code and body from the preceding HTTP ... |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
register_task_via_http [HTTP]
    │
    ▼
verify_task_via_http [HTTP]
    │
    ▼
format_http_result [INLINE]
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
java -jar target/chaining-http-tasks-1.0.0.jar
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
java -jar target/chaining-http-tasks-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow http_chain_demo \
  --version 1 \
  --input '{"taskName": "test", "conductorApiUrl": "https://example.com"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w http_chain_demo -s COMPLETED -c 5
```

## How to Extend

Point the HTTP system tasks at your real API endpoints and replace the prepare/process workers with your request-building and response-handling logic, the chained orchestration workflow runs unchanged.

- **PrepareRequestWorker** (`http_prepare_request`): build the initial request context with authentication tokens, API endpoint URLs, and query parameters from your configuration store
- **ProcessResponseWorker** (`http_process_response`): parse and validate the final API response, transform it into your domain model, and write results to your database or message queue

Changing the target APIs or adding new HTTP tasks to the chain does not affect the workers, since request preparation and response processing remain decoupled from the actual API calls.

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
chaining-http-tasks/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/chaininghttptasks/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ChainingHttpTasksExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PrepareRequestWorker.java
│       └── ProcessResponseWorker.java
└── src/test/java/chaininghttptasks/workers/
    ├── PrepareRequestWorkerTest.java        # 5 tests
    └── ProcessResponseWorkerTest.java        # 6 tests
```
