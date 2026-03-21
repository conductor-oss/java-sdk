# Multi-Cluster Processing in Java Using Conductor :  Partition Data, Process in Parallel Across Clusters, Aggregate

A Java Conductor workflow example for multi-cluster data processing. preparing a job by partitioning the dataset, processing each partition on a different geographic cluster (us-east-1 and us-west-2) in parallel via `FORK_JOIN`, and aggregating the results into a unified output. Uses [Conductor](https://github.

## One Cluster Is Not Enough

Your dataset has grown beyond what a single cluster can process in the required time window. You need to split the workload across clusters in different regions. sending partition A to us-east-1 and partition B to us-west-2,  then merge the results. This cuts processing time in half, adds geographic redundancy, and keeps data closer to regional users.

Coordinating multi-cluster processing means partitioning the data correctly, dispatching each partition to the right cluster, waiting for both to finish (while handling the case where one cluster is slower or fails), and merging results that may have different schemas or record counts. Building this with scripts that SSH into different clusters becomes unmanageable.

## The Solution

**You write the per-cluster processing logic. Conductor handles parallel dispatch, cross-region retries, and result aggregation.**

`MclPrepareWorker` takes the dataset size, partitions it into two halves, and returns the partition references. A `FORK_JOIN` dispatches both partitions simultaneously. `MclClusterEastWorker` processes partition A on us-east-1 while `MclClusterWestWorker` processes partition B on us-west-2. The `JOIN` waits for both clusters to complete. `MclAggregateWorker` merges the per-cluster results and record counts into a single output. Conductor handles the parallel dispatch, retries if one cluster's processing fails, and records per-cluster metrics so you can compare processing times and identify regional bottlenecks.

### What You Write: Workers

Four workers handle the multi-cluster pipeline. Data partitioning, parallel per-region processing on us-east-1 and us-west-2, and cross-cluster result aggregation.

| Worker | Task | What It Does |
|---|---|---|
| **MclAggregateWorker** | `mcl_aggregate` | Combines results from east and west clusters, summing total records processed |
| **MclClusterEastWorker** | `mcl_cluster_east` | Processes the assigned data partition on the us-east-1 cluster and reports record count and latency |
| **MclClusterWestWorker** | `mcl_cluster_west` | Processes the assigned data partition on the us-west-2 cluster and reports record count and latency |
| **MclPrepareWorker** | `mcl_prepare` | Splits the dataset into two equal partitions for distribution across clusters |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
mcl_prepare
    │
    ▼
FORK_JOIN
    ├── mcl_cluster_east
    └── mcl_cluster_west
    │
    ▼
JOIN (wait for all branches)
mcl_aggregate

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
java -jar target/multi-cluster-1.0.0.jar

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
java -jar target/multi-cluster-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow multi_cluster_demo \
  --version 1 \
  --input '{"jobId": "TEST-001", "datasetSize": 10}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w multi_cluster_demo -s COMPLETED -c 5

```

## How to Extend

Each worker targets one cluster region. replace the simulated processing with real EMR or Spark job submissions to regional clusters and the partition-process-aggregate pipeline runs unchanged.

- **MclClusterEastWorker / MclClusterWestWorker**: submit jobs to real compute clusters via Kubernetes API (cross-cluster kubectl), AWS EMR `addJobFlowSteps()`, or Spark `spark-submit` against regional clusters
- **MclPrepareWorker** (`mcl_prepare`): implement real data partitioning using Hadoop InputSplit, Spark partitionBy, or S3 prefix-based sharding across regions
- **MclAggregateWorker** (`mcl_aggregate`): merge results from multiple clusters using UNION ALL queries, Spark DataFrame joins, or custom merging logic that handles schema differences between clusters

The per-cluster result contract stays fixed. Swap simulated processing for real Spark or Flink jobs on each regional cluster and the partition-aggregate pipeline runs unchanged.

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
multi-cluster/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multicluster/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultiClusterExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MclAggregateWorker.java
│       ├── MclClusterEastWorker.java
│       ├── MclClusterWestWorker.java
│       └── MclPrepareWorker.java
└── src/test/java/multicluster/workers/
    ├── MclAggregateWorkerTest.java        # 4 tests
    ├── MclClusterEastWorkerTest.java        # 4 tests
    ├── MclClusterWestWorkerTest.java        # 4 tests
    └── MclPrepareWorkerTest.java        # 4 tests

```
