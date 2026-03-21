# Dynamic Data Pipeline in Java Using Conductor :  Validate, Transform, Enrich, Publish

A Java Conductor workflow example for dynamic data pipelines. validating incoming payloads against configurable rules, transforming them into the target format (JSON, Avro, Parquet), enriching with data from external APIs, and publishing to an event bus. Each step is driven by its own JSON configuration, making the pipeline composable and reconfigurable without code changes. Uses [Conductor](https://github.

## Configurable Pipelines Without Hard-Coded Steps

Different data sources need different processing. an API webhook payload needs schema validation and JSON normalization, a batch CSV upload needs field mapping and type coercion, and a Kafka event needs enrichment from a lookup table before publishing. Hard-coding each pipeline variant means duplicating orchestration logic, and adding a new step (e.g., deduplication, PII masking) means touching every pipeline.

The dynamic workflow approach treats each step as a configurable unit: the validate step has a config saying which fields are required, the transform step knows the target format, the enrich step knows which external API to call, and the publish step knows the destination topic. Each step receives the previous step's output plus its own configuration, making the pipeline a chain of independent, reconfigurable stages.

## The Solution

**You write the validate-transform-enrich logic. Conductor handles stage chaining, retries, and pipeline observability.**

`DwValidateWorker` checks the payload against its validation config (required fields, type constraints). `DwTransformWorker` converts the validated data into the target format specified in its config (e.g., JSON normalization). `DwEnrichWorker` augments the transformed data with external API lookups based on its enrichment config. `DwPublishWorker` sends the final result to the configured event bus target. Each step passes its output to the next, and Conductor ensures the chain runs in order, retries any failed step, and tracks every input/output so you can debug exactly which configuration and data produced each result.

### What You Write: Workers

Four config-driven workers form the data pipeline: validation, transformation, enrichment, and publishing, each parameterized by its own JSON configuration block.

| Worker | Task | What It Does |
|---|---|---|
| **DwEnrichWorker** | `dw_enrich` | Enrich step in a dynamic pipeline. |
| **DwPublishWorker** | `dw_publish` | Publish step in a dynamic pipeline. |
| **DwTransformWorker** | `dw_transform` | Transform step in a dynamic pipeline. |
| **DwValidateWorker** | `dw_validate` | Validate step in a dynamic pipeline. |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
dw_validate
    │
    ▼
dw_transform
    │
    ▼
dw_enrich
    │
    ▼
dw_publish

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
java -jar target/dynamic-workflows-1.0.0.jar

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
java -jar target/dynamic-workflows-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow dynamic_workflow_demo \
  --version 1 \
  --input '{"pipelineName": "test", "payload": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w dynamic_workflow_demo -s COMPLETED -c 5

```

## How to Extend

Each worker is a config-driven pipeline stage. replace the simulated validation and transformation with real JSON Schema validators or JOLT transforms and the dynamic pipeline runs unchanged.

- **DwValidateWorker** (`dw_validate`): integrate JSON Schema validation (`everit-org/json-schema`), Apache Avro schema validation, or custom business rule engines for real payload validation
- **DwTransformWorker** (`dw_transform`): use Jackson for JSON transformations, Apache Avro for schema-based conversions, or JOLT for declarative JSON-to-JSON transforms
- **DwPublishWorker** (`dw_publish`): publish to a real Kafka topic (`kafka-clients`), SNS/SQS, or a webhook endpoint instead of simulating the event bus publish

The stage output contract stays fixed. Adding a new pipeline step means adding one worker and one config entry, not modifying existing stages.

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
dynamic-workflows/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dynamicworkflows/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DynamicWorkflowsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DwEnrichWorker.java
│       ├── DwPublishWorker.java
│       ├── DwTransformWorker.java
│       └── DwValidateWorker.java
└── src/test/java/dynamicworkflows/workers/
    ├── DwEnrichWorkerTest.java        # 7 tests
    ├── DwPublishWorkerTest.java        # 7 tests
    ├── DwTransformWorkerTest.java        # 7 tests
    └── DwValidateWorkerTest.java        # 7 tests

```
