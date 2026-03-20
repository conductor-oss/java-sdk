# Rolling Update in Java with Conductor :  Analyze, Plan, Execute, Verify

Orchestrates zero-downtime rolling updates by analyzing current state, planning the update strategy, executing the rollout, and verifying all replicas are healthy. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## Zero-Downtime Updates Need Careful Orchestration

Updating 20 instances of a service simultaneously causes a full outage while the new version starts up. A rolling update replaces instances in batches .  update 2, verify they're healthy, update the next 2, and so on. If a batch fails health checks, the rollout stops before affecting more instances.

The batch size and health check interval determine the trade-off between speed and safety. Updating 1 at a time is safest but slow (20 rounds). Updating 5 at a time is faster but riskier (a bad version affects 25% of capacity before detection). The plan step should consider current traffic levels, resource headroom, and the service's tolerance for reduced capacity during the update.

## The Solution

**You write the batch update and health check logic. Conductor handles rollout sequencing, batch-by-batch verification, and automatic rollback triggers.**

`AnalyzeWorker` examines the current deployment .  instance count, health status, traffic distribution, and resource utilization ,  to determine the starting state. `PlanWorker` calculates the rollout strategy ,  batch size, health check wait time between batches, rollback triggers, and success criteria. `ExecuteWorker` performs the rolling update in batches ,  updating instances, waiting for health checks, and proceeding to the next batch. `VerifyWorker` confirms the full rollout completed ,  all instances running the new version, health checks passing, and metrics stable. Conductor sequences these steps and records each batch's execution for rollout audit.

### What You Write: Workers

Four workers manage the rolling update. Analyzing current state, planning the batch strategy, executing the rollout, and verifying all replicas are healthy.

| Worker | Task | What It Does |
|---|---|---|
| **Analyze** | `ru_analyze` | Analyzes the current deployment state before a rolling update. |
| **ExecuteUpdate** | `ru_execute` | Executes the rolling update according to the plan. |
| **PlanUpdate** | `ru_plan` | Plans the rolling update strategy (batch size, max unavailable, etc.). |
| **VerifyUpdate** | `ru_verify` | Verifies all replicas are healthy after the rolling update. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
ru_analyze
    │
    ▼
ru_plan
    │
    ▼
ru_execute
    │
    ▼
ru_verify
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
java -jar target/rolling-update-1.0.0.jar
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
java -jar target/rolling-update-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rolling_update_workflow \
  --version 1 \
  --input '{"service": "test-value", "newVersion": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rolling_update_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one rollout phase .  replace the simulated calls with Kubernetes rollout controls, AWS ECS rolling updates, or custom Ansible playbooks, and the update workflow runs unchanged.

- **Analyze** (`ru_analyze`): examine the current deployment state: how many instances are running, which version they're on, current traffic levels, and whether it's safe to begin the update
- **PlanUpdate** (`ru_plan`): implement dynamic batch sizing based on current traffic and error budget, with automatic rollback triggers if error rate exceeds threshold during any batch
- **ExecuteUpdate** (`ru_execute`): use Kubernetes `kubectl rollout` with maxSurge/maxUnavailable controls, AWS ECS rolling update, or custom Ansible playbooks for batch-by-batch instance updates
- **VerifyUpdate** (`ru_verify`): compare post-update error rates and latency against pre-update baselines, check all instances report the new version, and verify load balancer health targets

Connect to Kubernetes or your deployment platform; the rollout pipeline keeps the same batch-update-verify interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
rolling-update/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/rollingupdate/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RollingUpdateExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── Analyze.java
│       ├── ExecuteUpdate.java
│       ├── PlanUpdate.java
│       └── VerifyUpdate.java
└── src/test/java/rollingupdate/workers/
    ├── AnalyzeTest.java        # 9 tests
    ├── ExecuteUpdateTest.java        # 8 tests
    ├── PlanUpdateTest.java        # 8 tests
    └── VerifyUpdateTest.java        # 8 tests
```
