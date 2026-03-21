# Container Deployment Pipeline in Java Using Conductor :  Build, Deploy, Scale, Monitor

A Java Conductor workflow example for container deployment. building a Docker image from a service name and tag, deploying it with a specified replica count, configuring horizontal auto-scaling (min/max replicas), and enabling monitoring for the new deployment. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Deploying Containers End-to-End

Shipping a new version of a containerized service means building the image, pushing it to a registry, creating a deployment with the right replica count, configuring the horizontal pod autoscaler so it can scale between 2 and 10 replicas based on load, and wiring up monitoring dashboards so you can see if the new version is healthy. Each step depends on the previous one. you can't deploy an image that hasn't been built, you can't configure scaling for a deployment that doesn't exist, and monitoring is useless if it's pointed at the wrong deployment ID.

When the build fails halfway through or the deploy times out, you need to know exactly which step broke, what the image URI was, and whether the deployment was partially created. A shell script won't give you that visibility, and a CI/CD pipeline only helps if you can trace the full execution history.

## The Solution

**You write the build, deploy, and scaling logic. Conductor handles sequencing, retries, and deployment audit trails.**

`CtrBuildWorker` builds the container image and returns the image URI (e.g., `registry/service:tag`). `CtrDeployWorker` takes that URI and the desired replica count, creates the deployment, and returns a deployment ID. `CtrScaleWorker` configures auto-scaling on the deployment. setting min replicas to 2 and max to 10. `CtrMonitorWorker` enables health checks and metrics collection for the deployment. Conductor chains these steps so each one uses the output of the previous, retries any failed step, and gives you a complete audit trail of every deployment: which image, how many replicas, what scaling rules, and when monitoring was enabled.

### What You Write: Workers

Four workers span the deployment pipeline: image building, container deployment, auto-scaling configuration, and monitoring enablement, each owning one phase of the release.

| Worker | Task | What It Does |
|---|---|---|
| **CtrBuildWorker** | `ctr_build` | Builds a container image for the given service and tag. |
| **CtrDeployWorker** | `ctr_deploy` | Deploys a container image with the specified replica count. |
| **CtrMonitorWorker** | `ctr_monitor` | Enables monitoring for a deployment. |
| **CtrScaleWorker** | `ctr_scale` | Configures auto-scaling for a deployment. |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
ctr_build
    │
    ▼
ctr_deploy
    │
    ▼
ctr_scale
    │
    ▼
ctr_monitor

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
  --workflow container_orchestration_demo \
  --version 1 \
  --input '{"serviceName": "test", "imageTag": "sample-imageTag", "replicas": "sample-replicas"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w container_orchestration_demo -s COMPLETED -c 5

```

## How to Extend

Each worker owns one deployment lifecycle step. replace the demo Docker and Kubernetes calls with real container platform APIs and the build-deploy-scale-monitor pipeline runs unchanged.

- **CtrBuildWorker** (`ctr_build`): trigger a real Docker build via the Docker Engine API, AWS CodeBuild `startBuild()`, or Google Cloud Build, then push the image to ECR/GCR/DockerHub
- **CtrDeployWorker** (`ctr_deploy`): call the Kubernetes API (`kubectl apply` or the Java Kubernetes client) to create a Deployment, or use ECS `createService()` / Cloud Run `createService()`
- **CtrMonitorWorker** (`ctr_monitor`): register the deployment with Datadog (`createMonitor`), Prometheus (scrape config update), or set up CloudWatch alarms for the ECS service

The deployment output contract stays fixed. Swap Kubernetes API calls for ECS or Docker Swarm and the build-deploy-scale-monitor pipeline runs unchanged.

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
container-orchestration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/containerorchestration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ContainerOrchestrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CtrBuildWorker.java
│       ├── CtrDeployWorker.java
│       ├── CtrMonitorWorker.java
│       └── CtrScaleWorker.java

```
