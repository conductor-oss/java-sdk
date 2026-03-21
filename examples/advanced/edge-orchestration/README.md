# Edge Computing Orchestration in Java Using Conductor :  Dispatch, Process, Collect, Merge

A Java Conductor workflow example for edge computing orchestration. dispatching a job to multiple edge nodes, executing processing on each node, collecting results from all nodes, and merging them into a single unified output. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Processing Data at the Edge

Edge computing pushes computation close to the data source. IoT sensors on a factory floor, cameras at retail locations, or CDN nodes across geographies. A central system needs to dispatch an inference job to edge nodes, wait for each node to process its local data (video frames, sensor readings, log files), collect the partial results, and merge them into a global view. If one node is slow or fails, the central system needs to know, not silently drop that node's results.

Coordinating a fleet of edge nodes means tracking which nodes received the job, which have reported back, handling nodes that go offline mid-processing, and merging heterogeneous partial results into a coherent aggregate. Building this with SSH scripts or ad-hoc HTTP polling becomes untenable past a handful of nodes.

## The Solution

**You write the dispatch and edge processing logic. Conductor handles node coordination, retries, and result merging.**

`EorDispatchWorker` takes the job ID and list of edge nodes, assigns work to each node, and returns the task assignments. `EorEdgeProcessWorker` simulates the edge-side processing. running the job on each node's local data and producing per-node results. `EorCollectWorker` gathers results from all nodes and counts how many reported back. `EorMergeWorker` combines the collected partial results into a single merged output. Conductor sequences these steps, retries dispatch or collection if a network call to an edge node fails, and records the full execution,  which nodes were assigned, which responded, and what the merged result looks like.

### What You Write: Workers

Four workers coordinate edge-fleet processing: job dispatch to nodes, per-node execution, result collection, and multi-node result merging, each independent of the edge topology.

| Worker | Task | What It Does |
|---|---|---|
| **EorCollectWorker** | `eor_collect` | Gathers results from all edge nodes, reporting total node count and records processed |
| **EorDispatchWorker** | `eor_dispatch` | Assigns data partitions to edge nodes for distributed processing |
| **EorEdgeProcessWorker** | `eor_edge_process` | Runs the processing task on each edge node and returns per-node record counts |
| **EorMergeWorker** | `eor_merge` | Merges results from all edge nodes into a single consolidated output |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
eor_dispatch
    │
    ▼
eor_edge_process
    │
    ▼
eor_collect
    │
    ▼
eor_merge

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
java -jar target/edge-orchestration-1.0.0.jar

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
java -jar target/edge-orchestration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow edge_orchestration_demo \
  --version 1 \
  --input '{"jobId": "TEST-001", "edgeNodes": "sample-edgeNodes"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w edge_orchestration_demo -s COMPLETED -c 5

```

## How to Extend

Each worker handles one phase of the dispatch-process-collect cycle. replace the demo edge node calls with real IoT Greengrass or MQTT APIs and the edge orchestration logic runs unchanged.

- **EorDispatchWorker** (`eor_dispatch`): call AWS IoT Greengrass `createDeployment`, Azure IoT Edge, or send MQTT messages to edge devices to dispatch real jobs
- **EorEdgeProcessWorker** (`eor_edge_process`): invoke real edge-side inference (TensorFlow Lite, ONNX Runtime) or data processing via SSH, gRPC, or the AWS IoT Jobs API
- **EorMergeWorker** (`eor_merge`): aggregate edge results into a dashboard (Grafana API), write to a time-series database (InfluxDB, TimescaleDB), or publish to a Kafka topic for downstream analytics

The per-node result contract stays fixed. Swap the demo edge calls for real gRPC or MQTT dispatch and the collect-merge pipeline runs unchanged.

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
edge-orchestration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/edgeorchestration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EdgeOrchestrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EorCollectWorker.java
│       ├── EorDispatchWorker.java
│       ├── EorEdgeProcessWorker.java
│       └── EorMergeWorker.java
└── src/test/java/edgeorchestration/workers/
    ├── EorCollectWorkerTest.java        # 4 tests
    ├── EorDispatchWorkerTest.java        # 4 tests
    ├── EorEdgeProcessWorkerTest.java        # 4 tests
    └── EorMergeWorkerTest.java        # 4 tests

```
