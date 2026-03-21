# Data Format Normalizer in Java Using Conductor :  Detect Format, Convert JSON/XML/CSV, Output Canonical Form

A Java Conductor workflow example for data normalization. detecting the input format of incoming data (JSON, XML, or CSV), routing to the appropriate format-specific converter via a `SWITCH` task, and producing a canonical output regardless of the source format. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Every Source System Speaks a Different Format

Your ERP sends XML, your CRM sends JSON, and your partner's FTP drop is CSV. Downstream analytics expects a single canonical format. Each integration point needs its own parser, and when a new source system joins with YAML or fixed-width files, you add another branch to a growing if/else chain.

The normalizer pattern detects the incoming format, routes to the correct converter, and produces the same canonical output structure regardless of how the data arrived. Adding a new format means adding one converter worker and one SWITCH case. not touching the rest of the pipeline.

## The Solution

**You write the format converters. Conductor handles detection routing, retries, and canonical output delivery.**

`NrmDetectFormatWorker` examines the raw input and source system metadata to determine whether the data is JSON, XML, or CSV. A `SWITCH` task routes to the matching converter: `NrmConvertJsonWorker` normalizes JSON payloads, `NrmConvertXmlWorker` transforms XML documents, and `NrmConvertCsvWorker` parses CSV rows. each producing the same canonical output structure. `NrmOutputWorker` emits the normalized result. Conductor's declarative routing means adding a new format is a one-worker, one-case change.

### What You Write: Workers

Five workers cover the normalization pipeline: format detection, and three format-specific converters (JSON, XML, CSV) plus canonical output delivery, each producing the same output structure regardless of source format.

| Worker | Task | What It Does |
|---|---|---|
| **NrmConvertCsvWorker** | `nrm_convert_csv` | Parses CSV input and converts it to the canonical format with row/column counts |
| **NrmConvertJsonWorker** | `nrm_convert_json` | Parses JSON input and converts it to the canonical format |
| **NrmConvertXmlWorker** | `nrm_convert_xml` | Parses XML input and converts it to the canonical format with element counts |
| **NrmDetectFormatWorker** | `nrm_detect_format` | Inspects the raw input to detect its format (JSON, CSV, or XML) for routing |
| **NrmOutputWorker** | `nrm_output` | Produces the final normalized output, recording the original and canonical formats |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
nrm_detect_format
    │
    ▼
SWITCH (nrm_switch_ref)
    ├── json: nrm_convert_json
    ├── xml: nrm_convert_xml
    ├── csv: nrm_convert_csv
    └── default: nrm_convert_json
    │
    ▼
nrm_output

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
java -jar target/normalizer-1.0.0.jar

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
java -jar target/normalizer-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow nrm_normalizer \
  --version 1 \
  --input '{"rawInput": "sample-rawInput", "sourceSystem": "api"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w nrm_normalizer -s COMPLETED -c 5

```

## How to Extend

Each worker converts one input format. replace the demo parsers with real JAXB, Jackson, or Apache Commons CSV libraries and the detect-convert-output pipeline runs unchanged.

- **NrmDetectFormatWorker** (`nrm_detect_format`): use Apache Tika for real format detection, or inspect Content-Type headers and file magic bytes to classify input format automatically
- **NrmConvertXmlWorker** (`nrm_convert_xml`): parse real XML using JAXB, Jackson XML, or DOM4j and transform to your canonical JSON schema
- **NrmConvertCsvWorker** (`nrm_convert_csv`): parse real CSV files using Apache Commons CSV or OpenCSV, mapping columns to canonical field names with configurable column mappings

The canonical output contract stays fixed. Adding a new format means adding one converter worker and one SWITCH case, not modifying existing converters.

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
normalizer/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/normalizer/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── NormalizerExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── NrmConvertCsvWorker.java
│       ├── NrmConvertJsonWorker.java
│       ├── NrmConvertXmlWorker.java
│       ├── NrmDetectFormatWorker.java
│       └── NrmOutputWorker.java
└── src/test/java/normalizer/workers/
    ├── NrmConvertCsvWorkerTest.java        # 4 tests
    ├── NrmConvertJsonWorkerTest.java        # 4 tests
    ├── NrmConvertXmlWorkerTest.java        # 4 tests
    ├── NrmDetectFormatWorkerTest.java        # 4 tests
    └── NrmOutputWorkerTest.java        # 4 tests

```
