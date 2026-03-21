# Config Management in Java with Conductor

Load, validate, deploy, and verify configuration. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

Deploying a configuration change across a distributed system requires loading the new config from a source (file, remote store), validating it against a schema, deploying it to all nodes, and verifying consistency. A bad config value pushed without validation can cause service outages across the fleet.

Without orchestration, config deployments are done via ad-hoc scripts that skip validation or push to only some nodes. There is no record of which config version each node is running, and rollbacks require manually reverting and re-pushing the previous config.

## The Solution

**You just write the config load, validation, deployment, and verification workers. Conductor handles ordered execution, crash recovery between deploy and verify, and a full audit trail of every config push.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

The pipeline chains four workers: LoadConfigWorker reads from a config source, ValidateConfigWorker checks it against a schema, DeployConfigWorker pushes it to nodes, and VerifyConfigWorker confirms hash consistency across the fleet.

| Worker | Task | What It Does |
|---|---|---|
| **DeployConfigWorker** | `cf_deploy_config` | Deploys the validated config to nodes in the target environment, returning a deployment ID and node count. |
| **LoadConfigWorker** | `cf_load_config` | Loads configuration from a source (file, remote store) for a target environment, returning the config map and schema version. |
| **ValidateConfigWorker** | `cf_validate_config` | Validates the loaded config against its schema, returning errors and warnings. |
| **VerifyConfigWorker** | `cf_verify_config` | Verifies all nodes are running with the deployed config by checking hash consistency. |

Workers implement service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients. the workflow coordination stays the same.

### The Workflow

```
cf_load_config
    │
    ▼
cf_validate_config
    │
    ▼
cf_deploy_config
    │
    ▼
cf_verify_config

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
java -jar target/config-management-1.0.0.jar

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
java -jar target/config-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow config_management_299 \
  --version 1 \
  --input '{"configSource": "api", "environment": "staging", "configData": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w config_management_299 -s COMPLETED -c 5

```

## How to Extend

Point each worker at your real config source (Spring Cloud Config, Consul, AWS Parameter Store), schema validator, and node fleet, the load-validate-deploy-verify workflow stays exactly the same.

- **DeployConfigWorker** (`cf_deploy_config`): push config via Kubernetes ConfigMap updates, Consul KV writes, or AWS AppConfig deployments
- **LoadConfigWorker** (`cf_load_config`): read from Spring Cloud Config Server, Consul KV, AWS Parameter Store, or etcd
- **ValidateConfigWorker** (`cf_validate_config`): validate against JSON Schema, OPA policies, or your config management system's built-in rules

Switching the config source from a file to Spring Cloud Config Server does not require any workflow modifications.

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
config-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/configmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ConfigManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeployConfigWorker.java
│       ├── LoadConfigWorker.java
│       ├── ValidateConfigWorker.java
│       └── VerifyConfigWorker.java
└── src/test/java/configmanagement/workers/
    ├── DeployConfigWorkerTest.java        # 3 tests
    ├── LoadConfigWorkerTest.java        # 3 tests
    ├── ValidateConfigWorkerTest.java        # 3 tests
    └── VerifyConfigWorkerTest.java        # 3 tests

```
