# Code Interpreter Agent in Java Using Conductor :  Analyze, Generate, Execute, Interpret

Code Interpreter Agent .  analyzes a data question, generates Python code, executes in a sandbox, and interprets the results through a sequential pipeline. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Answering Data Questions Requires Running Code, Not Just Generating It

"What's the correlation between marketing spend and revenue in Q3?" can't be answered by an LLM alone .  it requires loading the dataset, computing a Pearson correlation coefficient, and possibly generating a scatter plot. The LLM can generate the code, but someone needs to run it, capture the output (including any charts), and explain what the results mean.

A code interpreter agent separates these concerns: analyze the question to determine what computation is needed (statistical analysis, data aggregation, visualization), generate the code, execute it in a sandbox where it can't access production systems, and interpret the raw output ("The correlation coefficient is 0.87, indicating a strong positive relationship between marketing spend and revenue"). Each step has different failure modes .  the code might have a syntax error, the sandbox might timeout, the output might need re-interpretation.

## The Solution

**You write the question analysis, code generation, sandbox execution, and interpretation logic. Conductor handles the pipeline, retries on execution timeouts, and full reproducibility tracking.**

`AnalyzeQuestionWorker` examines the question and dataset to determine the analysis type (statistical, aggregation, visualization), required libraries, and output format. `GenerateCodeWorker` produces executable code (Python with pandas/matplotlib or SQL) based on the analysis plan. `ExecuteSandboxWorker` runs the code in an isolated environment, captures stdout, stderr, and any generated files (charts, CSVs). `InterpretResultWorker` translates the raw execution output into a natural language answer with key findings. Conductor chains these steps, retries code execution on timeout, and records the generated code and output for reproducibility.

### What You Write: Workers

Four workers form the code interpreter pipeline. Analyzing the question, generating executable code, running it in a sandbox, and interpreting the output.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeQuestionWorker** | `ci_analyze_question` | Analyzes a data question against a dataset schema to determine the type of analysis needed, the operations required, ... |
| **ExecuteSandboxWorker** | `ci_execute_sandbox` | Simulates executing generated code in a sandboxed environment. Returns the execution result including stdout with a t... |
| **GenerateCodeWorker** | `ci_generate_code` | Generates Python code based on the analysis plan and data schema. Produces a pandas-based script that performs group-... |
| **InterpretResultWorker** | `ci_interpret_result` | Interprets the execution results in the context of the original question. Provides a human-readable answer, a structu... |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
ci_analyze_question
    │
    ▼
ci_generate_code
    │
    ▼
ci_execute_sandbox
    │
    ▼
ci_interpret_result
```

## Example Output

```
=== Code Interpreter Agent Demo ===

Step 1: Registering task definitions...
  Registered: ci_analyze_question, ci_generate_code, ci_execute_sandbox, ci_interpret_result

Step 2: Registering workflow 'code_interpreter_agent'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [ci_analyze_question] Analyzing question:
  [ci_execute_sandbox] Executing
  [ci_generate_code] Generating
  [ci_interpret_result] Interpreting results for:

  Status: COMPLETED
  Output: {analysis=..., dataSchema=..., language=..., result=...}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/code-interpreter-1.0.0.jar
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
java -jar target/code-interpreter-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow code_interpreter_agent \
  --version 1 \
  --input '{"question": "What is the average sales by region? Which region performs best?", "What is the average sales by region? Which region performs best?": "dataset", "dataset": {"key": "value"}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w code_interpreter_agent -s COMPLETED -c 5
```

## How to Extend

Each worker owns one phase of the code interpretation cycle. Connect an LLM for code generation, a real sandbox (Docker, AWS Lambda, Jupyter kernel) for execution, and an LLM for result interpretation, and the analyze-generate-execute-interpret workflow runs unchanged.

- **ExecuteSandboxWorker** (`ci_execute_sandbox`): integrate with a real sandbox: Docker containers with resource limits, AWS Lambda for serverless execution, or Jupyter kernel gateway for notebook-style computation
- **GenerateCodeWorker** (`ci_generate_code`): use GPT-4 with the dataset schema as context and function-calling to generate syntactically correct code, with automatic linting before execution
- **InterpretResultWorker** (`ci_interpret_result`): use an LLM to generate contextual explanations of statistical results, with visualization descriptions for charts and confidence intervals for estimates

Replace with real code generation and sandbox execution; the analysis-to-interpretation pipeline maintains the same data contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
code-interpreter/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/codeinterpreter/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CodeInterpreterExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeQuestionWorker.java
│       ├── ExecuteSandboxWorker.java
│       ├── GenerateCodeWorker.java
│       └── InterpretResultWorker.java
└── src/test/java/codeinterpreter/workers/
    ├── AnalyzeQuestionWorkerTest.java        # 9 tests
    ├── ExecuteSandboxWorkerTest.java        # 9 tests
    ├── GenerateCodeWorkerTest.java        # 9 tests
    └── InterpretResultWorkerTest.java        # 9 tests
```
