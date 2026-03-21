# Tool Use Rate Limiting in Java Using Conductor :  Check Limits, Execute-or-Queue, Delayed Execution

Tool Use Rate Limiting. checks API rate limits before tool execution, queuing and delaying requests when throttled. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## APIs Have Rate Limits :  Respect Them

Most external APIs enforce rate limits: OpenAI allows a certain number of requests per minute, Google Maps has a daily quota, and many services return 429 (Too Many Requests) when you exceed the limit. Hitting rate limits causes errors, potential API key suspension, and degraded user experience.

Proactive rate limiting checks the current request count before making the call. If you're under the limit, execute immediately. If you've hit the limit, queue the request and execute it after a delay (when the rate window resets). This is better than reactive rate limiting (retry after 429) because it avoids the error entirely and maintains consistent response times for queued requests.

## The Solution

**You write the rate-limit checking, queuing, and delayed execution logic. Conductor handles the allowed/throttled routing, queue management, and throttle-rate analytics.**

`CheckRateLimitWorker` checks the current request count against the configured limit for the tool and API key, returning whether the request is allowed or throttled. Conductor's `SWITCH` routes accordingly: allowed requests go to `ExecuteToolWorker` for immediate execution. Throttled requests (default case) go to `QueueRequestWorker` which schedules the request for later, then `DelayedExecuteWorker` which runs the tool after the rate window resets. Conductor records the rate limit decision for each request, enabling you to track throttle rates and adjust limits.

### What You Write: Workers

Four workers manage rate limits. Checking the quota, routing allowed requests to immediate execution, and routing throttled requests through a queue for delayed execution.

| Worker | Task | What It Does |
|---|---|---|
| **CheckRateLimitWorker** | `rl_check_rate_limit` | Checks the rate limit for the given tool/API key combination. Simulates near-limit conditions: quotaUsed=98, quotaLim... |
| **DelayedExecuteWorker** | `rl_delayed_execute` | Executes a previously queued tool request after a delay. Returns the same translation result with executedImmediately... |
| **ExecuteToolWorker** | `rl_execute_tool` | Executes the tool immediately when the rate limit allows it. Returns a demo translation result with executedImme... |
| **QueueRequestWorker** | `rl_queue_request` | Queues a throttled request for later execution. Returns a fixed queueId and the estimated wait time. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
rl_check_rate_limit
    │
    ▼
SWITCH (rate_limit_decision_ref)
    ├── allowed: rl_execute_tool
    └── default: rl_queue_request -> rl_delayed_execute

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
java -jar target/tool-use-rate-limiting-1.0.0.jar

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
java -jar target/tool-use-rate-limiting-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tool_use_rate_limiting \
  --version 1 \
  --input '{"toolName": "test", "toolArgs": "sample-toolArgs", "apiKey": "sample-apiKey"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tool_use_rate_limiting -s COMPLETED -c 5

```

## How to Extend

Each worker handles one rate-limiting concern. Implement Redis-based sliding window counters for limit checking, SQS or RabbitMQ for request queuing, and exponential backoff for delayed execution, and the check-route-execute rate-limiting workflow runs unchanged.

- **CheckRateLimitWorker** (`rl_check_rate_limit`): use Redis `INCR` with `EXPIRE` for sliding-window rate limiting, or implement token bucket algorithm for burst-tolerant rate control
- **QueueRequestWorker** (`rl_queue_request`): use SQS, RabbitMQ, or Redis Streams to queue throttled requests with priority ordering and estimated execution time
- **DelayedExecuteWorker** (`rl_delayed_execute`): implement exponential backoff for repeated throttling, with circuit breaker pattern if the API is consistently overloaded

Connect to Redis for real quota tracking; the rate-limiting workflow maintains the same allowed/throttled routing interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
tool-use-rate-limiting/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/toolratelimit/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ToolUseRateLimitingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckRateLimitWorker.java
│       ├── DelayedExecuteWorker.java
│       ├── ExecuteToolWorker.java
│       └── QueueRequestWorker.java
└── src/test/java/toolratelimit/workers/
    ├── CheckRateLimitWorkerTest.java        # 8 tests
    ├── DelayedExecuteWorkerTest.java        # 9 tests
    ├── ExecuteToolWorkerTest.java        # 7 tests
    └── QueueRequestWorkerTest.java        # 8 tests

```
