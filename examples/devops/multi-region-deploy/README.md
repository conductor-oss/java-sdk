# Multi-Region Deploy in Java with Conductor :  Deploy Primary, Verify, Deploy Secondary, Verify Global

Automates multi-region deployments using [Conductor](https://github.com/conductor-oss/conductor). This workflow deploys a service to the primary region first, verifies it is healthy, then rolls out to secondary regions, and runs a global verification to confirm all regions are serving traffic correctly.

## Deploying Across Regions Safely

You run your payment service in us-east-1, eu-west-1, and ap-southeast-1. A new version needs to go out, but deploying to all three regions simultaneously is risky. If the release has a bug, all regions go down at once. The safe approach: deploy to the primary region first, verify it is healthy, then fan out to the secondary regions, and finally run a global check to confirm every region is serving traffic. If the primary deployment fails health checks, the secondary regions never get the bad version.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the regional deploy and verification logic. Conductor handles primary-first sequencing, health gates before secondary rollout, and cross-region consistency checks.**

`DeployPrimaryWorker` deploys the new version to the primary region with canary or blue-green strategy. `VerifyPrimaryWorker` monitors the primary region for a stabilization period .  checking error rates, latency, and business metrics against baselines. `DeploySecondaryWorker` deploys to all remaining regions once the primary is verified healthy. `VerifyGlobalWorker` confirms cross-region consistency ,  same version running everywhere, inter-region traffic routing correctly, and global health checks passing. Conductor sequences these steps, halting secondary deployment if primary verification fails.

### What You Write: Workers

Four workers manage the multi-region rollout. Deploying to the primary region first, verifying health, then fanning out to secondary regions, and running a global check.

| Worker | Task | What It Does |
|---|---|---|
| **DeployPrimaryWorker** | `mrd_deploy_primary` | Deploys service to the primary region. |
| **DeploySecondaryWorker** | `mrd_deploy_secondary` | Deploys service to secondary regions. |
| **VerifyGlobalWorker** | `mrd_verify_global` | Verifies all regions are healthy and serving traffic. |
| **VerifyPrimaryWorker** | `mrd_verify_primary` | Verifies health of the primary region deployment. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
mrd_deploy_primary
    │
    ▼
mrd_verify_primary
    │
    ▼
mrd_deploy_secondary
    │
    ▼
mrd_verify_global

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
java -jar target/multi-region-deploy-1.0.0.jar

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
java -jar target/multi-region-deploy-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow multi_region_deploy_workflow \
  --version 1 \
  --input '{"service": "order-service", "version": "1.0", "regions": "us-east-1"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w multi_region_deploy_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one deployment phase .  replace the simulated calls with AWS CodeDeploy, ArgoCD multi-cluster sync, or Kubernetes rolling updates per region, and the multi-region workflow runs unchanged.

- **DeployPrimaryWorker** (`mrd_deploy_primary`): deploy via AWS CodeDeploy with blue-green or canary, ArgoCD sync to the primary cluster, or Kubernetes rolling update with deployment strategy
- **VerifyPrimaryWorker** (`mrd_verify_primary`): compare error rates and latency against pre-deployment baselines using Datadog, New Relic, or CloudWatch anomaly detection
- **DeploySecondaryWorker** (`mrd_deploy_secondary`): deploy to secondary regions (us-west-2, eu-west-1, etc.) using the same artifact verified in the primary region, with region-specific configuration
- **VerifyGlobalWorker** (`mrd_verify_global`): check version consistency across regions via health endpoints, verify database replication lag, and test cross-region failover readiness

Wire in your cloud provider's deployment APIs; the region-by-region rollout uses the same deploy-and-verify contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
multi-region-deploy/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multiregiondeploy/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultiRegionDeployExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeployPrimaryWorker.java
│       ├── DeploySecondaryWorker.java
│       ├── VerifyGlobalWorker.java
│       └── VerifyPrimaryWorker.java
└── src/test/java/multiregiondeploy/workers/
    ├── DeployPrimaryWorkerTest.java        # 8 tests
    ├── DeploySecondaryWorkerTest.java        # 8 tests
    ├── VerifyGlobalWorkerTest.java        # 7 tests
    └── VerifyPrimaryWorkerTest.java        # 7 tests

```
