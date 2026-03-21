# Implementing Immutable Audit Logging in Java with Conductor :  Event Capture, Context Enrichment, Immutable Storage, and Integrity Verification

A Java Conductor workflow example for audit logging. capturing security-relevant events, enriching with context (who, what, when, from where), storing in an immutable log, and verifying log integrity.

## The Problem

You need a tamper-proof audit trail for compliance (SOC2, HIPAA, PCI). Every security-relevant action (login, permission change, data access) must be captured with full context (actor, action, resource, timestamp, source IP), stored in an immutable log that can't be modified after the fact, and periodically verified for integrity (no entries deleted or modified).

Without orchestration, audit logs are application-level log.info() calls that can be modified or deleted. Context enrichment is inconsistent. some logs include the actor, some don't. Immutability is an aspiration rather than a guarantee, and nobody verifies that the log hasn't been tampered with.

## The Solution

**You just write the event capture and immutable storage logic. Conductor handles ordered execution, durable delivery to the immutable store, and a meta-audit trail of every logging operation itself.**

Each audit concern is an independent worker. event capture, context enrichment, immutable storage, and integrity verification. Conductor runs them in sequence: capture the event, enrich with full context, store immutably, then verify integrity. Every audit operation is itself tracked by Conductor, creating a meta-audit trail. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers form the audit chain: CaptureEventWorker records security-relevant actions, EnrichContextWorker adds who/what/where context, StoreImmutableWorker writes to a tamper-proof log, and VerifyIntegrityWorker validates the hash chain.

| Worker | Task | What It Does |
|---|---|---|
| **CaptureEventWorker** | `al_capture_event` | Captures a security-relevant event with actor, action, and resource details |
| **EnrichContextWorker** | `al_enrich_context` | Enriches the event with IP address, device info, session data, and role context |
| **StoreImmutableWorker** | `al_store_immutable` | Writes the enriched event to an immutable, append-only audit log |
| **VerifyIntegrityWorker** | `al_verify_integrity` | Verifies log integrity by validating the hash chain. confirms no entries were modified or deleted |

Workers implement security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations. the workflow logic stays the same.

### The Workflow

```
al_capture_event
    │
    ▼
al_enrich_context
    │
    ▼
al_store_immutable
    │
    ▼
al_verify_integrity

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
java -jar target/audit-logging-1.0.0.jar

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
java -jar target/audit-logging-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow audit_logging_workflow \
  --version 1 \
  --input '{"actor": "sample-actor", "action": "process", "resource": "api"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w audit_logging_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one audit concern. connect CaptureEventWorker to your API gateway or application hooks, StoreImmutableWorker to AWS QLDB or WORM storage, and the capture-enrich-store-verify workflow stays the same.

- **CaptureEventWorker** (`al_capture_event`): capture events from application hooks, API gateway logs, database audit logs, or cloud trail services
- **EnrichContextWorker** (`al_enrich_context`): add actor identity from your IdP, source IP geolocation, device fingerprint, and session context
- **StoreImmutableWorker** (`al_store_immutable`): write to append-only stores. AWS QLDB, Azure Immutable Blob Storage, blockchain-based audit logs, or WORM storage

Connect to your actual immutable store and the capture-enrich-store-verify pipeline carries forward without changes.

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
audit-logging-audit-logging/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/auditlogging/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CaptureEventWorker.java
│       ├── EnrichContextWorker.java
│       ├── StoreImmutableWorker.java
│       └── VerifyIntegrityWorker.java
└── src/test/java/auditlogging/
    └── MainExampleTest.java        # 2 tests. workflow resource loading, worker instantiation

```
