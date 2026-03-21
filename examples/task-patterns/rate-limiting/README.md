# Rate Limiting in Java with Conductor

Rate limiting demo. demonstrates task-level rate limiting with concurrency and frequency constraints configured entirely in the task definition. The worker code contains zero throttling logic. Conductor enforces the limits across all workflow instances automatically. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to call an external API that enforces rate limits. Say, 5 requests per 10-second window with a maximum of 2 concurrent connections. When hundreds of workflow instances are running simultaneously, each one trying to call the same API, you need to throttle at the task level so the API is not overwhelmed. Without throttling, you get 429 (Too Many Requests) errors, the API blocks your client, and workflows fail in bursts.

Without orchestration, you'd implement a token bucket or sliding window rate limiter in your application code, shared across all threads via a concurrent data structure or Redis. That rate limiter must survive process restarts, handle distributed deployments where multiple instances share the same limit, and be tuned per-API. Building and maintaining a example-grade distributed rate limiter is a significant engineering effort.

## The Solution

**You just write the API call worker with zero throttling logic. Conductor enforces concurrency and frequency limits via the task definition.**

This example demonstrates Conductor's task-level rate limiting. concurrency and frequency constraints configured in the task definition, not in your code. The task definition for `rl_api_call` sets `rateLimitPerFrequency: 5` (max 5 executions per window), `rateLimitFrequencyInSeconds: 10` (10-second window), and `concurrentExecLimit: 2` (max 2 running at once). The RlApiCallWorker simply takes a batchId and returns a deterministic result string ("batch-{batchId}-done"). When many workflow instances start simultaneously, Conductor queues excess tasks and releases them only when the rate limit permits. The worker code contains zero throttling logic, it just makes the call and returns the result.

### What You Write: Workers

A single worker demonstrates zero-code rate limiting: RlApiCallWorker processes a batch and returns a result string, with no throttling logic whatsoever. Conductor enforces the concurrency cap and frequency window entirely through the task definition.

| Worker | Task | What It Does |
|---|---|---|
| **RlApiCallWorker** | `rl_api_call` | Takes a batchId (string or number) and returns result="batch-{batchId}-done". Defaults batchId to "unknown" if null or blank. The rate limiting (5 per 10s, max 2 concurrent) is enforced by Conductor via the task definition, the worker has no throttling logic. |

The demo worker produces a realistic output shape so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

### The Workflow

```
rl_api_call (rate-limited: 5 per 10s, max 2 concurrent)

```

### Rate Limit Configuration

The rate limiting is set on the **task definition**, not in the workflow or worker code:

```json
{
  "name": "rl_api_call",
  "rateLimitPerFrequency": 5,
  "rateLimitFrequencyInSeconds": 10,
  "concurrentExecLimit": 2
}

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
java -jar target/rate-limiting-1.0.0.jar

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
java -jar target/rate-limiting-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
# Start a single workflow
conductor workflow start \
  --workflow rate_limit_demo \
  --version 1 \
  --input '{"batchId": 42}'

# Start several to observe rate limiting in action
for i in $(seq 1 10); do
  conductor workflow start \
    --workflow rate_limit_demo \
    --version 1 \
    --input "{\"batchId\": $i}"
done

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rate_limit_demo -s COMPLETED -c 5

```

## How to Extend

Replace the stub API call with your real rate-limited integration (Stripe, Twilio, Salesforce), and the task-definition-driven throttling works unchanged.

- **RlApiCallWorker** (`rl_api_call`): make the real rate-limited API call: Stripe for payment processing (100 req/s), Twilio for SMS sending (150 msg/s), Salesforce for CRM updates (100 req/s per user), or any third-party API with rate limits. The worker just makes the call. Conductor handles the throttling.
- **Tune the limits**: adjust `rateLimitPerFrequency`, `rateLimitFrequencyInSeconds`, and `concurrentExecLimit` in the task definition to match your API's published rate limits. No code changes required.
- **Per-API rate limiting**: create separate task definitions for each rate-limited API (e.g., `rl_stripe_call`, `rl_twilio_call`), each with its own rate limit configuration.

Swapping in real rate-limited API calls (Stripe, Twilio, Salesforce) and adjusting the frequency and concurrency limits in the task definition requires no changes to the worker code or workflow structure.

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
rate-limiting/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ratelimiting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup (with TaskDef rate limit registration)
│   ├── RateLimitingExample.java     # Main entry point (supports --workers mode)
│   └── workers/
│       └── RlApiCallWorker.java
└── src/test/java/ratelimiting/workers/
    └── RlApiCallWorkerTest.java     # 7 tests

```
