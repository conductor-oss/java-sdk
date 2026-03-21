# Permission Sync in Java Using Conductor

A Java Conductor workflow example demonstrating Permission Sync. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

Your identity provider is the source of truth for user permissions, but roles and access levels have drifted out of sync with downstream systems. The operations team needs to scan the source system and all target systems to capture current permission states, diff the permissions to identify discrepancies (missing roles, extra grants), apply the corrections to bring targets into alignment, and verify that all systems now match the source of truth. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the permission-scanning, diffing, correction, and verification workers. Conductor handles the sync pipeline and discrepancy data flow.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

ScanSystemsWorker captures permission states from source and targets, DiffWorker identifies missing grants per role, SyncWorker applies corrections, and VerifyWorker confirms all systems are now aligned.

| Worker | Task | What It Does |
|---|---|---|
| **DiffWorker** | `pms_diff` | Compares source and target permission sets, identifying missing grants per role (admin, editor, viewer) |
| **ScanSystemsWorker** | `pms_scan_systems` | Scans permissions from the source system and all target systems, capturing role counts for each |
| **SyncWorker** | `pms_sync` | Applies the detected permission changes to target systems, tracking success and failure counts |
| **VerifyWorker** | `pms_verify` | Re-checks all systems to confirm permissions are now in sync, recording the verification timestamp |

Workers simulate user lifecycle operations .  account creation, verification, profile setup ,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
pms_scan_systems
    │
    ▼
pms_diff
    │
    ▼
pms_sync
    │
    ▼
pms_verify

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
java -jar target/permission-sync-1.0.0.jar

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
java -jar target/permission-sync-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow pms_permission_sync \
  --version 1 \
  --input '{"sourceSystem": "api", "targetSystems": "production"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w pms_permission_sync -s COMPLETED -c 5

```

## How to Extend

Each worker handles one sync step .  connect your identity provider (Okta, Azure AD) as the source of truth and your downstream systems (database ACLs, API gateway, SaaS apps) as targets, and the permission-sync workflow stays the same.

- **ScanSystemsWorker** (`pms_scan_systems`): query role and permission data from your identity provider (Auth0, Okta, Cognito) and target systems (database ACLs, API gateway policies, cloud IAM)
- **DiffWorker** (`pms_diff`): compare permission sets between source and targets, identifying missing grants, stale permissions, and role mismatches
- **SyncWorker** (`pms_sync`): apply permission changes via the Auth0 Management API, AWS IAM API, or your API gateway's admin interface to resolve all detected diffs
- **VerifyWorker** (`pms_verify`): re-scan all systems to confirm permissions are now in sync and log the verification result for compliance audit trails

Integrate your IAM provider and target systems and the scan-diff-sync-verify permission reconciliation pipeline runs without modification.

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
permission-sync/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/permissionsync/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PermissionSyncExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DiffWorker.java
│       ├── ScanSystemsWorker.java
│       ├── SyncWorker.java
│       └── VerifyWorker.java
└── src/test/java/permissionsync/workers/
    ├── DiffWorkerTest.java        # 3 tests
    ├── ScanSystemsWorkerTest.java        # 3 tests
    ├── SyncWorkerTest.java        # 3 tests
    └── VerifyWorkerTest.java        # 3 tests

```
