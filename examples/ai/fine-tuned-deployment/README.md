# Fine-Tuned Model Deployment in Java Using Conductor :  Validate, Stage, Test, Promote

A Java Conductor workflow that takes a fine-tuned model from training output to production serving. validating model artifacts (weights, config, tokenizer), deploying to a staging endpoint, running acceptance tests against the staged model, and promoting to production only if tests pass. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the deployment pipeline as independent workers,  you write the validation, deployment, and testing logic, Conductor handles conditional promotion, failure notifications, retries, and observability.

## Shipping a Fine-Tuned Model Safely

After fine-tuning a model, you can't just push it to production. The model artifacts need validation. are the weights complete, does the config match the base model, is the tokenizer present? Then the model needs to be deployed to a staging environment where acceptance tests verify that it produces sensible outputs and meets latency requirements. Only if all tests pass should the model be promoted to the production endpoint. If tests fail, the team needs to be notified with details about which tests broke.

This creates a conditional pipeline: validate, stage, test, then branch. promote on success, notify on failure. If the staging deployment times out, you need to retry it without re-running validation. If the test runner fails mid-suite, you need to know which tests passed before the failure. And you need a complete audit trail showing exactly which model version was promoted to production and when.

Without orchestration, this becomes a brittle deployment script where a staging timeout means starting over, test failures are discovered only by reading logs, and there's no record of which model version is serving traffic.

## The Solution

**You write the model validation, staging deployment, and acceptance testing logic. Conductor handles the conditional promotion, failure notifications, and observability.**

Each stage of the deployment pipeline is an independent worker. model validation, staging deployment, acceptance testing, production promotion, team notification. Conductor's `SWITCH` task routes to promotion or failure notification based on test results. If staging deployment times out, Conductor retries it automatically. Every deployment is tracked end-to-end, so you can see exactly when model `ft-gpt4-v3` was validated, staged, tested, and promoted.

### What You Write: Workers

Four workers plus a test runner cover the deployment lifecycle. model validation, staging deployment, test execution with a SWITCH gate, production promotion on pass, and team notification on failure.

| Worker | Task | What It Does |
|---|---|---|
| **DeployStagingWorker** | `ftd_deploy_staging` | Deploys the validated model to a staging endpoint. |
| **NotifyWorker** | `ftd_notify` | Sends notifications about pipeline status (completion or failure). |
| **PromoteProductionWorker** | `ftd_promote_production` | Promotes the model from staging to production. |
| **ValidateModelWorker** | `ftd_validate_model` | Validates fine-tuned model artifacts (weights, config, tokenizer). |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
ftd_validate_model
    │
    ▼
ftd_deploy_staging
    │
    ▼
ftd_run_tests
    │
    ▼
SWITCH (route_ref)
    ├── true: ftd_promote_production
    └── default: ftd_notify
    │
    ▼
ftd_notify

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
java -jar target/fine-tuned-deployment-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for live test execution (optional. falls back to simulated) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/fine-tuned-deployment-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow fine_tuned_deploy_wf \
  --version 1 \
  --input '{"modelId": "TEST-001", "modelVersion": "gpt-4o-mini", "baseModel": "gpt-4o-mini"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w fine_tuned_deploy_wf -s COMPLETED -c 5

```

## How to Extend

Each worker handles one deployment lifecycle step. swap in MLflow for model validation, SageMaker or vLLM for staging deployment, and Slack for notifications, and the promote-or-notify pipeline runs unchanged.

- **ValidateModelWorker** (`ftd_validate_model`): integrate with your model registry (MLflow, Weights & Biases) to validate artifact checksums and metadata
- **DeployStagingWorker** (`ftd_deploy_staging`): swap in real deployment calls to SageMaker, Vertex AI, or a Kubernetes inference server (vLLM, TGI)
- **RunTestsWorker** (`ftd_run_tests`): replace with real acceptance test suites that call the staged endpoint with golden test cases
- **PromoteProductionWorker** (`ftd_promote_production`): swap in blue-green or canary deployment via your serving infrastructure
- **NotifyWorker** (`ftd_notify`): integrate Slack, PagerDuty, or email notifications for deployment status updates

The deploy/test/promote contract stays fixed. swap in real model registries, staging environments, or notification channels without changing the deployment workflow.

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
fine-tuned-deployment/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/finetuneddeployment/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FineTunedDeploymentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeployStagingWorker.java
│       ├── NotifyWorker.java
│       ├── PromoteProductionWorker.java
│       └── ValidateModelWorker.java
└── src/test/java/finetuneddeployment/workers/
    ├── DeployStagingWorkerTest.java        # 3 tests
    ├── NotifyWorkerTest.java        # 3 tests
    ├── PromoteProductionWorkerTest.java        # 3 tests
    ├── RunTestsWorkerTest.java        # 4 tests
    └── ValidateModelWorkerTest.java        # 3 tests

```
