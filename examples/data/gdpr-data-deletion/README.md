# GDPR Data Deletion in Java Using Conductor :  Record Discovery, Identity Verification, Cross-System Erasure, and Audit Logging

A Java Conductor workflow example for GDPR Article 17 right-to-erasure compliance. discovering all records associated with a user across multiple systems (user accounts, analytics, billing, support, marketing), verifying the requester's identity before any deletion occurs, erasing the user's data from every system where records were found, and generating a GDPR-compliant audit log that proves the deletion was completed. Uses [Conductor](https://github.

## The Problem

When a user submits a GDPR erasure request, their data lives in five or more systems: profile and credentials in the user accounts database, clickstream and session events in analytics, invoices and payment info in billing, ticket messages and attachments in support, and email preferences in marketing. You need to find every record tied to that user across all of these systems, verify the requester's identity before touching anything (a deletion request from an unverified source must be rejected), delete from every system, and produce an audit trail that proves to regulators exactly what was found, when it was deleted, and from which systems. You have 30 days to comply.

Without orchestration, you'd write a single deletion script that queries each system, deletes inline, and hopes nothing fails halfway. If the billing system is temporarily down after you've already wiped analytics, the user's data is partially deleted with no record of what remains. There's no identity verification gate, the script just deletes. There's no audit log that a regulator would accept, because there's no durable record of what was found before deletion. Adding a new system (say, a recommendation engine that stores user embeddings) means modifying tightly coupled code with no visibility into which system's deletion succeeded or failed.

## The Solution

**You just write the record discovery, identity verification, cross-system deletion, and audit log workers. Conductor handles strict ordering so deletion never runs before identity verification, retries when downstream systems are temporarily unavailable, and a durable audit trail for regulatory compliance.**

Each stage of the erasure pipeline is a simple, independent worker. The record finder scans user accounts, analytics, billing, support, and marketing systems to build a complete inventory of every record tied to the userId. The identity verifier checks the verification token before any deletion can proceed. If verification fails, the workflow aborts without deleting anything. The data deleter iterates through every discovered record and erases it, tracking deletion status per record and per system. The audit log generator creates a compliance-ready record containing the request ID, the user ID, every record that was deleted, and the exact timestamp of deletion. Conductor executes them in strict sequence, ensures deletion only runs after identity verification passes, retries if a system is temporarily unavailable, and provides a complete audit trail of every step. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers implement the GDPR erasure pipeline: discovering all records across five systems tied to a user, verifying the requester's identity before any deletion, erasing data from every system, and generating a compliance-ready audit log.

| Worker | Task | What It Does |
|---|---|---|
| **DeleteDataWorker** | `gr_delete_data` | Deletes user data from all systems if identity is verified. |
| **FindRecordsWorker** | `gr_find_records` | Finds all records associated with a user across multiple systems. |
| **GenerateAuditLogWorker** | `gr_generate_audit_log` | Generates a GDPR-compliant audit log for the deletion request. |
| **VerifyIdentityWorker** | `gr_verify_identity` | Verifies the identity of the user requesting data deletion. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
gr_find_records
    │
    ▼
gr_verify_identity
    │
    ▼
gr_delete_data
    │
    ▼
gr_generate_audit_log

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
java -jar target/gdpr-data-deletion-1.0.0.jar

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
java -jar target/gdpr-data-deletion-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow gdpr_data_deletion \
  --version 1 \
  --input '{"userId": "TEST-001", "requestId": "TEST-001", "verificationToken": "sample-verificationToken"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w gdpr_data_deletion -s COMPLETED -c 5

```

## How to Extend

Query real system catalogs across PostgreSQL, Elasticsearch, and SaaS APIs for record discovery, validate identity via Auth0 or Okta, and write audit logs to an immutable store, the GDPR erasure workflow runs unchanged.

- **FindRecordsWorker** → query real system catalogs to discover user data: JDBC queries across PostgreSQL/MySQL databases, Elasticsearch index scans, S3 object listings by user prefix, and API calls to SaaS platforms (Salesforce, HubSpot, Zendesk) to build a complete data inventory
- **VerifyIdentityWorker** → validate identity against your auth provider (Auth0, Okta, Cognito) or require email confirmation via a WAIT task that pauses the workflow until the user clicks a verification link
- **DeleteDataWorker** → execute real deletions per system: `DELETE FROM` via JDBC, Elasticsearch `_delete_by_query`, S3 `deleteObjects`, and SaaS API deletion endpoints, with idempotent operations so Conductor retries are safe
- **GenerateAuditLogWorker** → write deletion certificates to an immutable audit store (append-only PostgreSQL table, AWS QLDB, or blockchain-anchored logs) that satisfies GDPR Article 5(2) accountability requirements

Connecting the discovery worker to real system APIs or adding new systems to the erasure list leaves the workflow unchanged, as long as each worker outputs the expected record inventory and deletion status.

**Add new stages** by inserting tasks in `workflow.json`, for example, a data backup step that snapshots the user's data before deletion (for legal hold scenarios), a notification step that emails the user confirming erasure is complete, or a propagation step that forwards the deletion request to third-party data processors under GDPR Article 17(2).

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
gdpr-data-deletion/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/gdprdatadeletion/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GdprDataDeletionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeleteDataWorker.java
│       ├── FindRecordsWorker.java
│       ├── GenerateAuditLogWorker.java
│       └── VerifyIdentityWorker.java
└── src/test/java/gdprdatadeletion/workers/
    ├── DeleteDataWorkerTest.java        # 7 tests
    ├── FindRecordsWorkerTest.java        # 7 tests
    ├── GenerateAuditLogWorkerTest.java        # 7 tests
    └── VerifyIdentityWorkerTest.java        # 6 tests

```
