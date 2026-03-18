# Blue Green Deploy in Java with Conductor

You need to ship v2.5.0 of the payment service. The old deploy process takes the service down for 90 seconds while the new containers start up, and last time, the health check took longer than expected, so the outage stretched to four minutes during peak checkout hours. Your Slack exploded. You could spin up the new version alongside the old one and flip traffic over, but the last time someone tried that manually, they updated the load balancer target group but forgot to run smoke tests on the new environment first. Traffic shifted to containers that couldn't connect to the database. Rolling back meant another manual LB change under pressure, at 2 AM, with the VP of Engineering watching. This workflow automates blue-green deployment: stand up the new environment, run smoke tests, switch traffic atomically, and verify. with a full audit trail so you know exactly what happened and when. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

Deploying a new version of a service must happen with zero downtime. Blue-green deployment achieves this by standing up the new version in an idle (green) environment, running smoke tests against it, switching live traffic from blue to green, and verifying the switch succeeded, all as an atomic, auditable sequence.

Without orchestration, deployment scripts become fragile shell pipelines where a failure at the traffic-switch step can leave both environments in an inconsistent state. There is no automatic rollback, no execution history, and diagnosing a failed deploy means grepping through CI logs.

## The Solution

**You just write the green-environment, traffic-switch, and verification workers. Conductor handles step sequencing, crash-safe resume during the traffic switch, and a complete deployment audit trail.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. Your workers just make the service calls.

### What You Write: Workers

Three workers carry out the deployment: PrepareGreenWorker stands up the new environment, SwitchTrafficWorker atomically redirects live traffic, and VerifyDeploymentWorker checks error rates and latency after the switch.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **PrepareGreenWorker** | `bg_prepare_green` | Deploys the new application version to the green environment and reports container count and image tag. | Simulated |
| **SwitchTrafficWorker** | `bg_switch_traffic` | Atomically shifts live traffic from blue to green by updating DNS or load-balancer rules. | Simulated |
| **VerifyDeploymentWorker** | `bg_verify_deployment` | Verifies the deployment succeeded by checking error rate, p99 latency, and rollback availability. | Simulated |

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
bg_prepare_green
    │
    ▼
bg_test_green
    │
    ▼
bg_switch_traffic
    │
    ▼
bg_verify_deployment
```

## Example Output

```
=== Example 296: Blue-Green Deployment ===

Step 1: Registering task definitions...
  Registered: bg_prepare_green, bg_test_green, bg_switch_traffic, bg_verify_deployment

Step 2: Registering workflow 'blue_green_deploy_296'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: 8e22af7b-9297-4b22-528e-cf8c9fb8dc7d

  [prepare] Deploying payment-service v2.5.0 to green...
  [switch] Switching traffic from
  [test] Running smoke tests on green environment...
  [verify] Verifying payment-service v2.5.0 on green...

  Status: COMPLETED
  Output: {deploymentStatus=success, activeEnvironment=production, version=2.5.0}

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
java -jar target/blue-green-deploy-1.0.0.jar
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
java -jar target/blue-green-deploy-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow blue_green_deploy_296 \
  --version 1 \
  --input '{"appName": "payment-service", "newVersion": "2.5.0", "currentEnv": "blue"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w blue_green_deploy_296 -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real container orchestrator (Kubernetes, ECS), load balancer, and monitoring stack, the blue-green deployment workflow stays exactly the same.

- **PrepareGreenWorker** (`bg_prepare_green`): call your container orchestrator (Kubernetes, ECS) to create a new deployment in the green namespace
- **SwitchTrafficWorker** (`bg_switch_traffic`): update your load balancer (ALB, Nginx, Istio VirtualService) to point to the green target group
- **VerifyDeploymentWorker** (`bg_verify_deployment`): query Prometheus, Datadog, or CloudWatch for real error-rate and latency metrics after the switch

Replacing the container orchestrator or monitoring stack behind any worker leaves the deployment pipeline untouched.

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
blue-green-deploy/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/bluegreendeploy/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BlueGreenDeployExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PrepareGreenWorker.java
│       ├── SwitchTrafficWorker.java
│       └── VerifyDeploymentWorker.java
└── src/test/java/bluegreendeploy/workers/
    ├── PrepareGreenWorkerTest.java        # 3 tests
    ├── SwitchTrafficWorkerTest.java        # 2 tests
    ├── TestGreenWorkerTest.java        # 2 tests
    └── VerifyDeploymentWorkerTest.java        # 2 tests
```
