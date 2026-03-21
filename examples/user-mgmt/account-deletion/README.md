# Account Deletion in Java Using Conductor

A Java Conductor workflow example demonstrating Account Deletion. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to permanently delete a user's account. Verifying their identity and deletion reason, backing up their data for regulatory retention, purging records across all database tables and connected systems, and sending a final confirmation that the account is gone. Each step depends on the previous one's output.

If the backup fails but deletion proceeds anyway, the user's data is gone with no recovery path. If deletion clears the primary database but misses a downstream system, personal data lingers in places it shouldn't, creating GDPR and CCPA liability. Without orchestration, you'd build a monolithic deletion handler that mixes identity verification, S3 backup uploads, multi-table cascading deletes, and email notifications, making it impossible to add a cooling-off period, extend the backup retention policy, or audit which tables were cleared for which deletion request.

## The Solution

**You just write the identity-verification, data-backup, account-purge, and confirmation workers. Conductor handles the deletion sequence and compliance data flow.**

VerifyDeletionWorker confirms the user's identity and validates the deletion reason (user request, policy violation, inactivity) before anything irreversible happens. BackupWorker exports the user's data: profile, activity history, stored files, to durable storage and returns a backup ID for retention compliance. DeleteAccountWorker purges the user's records across all database tables (reporting 12 tables cleared) and marks the account as deleted with a timestamp. ConfirmDeletionWorker sends the user a final notification confirming their account has been removed and providing the backup reference if they ever need a data retrieval within the retention window. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

VerifyDeletionWorker confirms identity and reason, BackupWorker exports data for retention compliance, DeleteAccountWorker purges records across 12 tables, and ConfirmDeletionWorker sends the final GDPR-compliant notification.

| Worker | Task | What It Does |
|---|---|---|
| **BackupWorker** | `acd_backup` | Exports the user's data to durable storage, returning a backup ID with a 30-day retention period |
| **ConfirmDeletionWorker** | `acd_confirm` | Sends a GDPR-compliant deletion confirmation notification to the user |
| **DeleteAccountWorker** | `acd_delete` | Purges the user's records across all database tables (12 tables) and timestamps the deletion |
| **VerifyDeletionWorker** | `acd_verify` | Verifies the user's identity and validates the deletion reason before proceeding |

Workers implement user lifecycle operations. account creation, verification, profile setup,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
acd_verify
    │
    ▼
acd_backup
    │
    ▼
acd_delete
    │
    ▼
acd_confirm

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
java -jar target/account-deletion-1.0.0.jar

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
java -jar target/account-deletion-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow acd_account_deletion \
  --version 1 \
  --input '{"userId": "TEST-001", "reason": "sample-reason"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w acd_account_deletion -s COMPLETED -c 5

```

## How to Extend

Each worker handles one deletion step. connect your object storage (S3, GCS) for data backup and your database cascade logic for multi-table purging, and the account-deletion workflow stays the same.

- **VerifyDeletionWorker** (`acd_verify`): verify the user's identity via your auth provider (Auth0, Okta, Cognito) and validate the deletion reason against your retention policy rules
- **BackupWorker** (`acd_backup`): export the user's profile, activity logs, and stored files to S3 or GCS with a retention-compliant lifecycle policy, returning the backup location and expiration date
- **DeleteAccountWorker** (`acd_delete`): execute cascading deletes across your primary database, search indices (Elasticsearch), cache layers (Redis), and downstream services (analytics, CRM, billing)
- **ConfirmDeletionWorker** (`acd_confirm`): send the deletion confirmation email via SendGrid or SES with the backup reference ID and data retrieval instructions for the retention period

Wire up your backup storage and database deletion APIs and the verify-backup-purge-confirm deletion sequence keeps running unchanged.

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
account-deletion/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/accountdeletion/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AccountDeletionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BackupWorker.java
│       ├── ConfirmDeletionWorker.java
│       ├── DeleteAccountWorker.java
│       └── VerifyDeletionWorker.java
└── src/test/java/accountdeletion/workers/
    ├── BackupWorkerTest.java        # 3 tests
    ├── ConfirmDeletionWorkerTest.java        # 3 tests
    ├── DeleteAccountWorkerTest.java        # 4 tests
    └── VerifyDeletionWorkerTest.java        # 3 tests

```
