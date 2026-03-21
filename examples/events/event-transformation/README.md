# Event Transformation in Java Using Conductor

Event Transformation Pipeline. parse raw events, enrich with context, map to CloudEvents schema, and deliver to target. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to transform raw events from one format into another before delivering them downstream. The pipeline must parse raw events (JSON, XML, CSV), enrich them with contextual data (geo-IP lookup, user profile enrichment), map them to a target schema (CloudEvents, custom schema), and deliver the transformed events to the target system. Without a transformation pipeline, every downstream consumer must implement its own parsing and enrichment logic.

Without orchestration, you'd build a monolithic transformer that parses, enriches, maps, and delivers in a single method. manually handling parse failures for malformed input, retrying enrichment API calls, managing schema version differences, and logging every transformation for debugging.

## The Solution

**You just write the parse, enrich, schema-map, and delivery workers. Conductor handles sequential transformation stages, retry on enrichment API failures, and full before/after tracking for every event.**

Each transformation stage is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing the pipeline (parse, enrich, map, deliver), retrying if the enrichment API or target system is unavailable, tracking every event's transformation with full before/after details, and resuming from the last stage if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers form the transformation pipeline: ParseEventWorker normalizes raw input, EnrichEventWorker adds contextual metadata, MapSchemaWorker converts to CloudEvents format, and OutputEventWorker delivers the transformed event to its destination.

| Worker | Task | What It Does |
|---|---|---|
| **EnrichEventWorker** | `et_enrich_event` | Enriches a parsed event with additional context: department, role, and location for the actor; project and environmen... |
| **MapSchemaWorker** | `et_map_schema` | Maps an enriched event to CloudEvents format (specversion 1.0). Produces: specversion, type, source, id, time, dataco... |
| **OutputEventWorker** | `et_output_event` | Delivers the mapped event to its target destination and returns delivery metadata: outputEventId, delivered flag, out... |
| **ParseEventWorker** | `et_parse_event` | Parses a raw event from a legacy format into a normalized structure. Extracts and renames fields: event_id->id, event... |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
et_parse_event
    │
    ▼
et_enrich_event
    │
    ▼
et_map_schema
    │
    ▼
et_output_event

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
java -jar target/event-transformation-1.0.0.jar

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
java -jar target/event-transformation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_transformation_wf \
  --version 1 \
  --input '{"rawEvent": "sample-rawEvent", "sourceFormat": "api", "targetFormat": "production"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_transformation_wf -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real legacy event source, enrichment context (user directory, project database), and CloudEvents-compatible target, the parse-enrich-map-deliver transformation pipeline workflow stays exactly the same.

- **Parser**: handle real input formats (JSON with Jackson, XML with JAXB, CSV with OpenCSV, Protobuf with protoc-generated classes)
- **Enricher**: call enrichment APIs (MaxMind for geo-IP, your user service for profile data, third-party APIs for entity resolution)
- **Schema mapper**: transform to CloudEvents spec, Apache Avro, or your custom schema using a mapping engine
- **Delivery worker**: publish to target systems (Kafka, REST API, S3, database) with delivery confirmation

Swapping the enrichment source or target schema format changes nothing in the parse-enrich-map-deliver pipeline.

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
event-transformation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventtransformation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventTransformationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EnrichEventWorker.java
│       ├── MapSchemaWorker.java
│       ├── OutputEventWorker.java
│       └── ParseEventWorker.java
└── src/test/java/eventtransformation/workers/
    ├── EnrichEventWorkerTest.java        # 10 tests
    ├── MapSchemaWorkerTest.java        # 9 tests
    ├── OutputEventWorkerTest.java        # 9 tests
    └── ParseEventWorkerTest.java        # 10 tests

```
