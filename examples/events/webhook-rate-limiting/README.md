# Webhook Rate Limiting in Java Using Conductor

Rate limit incoming webhooks per sender. Identifies the sender, checks their request rate, and uses SWITCH to allow processing or queue for throttling. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to rate-limit incoming webhooks per sender to protect your system from webhook floods. Each incoming webhook must be identified by sender, checked against their rate limit, and either allowed through for processing or queued for throttled delivery. Without rate limiting, a misbehaving sender can overwhelm your processing pipeline and degrade service for all other senders.

Without orchestration, you'd implement a rate limiter with token buckets or sliding windows in middleware, manually tracking per-sender request counts, handling Redis failures gracefully, and balancing between rejecting requests (losing data) and queuing them (risking memory exhaustion).

## The Solution

**You just write the sender-identification, rate-check, process, and throttle-queue workers. Conductor handles SWITCH-based allow/throttle routing, per-sender rate tracking, and a full audit of every rate-limit decision.**

Each rate-limiting concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of identifying the sender, checking their rate, routing via a SWITCH task to process or throttle, and tracking every webhook's rate-limit decision. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers enforce per-sender rate limits: IdentifySenderWorker extracts the sender identity, CheckRateWorker evaluates their request count, ProcessAllowedWorker handles permitted requests, and QueueThrottledWorker defers excess traffic.

| Worker | Task | What It Does |
|---|---|---|
| **CheckRateWorker** | `wl_check_rate` | Checks the current request rate for a sender against the rate limit. |
| **IdentifySenderWorker** | `wl_identify_sender` | Identifies the sender of an incoming webhook request. |
| **ProcessAllowedWorker** | `wl_process_allowed` | Processes an allowed webhook request. |
| **QueueThrottledWorker** | `wl_queue_throttled` | Queues a throttled webhook request for later retry. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### The Workflow

```
wl_identify_sender
    │
    ▼
wl_check_rate
    │
    ▼
SWITCH (switch_ref)
    ├── allowed: wl_process_allowed
    ├── throttled: wl_queue_throttled
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
java -jar target/webhook-rate-limiting-1.0.0.jar
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
java -jar target/webhook-rate-limiting-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow webhook_rate_limiting \
  --version 1 \
  --input '{"senderId": "TEST-001", "payload": "test-value", "rateLimit": 10}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w webhook_rate_limiting -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real sender identity resolver (API keys, OAuth), rate limiter (Redis token bucket, AWS API Gateway), and throttle queue, the identify-check-route rate-limiting workflow stays exactly the same.

- **Sender identifier**: extract sender identity from API keys, webhook signatures, IP addresses, or OAuth tokens
- **Rate checker**: implement token bucket or sliding window algorithms using Redis (INCR + EXPIRE) or a rate-limiting service (Kong, AWS API Gateway)
- **Processor**: implement your actual webhook processing logic for allowed requests
- **Throttle handler**: queue throttled webhooks for delayed processing, or return 429 Too Many Requests with Retry-After headers

Connecting CheckRateWorker to a Redis-backed counter or QueueThrottledWorker to a real delay queue requires no workflow modifications.

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
webhook-rate-limiting/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/webhookratelimiting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WebhookRateLimitingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckRateWorker.java
│       ├── IdentifySenderWorker.java
│       ├── ProcessAllowedWorker.java
│       └── QueueThrottledWorker.java
└── src/test/java/webhookratelimiting/workers/
    ├── CheckRateWorkerTest.java        # 9 tests
    ├── IdentifySenderWorkerTest.java        # 8 tests
    ├── ProcessAllowedWorkerTest.java        # 8 tests
    └── QueueThrottledWorkerTest.java        # 9 tests
```
