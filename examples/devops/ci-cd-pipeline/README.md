# CI/CD Pipeline in Java with Conductor -- Build, Parallel Tests, Deploy Staging, Deploy Production

Someone pushed to main. Seven CI jobs kicked off in three different systems. The unit tests passed, but the integration test job silently timed out and reported "success" because the exit code wasn't checked. The security scan found a critical vulnerability in a transitive dependency, but it posted to a Slack channel nobody monitors. Meanwhile, the deploy job grabbed a stale artifact from the previous run because two builds wrote to the same S3 path. Production is now running code that failed integration tests, with a known CVE, and the deploy log says "all green." This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full CI/CD pipeline -- build, parallel tests, staging deploy, production promotion -- as a single traceable execution where every failure halts the line.

## From Git Commit to Production in One Pipeline

A developer pushes a commit to the main branch. The CI/CD pipeline must build the project from that commit SHA, run unit tests and integration tests in parallel (cutting test time in half), deploy to staging if tests pass, run smoke tests against staging, and deploy to production if staging is healthy. Each step depends on the previous one's success, and any failure should halt the pipeline with clear reporting.

The pipeline must be idempotent: re-running it with the same commit SHA should produce the same build. Tests should run in parallel to minimize pipeline time. The staging deployment must be verified before production deployment begins. And every pipeline run needs a complete audit trail -- what was built, what tests ran, what was deployed, and when.

## The Solution

**You write the build and deploy logic. Conductor handles parallel test execution, staging-before-production gating, and full pipeline traceability.**

`BuildWorker` checks out the specified commit, compiles the code, and produces build artifacts. `FORK_JOIN` dispatches unit tests and integration tests to run in parallel. After `JOIN` collects both results, `DeployStagingWorker` deploys the build to the staging environment and runs smoke tests. `DeployProdWorker` promotes the verified build to production. Conductor runs tests in parallel, tracks the full pipeline execution, and records build-to-deploy traceability.

### What You Write: Workers

Four workers run the CI/CD pipeline -- building from a commit, running tests in parallel, deploying to staging, and promoting to production.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **Build** | `cicd_build` | Builds the application from the specified repo, branch, and commit. | Simulated |
| **DeployProd** | `cicd_deploy_prod` | Deploys the build to production environment. | Simulated |
| **DeployStaging** | `cicd_deploy_staging` | Deploys the build to staging environment. | Simulated |
| **SecurityScan** | `cicd_security_scan` | Runs security scan for the build. | Simulated |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls -- the workflow and rollback logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically -- configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status -- no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Parallel execution** | FORK_JOIN runs multiple tasks simultaneously and waits for all to complete |

### The Workflow

```
cicd_build
    │
    ▼
FORK_JOIN
    ├── cicd_unit_test
    ├── cicd_integration_test
    └── cicd_security_scan
    │
    ▼
JOIN (wait for all branches)
cicd_deploy_staging
    │
    ▼
cicd_deploy_prod
```

## Example Output

```
=== CI/CD Pipeline Demo ===

Step 1: Registering task definitions...
  Registered: cicd_build, cicd_deploy_prod, cicd_deploy_staging, cicd_integration_test, cicd_security_scan, cicd_unit_test

Step 2: Registering workflow 'cicd_pipeline_workflow'...
  Workflow registered.

Step 3: Starting workers...
  6 workers polling.

Step 4: Starting workflow...
  Workflow ID: 5f3cd542-8bcc-6e5f-04f3-90d44546cb8f

[cicd_build] Building main@...
[cicd_unit_test] 342 tests passed
[cicd_integration_test] 28 tests passed
[cicd_security_scan] No vulnerabilities found
[cicd_deploy_staging] Deployed app:1.2.3 to staging
[cicd_deploy_prod] Deployed app:1.2.3 to production

  Status: COMPLETED
  Output: {buildId=BLD-100001, deployed=true}

Result: PASSED
```
## Running It

### Prerequisites

- **Java 21+** -- verify with `java -version`
- **Maven 3.8+** -- verify with `mvn -version`
- **Docker** -- to run Conductor

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/ci-cd-pipeline-1.0.0.jar
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
java -jar target/ci-cd-pipeline-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cicd_pipeline_workflow \
  --version 1 \
  --input '{"repoUrl": "github.com/acme/api", "branch": "main", "commitSha": "abc1234def5678"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cicd_pipeline_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker owns one pipeline stage -- replace the simulated build and deploy calls with Jenkins, GitHub Actions, or ArgoCD, and the CI/CD pipeline runs unchanged.

- **Build** (`cicd_build`) -- integrate with Maven/Gradle for Java builds, Docker for container image building, and push artifacts to ECR/Docker Hub/Artifactory
- **SecurityScan** (`cicd_security_scan`) -- run Snyk, Trivy, or OWASP Dependency-Check for vulnerability scanning, with configurable severity thresholds to gate deployment
- **DeployStaging** (`cicd_deploy_staging`) -- deploy to a staging environment via Helm upgrade, Kubernetes apply, or AWS ECS service update for pre-production validation
- **DeployProd** (`cicd_deploy_prod`) -- deploy via ArgoCD GitOps sync, AWS CodeDeploy, or Kubernetes rolling update with canary analysis before full rollout

Wire in your real build system and deployment targets; the CI/CD pipeline preserves the same stage-gate contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
ci-cd-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/cicdpipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CiCdPipelineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── Build.java
│       ├── DeployProd.java
│       ├── DeployStaging.java
│       └── SecurityScan.java
└── src/test/java/cicdpipeline/workers/
    ├── BuildTest.java        # 7 tests
    ├── DeployProdTest.java        # 7 tests
    ├── SecurityScanTest.java        # 7 tests
    └── UnitTestTest.java        # 7 tests
```
