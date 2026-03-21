# Task Routing in Java Using Conductor :  Analyze Requirements, Select Worker Pool, Dispatch, Verify

A Java Conductor workflow example for intelligent task routing. analyzing a task's resource requirements (CPU, GPU, memory) and region constraints, selecting the optimal worker pool to handle it, dispatching the task to that pool, and verifying successful execution. Uses [Conductor](https://github.

## Tasks Have Different Resource Needs :  Send Them to the Right Pool

An ML inference task needs a GPU-equipped worker in us-east-1. A data transformation task needs a high-memory worker in any region. A simple validation task can run on any small worker anywhere. Sending all tasks to the same pool wastes GPU resources on validation and starves inference tasks when the general pool is full.

Intelligent task routing means analyzing each task's requirements (GPU type, memory needs, region affinity), matching those requirements against available worker pools, dispatching to the best-fit pool, and verifying that the task ran successfully on the selected infrastructure.

## The Solution

**You write the requirements analysis and pool selection logic. Conductor handles dispatch sequencing, retries, and routing audit trails.**

`TrtAnalyzeRequirementsWorker` examines the task type, resource needs, and region constraints to build a requirements profile. `TrtSelectPoolWorker` matches the requirements against available worker pools and selects the best fit. considering capacity, cost, and locality. `TrtDispatchWorker` sends the task to the selected pool. `TrtVerifyWorker` confirms the task executed successfully on the target infrastructure. Conductor sequences these steps and records the routing decision,  which pool was selected, why, and whether the task succeeded there.

### What You Write: Workers

Four workers manage intelligent dispatch: requirements analysis, pool selection based on capacity and locality, task dispatch, and execution verification, each decoupled from the infrastructure it targets.

| Worker | Task | What It Does |
|---|---|---|
| **TrtAnalyzeRequirementsWorker** | `trt_analyze_requirements` | Determines resource requirements (CPU, memory, GPU) and lists available worker pools with their capabilities |
| **TrtDispatchWorker** | `trt_dispatch` | Dispatches the task to the selected pool with a unique dispatch ID |
| **TrtSelectPoolWorker** | `trt_select_pool` | Selects the best-fit worker pool based on requirements and current load |
| **TrtVerifyWorker** | `trt_verify` | Confirms that the dispatched task was executed successfully on the target pool |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
trt_analyze_requirements
    │
    ▼
trt_select_pool
    │
    ▼
trt_dispatch
    │
    ▼
trt_verify

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
java -jar target/task-routing-1.0.0.jar

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
java -jar target/task-routing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow trt_task_routing \
  --version 1 \
  --input '{"taskType": "standard", "resourceNeeds": "api", "region": "us-east-1"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w trt_task_routing -s COMPLETED -c 5

```

## How to Extend

Each worker handles one routing decision. replace the simulated pool selection with real Kubernetes node affinity or cloud resource APIs and the analyze-select-dispatch pipeline runs unchanged.

- **TrtAnalyzeRequirementsWorker** (`trt_analyze_requirements`): parse real resource requirements from task metadata: Kubernetes resource requests, AWS instance type requirements, or custom resource labels
- **TrtSelectPoolWorker** (`trt_select_pool`): query real cluster capacity via Kubernetes API (`kubectl get nodes --show-labels`), AWS Auto Scaling group status, or a custom resource broker to find the best-fit pool
- **TrtDispatchWorker** (`trt_dispatch`): submit to real compute pools: Kubernetes Job with nodeSelector/tolerations, AWS Batch `submitJob()` with compute environment selection, or SLURM `sbatch` with partition targeting

The requirements and dispatch contract stays fixed. Swap the simulated pool selection for real Kubernetes node selectors or Nomad job placement and the analyze-select-verify pipeline runs unchanged.

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
task-routing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/taskrouting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TaskRoutingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── TrtAnalyzeRequirementsWorker.java
│       ├── TrtDispatchWorker.java
│       ├── TrtSelectPoolWorker.java
│       └── TrtVerifyWorker.java
└── src/test/java/taskrouting/workers/
    ├── TrtAnalyzeRequirementsWorkerTest.java        # 4 tests
    ├── TrtDispatchWorkerTest.java        # 4 tests
    ├── TrtSelectPoolWorkerTest.java        # 4 tests
    └── TrtVerifyWorkerTest.java        # 4 tests

```
