# Data Lineage in Java Using Conductor :  Source Registration, Transformation Tracking, and Lineage Graph Construction

A Java Conductor workflow example for data lineage tracking: registering the data source origin, applying sequential transformations while recording each step's impact, recording the final destination, and building a lineage graph that shows exactly how each record was transformed from source to destination. Uses [Conductor](https://github.

## The Problem

When a number looks wrong in a report, your first question is "where did this data come from and what happened to it along the way?" You need end-to-end lineage tracking: recording where data originated (which database, API, or file), documenting every transformation applied (name normalization, email lowercasing, field derivations), noting where the data landed (which table, data warehouse, or API), and building a graph that traces any record's journey from source to destination. Without lineage, debugging data quality issues is guesswork, compliance audits are painful, and impact analysis for schema changes is impossible.

Without orchestration, lineage tracking is an afterthought bolted onto transformation scripts, a log line here, a metadata table update there. Transformations happen inline with no structured record of what changed. If a transformation step fails and gets manually restarted, the lineage becomes inconsistent. There's no unified graph showing the full journey, and adding a new transformation step means remembering to update the lineage tracking in a completely separate place.

## The Solution

**You just write the source registration, transformation, destination recording, and lineage graph workers. Conductor handles sequential execution with built-in observability at both the orchestration and application level, giving you lineage tracking alongside retries and crash recovery.**

Each stage of the pipeline is a simple, independent worker that both transforms data and appends to the lineage chain. The source registrar records the origin system and initializes the lineage metadata. Each transformation worker applies its logic (uppercase names, lowercase emails) and appends a lineage entry documenting what it changed. The destination recorder notes where the final data lands. The graph builder assembles all lineage entries into a structured graph showing the full source-to-destination journey with every transformation step. Conductor executes them in sequence, passes the growing lineage chain between steps, and provides built-in observability for every transformation's inputs and outputs. Giving you lineage tracking both at the application level and the orchestration level. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers track lineage across the data pipeline: registering the source origin, applying sequential transformations that each append lineage metadata, recording the destination, and building a source-to-destination lineage graph.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyTransform1Worker** | `ln_apply_transform_1` | Applies transform 1: uppercase names and tracks lineage. |
| **ApplyTransform2Worker** | `ln_apply_transform_2` | Applies transform 2: lowercase emails and tracks lineage. |
| **BuildLineageGraphWorker** | `ln_build_lineage_graph` | Builds a lineage graph summary from collected lineage entries. |
| **RecordDestinationWorker** | `ln_record_destination` | Records the destination in the lineage chain. |
| **RegisterSourceWorker** | `ln_register_source` | Registers the data source and initializes lineage tracking. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
ln_register_source
    │
    ▼
ln_apply_transform_1
    │
    ▼
ln_apply_transform_2
    │
    ▼
ln_record_destination
    │
    ▼
ln_build_lineage_graph

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
java -jar target/data-lineage-1.0.0.jar

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
java -jar target/data-lineage-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_lineage \
  --version 1 \
  --input '{"records": "sample-records", "sourceName": "test", "destName": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_lineage -s COMPLETED -c 5

```

## How to Extend

Register sources in Apache Atlas or DataHub, emit OpenLineage events from each transform, and build a graph in Neo4j, the lineage tracking workflow runs unchanged.

- **RegisterSourceWorker** → register sources in Apache Atlas, DataHub, or OpenLineage-compatible metadata stores with connection details and schema snapshots
- **ApplyTransform1/2Workers** → apply real transformations (dbt models, SQL transforms, Python scripts) while emitting OpenLineage events for each step
- **RecordDestinationWorker** → record the destination table/topic/API in your lineage store, linking it to the final transformed output
- **BuildLineageGraphWorker** → construct a real lineage graph in Neo4j, Apache Atlas, or DataHub; generate visual DAG representations for data governance dashboards

Adding new transformation steps or connecting to a real lineage store like Apache Atlas does not affect the pipeline, as long as each worker appends its lineage entry in the expected format.

**Add new stages** by inserting tasks in `workflow.json`, for example, additional transformation steps (each automatically tracked in the lineage chain), a data quality check between transforms, or an impact analysis step that identifies downstream consumers affected by source schema changes.

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
data-lineage/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/datalineage/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataLineageExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyTransform1Worker.java
│       ├── ApplyTransform2Worker.java
│       ├── BuildLineageGraphWorker.java
│       ├── RecordDestinationWorker.java
│       └── RegisterSourceWorker.java
└── src/test/java/datalineage/workers/
    ├── ApplyTransform1WorkerTest.java        # 6 tests
    ├── ApplyTransform2WorkerTest.java        # 6 tests
    ├── BuildLineageGraphWorkerTest.java        # 6 tests
    ├── RecordDestinationWorkerTest.java        # 6 tests
    └── RegisterSourceWorkerTest.java        # 6 tests

```
