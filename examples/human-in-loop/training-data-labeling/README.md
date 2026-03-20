# Training Data Labeling in Java Using Conductor :  Batch Preparation, Parallel Annotator WAIT Tasks via FORK_JOIN, Inter-Annotator Agreement Computation, and Label Storage

A Java Conductor workflow example for ML training data quality .  preparing a labeling batch, using FORK_JOIN to run two parallel WAIT tasks so independent annotators label the same items simultaneously, computing inter-annotator agreement (matching vs: differing labels, agreement percentage) after both annotators complete, and storing the final labels with quality metrics. The FORK_JOIN ensures neither annotator sees the other's labels, and the agreement computation compares label arrays element-by-element to produce agreements, disagreements, total, and agreementPct. Uses [Conductor](https://github.## Training Data Labeling Needs Parallel Annotators and Agreement Computation

High-quality training data requires multiple annotators to label the same items independently (via parallel WAIT tasks), then compute inter-annotator agreement to measure label reliability. The workflow prepares a batch, two annotators label it in parallel, agreement is computed (e.g., Cohen's kappa), and the final labels are stored. If the agreement computation fails, you need to retry it without asking the annotators to re-label.

## The Solution

**You just write the batch-preparation, agreement-computation, and label-storage workers. Conductor handles the parallel annotator waits and the join.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging .  your code handles the decision logic.

### What You Write: Workers

PrepareBatchWorker identifies items to label, ComputeAgreementWorker compares annotator labels element-by-element, and StoreLabelsWorker persists the final training data, the parallel annotator WAIT tasks are orchestrated via FORK_JOIN.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareBatchWorker** | `tdl_prepare_batch` | Prepares a batch of unlabeled data items for annotation .  identifies the items to label and signals the batch is ready for annotators |
| *FORK_JOIN + WAIT* | `annotator1_wait` / `annotator2_wait` | Two parallel WAIT tasks .  each annotator independently labels the batch items and submits their labels array via `POST /tasks/{taskId}`; the JOIN waits for both to complete before proceeding | Built-in Conductor FORK_JOIN + WAIT ,  no worker needed |
| **ComputeAgreementWorker** | `tdl_compute_agreement` | Compares the two annotators' label arrays element-by-element .  counts agreements, disagreements, total items, and computes agreementPct as a quality metric for the labeled batch |
| **StoreLabelsWorker** | `tdl_store_labels` | Stores the final labeled data with both annotators' labels and the agreement percentage .  persists the training data for model consumption |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments .  the workflow structure stays the same.

### The Workflow

```
tdl_prepare_batch
    │
    ▼
FORK_JOIN
    ├── annotator1_wait
    └── annotator2_wait
    │
    ▼
JOIN (wait for all branches)
tdl_compute_agreement
    │
    ▼
tdl_store_labels
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
java -jar target/training-data-labeling-1.0.0.jar
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
java -jar target/training-data-labeling-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow training_data_labeling \
  --version 1 \
  --input '{"batchId": "TEST-001"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w training_data_labeling -s COMPLETED -c 5
```

## How to Extend

Each worker handles one stage of the labeling pipeline .  connect your annotation platform (Label Studio, Labelbox, Scale AI) for batch management and your ML data store for label persistence, and the dual-annotator workflow stays the same.

- **ComputeAgreementWorker** (`tdl_compute_agreement`): compute real inter-annotator agreement metrics like Cohen's kappa, Fleiss' kappa, or Krippendorff's alpha
- **PrepareBatchWorker** (`tdl_prepare_batch`): pull unlabeled data from a training data platform like Labelbox, Scale AI, or a custom database, and prepare annotation guidelines
- **StoreLabelsWorker** (`tdl_store_labels`): write final labels to a training data platform, trigger model retraining pipelines, or export to ML frameworks like Hugging Face Datasets

Integrate Labelbox or Scale AI for real annotation and the parallel labeling, agreement computation, and storage pipeline stays intact.

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
training-data-labeling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/trainingdatalabeling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TrainingDataLabelingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ComputeAgreementWorker.java
│       ├── PrepareBatchWorker.java
│       └── StoreLabelsWorker.java
└── src/test/java/trainingdatalabeling/workers/
    ├── ComputeAgreementWorkerTest.java        # 9 tests
    ├── PrepareBatchWorkerTest.java        # 3 tests
    └── StoreLabelsWorkerTest.java        # 3 tests
```
