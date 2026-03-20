# Event Audit Trail in Java Using Conductor

Sequential event audit trail workflow: log_received -> validate_event -> log_validated -> process_event -> log_processed -> finalize_audit. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to maintain a complete audit trail for every event that passes through your system. Each event must be logged on receipt, validated against business rules, logged again after validation, processed, logged after processing, and have its audit record finalized. Regulatory compliance (SOX, HIPAA, GDPR) often demands proof that every event was received, validated, and processed with timestamps at each stage.

Without orchestration, you'd sprinkle logging calls throughout your event processor, hoping none are skipped by early returns or exceptions .  manually correlating log entries across services, ensuring audit records are never lost even when processing fails, and building custom reporting to satisfy auditors.

## The Solution

**You just write the audit-logging, validation, event-processing, and audit-finalization workers. Conductor handles guaranteed step completion for regulatory compliance, crash-safe audit continuity, and a built-in execution history that doubles as the audit trail.**

Each audit concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing the six-step audit chain (log received, validate, log validated, process, log processed, finalize), guaranteeing that every step is recorded even if a later step fails, and providing a complete execution history that serves as the audit trail itself. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Six workers build a compliance-grade audit chain: LogReceivedWorker, ValidateEventWorker, LogValidatedWorker, ProcessEventWorker, LogProcessedWorker, and FinalizeAuditWorker each stamp a verifiable timestamp at every stage of the event lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **FinalizeAuditWorker** | `at_finalize_audit` | Finalizes the audit trail. |
| **LogProcessedWorker** | `at_log_processed` | Logs that an event has been processed. |
| **LogReceivedWorker** | `at_log_received` | Logs that an event has been received. |
| **LogValidatedWorker** | `at_log_validated` | Logs that an event has been validated. |
| **ProcessEventWorker** | `at_process_event` | Processes an event. |
| **ValidateEventWorker** | `at_validate_event` | Validates an incoming event. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
at_log_received
    │
    ▼
at_validate_event
    │
    ▼
at_log_validated
    │
    ▼
at_process_event
    │
    ▼
at_log_processed
    │
    ▼
at_finalize_audit
```

## Example Output

```
=== Event Audit Trail Demo ===

Step 1: Registering task definitions...
  Registered: at_log_received, at_validate_event, at_log_validated, at_process_event, at_log_processed, at_finalize_audit

Step 2: Registering workflow 'event_audit_trail_wf'...
  Workflow registered.

Step 3: Starting workers...
  6 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [at_finalize_audit] Finalizing audit for event:
  [at_log_processed] Logging processed event:
  [at_log_received] Logging received event:
  [at_log_validated] Logging validated event:
  [at_process_event] Processing event:
  [at_validate_event] Validating event:

  Status: COMPLETED
  Output: {auditTrailId=..., totalStages=..., finalized=..., logged=...}

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
java -jar target/event-audit-trail-1.0.0.jar
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
java -jar target/event-audit-trail-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_audit_trail_wf \
  --version 1 \
  --input '{"eventId": "evt-audit-001", "evt-audit-001": "eventType", "eventType": "order.created", "order.created": "eventData", "eventData": {"key": "value"}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_audit_trail_wf -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real audit log store (immutable database, S3), event validation rules, and compliance reporting system, the log-validate-process-finalize audit trail workflow stays exactly the same.

- **AtLogReceivedWorker** (`at_log_received`): write audit entries to an append-only audit log (AWS CloudTrail, database audit table, blockchain-based ledger)
- **AtValidateEventWorker** (`at_validate_event`): validate events against your business rules engine with schema checks and authorization verification
- **AtProcessEventWorker** (`at_process_event`): implement your actual event processing logic while capturing processing metadata for the audit trail
- **AtFinalizeAuditWorker** (`at_finalize_audit`): seal the audit record with a cryptographic hash, publish to your compliance reporting system, and archive for retention

Replacing the simulated audit log with a real compliance store (S3, immutable ledger) leaves the six-step audit chain intact.

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
event-audit-trail/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventaudittrail/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventAuditTrailExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FinalizeAuditWorker.java
│       ├── LogProcessedWorker.java
│       ├── LogReceivedWorker.java
│       ├── LogValidatedWorker.java
│       ├── ProcessEventWorker.java
│       └── ValidateEventWorker.java
└── src/test/java/eventaudittrail/workers/
    ├── FinalizeAuditWorkerTest.java        # 9 tests
    ├── LogProcessedWorkerTest.java        # 9 tests
    ├── LogReceivedWorkerTest.java        # 9 tests
    ├── LogValidatedWorkerTest.java        # 9 tests
    ├── ProcessEventWorkerTest.java        # 8 tests
    └── ValidateEventWorkerTest.java        # 8 tests
```
