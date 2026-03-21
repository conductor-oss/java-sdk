# API Test Generation in Java with Conductor :  Auto-Generate and Run Tests from an OpenAPI Spec

A Java Conductor workflow that automatically generates API tests from an OpenAPI specification. parsing the spec to discover endpoints, generating test cases for each endpoint, running the test suite, and producing a pass/fail report. Given a spec URL and format (OpenAPI 3, Swagger, etc.), the pipeline outputs endpoint counts, test counts, and pass rates without manual test authoring. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the parse-generate-run-report pipeline.

## From API Spec to Test Report in Four Steps

Every API needs tests, but writing them by hand is tedious and falls behind as endpoints change. An OpenAPI spec already describes your endpoints, parameters, and expected responses. the information needed to generate tests automatically. The challenge is coordinating the pipeline: parse the spec into a list of endpoints, generate test cases for each one, execute the suite, and compile the results into a report.

This workflow takes a `specUrl` and `format` as input, parses the spec to extract endpoints (paths, methods, parameters), generates a test suite covering each endpoint, runs those tests, and produces a report with pass rates. Each step's output feeds the next via JSONPath. the parsed endpoints feed test generation, the test suite feeds execution, and the execution results feed the report.

## The Solution

**You just write the spec-parsing, test-generation, execution, and reporting workers. Conductor handles the pipeline sequencing and endpoint data flow.**

Four workers form the test generation pipeline. spec parsing, test generation, test execution, and reporting. The spec parser extracts endpoint metadata (paths like `/users`, `/orders`, methods like GET/POST, and their parameters). The test generator creates test cases for each endpoint. The runner executes them and calculates pass rates. The reporter compiles everything into a summary. Conductor sequences the four steps and passes each output to the next input automatically.

### What You Write: Workers

ParseSpecWorker extracts endpoints and methods from the OpenAPI spec, and ReportWorker compiles pass/fail counts and pass rates, each step in the API test pipeline operates independently.

| Worker | Task | What It Does |
|---|---|---|
| **ParseSpecWorker** | `atg_parse_spec` | Parses the input and computes endpoints, endpoint count |
| **ReportWorker** | `atg_report` | Compiles test execution results into a summary report with pass/fail counts and pass rate. |

Workers implement domain operations. lead scoring, contact enrichment, deal updates,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
atg_parse_spec
    │
    ▼
atg_generate_tests
    │
    ▼
atg_run_tests
    │
    ▼
atg_report

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
java -jar target/api-test-generation-1.0.0.jar

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
java -jar target/api-test-generation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow atg_api_test_generation \
  --version 1 \
  --input '{"specUrl": "https://example.com", "format": "json"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w atg_api_test_generation -s COMPLETED -c 5

```

## How to Extend

Each worker handles one test-gen step. connect your OpenAPI parser (Swagger, Redocly) for spec analysis and your test runner (REST Assured, Postman, Newman) for execution, and the API-test workflow stays the same.

- **ParseSpecWorker** (`atg_parse_spec`): swap in a real OpenAPI parser (e.g., Swagger Parser, openapi4j) to extract endpoints from live specs
- **ReportWorker** (`atg_report`): integrate with CI systems (GitHub Actions, Jenkins) to post test results as PR comments or pipeline artifacts

Swap in a real OpenAPI parser and test runner and the spec-to-report pipeline continues to generate tests without workflow changes.

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
api-test-generation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/apitestgeneration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ApiTestGenerationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ParseSpecWorker.java
│       └── ReportWorker.java
└── src/test/java/apitestgeneration/workers/
    ├── GenerateTestsWorkerTest.java        # 2 tests
    ├── ParseSpecWorkerTest.java        # 2 tests
    ├── ReportWorkerTest.java        # 2 tests
    └── RunTestsWorkerTest.java        # 2 tests

```
