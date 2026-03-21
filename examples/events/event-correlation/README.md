# Event Correlation in Java Using Conductor

Event Correlation. init correlation session, fork to receive order/payment/shipping events in parallel, join, correlate, and process. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to correlate related events that arrive independently from different sources. An order event, a payment event, and a shipping event may arrive at different times from different services, but they all belong to the same business transaction. The workflow must initialize a correlation session, receive all expected events (potentially in parallel), correlate them by matching fields, and process the fully correlated result.

Without orchestration, you'd build a stateful correlation engine with in-memory maps keyed by correlation ID, timeout logic for events that never arrive, manual cleanup of stale sessions, and complex multi-threaded code to handle events arriving in any order. all while hoping state is not lost on restarts.

## The Solution

**You just write the correlation-init, event-receiver, event-correlation, and processing workers. Conductor handles parallel event reception, durable session state, and automatic join before correlation.**

Each correlation concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of initializing the session, receiving events in parallel via FORK_JOIN, correlating them after all arrive, and processing the result,  with durable state that survives restarts and full visibility into which events have arrived. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Six workers correlate cross-service events: InitCorrelationWorker opens a session, ReceiveOrderWorker, ReceivePaymentWorker, and ReceiveShippingWorker collect events in parallel via FORK_JOIN, CorrelateEventsWorker matches them, and ProcessCorrelatedWorker acts on the unified result.

| Worker | Task | What It Does |
|---|---|---|
| **CorrelateEventsWorker** | `ec_correlate_events` | Correlates order, payment, and shipping events into a unified data set. Returns deterministic output with fixed match... |
| **InitCorrelationWorker** | `ec_init_correlation` | Initializes a correlation session with a fixed correlation ID and timestamp. Returns deterministic output with no ran... |
| **ProcessCorrelatedWorker** | `ec_process_correlated` | Processes correlated event data and determines the action to take. Returns deterministic output with fixed timestamps. |
| **ReceiveOrderWorker** | `ec_receive_order` | Simulates receiving an order event for event correlation. Returns deterministic, fixed order data. |
| **ReceivePaymentWorker** | `ec_receive_payment` | Simulates receiving a payment event for event correlation. Returns deterministic, fixed payment data. |
| **ReceiveShippingWorker** | `ec_receive_shipping` | Simulates receiving a shipping event for event correlation. Returns deterministic, fixed shipping data. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
ec_init_correlation
    │
    ▼
FORK_JOIN
    ├── ec_receive_order
    ├── ec_receive_payment
    └── ec_receive_shipping
    │
    ▼
JOIN (wait for all branches)
ec_correlate_events
    │
    ▼
ec_process_correlated

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
java -jar target/event-correlation-1.0.0.jar

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
java -jar target/event-correlation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_correlation_wf \
  --version 1 \
  --input '{"correlationId": "TEST-001", "expectedEvents": "sample-expectedEvents"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_correlation_wf -s COMPLETED -c 5

```

## How to Extend

Wire each receiver worker to your real event sources (Kafka topics, SQS queues) and the correlation worker to your matching logic, the parallel-receive-correlate-process workflow stays exactly the same.

- **EcInitCorrelationWorker** (`ec_init_correlation`): create a correlation session in your event store (Redis, DynamoDB) with TTL-based expiration for events that never arrive
- **EcCorrelateEventsWorker** (`ec_correlate_events`): match events by correlation ID, order ID, or custom business keys; handle partial correlations and missing events
- **EcProcessCorrelatedWorker** (`ec_process_correlated`): execute business logic on the fully correlated event set (e.g., reconcile payment with order, trigger fulfillment)

Adding a new event source means adding one receive worker and a FORK_JOIN branch, the correlation and processing logic stays the same.

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
event-correlation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventcorrelation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventCorrelationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CorrelateEventsWorker.java
│       ├── InitCorrelationWorker.java
│       ├── ProcessCorrelatedWorker.java
│       ├── ReceiveOrderWorker.java
│       ├── ReceivePaymentWorker.java
│       └── ReceiveShippingWorker.java
└── src/test/java/eventcorrelation/workers/
    ├── CorrelateEventsWorkerTest.java        # 10 tests
    ├── InitCorrelationWorkerTest.java        # 8 tests
    ├── ProcessCorrelatedWorkerTest.java        # 9 tests
    ├── ReceiveOrderWorkerTest.java        # 8 tests
    ├── ReceivePaymentWorkerTest.java        # 8 tests
    └── ReceiveShippingWorkerTest.java        # 8 tests

```
