# Pipeline Versioning in Java Using Conductor :  Snapshot Config, Tag, Test, Promote

A Java Conductor workflow example for pipeline versioning. snapshotting the current pipeline configuration, tagging it with a version, running integration tests against the tagged version, and promoting it to the target environment. Uses [Conductor](https://github.

## Pipeline Configs Change, and You Need to Know What Ran When

Your ETL pipeline's configuration. source tables, transformation rules, destination schemas,  changes frequently. A config change on Tuesday breaks the Wednesday morning run, and nobody can tell which configuration was active when. Rolling back means guessing which version of the config file was deployed, or restoring from a backup that may include unrelated changes.

Pipeline versioning means taking a snapshot of the full configuration before each change, tagging it with a version (like `v2.1-staging`), running tests against the tagged version to verify it works, and promoting it to production only if tests pass. Each step produces outputs the next step needs. the snapshot ID feeds into the tag, the tag feeds into the test run, and test results gate the promotion.

## The Solution

**You write the snapshot and promotion logic. Conductor handles version gating, test orchestration, and deployment tracking.**

`PvrSnapshotConfigWorker` captures the current pipeline configuration as a frozen snapshot. `PvrTagVersionWorker` assigns a version tag (e.g., `v2.1`) to the snapshot. `PvrTestPipelineWorker` runs the pipeline against test data using the tagged configuration to verify correctness. `PvrPromoteWorker` deploys the tested, tagged version to the target environment. Conductor records the snapshot, tag, test results, and promotion status for every version. giving you a complete audit trail of what configuration was running in each environment at any point in time.

### What You Write: Workers

Three workers manage the version lifecycle: configuration snapshotting, version tagging, and environment promotion, each gating the next stage to prevent untested configs from reaching production.

| Worker | Task | What It Does |
|---|---|---|
| **PvrPromoteWorker** | `pvr_promote` | Promotes the pipeline version to the target environment (e.g., production) if tests passed |
| **PvrSnapshotConfigWorker** | `pvr_snapshot_config` | Captures a snapshot of the current pipeline configuration (steps, parallelism) with a config hash |
| **PvrTagVersionWorker** | `pvr_tag_version` | Applies a version tag to the pipeline snapshot for future reference and rollback |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
pvr_snapshot_config
    │
    ▼
pvr_tag_version
    │
    ▼
pvr_test_pipeline
    │
    ▼
pvr_promote

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
java -jar target/pipeline-versioning-1.0.0.jar

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
java -jar target/pipeline-versioning-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow pipeline_versioning_demo \
  --version 1 \
  --input '{"pipelineName": "test", "versionTag": "1.0", "environment": "staging"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w pipeline_versioning_demo -s COMPLETED -c 5

```

## How to Extend

Each worker manages one versioning gate. replace the simulated config snapshots with real Git commits or AppConfig APIs and the tag-test-promote pipeline runs unchanged.

- **PvrSnapshotConfigWorker** (`pvr_snapshot_config`): store pipeline configs in Git (commit + SHA), DynamoDB, or a config management service like AWS AppConfig or HashiCorp Consul
- **PvrTestPipelineWorker** (`pvr_test_pipeline`): run real integration tests: execute the pipeline against a test dataset, compare outputs to golden files, and check data quality assertions
- **PvrPromoteWorker** (`pvr_promote`): deploy the tagged config to production via Terraform apply, Kubernetes ConfigMap update, or Airflow variable refresh

The snapshot and tag output contract stays fixed. Swap the simulated config store for a real Git-backed config repo or AWS Parameter Store and the version-test-promote pipeline runs unchanged.

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
pipeline-versioning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/pipelineversioning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PipelineVersioningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PvrPromoteWorker.java
│       ├── PvrSnapshotConfigWorker.java
│       └── PvrTagVersionWorker.java
└── src/test/java/pipelineversioning/workers/
    ├── PvrPromoteWorkerTest.java        # 4 tests
    ├── PvrSnapshotConfigWorkerTest.java        # 4 tests
    ├── PvrTagVersionWorkerTest.java        # 4 tests
    └── PvrTestPipelineWorkerTest.java        # 4 tests

```
