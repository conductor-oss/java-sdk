# Calculator Agent in Java Using Conductor :  Parse Expressions, Compute Steps, Explain Results

Calculator Agent. parse a math expression, compute step-by-step following PEMDAS, and explain the result. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Math Agents Need to Show Their Work

An LLM asked to compute "(15.7 + 3.3) * 2.5 / (1 + 0.1)" will often get the wrong answer. large language models are unreliable at arithmetic. A calculator agent separates understanding from computation: first parse the expression into structured operations (identify operands, operators, and precedence), then compute each step with proper floating-point precision, then explain the solution process so the user understands the reasoning.

Without separation, the LLM tries to do all three at once and often makes arithmetic errors that it confidently presents as correct. By externalizing computation to a dedicated worker, the math is always right. the LLM's role is limited to parsing intent and explaining results, which it does well.

## The Solution

**You write the parsing, computation, and explanation logic. Conductor handles the step-by-step pipeline and ensures each phase uses the prior phase's output.**

`ParseExpressionWorker` analyzes the mathematical expression and breaks it into an ordered list of operations with operands, operators, and evaluation order. `ComputeStepsWorker` evaluates each operation step-by-step with the specified precision, tracking intermediate results. `ExplainResultWorker` generates a natural language walkthrough of the computation, explaining each step and the final answer. Conductor chains these three steps, ensuring the computation uses the parser's output and the explanation uses the computation's intermediate results.

### What You Write: Workers

Three workers separate math reasoning from computation. Parsing the expression, computing each step with proper precision, and explaining the result.

| Worker | Task | What It Does |
|---|---|---|
| **ComputeStepsWorker** | `ca_compute_steps` | Computes the result of a parsed expression step-by-step, following the operation order from the parser. |
| **ExplainResultWorker** | `ca_explain_result` | Generates a human-readable explanation of how a mathematical expression was evaluated, referencing PEMDAS rules. |
| **ParseExpressionWorker** | `ca_parse_expression` | Parses a mathematical expression into tokens and determines the order of operations following PEMDAS rules. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
ca_parse_expression
    │
    ▼
ca_compute_steps
    │
    ▼
ca_explain_result

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
java -jar target/calculator-agent-1.0.0.jar

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
java -jar target/calculator-agent-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow calculator_agent \
  --version 1 \
  --input '{"expression": "sample-expression", "precision": "sample-precision"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w calculator_agent -s COMPLETED -c 5

```

## How to Extend

Each worker owns one stage of the computation pipeline. Plug in a real expression parser (ANTLR), arbitrary-precision arithmetic (BigDecimal or SymPy), and an LLM for natural language explanations, and the parse-compute-explain workflow runs unchanged.

- **ParseExpressionWorker** (`ca_parse_expression`): use a proper expression parser (ANTLR or a Pratt parser) to handle complex expressions with nested parentheses, functions (sin, log, sqrt), and variable substitution
- **ComputeStepsWorker** (`ca_compute_steps`): use Java's BigDecimal for arbitrary-precision arithmetic, or integrate with SymPy (via Jython or subprocess) for symbolic computation and algebraic simplification
- **ExplainResultWorker** (`ca_explain_result`): use an LLM to generate contextual explanations adapted to the user's level (student vs: engineer), with LaTeX formatting for mathematical notation

Swap in a real math library or LLM parser; the parse-compute-explain pipeline maintains the same interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
calculator-agent/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/calculatoragent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CalculatorAgentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ComputeStepsWorker.java
│       ├── ExplainResultWorker.java
│       └── ParseExpressionWorker.java
└── src/test/java/calculatoragent/workers/
    ├── ComputeStepsWorkerTest.java        # 9 tests
    ├── ExplainResultWorkerTest.java        # 9 tests
    └── ParseExpressionWorkerTest.java        # 9 tests

```
