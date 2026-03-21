# ML Experiment Tracking in Java Using Conductor :  Define, Run, Log Metrics, Compare, Decide

A Java Conductor workflow example for ML experiment tracking .  defining an experiment with a hypothesis, running the training job, logging metrics, comparing the result against a baseline, and making a go/no-go decision based on statistical significance. Uses [Conductor](https://github.

## Experiments Without Reproducibility Are Guesswork

You tuned a hyperparameter, retrained the model, and accuracy went up 2%. Was it the hyperparameter change, or did the training data shift? Without a structured experiment record .  hypothesis, configuration, metrics, baseline comparison ,  you can't answer that question. Teams run dozens of experiments a week, and without systematic tracking, knowledge about what was tried and what worked lives in Slack threads and Jupyter notebooks that nobody can find six months later.

Experiment tracking means defining the experiment upfront (name, hypothesis, baseline metric), running it with recorded configuration, logging the resulting metrics, comparing against the baseline with statistical significance testing, and making a recorded decision to adopt or reject the change. Each step produces data the next step needs .  you can't compare without metrics, and you can't decide without the comparison.

## The Solution

**You write the training and comparison logic. Conductor handles experiment sequencing, retries, and reproducibility tracking.**

`ExtDefineExperimentWorker` creates the experiment record with an ID and configuration based on the hypothesis. `ExtRunExperimentWorker` executes the training job and produces metrics (accuracy, loss, latency). `ExtLogMetricsWorker` persists those metrics to the experiment tracking store. `ExtCompareWorker` compares the primary metric against the baseline and determines whether the improvement is statistically significant. `ExtDecideWorker` makes the final adopt/reject decision based on the improvement percentage and significance. Conductor records the full chain .  from hypothesis to decision ,  so every experiment is reproducible and auditable.

### What You Write: Workers

Five workers cover the experiment lifecycle: definition, training execution, metrics logging, baseline comparison, and adoption decision, each capturing one piece of the reproducibility chain.

| Worker | Task | What It Does |
|---|---|---|
| **ExtCompareWorker** | `ext_compare` | Computes improvement percentage vs. baseline and determines statistical significance |
| **ExtDecideWorker** | `ext_decide` | Makes a promote/reject/rerun decision based on whether results are statistically significant |
| **ExtDefineExperimentWorker** | `ext_define_experiment` | Exts Define Experiment and computes experiment id, config |
| **ExtLogMetricsWorker** | `ext_log_metrics` | Records experiment metrics to a tracking server and returns the run URL |
| **ExtRunExperimentWorker** | `ext_run_experiment` | Executes the training run and produces accuracy, precision, and recall metrics |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### The Workflow

```
ext_define_experiment
    │
    ▼
ext_run_experiment
    │
    ▼
ext_log_metrics
    │
    ▼
ext_compare
    │
    ▼
ext_decide

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
java -jar target/experiment-tracking-1.0.0.jar

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
java -jar target/experiment-tracking-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow experiment_tracking_demo \
  --version 1 \
  --input '{"experimentName": "test", "hypothesis": "sample-hypothesis", "baselineMetric": "sample-baselineMetric"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w experiment_tracking_demo -s COMPLETED -c 5

```

## How to Extend

Each worker handles one experiment lifecycle step .  replace the simulated training runs with real MLflow or Weights & Biases APIs and the define-run-compare-decide pipeline runs unchanged.

- **ExtRunExperimentWorker** (`ext_run_experiment`): trigger a real training job via SageMaker `createTrainingJob()`, Weights & Biases run, or MLflow `mlflow.start_run()` and collect actual metrics
- **ExtLogMetricsWorker** (`ext_log_metrics`): log metrics to MLflow Tracking (`mlflow.log_metrics`), Weights & Biases (`wandb.log`), or a custom metrics database
- **ExtCompareWorker** (`ext_compare`): run real statistical tests (t-test, bootstrap confidence intervals) using Apache Commons Math or a Python subprocess with scipy.stats

The experiment record contract stays fixed. Swap the simulated training for a real MLflow or W&B integration and the compare-decide pipeline runs unchanged.

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
experiment-tracking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/experimenttracking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ExperimentTrackingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExtCompareWorker.java
│       ├── ExtDecideWorker.java
│       ├── ExtDefineExperimentWorker.java
│       ├── ExtLogMetricsWorker.java
│       └── ExtRunExperimentWorker.java
└── src/test/java/experimenttracking/workers/
    ├── ExtCompareWorkerTest.java        # 4 tests
    ├── ExtDecideWorkerTest.java        # 4 tests
    ├── ExtDefineExperimentWorkerTest.java        # 4 tests
    ├── ExtLogMetricsWorkerTest.java        # 4 tests
    └── ExtRunExperimentWorkerTest.java        # 4 tests

```
