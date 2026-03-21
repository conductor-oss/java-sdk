# Service Migration in Java with Conductor

Orchestrates a service migration between environments using [Conductor](https://github.com/conductor-oss/conductor). This workflow assesses dependencies, replicates the service to the target environment, performs the traffic cutover, and validates that all endpoints are responding correctly.

## Moving Services Without Downtime

Your payment-service needs to move from the legacy environment to the new Kubernetes cluster. It has 3 downstream dependencies and 2 data stores. You need to assess what will break, replicate the service, cut over traffic at the right moment, and validate every endpoint still works. A botched migration means payment failures and revenue loss.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the migration logic. Conductor handles dependency assessment, cutover sequencing, and validation gates.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. your workers call the infrastructure APIs.

### What You Write: Workers

Four workers manage the migration. Assessing dependencies, replicating the service, cutting over traffic, and validating endpoints in the target environment.

| Worker | Task | What It Does |
|---|---|---|
| **AssessWorker** | `sm_assess` | Inventories the service's dependencies and data stores to build a migration plan |
| **CutoverWorker** | `sm_cutover` | Switches live traffic from the source environment to the target environment |
| **ReplicateWorker** | `sm_replicate` | Replicates the service, configuration, and data to the target environment |
| **ValidateWorker** | `sm_validate` | Confirms all endpoints in the target environment are responding correctly after cutover |

Workers implement infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls. the workflow and rollback logic stay the same.

### The Workflow

```
sm_assess
    │
    ▼
sm_replicate
    │
    ▼
sm_cutover
    │
    ▼
sm_validate

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
java -jar target/service-migration-1.0.0.jar

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
java -jar target/service-migration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow service_migration_workflow \
  --version 1 \
  --input '{"service": "order-service", "sourceEnv": "api", "targetEnv": "production"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w service_migration_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one migration phase. replace the simulated calls with AWS DMS, Istio traffic shifting, or Kubernetes resource cloning, and the migration workflow runs unchanged.

- **AssessWorker** (`sm_assess`): query your service mesh (Istio, Linkerd) or CMDB to discover dependencies, and AWS DMS for data store replication readiness
- **CutoverWorker** (`sm_cutover`): update load balancer backends, service mesh routing rules, or DNS weighted records to shift traffic
- **ReplicateWorker** (`sm_replicate`): use AWS DMS for database replication, Kubernetes resource cloning, or Terraform to provision the target environment

Point the workers at your actual infrastructure APIs and the migration workflow operates with the same data contract.

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
service-migration-service-migration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/servicemigration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessWorker.java
│       ├── CutoverWorker.java
│       ├── ReplicateWorker.java
│       └── ValidateWorker.java
└── src/test/java/servicemigration/
    └── MainExampleTest.java        # 2 tests. workflow resource loading, worker instantiation

```
