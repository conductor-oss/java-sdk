# Conditional Tool Use in Java Using Conductor :  Classify Query, Route to Calculator/Interpreter/Search

Tool Use Conditional. classifies a user query and routes to the appropriate tool (calculator, interpreter, or web search) via a SWITCH task. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Different Questions Need Different Tools

"What's the square root of 144?" needs a calculator. "Write a Python function to sort a list" needs a code interpreter. "What happened at the G7 summit?" needs web search. A tool-using agent must first determine which type of question it's looking at, then route to the appropriate tool.

Classification determines the tool, and getting it wrong wastes resources and returns poor results. A math question sent to web search gets irrelevant links. A code question sent to the calculator gets an error. The `SWITCH` pattern makes this routing explicit and auditable. you can see which queries are classified into which categories, track accuracy, and add new categories (data analysis, translation, scheduling) without modifying existing tool workers.

## The Solution

**You write the query classifier and individual tool handlers. Conductor handles the conditional routing, per-tool retries, and classification analytics.**

`ClassifyQueryWorker` analyzes the user's query and determines the type. math (arithmetic, equations), code (programming, algorithms), or search (factual questions, current events),  with a confidence score. Conductor's `SWITCH` routes to the matching tool: `CalculatorWorker` for math computations, `InterpreterWorker` for code execution, or `WebSearchWorker` for information retrieval. Each tool handles its domain and returns results in its own format. Conductor tracks which tool handles each query type, enabling classification accuracy analysis.

### What You Write: Workers

Four workers handle conditional routing. Classifying the query type, then routing to the calculator, code interpreter, or web search via SWITCH.

| Worker | Task | What It Does |
|---|---|---|
| **CalculatorWorker** | `tc_calculator` | Handles math-category queries by simulating a calculator tool. Returns an answer string, a calculation object with ex... |
| **ClassifyQueryWorker** | `tc_classify_query` | Classifies a user query into one of three categories (math, code, search) and routes it to the appropriate downstream... |
| **InterpreterWorker** | `tc_interpreter` | Handles code-category queries by simulating a code interpreter tool. Returns an answer with generated code, the code ... |
| **WebSearchWorker** | `tc_web_search` | Handles search-category queries by simulating a web search tool. Returns an answer string, a list of search results, ... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
tc_classify_query
    │
    ▼
SWITCH (route_to_tool_ref)
    ├── math: tc_calculator
    ├── code: tc_interpreter
    ├── search: tc_web_search
    └── default: tc_web_search

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
java -jar target/tool-use-conditional-1.0.0.jar

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
java -jar target/tool-use-conditional-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tool_use_conditional \
  --version 1 \
  --input '{"userQuery": "What is workflow orchestration?"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tool_use_conditional -s COMPLETED -c 5

```

## How to Extend

Each tool worker handles one query category. Connect Wolfram Alpha for math, a Docker sandbox for code interpretation, and SerpAPI for web search, and the classify-then-route conditional workflow runs unchanged.

- **ClassifyQueryWorker** (`tc_classify_query`): use GPT-4 with few-shot examples for multi-class intent classification, or train a lightweight BERT classifier on labeled query data for faster, cheaper classification
- **CalculatorWorker** (`tc_calculator`): integrate with Wolfram Alpha for advanced math (calculus, linear algebra, symbolic computation) or use Java's BigDecimal for precise arithmetic
- **InterpreterWorker** (`tc_interpreter`): execute code in a Docker sandbox with resource limits, supporting multiple languages (Python, JavaScript, SQL) with proper output capture

Swap in real tool implementations; the classify-and-route pipeline preserves the same category-based routing interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
tool-use-conditional/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/tooluseconditional/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ToolUseConditionalExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CalculatorWorker.java
│       ├── ClassifyQueryWorker.java
│       ├── InterpreterWorker.java
│       └── WebSearchWorker.java
└── src/test/java/tooluseconditional/workers/
    ├── CalculatorWorkerTest.java        # 9 tests
    ├── ClassifyQueryWorkerTest.java        # 9 tests
    ├── InterpreterWorkerTest.java        # 9 tests
    └── WebSearchWorkerTest.java        # 9 tests

```
