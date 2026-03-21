# Self-Correction Agent in Java Using Conductor :  Generate Code, Test, Diagnose, Fix

Self-Correction .  generates code, runs tests, and if tests fail diagnoses and fixes the code before delivering. Uses [Conductor](https://github.

## AI-Generated Code Needs Testing and Self-Repair

LLM-generated code works on the first try about 60-70% of the time. The remaining 30-40% has bugs .  off-by-one errors, missing edge cases, incorrect API usage. A self-correcting agent doesn't just generate code; it tests the code, and if tests fail, it diagnoses what went wrong and fixes it.

This creates a conditional pipeline: generate, test, then branch. On success, deliver the code. On failure, diagnose (analyze the test output, identify the root cause .  "Array index starts at 1 but should start at 0") and fix (generate corrected code targeting the specific issue). The fixed code then goes through delivery. Without orchestration, implementing this generate-test-fix loop with proper state management and failure routing requires careful branching logic.

## The Solution

**You write the code generation, testing, diagnosis, and fix logic. Conductor handles the pass/fail routing, conditional repair path, and delivery.**

`GenerateCodeWorker` produces code from the requirement specification. `RunTestsWorker` executes test cases against the generated code and returns pass/fail status with detailed test output. Conductor's `SWITCH` routes based on test results: passing tests go to `DeliverWorker` for direct delivery. Failing tests go through `DiagnoseWorker` (analyzing test failures to identify root causes) and `FixWorker` (generating corrected code targeting the specific issues) before reaching `DeliverWorker`. Conductor records the original code, test results, diagnosis, and fixes .  showing exactly what was wrong and how it was corrected.

### What You Write: Workers

Four workers implement self-correction. Generating code, running tests, and on failure diagnosing the issue and applying a fix before delivery.

| Worker | Task | What It Does |
|---|---|---|
| **DeliverWorker** | `sc_deliver` | Delivers the final code. Accepts the code, test pass count, and an optional wasFixed flag. |
| **DiagnoseWorker** | `sc_diagnose` | Diagnoses errors found during testing. Returns a deterministic diagnosis identifying the missing negative-number guard. |
| **FixWorker** | `sc_fix` | Fixes code based on a diagnosis. Returns deterministic fixed code with a negative input guard added. |
| **GenerateCodeWorker** | `sc_generate_code` | Generates code from a requirement description. Returns a deterministic fibonacci function implementation. |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

### The Workflow

```
sc_generate_code
    │
    ▼
sc_run_tests
    │
    ▼
SWITCH (test_result_switch_ref)
    ├── pass: sc_deliver
    ├── fail: sc_diagnose -> sc_fix -> sc_deliver

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
java -jar target/self-correction-1.0.0.jar

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
java -jar target/self-correction-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow self_correction \
  --version 1 \
  --input '{"requirement": "sample-requirement"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w self_correction -s COMPLETED -c 5

```

## How to Extend

Each worker handles one phase of the generate-test-fix cycle. Use an LLM for code generation, real test runners (JUnit, pytest, Jest) for validation, and LLM-based diagnosis for targeted fixes, and the conditional self-correction workflow runs unchanged.

- **RunTestsWorker** (`sc_run_tests`): execute real test suites: JUnit via Maven Surefire for Java, pytest for Python, or Jest for JavaScript, with actual pass/fail results and stack traces
- **DiagnoseWorker** (`sc_diagnose`): use an LLM with the failing test output and generated code as context to produce specific, actionable diagnoses (not just "test failed" but "the sort function doesn't handle empty arrays")
- **FixWorker** (`sc_fix`): use an LLM with the diagnosis and original code to generate targeted fixes, or implement a multi-attempt fix loop where the agent retries with different approaches if the first fix doesn't pass tests

Replace with real code generation and test execution; the generate-test-fix pipeline keeps the same pass/fail routing interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

A Java Conductor workflow that generates code, runs tests, and self-corrects on failure .  generating code from a requirement, running test cases, then routing via `SWITCH`: if tests pass, the code is delivered; if tests fail, the agent diagnoses the failures, fixes the code, and delivers the corrected version. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate code generation, testing, conditional diagnosis, and fixing as independent workers ,  you write the generation and testing logic, Conductor handles the pass/fail routing, retries, durability, and observability.

## Project Structure

```
self-correction/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/selfcorrection/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SelfCorrectionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeliverWorker.java
│       ├── DiagnoseWorker.java
│       ├── FixWorker.java
│       └── GenerateCodeWorker.java
└── src/test/java/selfcorrection/workers/
    ├── DeliverWorkerTest.java        # 8 tests
    ├── DiagnoseWorkerTest.java        # 8 tests
    ├── FixWorkerTest.java        # 8 tests
    ├── GenerateCodeWorkerTest.java        # 8 tests
    └── RunTestsWorkerTest.java        # 8 tests

```
