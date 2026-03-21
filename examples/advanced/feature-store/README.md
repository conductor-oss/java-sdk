# Feature Store Pipeline in Java Using Conductor :  Compute, Validate, Register, Serve

A Java Conductor workflow example for feature store management. computing features from a source table, validating them against quality constraints, registering the validated feature group in the feature registry, and enabling the features for online serving. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Features Rot Without a Pipeline

ML models depend on features. user_lifetime_value, avg_session_duration, days_since_last_purchase,  that are computed from raw data. When a data scientist computes features in a notebook, they work for that one training run. But serving those same features in production requires computing them on a schedule, validating that distributions haven't drifted, registering the new version in a catalog so models know where to find them, and enabling low-latency serving for real-time inference.

Without a pipeline, feature computation lives in ad-hoc scripts, validation is skipped, and the registry is a spreadsheet. The model in production reads stale features because nobody updated the serving layer, or worse, serves features computed with a bug that validation would have caught.

## The Solution

**You write the feature computation and validation logic. Conductor handles the registry pipeline, retries, and serving coordination.**

`FstComputeFeaturesWorker` reads the source table, computes the feature group by entity key, and produces feature vectors with statistics (mean, stddev, null rates). `FstValidateWorker` checks the computed features against quality constraints. no null rates above threshold, distributions within expected ranges, schema matches the registered definition. `FstRegisterWorker` writes the validated features to the feature registry with a version number. `FstServeWorker` enables the registered feature group for online serving so models can query features by entity key in real time. Conductor ensures features are never served before validation passes, and records the version, validation results, and serving status for every run.

### What You Write: Workers

Four workers manage the feature lifecycle: computation from raw data, quality validation, registry registration, and online serving enablement, ensuring features are never served before validation passes.

| Worker | Task | What It Does |
|---|---|---|
| **FstComputeFeaturesWorker** | `fst_compute_features` | Computes feature values from raw data, reporting feature count, row count, and null rate statistics |
| **FstRegisterWorker** | `fst_register` | Registers the feature group in the feature store registry with a unique ID |
| **FstServeWorker** | `fst_serve` | Enables online serving for the feature group and returns the serving endpoint URL |
| **FstValidateWorker** | `fst_validate` | Validates computed features for schema correctness and assigns a version |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
fst_compute_features
    │
    ▼
fst_validate
    │
    ▼
fst_register
    │
    ▼
fst_serve

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
java -jar target/feature-store-1.0.0.jar

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
java -jar target/feature-store-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow feature_store_demo \
  --version 1 \
  --input '{"featureGroupName": "test", "sourceTable": "api", "entityKey": "sample-entityKey"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w feature_store_demo -s COMPLETED -c 5

```

## How to Extend

Each worker manages one feature lifecycle step. replace the demo compute and validation with real Feast or Tecton APIs and the feature registration pipeline runs unchanged.

- **FstComputeFeaturesWorker** (`fst_compute_features`): run real feature engineering with Feast (`feast materialize`), Spark SQL queries against your data warehouse, or Tecton feature pipelines
- **FstValidateWorker** (`fst_validate`): use Great Expectations or Deequ for data quality validation, checking for distribution drift, null rates, and schema conformance
- **FstServeWorker** (`fst_serve`): enable online serving via Feast Online Store (Redis/DynamoDB backend), SageMaker Feature Store, or Vertex AI Feature Store

The feature vector output contract stays fixed. Swap the demo computation for a real Feast or Tecton integration and the validate-register-serve pipeline runs unchanged.

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
feature-store/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/featurestore/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FeatureStoreExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FstComputeFeaturesWorker.java
│       ├── FstRegisterWorker.java
│       ├── FstServeWorker.java
│       └── FstValidateWorker.java
└── src/test/java/featurestore/workers/
    ├── FstComputeFeaturesWorkerTest.java        # 4 tests
    ├── FstRegisterWorkerTest.java        # 4 tests
    ├── FstServeWorkerTest.java        # 4 tests
    └── FstValidateWorkerTest.java        # 4 tests

```
