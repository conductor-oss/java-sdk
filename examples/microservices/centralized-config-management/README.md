# Centralized Config Management in Java with Conductor

Centralized config management with staged rollout. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

Changing a configuration value across a fleet of microservices is error-prone. Each service may read config from a different source, and applying an invalid value can cause cascading failures. Centralized config management validates changes, plans a staged rollout (canary -> 25% -> 100%), applies the config, and verifies all services are running with the updated value.

Without orchestration, config changes are pushed ad-hoc via scripts or manual kubectl commands, with no validation gate and no way to know whether all services actually picked up the new value. Rolling back a bad config change requires another manual push.

## The Solution

**You just write the config validation, rollout planning, and config-apply workers. Conductor handles validation gating, staged execution, and a durable record of every config change across the fleet.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers handle the config lifecycle: CfgValidateWorker checks the key-value pair against schema rules, CfgStageRolloutWorker plans a graduated rollout, CfgApplyWorker pushes the change, and CfgVerifyWorker confirms fleet-wide consistency.

| Worker | Task | What It Does |
|---|---|---|
| **CfgApplyWorker** | `cfg_apply_config` | Applies the validated config to target services following the rollout plan. |
| **CfgStageRolloutWorker** | `cfg_stage_rollout` | Creates a staged rollout plan (canary, 25%, 100%) with intervals between stages. |
| **CfgValidateWorker** | `cfg_validate` | Validates the config key/value pair against schema rules and type constraints. |
| **CfgVerifyWorker** | `cfg_verify` | Verifies all services are running with the updated config and are healthy. |

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
cfg_validate
    │
    ▼
cfg_stage_rollout
    │
    ▼
cfg_apply_config
    │
    ▼
cfg_verify
```

## Example Output

```
=== Example 318: Config Management (Centralized Rollout) ===

Step 1: Registering task definitions...
  Registered: cfg_validate, cfg_stage_rollout, cfg_apply_config, cfg_verify

Step 2: Registering workflow 'config_management_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [apply] Rolling out
  [stage] Planning rollout to services
  [validate] Config
  [verify] All services running with updated config

  Status: COMPLETED
  Output: {appliedServices=..., version=..., plan=..., valid=...}

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
java -jar target/centralized-config-management-1.0.0.jar
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
java -jar target/centralized-config-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow config_management_workflow \
  --version 1 \
  --input '{"configKey": "sample-configKey", "max_connections": "sample-max-connections", "configValue": "sample-configValue", "200": "sample-200", "targetServices": "sample-targetServices", "api": "sample-api", "worker": "sample-worker"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w config_management_workflow -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real config store (Consul, etcd, AWS AppConfig) and fleet health-check endpoints, the validate-stage-apply-verify workflow stays exactly the same.

- **CfgApplyWorker** (`cfg_apply_config`): push config updates via Consul KV, etcd, AWS AppConfig, or Kubernetes ConfigMaps
- **CfgStageRolloutWorker** (`cfg_stage_rollout`): compute rollout stages based on your fleet topology and create a staged deployment plan
- **CfgValidateWorker** (`cfg_validate`): validate against JSON Schema or your config management system's rules (Consul, Spring Cloud Config)

Migrating from Consul to etcd or AWS AppConfig behind the apply worker requires no workflow changes.

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
centralized-config-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/centralizedconfigmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CentralizedConfigManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CfgApplyWorker.java
│       ├── CfgStageRolloutWorker.java
│       ├── CfgValidateWorker.java
│       └── CfgVerifyWorker.java
└── src/test/java/centralizedconfigmanagement/workers/
    ├── CfgApplyWorkerTest.java        # 2 tests
    ├── CfgStageRolloutWorkerTest.java        # 2 tests
    ├── CfgValidateWorkerTest.java        # 2 tests
    └── CfgVerifyWorkerTest.java        # 2 tests
```
