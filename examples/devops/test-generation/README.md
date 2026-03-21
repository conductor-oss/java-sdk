# Test Generation in Java with Conductor

A Java Conductor workflow that automatically generates unit tests from source code .  analyzing the source file to discover functions and their signatures, generating test cases for each function, validating the generated tests for syntactic correctness, and producing a coverage report. Given a source file, language, and test framework, the pipeline outputs validated test cases and a pass/fail report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the analyze-generate-validate-report pipeline.

## Writing Tests Nobody Wants to Write

Developers know they should write tests, but doing it manually is tedious .  especially for existing code with many functions and edge cases. You need to parse the source code to understand function signatures and behavior, generate meaningful test cases that cover normal and edge scenarios, validate that the generated tests actually compile, and report on coverage. Doing this by hand for every function in a codebase does not scale.

This workflow automates test generation for a single source file. The code analyzer parses the file to extract function signatures and metadata. The test generator creates test cases for each discovered function. The validator checks that generated tests are syntactically correct. The reporter summarizes coverage and results. Each step feeds the next .  discovered functions drive test generation, generated tests feed validation, and validation results feed the report.

## The Solution

**You just write the code-analysis, test-generation, validation, and reporting workers. Conductor handles the test-gen pipeline and coverage data flow.**

Each worker handles one CRM operation. Conductor manages the customer lifecycle pipeline, assignment routing, follow-up scheduling, and activity tracking.

### What You Write: Workers

AnalyzeCodeWorker parses source files to discover function signatures and metadata, then ReportWorker summarizes coverage and pass rates, each step in the test generation pipeline operates independently.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeCodeWorker** | `tge_analyze_code` | Parses the source file to discover functions, their signatures, parameters, and return types. |
| **ReportWorker** | `tge_report` | Generates a coverage report summarizing test counts, pass rates, and per-function results. |

Workers implement domain operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
tge_analyze_code
    │
    ▼
tge_generate_tests
    │
    ▼
tge_validate_tests
    │
    ▼
tge_report

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
java -jar target/test-generation-1.0.0.jar

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
java -jar target/test-generation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tge_test_generation \
  --version 1 \
  --input '{"sourceFile": "api", "language": "en", "framework": "sample-framework"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tge_test_generation -s COMPLETED -c 5

```

## How to Extend

Each worker handles one generation step .  connect your LLM (Claude, Codex) for test creation and your test runner (JUnit, pytest, Jest) for validation, and the test-generation workflow stays the same.

- **AnalyzeCodeWorker** (`tge_analyze_code`): use tree-sitter or JavaParser for real AST-based code analysis
- **ReportWorker** (`tge_report`): integrate with JaCoCo, Istanbul, or Codecov for real coverage reporting

Connect a real code parser and test framework runner and the analyze-generate-validate-report pipeline keeps functioning unchanged.

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
test-generation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/testgeneration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TestGenerationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeCodeWorker.java
│       └── ReportWorker.java
└── src/test/java/testgeneration/workers/
    ├── AnalyzeCodeWorkerTest.java        # 2 tests
    ├── GenerateTestsWorkerTest.java        # 2 tests
    ├── ReportWorkerTest.java        # 2 tests
    └── ValidateTestsWorkerTest.java        # 2 tests

```
