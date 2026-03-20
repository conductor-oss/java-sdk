# Wire Tap Pattern in Java Using Conductor :  Process Messages While Tapping an Audit Copy in Parallel

A Java Conductor workflow example for the wire tap pattern .  receiving a message and simultaneously processing it through the main business flow while tapping a copy to an audit/monitoring system, using `FORK_JOIN` for parallel execution. Uses [Conductor](https://github.## Auditing Must Not Slow Down the Main Flow

Every payment transaction must be processed and also logged to the compliance audit trail. If you audit synchronously before processing, you add latency to every transaction. If you audit after processing, a crash between process and audit means a transaction exists without an audit record. The wire tap pattern solves this by running both in parallel .  the main flow and the audit tap execute simultaneously, so neither blocks the other.

Building this manually means spawning two threads per message, ensuring the audit log captures the exact same data the main flow received, handling the case where the audit write fails but the main flow succeeds, and joining both results before moving on.

## The Solution

**You write the business logic and audit tap. Conductor handles parallel execution, independent retries, and completion tracking.**

`WtpReceiveWorker` ingests the message. A `FORK_JOIN` immediately splits into two parallel paths: `WtpMainFlowWorker` processes the message through the normal business logic, while `WtpTapAuditWorker` writes a copy to the audit log based on the configured audit level. Both run simultaneously .  the audit tap doesn't add latency to the main flow. The `JOIN` waits for both to complete. Conductor ensures both paths see the exact same input, retries either independently if one fails, and records whether both the main flow and audit tap succeeded.

### What You Write: Workers

Three workers split between the main flow and the audit tap: message reception, business logic processing, and parallel audit logging, neither path blocking the other.

| Worker | Task | What It Does |
|---|---|---|
| **WtpMainFlowWorker** | `wtp_main_flow` | Processes the message through the primary business logic path |
| **WtpReceiveWorker** | `wtp_receive` | Receives the incoming message and makes it available to both the main flow and the tap |
| **WtpTapAuditWorker** | `wtp_tap_audit` | Copies the message to an audit log without affecting the main flow (the wire tap) |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### The Workflow

```
wtp_receive
    │
    ▼
FORK_JOIN
    ├── wtp_main_flow
    └── wtp_tap_audit
    │
    ▼
JOIN (wait for all branches)
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
java -jar target/wire-tap-1.0.0.jar
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
java -jar target/wire-tap-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wtp_wire_tap \
  --version 1 \
  --input '{"message": "test-value", "auditLevel": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wtp_wire_tap -s COMPLETED -c 5
```

## How to Extend

Each worker handles either the main flow or the audit tap .  replace the simulated audit writes with real Elasticsearch or compliance logging APIs and the parallel tap-while-processing pattern runs unchanged.

- **WtpMainFlowWorker** (`wtp_main_flow`): run your real business logic: process payment transactions, handle order fulfillment, or execute trade orders
- **WtpTapAuditWorker** (`wtp_tap_audit`): write to a real audit store: append-only Kafka topic for immutable logs, S3 for compliance archives, or Elasticsearch for searchable audit trails
- **WtpReceiveWorker** (`wtp_receive`): consume from a real message source: Kafka consumer, SQS queue, or webhook endpoint, passing the raw message to both the main flow and audit tap

The main-flow and audit-tap output contracts stay fixed. Swap the simulated audit log for a real compliance database or Splunk sink and the parallel tap runs unchanged.

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
wire-tap/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/wiretap/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WireTapExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── WtpMainFlowWorker.java
│       ├── WtpReceiveWorker.java
│       └── WtpTapAuditWorker.java
└── src/test/java/wiretap/workers/
    ├── WtpMainFlowWorkerTest.java        # 4 tests
    ├── WtpReceiveWorkerTest.java        # 4 tests
    └── WtpTapAuditWorkerTest.java        # 4 tests
```
