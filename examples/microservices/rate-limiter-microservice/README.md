# Rate Limiter Microservice in Java with Conductor

Distributed rate limiting workflow that checks quotas, processes or rejects requests, and updates counters per client. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

API rate limiting protects backend services from being overwhelmed. Each incoming request must have its client quota checked, and based on the result, the request is either processed (with the counter incremented) or rejected with a retry-after hint. The check and update must be consistent to avoid exceeding the limit.

Without orchestration, rate-limiting logic is embedded in API gateway middleware with no visibility into per-client quota usage. Changing rate limits requires redeploying the gateway, and there is no audit trail of rejected requests.

## The Solution

**You just write the quota-check, request-processing, rejection, and counter-update workers. Conductor handles conditional allow/reject routing via SWITCH, per-client retry policies, and an audit trail of every rate-limit decision.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers implement distributed rate limiting: CheckQuotaWorker evaluates per-client quotas, ProcessRequestWorker handles allowed requests, RejectRequestWorker returns 429 responses, and UpdateCounterWorker increments the usage counter.

| Worker | Task | What It Does |
|---|---|---|
| **CheckQuotaWorker** | `rl_check_quota` | Checks the rate limit quota for a client on a given endpoint. |
| **ProcessRequestWorker** | `rl_process_request` | Processes the request when quota is available. |
| **RejectRequestWorker** | `rl_reject_request` | Rejects a request when the rate limit quota is exceeded. |
| **UpdateCounterWorker** | `rl_update_counter` | Updates the rate limit counter after processing a request. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
rl_check_quota
    │
    ▼
SWITCH (decision_ref)
    ├── false: rl_reject_request
    └── default: rl_process_request -> rl_update_counter
```

## Example Output

```
=== Rate Limiter Microservice Demo ===

Step 1: Registering task definitions...
  Registered: rl_check_quota, rl_process_request, rl_update_counter, rl_reject_request

Step 2: Registering workflow 'rate_limiter_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [rl_check_quota]
  [rl_process_request] Request processed successfully
  [rl_reject_request] Rate limit exceeded for
  [rl_update_counter] Incremented counter for

  Status: COMPLETED
  Output: {allowed=..., remaining=..., limit=..., window=...}

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
java -jar target/rate-limiter-microservice-1.0.0.jar
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
java -jar target/rate-limiter-microservice-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rate_limiter_workflow \
  --version 1 \
  --input '{"clientId": "client-99", "client-99": "endpoint", "endpoint": "/api/orders", "/api/orders": "request", "request": {"key": "value"}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rate_limiter_workflow -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real Redis-backed rate counter, backend service, and HTTP response layer, the quota-check-process-or-reject workflow stays exactly the same.

- **CheckQuotaWorker** (`rl_check_quota`): query a Redis-backed sliding-window or token-bucket counter for real quota data
- **ProcessRequestWorker** (`rl_process_request`): forward the request to the actual backend service for processing
- **RejectRequestWorker** (`rl_reject_request`): return an HTTP 429 response with Retry-After and X-RateLimit-Remaining headers

Swapping the quota store from in-memory to a Redis sliding-window counter does not change the check-process-or-reject workflow.

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
rate-limiter-microservice/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ratelimitermicroservice/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RateLimiterMicroserviceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckQuotaWorker.java
│       ├── ProcessRequestWorker.java
│       ├── RejectRequestWorker.java
│       └── UpdateCounterWorker.java
└── src/test/java/ratelimitermicroservice/workers/
    ├── CheckQuotaWorkerTest.java        # 8 tests
    ├── ProcessRequestWorkerTest.java        # 7 tests
    ├── RejectRequestWorkerTest.java        # 8 tests
    └── UpdateCounterWorkerTest.java        # 7 tests
```
