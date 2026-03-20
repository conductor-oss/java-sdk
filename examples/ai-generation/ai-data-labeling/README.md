# AI Data Labeling in Java Using Conductor :  Prepare, Parallel Labelers, Reconcile Disagreements, Export

A Java Conductor workflow that orchestrates data labeling at scale .  preparing the dataset, dispatching multiple labelers to annotate the same data in parallel via `FORK_JOIN` for quality through redundancy, reconciling disagreements between labelers, and exporting the labeled dataset. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate data preparation, parallel labeling, reconciliation, and export as independent workers ,  you write the labeling logic, Conductor handles parallelism, retries, durability, and observability.

## Quality Labels Need Multiple Annotators and Reconciliation

Training a machine learning model on poorly labeled data produces a poor model. Quality labeling uses redundancy: multiple labelers annotate the same data independently, and disagreements are resolved through reconciliation (majority vote, expert review, or confidence-weighted consensus). This catches labeler mistakes and ambiguous cases.

Parallel labeling means multiple annotators work simultaneously .  cutting labeling time proportionally. But all labelers must finish before reconciliation can begin. If one labeler times out, the others' work is still valid. After reconciliation, the final labeled dataset needs export in the format required by your training pipeline (COCO JSON, Pascal VOC XML, CSV with labels).

## The Solution

**You just write the data preparation, annotation, disagreement reconciliation, and labeled dataset export logic. Conductor handles parallel labeler coordination, disagreement routing, and progress tracking across annotation tasks.**

`PrepareDataWorker` loads and preprocesses the dataset .  sampling, shuffling, and formatting items for annotation. `FORK_JOIN` dispatches multiple labelers to annotate the same items independently in parallel ,  each labeler returns labels, confidence scores, and annotation time. After `JOIN` collects all annotations, `ReconcileWorker` resolves disagreements using majority vote or confidence-weighted consensus, flagging items where labelers strongly disagreed for expert review. `ExportWorker` formats the reconciled labels into the target format and exports the labeled dataset. Conductor tracks per-labeler accuracy and agreement rates.

### What You Write: Workers

Labeling workers run in parallel with independent annotation tasks, while a reconciliation worker resolves disagreements downstream.

| Worker | Task | What It Does |
|---|---|---|
| **ExportWorker** | `adl_export` | Exports reconciled labeled samples in COCO format to the output path for model training |
| **Labeler1Worker** | `adl_labeler_1` | Labeled 500 samples .  classification labels applied |
| **Labeler2Worker** | `adl_labeler_2` | Labeled 500 samples .  classification labels applied |
| **PrepareDataWorker** | `adl_prepare_data` | Prepares and preprocesses the dataset samples for labeling |
| **ReconcileWorker** | `adl_reconcile` | Inter-annotator agreement: 94% .  conflicts resolved |

Workers simulate AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode .  the generation workflow stays the same.

### The Workflow

```
adl_prepare_data
    │
    ▼
FORK_JOIN
    ├── adl_labeler_1
    └── adl_labeler_2
    │
    ▼
JOIN (wait for all branches)
adl_reconcile
    │
    ▼
adl_export
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
java -jar target/ai-data-labeling-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for live AI labeling (optional .  falls back to simulated) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/ai-data-labeling-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow adl_data_labeling \
  --version 1 \
  --input '{"datasetId": "TEST-001", "labelType": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w adl_data_labeling -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real labeling tools. Label Studio or Labelbox for human annotation, GPT-4 or Claude as AI labelers, your ML training pipeline for COCO/HuggingFace export, and the workflow runs identically in production.

- **Labeler workers**: use GPT-4 or Claude as AI labelers with task-specific prompts, or integrate with Label Studio/Labelbox APIs for human-in-the-loop annotation workflows
- **ReconcileWorker** (`adl_reconcile`): implement inter-annotator agreement metrics (Cohen's kappa, Fleiss' kappa), with automatic escalation to expert reviewers for low-agreement items
- **ExportWorker** (`adl_export`): export to standard ML formats: COCO JSON for object detection, HuggingFace Datasets for NLP, or CSV/Parquet for tabular classification

Switch annotation tools or reconciliation algorithms without altering the pipeline structure.

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
ai-data-labeling-ai-data-labeling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/aidatalabeling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AiDataLabelingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExportWorker.java
│       ├── Labeler1Worker.java
│       ├── Labeler2Worker.java
│       ├── PrepareDataWorker.java
│       └── ReconcileWorker.java
└── src/test/java/aidatalabeling/workers/
    ├── ExportWorkerTest.java        # 1 tests
    └── ReconcileWorkerTest.java        # 1 tests
```
