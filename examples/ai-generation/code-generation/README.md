# Code Generation in Java with Conductor :  From Requirements to Validated, Tested Code

A Java Conductor workflow that generates code from natural language requirements. parsing requirements into structured specs, generating source code in a specified language and framework, validating syntax, and running test cases against the output. Given `requirements`, `language`, and `framework`, the pipeline produces source files, syntax validation results, test pass/fail status, and a line count. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the parse-generate-validate-test pipeline.

## Generating Code That Actually Works

Generating code from requirements is not just about producing text that looks like code. The output needs to be syntactically valid and functionally correct. This means parsing the requirements into actionable specs, generating code in the right language with the right framework conventions, checking that it compiles, and running test cases to verify behavior. Each step depends on the previous one. you cannot validate code that has not been generated, and you cannot run tests without knowing the expected behavior from the requirements.

This workflow takes natural language requirements and produces tested code. The parser extracts structured specs from the requirements. The generator creates source code (e.g., REST API routes) along with test cases in the specified language and framework. The validator checks syntax. The test runner executes the generated test cases against the generated code. The workflow outputs the code, line count, syntax validity, and test results.

## The Solution

**You just write the requirement-parsing, code-generation, validation, and test-execution workers. Conductor handles the pipeline sequencing and data routing.**

Four workers form the code generation pipeline. requirement parsing, code generation, syntax validation, and test execution. The parser converts free-text requirements into structured specs. The generator produces source files (like `routes/users.js`, `routes/orders.js`) with accompanying test cases. The validator checks the generated code for syntax errors. The test runner executes the test cases and reports pass/fail. Conductor sequences the pipeline and routes parsed specs, generated code, and test cases between steps via JSONPath.

### What You Write: Workers

ParseRequirementsWorker converts natural language to structured specs, GenerateCodeWorker produces source files with test cases, and ValidateWorker checks syntax, each step in the code generation pipeline runs independently.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateCodeWorker** | `cdg_generate_code` | Generates source code and test files in the specified language and framework from parsed requirements. |
| **ParseRequirementsWorker** | `cdg_parse_requirements` | Converts natural-language requirements into structured specs (entities, endpoints, operations). |
| **ValidateWorker** | `cdg_validate` | Checks the generated code for syntax errors and lint violations. |

Workers implement domain operations. lead scoring, contact enrichment, deal updates,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
cdg_parse_requirements
    │
    ▼
cdg_generate_code
    │
    ▼
cdg_validate
    │
    ▼
cdg_test

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
java -jar target/code-generation-1.0.0.jar

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
java -jar target/code-generation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cdg_code_generation \
  --version 1 \
  --input '{"requirements": "sample-requirements", "language": "en", "framework": "sample-framework"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cdg_code_generation -s COMPLETED -c 5

```

## How to Extend

Each worker handles one stage of the generation pipeline. connect your LLM (Claude, GPT-4, Codex) for code generation and your CI system for syntax validation and test execution, and the code-gen workflow stays the same.

- **GenerateCodeWorker** (`cdg_generate_code`): swap in an LLM (GPT-4, Claude, Codex) for real code generation from structured specs
- **ParseRequirementsWorker** (`cdg_parse_requirements`): use NLP or an LLM to extract entities, relationships, and constraints from natural language
- **ValidateWorker** (`cdg_validate`): integrate with language-specific compilers or linters (ESLint, javac, rustc) for real syntax validation

Plug in a real LLM for code generation and the parse-generate-validate pipeline operates without modification.

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
code-generation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/codegeneration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CodeGenerationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GenerateCodeWorker.java
│       ├── ParseRequirementsWorker.java
│       └── ValidateWorker.java
└── src/test/java/codegeneration/workers/
    ├── GenerateCodeWorkerTest.java        # 2 tests
    ├── ParseRequirementsWorkerTest.java        # 2 tests
    ├── TestWorkerTest.java        # 2 tests
    └── ValidateWorkerTest.java        # 2 tests

```
