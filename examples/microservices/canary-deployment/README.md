# Canary Deployment in Java with Conductor

You merge the PR, CI goes green, and you deploy to prod. Two minutes later, 100% of your users are hitting the new code, and the new code has a subtle bug that doubles response latency on the `/checkout` endpoint. By the time your on-call notices the Datadog alert and rolls back, 40,000 users have experienced broken checkouts. The postmortem is always the same: "we should have tested with a small percentage of traffic first." But doing that manually: deploying a canary, shifting 5% of traffic, watching error rates, deciding whether to promote or rollback, is tedious and error-prone, so teams skip it. This workflow automates the entire canary lifecycle: deploy, shift traffic gradually, analyze metrics, and promote or roll back automatically. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

Rolling out a new service version to 100% of traffic at once is risky, a bug can affect all users instantly. Canary deployment mitigates this by deploying the new version alongside the old one, gradually shifting a percentage of traffic to the canary, analyzing error rates and latency, and then promoting or rolling back based on the results.

Without orchestration, the deploy-shift-analyze-decide pipeline is typically a set of loosely coupled CI/CD steps with no unified state. If the analysis step fails, the traffic shift may not be reverted, and there is no single place to see the full canary lifecycle.

## The Solution

**You just write the deploy, traffic-shift, metrics-analysis, and promote-or-rollback workers. Conductor handles staged execution, durable state between deploy and promote, and a full audit of every canary decision.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. Your workers just make the service calls.

### What You Write: Workers

Four workers drive the canary lifecycle: DeployCanaryWorker provisions the new version, ShiftTrafficWorker steers a percentage of requests, AnalyzeMetricsWorker evaluates error rates, and PromoteOrRollbackWorker makes the final call.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **AnalyzeMetricsWorker** | `cd_analyze_metrics` | Collects and analyzes error rate and p99 latency from the canary during a monitoring window. | Simulated |
| **DeployCanaryWorker** | `cd_deploy_canary` | Deploys the new service version as a canary instance alongside the stable version. | Simulated |
| **PromoteOrRollbackWorker** | `cd_promote_or_rollback` | Compares the canary error rate against a threshold and decides whether to promote to full rollout or roll back. | Simulated |
| **ShiftTrafficWorker** | `cd_shift_traffic` | Shifts a specified percentage of live traffic to the canary instances. | Simulated |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients, the workflow coordination stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
cd_deploy_canary
    │
    ▼
cd_shift_traffic
    │
    ▼
cd_analyze_metrics
    │
    ▼
cd_promote_or_rollback
```

## Example Output

```
=== Canary Deployment Demo ===

Step 1: Registering task definitions...
  Registered: cd_deploy_canary, cd_shift_traffic, cd_analyze_metrics, cd_promote_or_rollback

Step 2: Registering workflow 'canary_deployment_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: 9a8c7ef4-bb5d-c905-4ecb-8295d88ea75d

  [deploy] Canary: user-service:4.1.0
  [traffic] Shifted 10% to canary
  [analyze] Error rate: 0.3%, p99 latency: 120ms (duration=5m)
  [decision] proceed (error: 0.3%)


  Status: COMPLETED
  Output: {decision=approved, errorRate=0.3}

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
java -jar target/canary-deployment-1.0.0.jar
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
java -jar target/canary-deployment-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow canary_deployment_workflow \
  --version 1 \
  --input '{"serviceName": "user-service", "newVersion": "4.1.0", "canaryPercentage": 10}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w canary_deployment_workflow -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real Kubernetes or ECS deployment API, load balancer weights, and Prometheus/Datadog metrics, the canary deploy-shift-analyze-decide workflow stays exactly the same.

- **AnalyzeMetricsWorker** (`cd_analyze_metrics`): query Prometheus, Datadog, or CloudWatch for real canary vs baseline metrics
- **DeployCanaryWorker** (`cd_deploy_canary`): call your container orchestrator (Kubernetes, ECS) to create canary pods/tasks with the new image
- **PromoteOrRollbackWorker** (`cd_promote_or_rollback`): integrate with your deployment API to execute a real full promotion (scale canary to 100%) or rollback (scale canary to 0%)

Pointing AnalyzeMetricsWorker at a different observability backend requires no changes to the canary workflow definition.

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
canary-deployment/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/canarydeployment/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CanaryDeploymentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeMetricsWorker.java
│       ├── DeployCanaryWorker.java
│       ├── PromoteOrRollbackWorker.java
│       └── ShiftTrafficWorker.java
└── src/test/java/canarydeployment/workers/
    ├── AnalyzeMetricsWorkerTest.java        # 8 tests
    ├── DeployCanaryWorkerTest.java        # 8 tests
    ├── PromoteOrRollbackWorkerTest.java        # 9 tests
    └── ShiftTrafficWorkerTest.java        # 8 tests
```
