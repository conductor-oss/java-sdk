# Quality Gate in Java Using Conductor :  Automated Test Suite, SWITCH on Pass/Fail, QA Engineer WAIT Sign-Off, and Production Deployment

A Java Conductor workflow example for deployment quality gates. running an automated test suite (42 tests), using a SWITCH to block deployment immediately if any test fails (TERMINATE with reason), or advancing to a QA engineer WAIT task for manual sign-off before deploying to production. The SWITCH ensures failed builds never reach a human reviewer, and the WAIT ensures passing builds still require human judgment before going live. If the deployment step fails after QA sign-off, Conductor retries the deploy without requiring the QA engineer to re-approve. Uses [Conductor](https://github.

## Deployments Need Automated Tests and Human QA Sign-Off

Before deploying to production, code must pass automated tests. If tests pass, a QA engineer must sign off via a WAIT task. If tests fail, deployment is blocked. The SWITCH task routes between the QA approval path and the failure path based on test results. This ensures no code ships without both automated and human verification.

## The Solution

**You just write the test-runner and deployment workers. Conductor handles the pass/fail routing, the QA sign-off hold, and the deploy retry.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

RunTestsWorker executes the test suite and reports pass/fail counts, and DeployWorker pushes to production, the SWITCH that blocks failed builds and the QA sign-off WAIT are handled by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **RunTestsWorker** | `qg_run_tests` | Runs the automated test suite. executes all tests and reports totalTests, passedTests, failedTests, and an allPassed flag that the SWITCH uses to gate deployment |
| *SWITCH* | `test_result_switch` | Checks `allPassed` from the test results: if false, terminates the workflow with "Automated tests failed. Deployment blocked."; if true (default), advances to QA sign-off and deployment | Built-in Conductor SWITCH + TERMINATE. no worker needed |
| *WAIT task* | `qa_signoff` | Pauses until a QA engineer reviews the test results and manually signs off on the release via `POST /tasks/{taskId}` | Built-in Conductor WAIT. no worker needed |
| **DeployWorker** | `qg_deploy` | Deploys the application to production after both automated tests pass and the QA engineer signs off |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
qg_run_tests
    │
    ▼
SWITCH (test_result_switch_ref)
    ├── false: tests_failed_terminate
    └── default: qa_signoff -> qg_deploy

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
java -jar target/quality-gate-1.0.0.jar

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
java -jar target/quality-gate-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow quality_gate_demo \
  --version 1 \
  --input '{"input": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w quality_gate_demo -s COMPLETED -c 5

```

## How to Extend

Each worker handles one step of the release pipeline. connect your CI system (Jenkins, GitHub Actions, CircleCI) for test execution and your deployment platform (AWS, Kubernetes) for rollout, and the quality-gate workflow stays the same.

- **RunTestsWorker** (`qg_run_tests`): invoke your CI/CD system's test API (Jenkins, GitHub Actions, GitLab CI) to run the real test suite, parse JUnit XML results, and return pass/fail counts
- **DeployWorker** (`qg_deploy`): trigger a real deployment via Kubernetes kubectl, AWS CodeDeploy, Terraform, or a CD pipeline like ArgoCD

Swap in your CI runner and deployment tool and the test-gate-signoff-deploy pipeline keeps functioning as configured.

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
quality-gate/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/qualitygate/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── QualityGateExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeployWorker.java
│       └── RunTestsWorker.java
└── src/test/java/qualitygate/workers/
    ├── DeployWorkerTest.java        # 5 tests
    └── RunTestsWorkerTest.java        # 6 tests

```
