# Implementing API Key Rotation in Java with Conductor :  Generate, Dual-Active Period, Consumer Migration, and Revocation

A Java Conductor workflow example for API key rotation. generating a new key, running both old and new keys simultaneously during a dual-active period, migrating consumers to the new key, and revoking the old key once all consumers have migrated.

## The Problem

You need to rotate API keys without downtime. Simply replacing the old key with a new one breaks every consumer that's still using the old key. Safe rotation requires generating a new key, keeping both old and new active simultaneously (dual-active period), migrating each consumer to the new key, and only revoking the old key once all consumers have switched. If revocation happens before migration, services go down.

Without orchestration, API key rotation is either avoided entirely (keys never rotate, increasing breach risk) or done recklessly (old key revoked before consumers migrate, causing outages). There's no tracking of which consumers have migrated, and the dual-active period is managed manually.

## The Solution

**You just write the key generation and consumer migration logic. Conductor handles strict ordering so revocation never happens before migration, retries if a consumer update fails, and tracking of which consumers are still on the old key.**

Each rotation step is an independent worker. key generation, dual-active activation, consumer migration, and old key revocation. Conductor runs them in strict sequence: generate the new key, activate dual-active mode, migrate consumers, then revoke the old key. Every rotation is tracked with consumer migration status,  you can see exactly which consumers are still on the old key. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers execute zero-downtime rotation: GenerateNewWorker creates a fresh API key, DualActiveWorker keeps both old and new keys valid simultaneously, MigrateConsumersWorker switches each consumer to the new key, and RevokeOldWorker retires the old key once all consumers have migrated.

| Worker | Task | What It Does |
|---|---|---|
| **DualActiveWorker** | `akr_dual_active` | Activates dual-key mode so both old and new keys are valid during the transition window |
| **GenerateNewWorker** | `akr_generate_new` | Generates a new API key for the specified service |
| **MigrateConsumersWorker** | `akr_migrate_consumers` | Migrates all consumers to the new key and confirms each has switched |
| **RevokeOldWorker** | `akr_revoke_old` | Revokes the old API key after all consumers have been migrated to the new one |

Workers implement security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations. the workflow logic stays the same.

### The Workflow

```
Input -> DualActiveWorker -> GenerateNewWorker -> MigrateConsumersWorker -> RevokeOldWorker -> Output

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
java -jar target/api-key-rotation-1.0.0.jar

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
java -jar target/api-key-rotation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow api_key_rotation \
  --version 1 \
  --input '{}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w api_key_rotation -s COMPLETED -c 5

```

## How to Extend

Each worker handles one rotation phase. connect GenerateNewWorker to your API gateway (Kong, AWS API Gateway), MigrateConsumersWorker to update consumer configs, and the generate-dual-active-migrate-revoke workflow stays the same.

- **DualActiveWorker** (`akr_dual_active`): configure both old and new keys as valid in your authentication layer during the migration window
- **GenerateNewWorker** (`akr_generate_new`): generate real API keys in your API gateway (Kong, AWS API Gateway) or key management system
- **MigrateConsumersWorker** (`akr_migrate_consumers`): notify consumers of the new key, update their configurations via config management, and verify they're using the new key

Connect to your API gateway's key management API, and the generate-migrate-revoke lifecycle continues without any orchestration changes.

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
api-key-rotation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/apikeyrotation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ApiKeyRotationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DualActiveWorker.java
│       ├── GenerateNewWorker.java
│       ├── MigrateConsumersWorker.java
│       └── RevokeOldWorker.java

```
