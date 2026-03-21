# Tool Use Validation in Java Using Conductor :  Generate Call, Validate Input, Execute, Validate Output, Deliver

Tool Use Validation. generate tool call, validate input, execute tool, validate output, and deliver results through a sequential pipeline. Uses [Conductor](https://github.

## Tool Calls Need Guardrails on Both Sides

An LLM generates a tool call: `search(query="DROP TABLE users")`. Without input validation, that malicious query goes straight to the search API. Or it generates `calculate(expression="1/0")`. Without output validation, the division-by-zero error crashes the pipeline.

Input validation catches bad parameters before execution. rejecting SQL injection attempts, enforcing value ranges, checking required fields, and ensuring type correctness. Output validation catches bad results after execution,  verifying the response matches the expected schema, checking for error responses disguised as successes, and performing sanity checks on returned values. Both validations are separate from the tool execution itself, so adding new validation rules doesn't require modifying tool code.

## The Solution

**You write the call generation, input/output validation, and execution logic. Conductor handles the validation pipeline, security gating, and audit trail for every tool call.**

`GenerateToolCallWorker` creates the tool call specification from the user request. tool name, parameters, and expected return type. `ValidateInputWorker` checks the parameters against the tool's input schema,  type correctness, required fields present, values within allowed ranges, and no injection patterns. `ExecuteToolWorker` runs the validated call. `ValidateOutputWorker` checks the response against the expected output schema,  correct structure, reasonable values, no error responses. `DeliverWorker` formats and delivers the validated result. Conductor chains all five steps and records validation results for security auditing.

### What You Write: Workers

Five workers add guardrails to tool calls. Generating the call, validating input, executing the tool, validating output, and delivering the verified result.

| Worker | Task | What It Does |
|---|---|---|
| **DeliverWorker** | `tv_deliver` | Delivers the final formatted result to the user, combining the validated tool output with the validation report into ... |
| **ExecuteToolWorker** | `tv_execute_tool` | Executes the validated tool call and returns the raw output along with execution metadata. |
| **GenerateToolCallWorker** | `tv_generate_tool_call` | Generates a tool call from a user request, producing structured tool arguments along with input and output schemas fo... |
| **ValidateInputWorker** | `tv_validate_input` | Validates tool input arguments against the provided schema. Returns validation status, the validated arguments, a lis... |
| **ValidateOutputWorker** | `tv_validate_output` | Validates the raw tool output against the expected output schema. Returns validation status, the validated output, an... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
tv_generate_tool_call
    │
    ▼
tv_validate_input
    │
    ▼
tv_execute_tool
    │
    ▼
tv_validate_output
    │
    ▼
tv_deliver

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
java -jar target/tool-use-validation-1.0.0.jar

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
java -jar target/tool-use-validation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tool_use_validation \
  --version 1 \
  --input '{"userRequest": "sample-userRequest", "toolName": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tool_use_validation -s COMPLETED -c 5

```

## How to Extend

Each worker handles one guardrail stage. Implement JSON Schema validation and injection detection for inputs, domain-specific sanity checks for outputs, and LLM-based call generation, and the generate-validate-execute-validate-deliver pipeline runs unchanged.

- **ValidateInputWorker** (`tv_validate_input`): implement JSON Schema validation, SQL injection pattern detection, and input sanitization using OWASP recommendations
- **ValidateOutputWorker** (`tv_validate_output`): use JSON Schema validation for structure, domain-specific sanity checks (prices must be positive, dates must be in the past/future as expected), and anomaly detection for unexpected values
- **GenerateToolCallWorker** (`tv_generate_tool_call`): use OpenAI function calling or Claude tool use for schema-aware call generation that's more likely to pass input validation

Wire in real validation schemas and tool implementations; the double-validation pipeline preserves the same security-gating interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
tool-use-validation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/toolusevalidation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ToolUseValidationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeliverWorker.java
│       ├── ExecuteToolWorker.java
│       ├── GenerateToolCallWorker.java
│       ├── ValidateInputWorker.java
│       └── ValidateOutputWorker.java
└── src/test/java/toolusevalidation/workers/
    ├── DeliverWorkerTest.java        # 9 tests
    ├── ExecuteToolWorkerTest.java        # 8 tests
    ├── GenerateToolCallWorkerTest.java        # 8 tests
    ├── ValidateInputWorkerTest.java        # 9 tests
    └── ValidateOutputWorkerTest.java        # 9 tests

```
