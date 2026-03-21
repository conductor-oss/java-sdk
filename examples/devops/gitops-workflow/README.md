# GitOps Workflow in Java with Conductor :  Detect Drift, Plan Sync, Apply, Verify State

Automates GitOps reconciliation using [Conductor](https://github.com/conductor-oss/conductor). This workflow detects configuration drift between a Git repository and a target Kubernetes cluster, plans the synchronization actions needed, applies the changes to bring the cluster into the desired state, and verifies the cluster matches Git after sync.

## Git Is the Source of Truth

Someone ran `kubectl edit` on the production deployment and changed the replica count. Now the cluster does not match what is in Git. GitOps reconciliation detects this drift, plans the sync to restore the desired state, applies the changes, and verifies the cluster is back in alignment. Without this, manual changes accumulate silently until the next Git-based deploy overwrites them, or worse, they persist and nobody knows the cluster differs from what the repo says.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the drift detection and sync logic. Conductor handles the detect-plan-apply-verify reconciliation loop and records every sync operation.**

`DetectDriftWorker` compares the desired state in the Git repository against the live cluster state, identifying resources that have drifted (modified, missing, or extra). `PlanSyncWorker` generates a sync plan listing the specific operations needed .  create, update, or delete ,  with a preview of each change. `ApplySyncWorker` executes the sync plan, applying changes to bring the cluster back to the desired state. `VerifyStateWorker` confirms every resource in the cluster matches the Git-defined specification after sync. Conductor records every drift detection and sync operation for GitOps audit trails.

### What You Write: Workers

Four workers handle GitOps reconciliation. Detecting drift between Git and the cluster, planning the sync, applying changes, and verifying state alignment.

| Worker | Task | What It Does |
|---|---|---|
| **ApplySyncWorker** | `go_apply_sync` | Applies the synchronization plan to bring cluster into desired state. |
| **DetectDriftWorker** | `go_detect_drift` | Detects configuration drift between Git repository and target cluster. |
| **PlanSyncWorker** | `go_plan_sync` | Plans the synchronization actions needed to reconcile drift. |
| **VerifyStateWorker** | `go_verify_state` | Verifies that the cluster state matches the Git repository after sync. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
go_detect_drift
    │
    ▼
go_plan_sync
    │
    ▼
go_apply_sync
    │
    ▼
go_verify_state

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
java -jar target/gitops-workflow-1.0.0.jar

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
java -jar target/gitops-workflow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow gitops_workflow \
  --version 1 \
  --input '{"repository": "sample-repository", "targetCluster": "production"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w gitops_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one reconciliation step .  replace the simulated calls with ArgoCD sync APIs, Flux reconciliation, or kubectl diff for real Git-to-cluster state management, and the GitOps workflow runs unchanged.

- **DetectDriftWorker** (`go_detect_drift`): use `kubectl diff` or ArgoCD's drift detection API to compare live state against Git manifests, with structured output showing exactly what changed
- **PlanSyncWorker** (`go_plan_sync`): generate a sync plan showing which resources will be created, updated, or deleted, similar to `terraform plan` output, for review before applying
- **ApplySyncWorker** (`go_apply_sync`): trigger ArgoCD sync operations, Flux reconciliation, or direct `kubectl apply` with dry-run verification before actual apply
- **VerifyStateWorker** (`go_verify_state`): check resource readiness conditions, pod health, and service endpoints to confirm the sync produced a healthy cluster state

Wire in ArgoCD or Flux for real reconciliation; the drift-detection workflow preserves the same sync contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
gitops-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/gitopsworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GitopsWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplySyncWorker.java
│       ├── DetectDriftWorker.java
│       ├── PlanSyncWorker.java
│       └── VerifyStateWorker.java
└── src/test/java/gitopsworkflow/workers/
    ├── ApplySyncWorkerTest.java        # 7 tests
    ├── DetectDriftWorkerTest.java        # 8 tests
    ├── PlanSyncWorkerTest.java        # 7 tests
    └── VerifyStateWorkerTest.java        # 7 tests

```
