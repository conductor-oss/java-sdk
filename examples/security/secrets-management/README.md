# Implementing Secrets Management in Java with Conductor -- Create, Distribute, Verify Access, and Schedule Rotation

An engineer who left the company six weeks ago still has working API keys. You know this because one of those keys is hardcoded in an environment variable on three production servers, pasted into a `.env` file in a private repo that four people have access to, and shared in a Slack DM from last March. Nobody rotated it when she left because nobody knew where it was used. The database password for the payments service hasn't been rotated in fourteen months, and the only record of who has access is a mental model in the head of a senior dev who's on vacation. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate secrets lifecycle management -- create credentials, distribute to authorized consumers only, verify access policies, and schedule automatic rotation -- with a full audit trail of every secret touched.

## The Problem

You need to manage secrets across your infrastructure -- API keys, database passwords, TLS certificates. Each secret must be created securely, distributed only to authorized services, access controls must be verified (no unauthorized consumers), and rotation must be scheduled before expiry. If distribution fails, services can't start. If access verification is skipped, unauthorized services gain access. If rotation isn't scheduled, secrets expire and cause outages.

Without orchestration, secrets management is a manual process -- someone creates a secret in Vault, copies it to environment variables, hopes the right services have access, and forgets to schedule rotation. Secrets are shared via Slack, stored in plaintext config files, and never rotated.

## The Solution

**You just write the vault integration and rotation logic. Conductor handles sequencing, retries on vault failures, and a full audit trail of every secret created and distributed.**

Each secrets concern is an independent worker -- secret creation, distribution to consumers, access verification, and rotation scheduling. Conductor runs them in sequence: create the secret, distribute to authorized consumers, verify that only the right services have access, then schedule rotation. Every operation is tracked for audit compliance. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers handle the secrets lifecycle: CreateSecretWorker generates credentials, DistributeWorker pushes them to authorized consumers, VerifyAccessWorker confirms access policies, and ScheduleRotationWorker sets up automatic renewal.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **CreateSecretWorker** | `sm_create_secret` | Creates a new secret (API key, database credential, or certificate) and returns a secret ID | Simulated |
| **DistributeWorker** | `sm_distribute` | Distributes the secret to authorized services and confirms delivery | Simulated |
| **ScheduleRotationWorker** | `sm_schedule_rotation` | Schedules automatic rotation on a configurable interval (e.g., every 90 days) | Simulated |
| **VerifyAccessWorker** | `sm_verify_access` | Verifies that access policies are correct and only authorized services can read the secret | Simulated |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations -- the workflow logic stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically -- configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status -- no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
sm_create_secret
    │
    ▼
sm_distribute
    │
    ▼
sm_verify_access
    │
    ▼
sm_schedule_rotation
```

## Example Output

```
=== Example 300: Secrets Management ===

Step 1: Registering task definitions...
  Registered: sm_create_secret, sm_distribute, sm_verify_access, sm_schedule_rotation

Step 2: Registering workflow 'secrets_management_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: 8fb478ac-0db3-0654-8b3a-52e0babfe1cc

  [create] Secret db-prod-password (database) created
  [distribute] Secret distributed to 3 authorized services
  [rotation] Automatic rotation scheduled every 90 days
  [verify] Access policies verified: only authorized services can read

  Status: COMPLETED

Result: PASSED
```

## Running It

### Prerequisites

- **Java 21+** -- verify with `java -version`
- **Maven 3.8+** -- verify with `mvn -version`
- **Docker** -- to run Conductor

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
java -jar target/secrets-management-1.0.0.jar
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

```bash
conductor workflow start \
  --workflow secrets_management_workflow \
  --version 1 \
  --input '{"secretName": "db-prod-password", "secretType": "database"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w secrets_management_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker maps directly to a secrets manager operation -- point CreateSecretWorker at HashiCorp Vault or AWS Secrets Manager, wire DistributeWorker to push credentials via Kubernetes Secrets, and the workflow itself stays unchanged.

- **CreateSecretWorker** (`sm_create_secret`) -- create real secrets in HashiCorp Vault, AWS Secrets Manager, Azure Key Vault, or GCP Secret Manager
- **DistributeWorker** (`sm_distribute`) -- push secrets to Kubernetes Secrets, environment variables, or application config stores via their APIs
- **ScheduleRotationWorker** (`sm_schedule_rotation`) -- configure automatic rotation in your secrets manager or schedule a Conductor workflow to run before expiry

Swap in your real vault client and the orchestration, retries, and audit trail carry over without modification.

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
secrets-management-secrets-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/secretsmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point
│   └── workers/
│       ├── CreateSecretWorker.java
│       ├── DistributeWorker.java
│       ├── ScheduleRotationWorker.java
│       └── VerifyAccessWorker.java
└── src/test/java/secretsmanagement/
    └── MainExampleTest.java        # 2 tests -- workflow resource loading, worker instantiation
```
