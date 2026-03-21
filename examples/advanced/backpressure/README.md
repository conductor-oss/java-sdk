# Queue Backpressure in Java Using Conductor :  Monitor Depth, Throttle Producers, Shed Load

A Java Conductor workflow example for queue backpressure management. monitoring queue depth against configurable thresholds, throttling producer rates when pressure builds, and shedding load when the queue hits critical levels. Uses [Conductor](https://github.

## When Queues Overflow

A downstream service slows down, consumers fall behind, and your queue depth starts climbing. At 10,000 messages, latency creeps up. At 50,000, memory pressure kicks in. At 100,000, the broker starts rejecting publishes and upstream services cascade-fail. The difference between a minor slowdown and a full outage is whether you react to queue pressure before it becomes critical.

Reacting means measuring queue depth, classifying the pressure level (ok, high, critical), and taking graduated action. letting traffic flow normally when depth is low, throttling producer rates when it climbs past a high-water mark, and actively shedding low-priority messages when depth crosses the critical threshold. Building that decision tree with the right fallbacks and observability is where manual approaches break down.

## The Solution

**You write the monitoring and throttling logic. Conductor handles the conditional routing, retries, and execution tracking.**

`BkpMonitorQueueWorker` samples the queue depth and classifies pressure as `ok`, `high`, or `critical` based on the configured thresholds. A `SWITCH` task routes to the appropriate response: `BkpHandleOkWorker` logs normal operation, `BkpThrottleWorker` reduces the producer rate by a calculated percentage, and `BkpShedLoadWorker` drops the lowest-priority messages to bring depth back under control. Conductor's conditional routing makes the graduated response declarative. no if/else chains, just a clean branch per pressure level.

### What You Write: Workers

The backpressure response splits across four workers: queue monitoring, normal-flow handling, rate throttling, and load shedding, each owning one pressure tier.

| Worker | Task | What It Does |
|---|---|---|
| **BkpHandleOkWorker** | `bkp_handle_ok` | Logs normal operation and signals that traffic can proceed without throttling |
| **BkpMonitorQueueWorker** | `bkp_monitor_queue` | Samples queue depth, classifies pressure level (ok/high/critical) against thresholds, and calculates throttle/shed percentages |
| **BkpShedLoadWorker** | `bkp_shed_load` | Drops low-priority messages by the calculated shed percentage to bring queue depth under control |
| **BkpThrottleWorker** | `bkp_throttle` | Reduces producer rate by the calculated throttle percentage when queue depth exceeds the high-water mark |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
bkp_monitor_queue
    │
    ▼
SWITCH (bkp_switch_ref)
    ├── ok: bkp_handle_ok
    ├── high: bkp_throttle
    ├── critical: bkp_shed_load
    └── default: bkp_handle_ok

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
java -jar target/backpressure-1.0.0.jar

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
java -jar target/backpressure-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow bkp_backpressure \
  --version 1 \
  --input '{"queueName": "test", "thresholdHigh": "sample-thresholdHigh", "thresholdCritical": "sample-thresholdCritical"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w bkp_backpressure -s COMPLETED -c 5

```

## How to Extend

Each worker addresses one pressure-response concern. replace the simulated queue metrics with real SQS or RabbitMQ monitoring APIs and the graduated throttle-or-shed logic runs unchanged.

- **BkpMonitorQueueWorker** (`bkp_monitor_queue`): query real queue metrics from SQS (`getQueueAttributes`), RabbitMQ management API, or Kafka consumer group lag via `kafka-consumer-groups.sh`
- **BkpThrottleWorker** (`bkp_throttle`): call your API gateway's rate-limiting endpoint (e.g., Kong, AWS API Gateway throttle settings) to dynamically reduce producer throughput
- **BkpShedLoadWorker** (`bkp_shed_load`): purge low-priority messages from the queue using SQS `deleteMessage` with priority-based filtering, or move them to a dead-letter queue for later replay

Each worker returns the same output shape, so replacing simulated metrics with real SQS or RabbitMQ monitoring APIs requires no workflow changes.

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
backpressure/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/backpressure/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BackpressureExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BkpHandleOkWorker.java
│       ├── BkpMonitorQueueWorker.java
│       ├── BkpShedLoadWorker.java
│       └── BkpThrottleWorker.java
└── src/test/java/backpressure/workers/
    ├── BkpHandleOkWorkerTest.java        # 4 tests
    ├── BkpMonitorQueueWorkerTest.java        # 4 tests
    ├── BkpShedLoadWorkerTest.java        # 4 tests
    └── BkpThrottleWorkerTest.java        # 4 tests

```
