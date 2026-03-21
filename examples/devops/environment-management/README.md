# Environment Management in Java with Conductor :  Create, Configure, Seed Data, Verify

Environment lifecycle orchestration: create, configure, seed data, and verify. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Developers Need Environments on Demand

"I need a staging environment for my feature branch" shouldn't take a week of DevOps tickets. Ephemeral environments should spin up in minutes: provision infrastructure from a template (Kubernetes namespace, database, message queue), configure the environment with the right service versions and feature flags, seed it with realistic test data, and run health checks to confirm everything is ready.

Environments have a TTL .  they're destroyed automatically after the specified hours to control costs. If the configuration step fails (wrong service version, missing secret), the environment is created but unusable ,  retrying should reconfigure, not recreate. And every environment must be tracked: who created it, which branch, when it was created, and when it was destroyed.

## The Solution

**You write the provisioning and configuration logic. Conductor handles create-configure-seed-verify sequencing, TTL-based teardown, and environment lifecycle tracking.**

`CreateEnvWorker` provisions a new environment from the specified template. Kubernetes namespace, database instance, and supporting services. `ConfigureWorker` sets up service versions, environment variables, feature flags, and secrets. `SeedDataWorker` populates the database with test data appropriate for the environment's purpose. `VerifyWorker` runs health checks against all services in the environment and confirms end-to-end functionality. Conductor records the environment lifecycle and can trigger automatic teardown after the TTL expires.

### What You Write: Workers

Four workers manage the environment lifecycle. Creating infrastructure, configuring services, seeding test data, and verifying health.

| Worker | Task | What It Does |
|---|---|---|
| **ConfigureEnv** | `em_configure` | Configures the environment. |
| **CreateEnv** | `em_create_env` | Creates a new environment. |
| **SeedData** | `em_seed_data` | Seeds test data into the environment. |
| **VerifyEnv** | `em_verify` | Verifies the environment is healthy. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
em_create_env
    │
    ▼
em_configure
    │
    ▼
em_seed_data
    │
    ▼
em_verify

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
java -jar target/environment-management-1.0.0.jar

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
java -jar target/environment-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow environment_management_workflow \
  --version 1 \
  --input '{"envName": "test", "template": "sample-template", "ttlHours": "sample-ttlHours"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w environment_management_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one environment lifecycle step .  replace the simulated calls with Terraform workspaces, Kubernetes namespace APIs, or Consul KV for real provisioning and configuration, and the management workflow runs unchanged.

- **CreateEnv** (`em_create_env`): use Terraform workspaces, Kubernetes namespaces via kubectl, or AWS CloudFormation stacks for real environment provisioning with isolation
- **ConfigureEnv** (`em_configure_env`): apply environment-specific configuration via Ansible playbooks, Consul KV writes, or Kubernetes ConfigMaps/Secrets for database URLs, API keys, and feature flags
- **SeedData** (`em_seed_data`): load fixtures via Flyway seed scripts, import sanitized production data snapshots, or generate synthetic data using Faker/DataFactory
- **VerifyEnv** (`em_verify`): run smoke tests via Newman (Postman collections), curl-based health checks, or custom test suites to confirm all services are operational

Connect to Terraform or Kubernetes for real provisioning; the environment pipeline uses the same create-configure-verify interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
environment-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/environmentmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EnvironmentManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConfigureEnv.java
│       ├── CreateEnv.java
│       ├── SeedData.java
│       └── VerifyEnv.java
└── src/test/java/environmentmanagement/workers/
    ├── CreateEnvTest.java        # 7 tests
    ├── SeedDataTest.java        # 7 tests
    └── VerifyEnvTest.java        # 7 tests

```
