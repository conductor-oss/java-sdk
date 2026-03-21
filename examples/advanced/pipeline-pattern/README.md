# Pipeline Pattern in Java Using Conductor :  Sequential Data Processing Through Stages

A Java Conductor workflow example for the pipeline pattern. passing raw data through a series of sequential processing stages where each stage transforms the data and passes its output to the next. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Data Transformation Requires a Clean Stage-by-Stage Flow

Raw sensor data arrives as unstructured readings. Stage 1 parses the binary payload into structured fields. Stage 2 converts units (Fahrenheit to Celsius, PSI to bar). Stage 3 applies calibration offsets. Stage 4 writes the calibrated data to the time-series database. Each stage must receive the exact output of the previous stage. feeding uncalibrated data to the database stage produces incorrect readings.

Building a pipeline as a monolithic function tangles parsing, conversion, calibration, and storage into a single class. When you need to add a fifth stage (anomaly detection), you're editing a 500-line method instead of adding a standalone worker.

## The Solution

**You write each transformation stage. Conductor handles stage ordering, retries, and data flow between stages.**

`PipStage1Worker` handles initial parsing or ingestion. `PipStage2Worker` applies the first transformation. `PipStage3Worker` performs the next processing step, using the previous stage's output. `PipStage4Worker` completes the pipeline and produces the final result. Each stage is a standalone worker that receives input, transforms it, and passes the result to the next stage. Conductor guarantees strict ordering, retries any failed stage, and records every stage's input and output. so you can inspect the data at any point in the pipeline.

### What You Write: Workers

Four stage workers form a sequential transformation chain: validation, format transformation, metadata enrichment, and final output, each receiving the exact output of the previous stage.

| Worker | Task | What It Does |
|---|---|---|
| **PipStage1Worker** | `pip_stage_1` | Validates the incoming data and passes it downstream if well-formed |
| **PipStage2Worker** | `pip_stage_2` | Transforms the validated data into the target format |
| **PipStage3Worker** | `pip_stage_3` | Enriches the transformed data with contextual metadata (region, currency) |
| **PipStage4Worker** | `pip_stage_4` | Finalizes the enriched data and produces the timestamped output record |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
pip_stage_1
    │
    ▼
pip_stage_2
    │
    ▼
pip_stage_3
    │
    ▼
pip_stage_4

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
java -jar target/pipeline-pattern-1.0.0.jar

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
java -jar target/pipeline-pattern-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow pip_pipeline_pattern \
  --version 1 \
  --input '{"rawData": {"key": "value"}, "pipelineId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w pip_pipeline_pattern -s COMPLETED -c 5

```

## How to Extend

Each worker is one stage in the transformation chain. replace the demo parsing and conversion with real sensor data parsers or unit conversion libraries and the sequential pipeline runs unchanged.

- **PipStage1Worker** (`pip_stage_1`): parse real input formats: binary sensor data via ByteBuffer, protocol buffers via protobuf-java, or raw log lines via regex/Grok patterns
- **PipStage2Worker** (`pip_stage_2`): apply real transformations: unit conversion libraries, data normalization (z-score, min-max), or schema mapping with Jackson/JOLT
- **PipStage4Worker** (`pip_stage_4`): write to real destinations: InfluxDB for time-series data, PostgreSQL for structured records, or publish to a Kafka topic for downstream consumers

Each stage's output contract stays fixed. Adding a fifth stage (e.g., anomaly detection) means inserting one worker, not modifying the existing stages.

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
pipeline-pattern/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/pipelinepattern/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PipelinePatternExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PipStage1Worker.java
│       ├── PipStage2Worker.java
│       ├── PipStage3Worker.java
│       └── PipStage4Worker.java
└── src/test/java/pipelinepattern/workers/
    ├── PipStage1WorkerTest.java        # 4 tests
    ├── PipStage2WorkerTest.java        # 4 tests
    ├── PipStage3WorkerTest.java        # 4 tests
    └── PipStage4WorkerTest.java        # 4 tests

```
