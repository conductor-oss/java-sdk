# Session Management in Java Using Conductor

A Java Conductor workflow example demonstrating Session Management. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

A user logs in and the system must manage their session lifecycle end-to-end. The system needs to create a new session with a JWT token and expiration, validate that the session is still active and has sufficient remaining TTL, refresh the session by extending its expiration before it lapses, and revoke the session when the user logs out or the session is terminated. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the session-creation, validation, refresh, and revocation workers. Conductor handles the session lifecycle sequencing and token data flow.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

CreateSessionWorker generates a JWT with expiration, ValidateSessionWorker checks remaining TTL, RefreshSessionWorker extends the expiration, and RevokeSessionWorker invalidates the token on logout.

| Worker | Task | What It Does |
|---|---|---|
| **CreateSessionWorker** | `ses_create` | Creates a new session for the user, generating a unique session ID, JWT token, and expiration time |
| **RefreshSessionWorker** | `ses_refresh` | Extends the session's expiration by issuing a new TTL, keeping the user logged in |
| **RevokeSessionWorker** | `ses_revoke` | Revokes the session by invalidating the token and recording the revocation timestamp |
| **ValidateSessionWorker** | `ses_validate` | Checks that the session is still valid and returns the remaining TTL |

Workers implement user lifecycle operations. account creation, verification, profile setup,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
ses_create
    │
    ▼
ses_validate
    │
    ▼
ses_refresh
    │
    ▼
ses_revoke

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
java -jar target/session-management-1.0.0.jar

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
java -jar target/session-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ses_session_management \
  --version 1 \
  --input '{"userId": "TEST-001", "deviceInfo": "sample-deviceInfo"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ses_session_management -s COMPLETED -c 5

```

## How to Extend

Each worker handles one session operation. connect your session store (Redis, DynamoDB, Memcached) for token management and your auth provider for JWT validation, and the session-management workflow stays the same.

- **CreateSessionWorker** (`ses_create`): generate a JWT via your identity provider (Auth0, Cognito) and store the session in Redis with a configurable TTL for fast validation
- **ValidateSessionWorker** (`ses_validate`): look up the session in Redis and verify the JWT signature and claims against your Auth0/Cognito JWKS endpoint
- **RefreshSessionWorker** (`ses_refresh`): call your identity provider's token refresh endpoint and update the session TTL in Redis with the new expiration
- **RevokeSessionWorker** (`ses_revoke`): add the session token to a Redis-backed blocklist and call your identity provider's revocation endpoint to invalidate the token

Swap in a real JWT library and Redis token store and the create-validate-refresh-revoke session lifecycle keeps functioning as defined.

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
session-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/sessionmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SessionManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateSessionWorker.java
│       ├── RefreshSessionWorker.java
│       ├── RevokeSessionWorker.java
│       └── ValidateSessionWorker.java
└── src/test/java/sessionmanagement/workers/
    ├── CreateSessionWorkerTest.java        # 4 tests
    ├── RefreshSessionWorkerTest.java        # 3 tests
    ├── RevokeSessionWorkerTest.java        # 3 tests
    └── ValidateSessionWorkerTest.java        # 3 tests

```
