# Signals in Java with Conductor

Signals demo .  send data to running workflows via WAIT task completion. Two WAIT tasks pause the workflow until external signals arrive with shipping and delivery data. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need an order fulfillment workflow that pauses and waits for external events at two points: first, it waits for a shipping confirmation (tracking number and carrier) from the warehouse or shipping partner, and second, it waits for a delivery confirmation (delivery timestamp and recipient signature) from the carrier. The workflow cannot continue past each wait point until the external system sends the signal with the required data. Between signals, the order sits in a known state .  prepared, shipped, or delivered ,  potentially for hours or days.

Without orchestration, you'd poll a database or message queue for shipping and delivery updates, maintaining a state machine in application code that tracks where each order is in the fulfillment process. If the polling service crashes, orders are stuck until it restarts. There is no built-in way for an external system to "push" data into a running process at a specific step, and correlating incoming signals with the right order workflow requires custom routing logic.

## The Solution

**You just write the order preparation, shipping processing, and delivery completion workers. Conductor handles the durable WAIT pauses and signal-to-workflow routing.**

This example demonstrates Conductor's WAIT task for pausing a workflow until an external signal arrives. SigPrepareWorker prepares the order for shipping. The workflow then hits a WAIT task (`wait_shipping`) and pauses indefinitely until an external system completes the task with shipping data (tracking number and carrier). When the signal arrives, SigProcessShippingWorker processes the shipping information. The workflow pauses again at a second WAIT task (`wait_delivery`) until the carrier sends delivery confirmation (timestamp and signature). Finally, SigCompleteWorker marks the order as delivered. Each WAIT task's output data comes from the external signal, wired into the next worker via `${wait_shipping_ref.output.trackingNumber}` and `${wait_delivery_ref.output.signature}`.

### What You Write: Workers

Three workers handle the order fulfillment flow between WAIT pauses: SigPrepareWorker readies the order for shipping, SigProcessShippingWorker handles the tracking data after the first external signal, and SigCompleteWorker marks delivery after the second signal arrives.

| Worker | Task | What It Does |
|---|---|---|
| **SigCompleteWorker** | `sig_complete` | Completes the order after the delivery signal is received. Takes orderId, deliveredAt, and signature. Returns { done:... |
| **SigPrepareWorker** | `sig_prepare` | Prepares the order for shipping. Takes an orderId and returns { ready: true }. |
| **SigProcessShippingWorker** | `sig_process_shipping` | Processes shipping information after the shipping signal is received. Takes orderId, trackingNumber, and carrier. Ret... |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
sig_prepare
    │
    ▼
wait_shipping [WAIT]
    │
    ▼
sig_process_shipping
    │
    ▼
wait_delivery [WAIT]
    │
    ▼
sig_complete
```

## Example Output

```
=== Signals: Send Data to Running Workflows via WAIT Task Completio ===

Step 1: Registering task definitions...
  Registered: sig_prepare, sig_process_shipping, sig_complete

Step 2: Registering workflow 'signal_demo'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [sig_complete] Completing order:
  [sig_prepare] Preparing order:
  [sig_process_shipping] Processing shipping for order:

  Status: COMPLETED
  Output: {orderId=..., deliveredAt=..., signature=..., done=...}

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
java -jar target/signals-1.0.0.jar
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
java -jar target/signals-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow signal_demo \
  --version 1 \
  --input '{"orderId": "ORD-12345", "ORD-12345": "sample-ORD-12345"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w signal_demo -s COMPLETED -c 5
```

## How to Extend

Replace the order preparation and processing workers with your real fulfillment logic, and the signal-driven WAIT-based workflow runs unchanged as external systems push shipping and delivery data.

- **SigPrepareWorker** (`sig_prepare`): generate a packing slip, reserve inventory, and create a shipment record in your WMS; the WAIT task after this step pauses until the warehouse or 3PL sends a shipping confirmation webhook
- **SigProcessShippingWorker** (`sig_process_shipping`): update the order status with the tracking number from the signal, send a shipping confirmation email to the customer via SendGrid/SES, and register for carrier tracking updates
- **SigCompleteWorker** (`sig_complete`): mark the order as delivered in your OMS, update the customer's order history, trigger a post-delivery satisfaction survey, and close the fulfillment case

Replacing the simulated order logic with real shipment tracking does not change the WAIT-based signal flow, since the durable pause and signal-to-workflow routing are managed entirely by Conductor.

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
signals/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/signals/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SignalsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── SigCompleteWorker.java
│       ├── SigPrepareWorker.java
│       └── SigProcessShippingWorker.java
└── src/test/java/signals/workers/
    ├── SigCompleteWorkerTest.java        # 6 tests
    ├── SigPrepareWorkerTest.java        # 6 tests
    └── SigProcessShippingWorkerTest.java        # 6 tests
```
