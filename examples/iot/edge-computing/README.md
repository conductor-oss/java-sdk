# Edge Computing Pipeline in Java with Conductor :  Task Offloading, On-Device Inference, and Cloud Sync

A Java Conductor workflow example that orchestrates an edge computing pipeline. offloading compute jobs to edge nodes, running on-device ML inference (object detection, classification), syncing results back to the cloud, and aggregating edge analytics into dashboards and time-series stores. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Why Edge Compute Pipelines Need Orchestration

Edge computing pushes inference and data processing out to devices at the network edge. cameras running object detection, gateways classifying sensor readings, robots making real-time decisions. But the results still need to flow back to the cloud for aggregation, dashboarding, and long-term storage. That creates a multi-hop pipeline: schedule the compute job on the right edge node, run the inference, sync the results over a potentially unreliable link, and aggregate everything centrally.

Each hop has its own failure mode. The edge node might be offline. The inference job might time out. The sync might fail due to bandwidth constraints or intermittent connectivity. Without orchestration, you'd build a custom pipeline manager that tracks job state across edge and cloud, retries failed syncs with exponential backoff, and logs everything manually. That manager becomes a distributed systems problem in itself.

## How This Workflow Solves It

**You just write the edge pipeline workers. Task offloading, on-device inference, result syncing, and cloud aggregation. Conductor handles multi-hop sequencing, unreliable-link sync retries, and latency tracking at every hop for pipeline optimization.**

Each stage of the edge-to-cloud pipeline is an independent worker. offload the job, run the inference, sync the results, aggregate in the cloud. Conductor sequences them, passes inference outputs (detected objects, classification labels, confidence scores) through to the sync and aggregation stages, retries failed syncs automatically, and tracks latency and data sizes at every hop. You get a reliable edge compute pipeline without writing state management or retry logic.

### What You Write: Workers

Four workers span the edge-to-cloud pipeline: OffloadTaskWorker schedules compute jobs on edge nodes, ProcessEdgeWorker runs on-device ML inference, SyncResultsWorker uploads results to cloud storage, and AggregateCloudWorker feeds dashboards and time-series stores.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateCloudWorker** | `edg_aggregate_cloud` | Aggregates synced edge results into cloud dashboards and time-series stores. |
| **OffloadTaskWorker** | `edg_offload_task` | Schedules a compute job on the target edge node and returns the edge job ID. |
| **ProcessEdgeWorker** | `edg_process_edge` | Runs on-device ML inference (object detection, classification) and returns results with processing time. |
| **SyncResultsWorker** | `edg_sync_results` | Uploads inference results from the edge node to cloud storage, tracking sync latency and payload size. |

Workers implement device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs. the workflow and alerting logic stay the same.

### The Workflow

```
edg_offload_task
    │
    ▼
edg_process_edge
    │
    ▼
edg_sync_results
    │
    ▼
edg_aggregate_cloud

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
java -jar target/edge-computing-1.0.0.jar

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
java -jar target/edge-computing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow edge_computing_workflow \
  --version 1 \
  --input '{"jobId": "TEST-001", "edgeNodeId": "TEST-001", "taskType": "standard", "payload": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w edge_computing_workflow -s COMPLETED -c 5

```

## How to Extend

Connect OffloadTaskWorker to your edge node scheduler, ProcessEdgeWorker to your on-device ML runtime (TensorFlow Lite, ONNX), and SyncResultsWorker to your cloud storage and dashboards. The workflow definition stays exactly the same.

- **OffloadTaskWorker** (`edg_offload_task`): call your edge orchestrator (AWS IoT Greengrass, Azure IoT Edge, K3s) to schedule the compute job on the target edge node and return the job ID and estimated completion time
- **ProcessEdgeWorker** (`edg_process_edge`): run the actual ML inference on the edge device (TensorFlow Lite, ONNX Runtime, OpenVINO) and return detected objects, classification labels, and confidence scores
- **SyncResultsWorker** (`edg_sync_results`): upload inference results from the edge node to cloud storage (S3, Azure Blob) over MQTT or HTTPS, tracking sync latency and payload size
- **AggregateCloudWorker** (`edg_aggregate_cloud`): write results to your time-series database (InfluxDB, TimescaleDB), update Grafana dashboards, and roll up per-node job counts

Point each worker at your edge device fleet or cloud analytics while keeping the same return schema, and the pipeline needs no reconfiguration.

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
edge-computing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/edgecomputing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EdgeComputingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateCloudWorker.java
│       ├── OffloadTaskWorker.java
│       ├── ProcessEdgeWorker.java
│       └── SyncResultsWorker.java
└── src/test/java/edgecomputing/workers/
    ├── OffloadTaskWorkerTest.java        # 2 tests
    └── ProcessEdgeWorkerTest.java        # 2 tests

```
