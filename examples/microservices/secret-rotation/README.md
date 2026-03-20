# Secret Rotation in Java with Conductor

Rotate secrets across services securely. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

Secrets (API keys, database passwords, encryption keys) must be rotated periodically to limit the blast radius of a leak. Rotation involves generating a new secret, storing it in a vault, updating every dependent service to use the new secret, and verifying that all services have switched over and the old secret is revoked.

Without orchestration, secret rotation is a manual runbook where an engineer generates a secret, updates Vault, then SSH-es into each service to restart it. Missing a service means it still uses the old secret, and there is no verification step.

## The Solution

**You just write the secret-generation, vault-storage, service-update, and rotation-verification workers. Conductor handles ordered rotation steps, crash-safe resume between vault storage and service update, and a complete rotation audit.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers automate the rotation lifecycle: GenerateSecretWorker produces a new cryptographic key, StoreSecretWorker writes it to a vault, UpdateServicesWorker rolls the change across dependent services, and VerifyRotationWorker confirms the old secret is revoked.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateSecretWorker** | `sr_generate_secret` | Generates a new cryptographic secret using the specified algorithm (e.g., AES-256) and returns its ID and expiry. |
| **StoreSecretWorker** | `sr_store_secret` | Stores the generated secret in a vault (e.g., HashiCorp Vault) at a versioned path. |
| **UpdateServicesWorker** | `sr_update_services` | Updates all target services to reference the new secret, reporting which services were updated. |
| **VerifyRotationWorker** | `sr_verify_rotation` | Verifies that all services are using the new secret and that the old secret has been revoked. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
sr_generate_secret
    │
    ▼
sr_store_secret
    │
    ▼
sr_update_services
    │
    ▼
sr_verify_rotation
```

## Example Output

```
=== Example 300: Secret Rotatio ===

Step 1: Registering task definitions...
  Registered: sr_generate_secret, sr_store_secret, sr_update_services, sr_verify_rotation

Step 2: Registering workflow 'secret_rotation_300'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [generate] Generating new secret for \"" + secretName + "\"...
  [store] Storing secret
  [update] Updating
  [verify] Verifying secret rotation for \"" + secretName + "\"...

  Status: COMPLETED
  Output: {secretId=..., algorithm=..., expiresAt=..., generatedAt=...}

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
java -jar target/secret-rotation-1.0.0.jar
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
java -jar target/secret-rotation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow secret_rotation_300 \
  --version 1 \
  --input '{"secretName": "sample-name", "db-password": "sample-db-password", "targetServices": "sample-targetServices", "api-service": "sample-api-service", "worker-service": "sample-worker-service"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w secret_rotation_300 -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real HSM or KMS, HashiCorp Vault or AWS Secrets Manager, and service restart mechanism, the generate-store-update-verify rotation workflow stays exactly the same.

- **GenerateSecretWorker** (`sr_generate_secret`): use your HSM, AWS KMS, or HashiCorp Vault's transit engine to generate secrets
- **StoreSecretWorker** (`sr_store_secret`): write to HashiCorp Vault, AWS Secrets Manager, or Azure Key Vault
- **UpdateServicesWorker** (`sr_update_services`): restart services or trigger config reloads via Kubernetes rollout, Consul watches, or AWS SSM parameter updates

Switching from HashiCorp Vault to AWS Secrets Manager behind StoreSecretWorker leaves the rotation pipeline unchanged.

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
secret-rotation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/secretrotation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SecretRotationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GenerateSecretWorker.java
│       ├── StoreSecretWorker.java
│       ├── UpdateServicesWorker.java
│       └── VerifyRotationWorker.java
└── src/test/java/secretrotation/workers/
    ├── GenerateSecretWorkerTest.java        # 3 tests
    ├── StoreSecretWorkerTest.java        # 3 tests
    ├── UpdateServicesWorkerTest.java        # 3 tests
    └── VerifyRotationWorkerTest.java        # 3 tests
```
