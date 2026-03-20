# Canary Release in Java with Conductor

Canary release with progressive traffic increase. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

A canary release progressively increases traffic to a new version in stages (e.g., 5% -> 50% -> 100%), monitoring health at each stage before proceeding. If anomalies are detected at any stage, the release is halted and traffic stays on the stable version.

Without orchestration, multi-stage rollouts require chaining CI/CD jobs with manual gates between stages, and there is no durable record of which stage was active when an issue occurred. Restarting a failed rollout from the correct stage requires manual intervention.

## The Solution

**You just write the canary deploy, monitor, traffic-increase, and rollout workers. Conductor handles multi-stage progression, durable pause between stages, and crash-safe recovery mid-rollout.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

This progressive release uses four workers: DeployCanaryWorker starts the canary at an initial traffic percentage, MonitorCanaryWorker watches for anomalies, IncreaseTrafficWorker bumps the percentage, and FullRolloutWorker promotes to 100%.

| Worker | Task | What It Does |
|---|---|---|
| **DeployCanaryWorker** | `cy_deploy_canary` | Deploys the new version at an initial traffic percentage (e.g., 5%). |
| **FullRolloutWorker** | `cy_full_rollout` | Promotes the canary to 100% traffic, completing the release. |
| **IncreaseTrafficWorker** | `cy_increase_traffic` | Increases the canary traffic percentage to the next stage (e.g., 50%) and scales up canary instances. |
| **MonitorCanaryWorker** | `cy_monitor_canary` | Monitors error rate, p99 latency, and anomalies during a configured monitoring window. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### The Workflow

```
cy_deploy_canary
    │
    ▼
cy_monitor_canary
    │
    ▼
cy_increase_traffic
    │
    ▼
cy_monitor_canary
    │
    ▼
cy_full_rollout
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
java -jar target/canary-release-1.0.0.jar
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
java -jar target/canary-release-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow canary_release_297 \
  --version 1 \
  --input '{"appName": "test", "newVersion": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w canary_release_297 -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real Kubernetes Deployment or Argo Rollouts API, Istio VirtualService weights, and observability stack, the progressive canary release workflow stays exactly the same.

- **DeployCanaryWorker** (`cy_deploy_canary`): call Kubernetes Deployment or Argo Rollouts to create canary pods at the specified traffic weight
- **FullRolloutWorker** (`cy_full_rollout`): scale the canary to 100% via Kubernetes Deployment, Argo Rollouts, or your deployment platform's API
- **IncreaseTrafficWorker** (`cy_increase_traffic`): update Istio VirtualService weights or AWS ALB target-group proportions

Replacing the monitoring or traffic-shifting implementation preserves the multi-stage release pipeline unchanged.

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
canary-release/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/canaryrelease/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CanaryReleaseExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeployCanaryWorker.java
│       ├── FullRolloutWorker.java
│       ├── IncreaseTrafficWorker.java
│       └── MonitorCanaryWorker.java
└── src/test/java/canaryrelease/workers/
    ├── DeployCanaryWorkerTest.java        # 3 tests
    ├── FullRolloutWorkerTest.java        # 2 tests
    ├── IncreaseTrafficWorkerTest.java        # 2 tests
    └── MonitorCanaryWorkerTest.java        # 2 tests
```
