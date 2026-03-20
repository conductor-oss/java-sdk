# Profile Update in Java Using Conductor

A Java Conductor workflow example demonstrating Profile Update. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

A user changes their display name and email address in their account settings. The system needs to validate the submitted fields for format and constraints, apply the updates to the user's profile record, sync the changed fields to downstream services (CRM, analytics, email platform), and notify the user that their profile was updated. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the field-validation, profile-update, downstream-sync, and notification workers. Conductor handles the update pipeline and cross-service propagation.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

ValidateFieldsWorker checks format constraints, UpdateProfileWorker applies the changes, SyncProfileWorker propagates to CRM and analytics, and NotifyChangesWorker confirms the update to the user.

| Worker | Task | What It Does |
|---|---|---|
| **NotifyChangesWorker** | `pfu_notify` | Sends the user a notification confirming which profile fields were changed |
| **SyncProfileWorker** | `pfu_sync` | Propagates the updated profile fields to 3 downstream services: CRM, analytics, and email |
| **UpdateProfileWorker** | `pfu_update` | Applies the validated field changes to the user's profile record and timestamps the update |
| **ValidateFieldsWorker** | `pfu_validate` | Validates the submitted profile fields for format and constraints, returning whether all fields passed |

Workers simulate user lifecycle operations .  account creation, verification, profile setup ,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
pfu_validate
    │
    ▼
pfu_update
    │
    ▼
pfu_sync
    │
    ▼
pfu_notify
```

## Example Output

```
=== Example 605: Profile Update ===

Step 1: Registering task definitions...
  Registered: pfu_validate, pfu_update, pfu_sync, pfu_notify

Step 2: Registering workflow 'pfu_profile_update'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [notify] User notified of profile changes
  [sync] Profile synced to 3 downstream services
  [update] Updated fields:
  [validate] Validating

  Status: COMPLETED
  Output: {notified=..., synced=..., services=..., updatedFields=...}

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
java -jar target/profile-update-1.0.0.jar
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
java -jar target/profile-update-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow pfu_profile_update \
  --version 1 \
  --input '{"userId": "USR-X7Y8Z9", "USR-X7Y8Z9": "updates", "updates": {"key": "value"}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w pfu_profile_update -s COMPLETED -c 5
```

## How to Extend

Each worker handles one update step .  connect your user database for profile writes and your downstream services (CRM, analytics, email platform) for field sync, and the profile-update workflow stays the same.

- **ValidateFieldsWorker** (`pfu_validate`): validate field formats, length constraints, and uniqueness checks (e.g., email uniqueness) against your database or Auth0/Cognito user store
- **UpdateProfileWorker** (`pfu_update`): persist the validated changes to your user database and update the user's attributes in your identity provider (Auth0, Cognito, Okta)
- **SyncProfileWorker** (`pfu_sync`): propagate the updated fields to downstream services: HubSpot CRM, Segment analytics, SendGrid contact lists, and any other integrated systems
- **NotifyChangesWorker** (`pfu_notify`): send a profile-change confirmation email via SendGrid or Twilio, alerting the user to the specific fields that were updated

Wire up your user database and downstream services and the validate-update-sync-notify profile pipeline keeps working as designed.

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
profile-update/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/profileupdate/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ProfileUpdateExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── NotifyChangesWorker.java
│       ├── SyncProfileWorker.java
│       ├── UpdateProfileWorker.java
│       └── ValidateFieldsWorker.java
└── src/test/java/profileupdate/workers/
    ├── NotifyChangesWorkerTest.java        # 3 tests
    ├── SyncProfileWorkerTest.java        # 3 tests
    ├── UpdateProfileWorkerTest.java        # 4 tests
    └── ValidateFieldsWorkerTest.java        # 4 tests
```
