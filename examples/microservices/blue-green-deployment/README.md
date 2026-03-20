# Blue Green Deployment in Java with Conductor

Orchestrates blue-green deployment: deploy to green, validate, switch traffic, and monitor. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

A zero-downtime deployment requires deploying the new version to a standby environment, validating it with health checks, switching traffic, and monitoring the new version under real load. Each step depends on the previous one, and a failure at any stage must be caught before live traffic is affected.

Without orchestration, these steps are strung together in CI/CD scripts where a failed health check might not prevent the traffic switch, and there is no durable record of which step succeeded or failed. Rolling back means running a separate, equally fragile script.

## The Solution

**You just write the deploy, validate, traffic-switch, and monitor workers. Conductor handles step ordering, durable state across the multi-step cutover, and automatic rollback visibility.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers manage the rollout lifecycle: DeployGreenWorker provisions the new version, ValidateGreenWorker runs smoke tests, SwitchTrafficWorker flips DNS or load-balancer rules, and MonitorGreenWorker watches real-time metrics.

| Worker | Task | What It Does |
|---|---|---|
| **DeployGreenWorker** | `bg_deploy_green` | Deploys the new version to the green environment. |
| **MonitorGreenWorker** | `bg_monitor_green` | Monitors the green environment after traffic switch. |
| **SwitchTrafficWorker** | `bg_switch_traffic` | Switches traffic from blue to green environment. |
| **ValidateGreenWorker** | `bg_validate_green` | Validates the green environment with smoke tests. |

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
bg_deploy_green
    │
    ▼
bg_validate_green
    │
    ▼
bg_switch_traffic
    │
    ▼
bg_monitor_green
```

## Example Output

```
=== Blue-Green Deployment Demo ===

Step 1: Registering task definitions...
  Registered: bg_deploy_green, bg_validate_green, bg_switch_traffic, bg_monitor_green

Step 2: Registering workflow 'blue_green_deployment'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [deploy] Deploying
  [monitor] Green running
  [switch] Traffic shifted:
  [validate]

  Status: COMPLETED
  Output: {deployed=..., environment=..., serviceName=..., imageTag=...}

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
java -jar target/blue-green-deployment-1.0.0.jar
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
java -jar target/blue-green-deployment-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow blue_green_deployment \
  --version 1 \
  --input '{"serviceName": "sample-name", "api-gateway": "sample-api-gateway", "newVersion": "sample-newVersion", "3.2.0": "sample-3.2.0", "imageTag": "sample-imageTag"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w blue_green_deployment -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real Kubernetes API, ALB target groups, and Datadog or Prometheus metrics, the deploy-validate-switch-monitor workflow stays exactly the same.

- **DeployGreenWorker** (`bg_deploy_green`): call the Kubernetes API or AWS ECS to deploy a new task definition in the green target group
- **MonitorGreenWorker** (`bg_monitor_green`): query your observability stack (Datadog, Prometheus, New Relic) for real-time error rates and latency
- **SwitchTrafficWorker** (`bg_switch_traffic`): update Route53 weighted records, ALB target groups, or Istio VirtualService weights

Changing the validation checks or the monitoring backend does not alter the deploy-validate-switch-monitor workflow.

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
blue-green-deployment/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/bluegreendeployment/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BlueGreenDeploymentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeployGreenWorker.java
│       ├── MonitorGreenWorker.java
│       ├── SwitchTrafficWorker.java
│       └── ValidateGreenWorker.java
└── src/test/java/bluegreendeployment/workers/
    ├── DeployGreenWorkerTest.java        # 8 tests
    ├── MonitorGreenWorkerTest.java        # 8 tests
    ├── SwitchTrafficWorkerTest.java        # 8 tests
    └── ValidateGreenWorkerTest.java        # 8 tests
```
