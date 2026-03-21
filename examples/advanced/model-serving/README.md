# Model Serving Pipeline in Java Using Conductor :  Load, Validate, Deploy, Test, Promote

A Java Conductor workflow example for deploying ML models to production serving .  loading a model from storage, validating it against test inputs, deploying to a staging endpoint, running smoke tests, and promoting to production. Uses [Conductor](https://github.

## Deploying Models to Production Is Not Just Copying a File

A data scientist trains a model and hands off a `.pt` file. Getting that file into production serving means loading it into the inference framework, validating that it produces expected outputs for known inputs (no NaN predictions, correct tensor shapes), deploying it to a staging endpoint, running smoke tests against real traffic patterns, and only then promoting it to handle production traffic. Skip any step and you risk serving garbage predictions.

When the smoke test fails .  maybe the model expects a different feature schema than what production sends ,  you need to see exactly which step failed, what the model's outputs looked like, and roll back without affecting the live endpoint.

## The Solution

**You write the model loading and smoke test logic. Conductor handles the staged rollout, retries, and deployment tracking.**

`MsvLoadModelWorker` loads the model from the specified path into the inference framework. `MsvValidateWorker` runs the model against test inputs to verify output shapes, value ranges, and latency. `MsvDeployWorker` creates a staging endpoint with the validated model. `MsvTestWorker` sends production-like requests to the staging endpoint and checks predictions against expected baselines. `MsvPromoteWorker` swaps the staging model into the production endpoint. Conductor sequences these steps, retries any that fail, and records every step's inputs and outputs so you can trace exactly why a deployment succeeded or was blocked.

### What You Write: Workers

Four workers manage the serving rollout: model loading, validation against test inputs, staged deployment, and production promotion, each gating the next step to prevent serving bad predictions.

| Worker | Task | What It Does |
|---|---|---|
| **MsvDeployWorker** | `msv_deploy` | Deploys the model to a serving endpoint with replicas in the target environment (staging/production) |
| **MsvLoadModelWorker** | `msv_load_model` | Loads the model artifact, resolving its serving signature, input shape, and size |
| **MsvPromoteWorker** | `msv_promote` | Promotes the model from staging to production if all validation tests passed |
| **MsvValidateWorker** | `msv_validate` | Validates the loaded model for correctness and reports any warnings |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### The Workflow

```
msv_load_model
    │
    ▼
msv_validate
    │
    ▼
msv_deploy
    │
    ▼
msv_test
    │
    ▼
msv_promote

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
java -jar target/model-serving-1.0.0.jar

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
java -jar target/model-serving-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow model_serving_demo \
  --version 1 \
  --input '{"modelName": "test", "modelVersion": "gpt-4o-mini", "modelPath": "gpt-4o-mini"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w model_serving_demo -s COMPLETED -c 5

```

## How to Extend

Each worker covers one deployment gate .  replace the simulated model loading with real TorchServe or SageMaker endpoint APIs and the load-validate-deploy-promote pipeline runs unchanged.

- **MsvLoadModelWorker** (`msv_load_model`): load real models from S3/GCS using TorchServe, TensorFlow Serving, or ONNX Runtime model loading APIs
- **MsvDeployWorker** (`msv_deploy`): create real inference endpoints via SageMaker `createEndpoint()`, Kubernetes deployments (Seldon Core, KServe), or Google Cloud AI Platform
- **MsvPromoteWorker** (`msv_promote`): shift production traffic using blue-green deployment (update load balancer), canary rollout (Istio traffic splitting), or SageMaker endpoint variant weights

The validation and deployment contract stays fixed. Swap the simulated inference framework for real TorchServe or TensorFlow Serving and the staged rollout pipeline runs unchanged.

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
model-serving/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/modelserving/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ModelServingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MsvDeployWorker.java
│       ├── MsvLoadModelWorker.java
│       ├── MsvPromoteWorker.java
│       └── MsvValidateWorker.java
└── src/test/java/modelserving/workers/
    ├── MsvDeployWorkerTest.java        # 4 tests
    ├── MsvLoadModelWorkerTest.java        # 4 tests
    ├── MsvPromoteWorkerTest.java        # 4 tests
    ├── MsvTestWorkerTest.java        # 4 tests
    └── MsvValidateWorkerTest.java        # 4 tests

```
