# Container Orchestration in Java with Conductor

Orchestrates a container build-scan-deploy pipeline using [Conductor](https://github.com/conductor-oss/conductor). This workflow builds a container image, scans it for vulnerabilities, deploys it to a Kubernetes cluster, and runs post-deployment health checks.## Shipping Containers Safely

A new version of your service is ready to ship. Before it reaches production, the container image must be built, scanned for critical vulnerabilities (you do not want to deploy a known CVE), deployed to the target cluster, and health-checked to confirm all containers are running. Skipping any step: especially the vulnerability scan, means shipping insecure code to production.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the build, scan, and deploy logic. Conductor handles the container pipeline sequencing, security gates, and health verification.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging .  your workers call the infrastructure APIs.

### What You Write: Workers

Four workers form the container pipeline. Building the image, scanning for vulnerabilities, deploying to Kubernetes, and running health checks.

| Worker | Task | What It Does |
|---|---|---|
| **BuildWorker** | `co_build` | Builds the container image from source and tags it with the specified version |
| **DeployWorker** | `co_deploy` | Deploys the scanned image to the target Kubernetes cluster |
| **HealthCheckWorker** | `co_health_check` | Runs post-deployment health checks to verify all containers are running and healthy |
| **ScanWorker** | `co_scan` | Scans the built image for security vulnerabilities and blocks deployment on critical findings |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
co_build
    │
    ▼
co_scan
    │
    ▼
co_deploy
    │
    ▼
co_health_check
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
java -jar target/container-orchestration-1.0.0.jar
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
java -jar target/container-orchestration-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow container_orchestration_workflow \
  --version 1 \
  --input '{"image": "test-value", "tag": "test-value", "cluster": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w container_orchestration_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker owns one pipeline stage .  replace the simulated calls with Docker Engine, Trivy vulnerability scanning, or Kubernetes rollout APIs, and the container workflow runs unchanged.

- **BuildWorker** (`co_build`): trigger Docker builds via Docker Engine API, or use Google Cloud Build / AWS CodeBuild APIs for remote builds
- **DeployWorker** (`co_deploy`): apply Kubernetes manifests via the Kubernetes Java client, or call Helm upgrade for chart-based deployments
- **HealthCheckWorker** (`co_health_check`): query Kubernetes pod readiness probes or use the Kubernetes API to check rollout status

Replace with real Docker builds and Kubernetes API calls; the build-scan-deploy pipeline preserves the same interface.

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
container-orchestration-container-orchestration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/containerorchestration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BuildWorker.java
│       ├── DeployWorker.java
│       ├── HealthCheckWorker.java
│       └── ScanWorker.java
└── src/test/java/containerorchestration/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation
```
