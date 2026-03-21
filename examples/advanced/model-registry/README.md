# ML Model Registry in Java Using Conductor :  Register, Version, Validate, Approve, Deploy

A Java Conductor workflow example for ML model lifecycle management. registering a trained model with its artifact and metrics, assigning a version number, validating performance against quality gates, routing through an approval process, and deploying the approved model to production. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Models Without a Registry Are Unmanageable

Your team trains dozens of models a month. Without a registry, model artifacts live in S3 buckets with names like `model-final-v2-FIXED.pkl`, nobody knows which version is in production, and deploying a new model means SSH-ing into a server and swapping a file. When the new model degrades accuracy, there's no way to roll back to the previous version because nobody recorded which artifact, metrics, or approval was associated with it.

A model registry formalizes the lifecycle: register the artifact with its training metrics, assign a monotonically increasing version, validate that accuracy/latency meet the deployment threshold, get human or automated approval, and deploy to the serving infrastructure. Each step depends on the previous one. you can't deploy an unapproved model, and you can't approve without validation results.

## The Solution

**You write the registration and validation logic. Conductor handles the approval pipeline, retries, and model lineage tracking.**

`MrgRegisterWorker` stores the model artifact and its training metrics in the registry. `MrgVersionWorker` assigns a version number to the registered model. `MrgValidateWorker` checks the metrics against quality gates. minimum accuracy, maximum latency, no data leakage indicators. `MrgApproveWorker` routes through the approval process (automated or human-in-the-loop). `MrgDeployWorker` pushes the approved, validated model version to the serving infrastructure. Conductor records the full lineage,  which artifact, what metrics, which version, who approved, when deployed.

### What You Write: Workers

Five workers manage the model lifecycle: artifact registration, version assignment, quality-gate validation, approval routing, and production deployment, each gating the next stage.

| Worker | Task | What It Does |
|---|---|---|
| **MrgApproveWorker** | `mrg_approve` | Approves or rejects the model based on validation results, recording the approver |
| **MrgDeployWorker** | `mrg_deploy` | Deploys the approved model to the serving infrastructure |
| **MrgRegisterWorker** | `mrg_register` | Registers the model artifact in the registry with a unique registry ID |
| **MrgValidateWorker** | `mrg_validate` | Runs schema checks, performance regression tests, and bias audits on the model |
| **MrgVersionWorker** | `mrg_version` | Assigns a semantic version to the model, tracking the previous version for rollback |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
mrg_register
    │
    ▼
mrg_version
    │
    ▼
mrg_validate
    │
    ▼
mrg_approve
    │
    ▼
mrg_deploy

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
java -jar target/model-registry-1.0.0.jar

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
java -jar target/model-registry-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow model_registry_demo \
  --version 1 \
  --input '{"modelName": "test", "modelArtifact": "gpt-4o-mini", "metrics": "sample-metrics"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w model_registry_demo -s COMPLETED -c 5

```

## How to Extend

Each worker manages one model lifecycle gate. replace the demo registry operations with real MLflow or SageMaker Model Registry APIs and the version-validate-approve-deploy pipeline runs unchanged.

- **MrgRegisterWorker** (`mrg_register`): store model artifacts in MLflow Model Registry, SageMaker Model Registry, or Vertex AI Model Registry with associated training metadata
- **MrgValidateWorker** (`mrg_validate`): run real validation: load the model, score against a held-out dataset, check accuracy/latency/fairness metrics against configurable thresholds
- **MrgDeployWorker** (`mrg_deploy`): deploy to SageMaker endpoints, Kubernetes (Seldon/KServe), or TorchServe via their respective APIs

The registration and validation contract stays fixed. Swap the demo store for a real MLflow Model Registry or SageMaker Model Registry and the approve-deploy pipeline runs unchanged.

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
model-registry/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/modelregistry/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ModelRegistryExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MrgApproveWorker.java
│       ├── MrgDeployWorker.java
│       ├── MrgRegisterWorker.java
│       ├── MrgValidateWorker.java
│       └── MrgVersionWorker.java
└── src/test/java/modelregistry/workers/
    ├── MrgApproveWorkerTest.java        # 4 tests
    ├── MrgDeployWorkerTest.java        # 4 tests
    ├── MrgRegisterWorkerTest.java        # 4 tests
    ├── MrgValidateWorkerTest.java        # 4 tests
    └── MrgVersionWorkerTest.java        # 4 tests

```
