# Deployment Rollback in Java with Conductor :  Detect Failure, Identify Version, Rollback, Verify

Automates deployment rollback using [Conductor](https://github.com/conductor-oss/conductor). This workflow detects a failing deployment (error rate spikes, health check failures), identifies the last known stable version, rolls back the deployment to that version, and verifies the service is healthy again.

## When a Deploy Goes Wrong

Your checkout-service just deployed version 2.5.0 and the error rate is spiking. Customers are seeing 500 errors. You need to act fast: confirm the deployment is actually failing (not just a transient blip), find the last stable version (2.4.3, deployed 3 days ago), roll back to it, and verify the error rate normalizes. Every minute of delay means lost revenue and frustrated users.

Without orchestration, someone notices the error rate in Grafana, runs `kubectl rollout undo` by hand, forgets which revision to target, picks the wrong one, and the service is now running a version from two weeks ago with a known bug. If the rollback itself fails halfway: pods stuck in CrashLoopBackOff, there's no automated verification, no structured record of what was rolled back or why, and no confidence that the service is actually healthy again.

## The Solution

**You write the failure detection and rollback logic. Conductor handles the detect-identify-rollback-verify sequence and records the complete rollback timeline.**

`DetectFailureWorker` analyzes deployment health signals. error rates, latency percentiles, health check status,  to confirm the deployment is failing and quantify the impact. `IdentifyVersionWorker` looks up the deployment history to find the last known-good version and its configuration. `RollbackDeployWorker` executes the rollback,  redeploying the previous version with its original configuration. `VerifyRollbackWorker` monitors health signals post-rollback to confirm the service has recovered,  checking error rates, latency, and health check status. Conductor sequences these steps and records the complete rollback timeline for incident review.

### What You Write: Workers

Four workers execute the rollback. Detecting deployment failures, identifying the last stable version, rolling back, and verifying recovery.

| Worker | Task | What It Does |
|---|---|---|
| **DetectFailureWorker** | `rb_detect_failure` | Detects deployment failures by checking error rate spikes, health check status, and anomaly signals |
| **IdentifyVersionWorker** | `rb_identify_version` | Looks up the last known stable version and its deployment timestamp from release history |
| **RollbackDeployWorker** | `rb_rollback_deploy` | Rolls back the deployment to the identified stable version |
| **VerifyRollbackWorker** | `rb_verify_rollback` | Verifies the service is healthy after rollback by checking error rates and health endpoints |

Workers implement infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls. the workflow and rollback logic stay the same.

### The Workflow

```
rb_detect_failure
    │
    ▼
rb_identify_version
    │
    ▼
rb_rollback_deploy
    │
    ▼
rb_verify_rollback

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
java -jar target/deployment-rollback-1.0.0.jar

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
java -jar target/deployment-rollback-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow deployment_rollback_workflow \
  --version 1 \
  --input '{"service": "order-service", "reason": "sample-reason"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w deployment_rollback_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one rollback step. replace the simulated calls with Datadog anomaly detection, Kubernetes rollout undo, or ArgoCD sync for real failure detection and recovery, and the rollback workflow runs unchanged.

- **DetectFailureWorker** (`rb_detect_failure`): query Datadog, New Relic, or Prometheus for error rate spikes correlated with deployment timestamps, with automatic threshold-based detection
- **IdentifyVersionWorker** (`rb_identify_version`): look up the last successful deployment in Argo CD, Spinnaker, or a deploy history database, returning the stable version tag and Git revision hash
- **RollbackDeployWorker** (`rb_rollback_deploy`): trigger a Kubernetes rollout undo, Helm rollback to a specific revision, AWS CodeDeploy rollback, or ArgoCD sync to the previous known-good commit
- **VerifyRollbackWorker** (`rb_verify_rollback`): poll health endpoints and error rate metrics for a configurable stabilization period (e.g., 5 minutes of clean health checks and error rate below 0.1%) before declaring success

Connect to your deployment platform and health monitoring; the rollback workflow runs with the same input/output contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
deployment-rollback-deployment-rollback/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/deploymentrollback/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DetectFailureWorker.java
│       ├── IdentifyVersionWorker.java
│       ├── RollbackDeployWorker.java
│       └── VerifyRollbackWorker.java
└── src/test/java/deploymentrollback/
    └── MainExampleTest.java        # 2 tests. workflow resource loading, worker instantiation

```
