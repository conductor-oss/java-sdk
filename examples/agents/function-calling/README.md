# Function Calling in Java Using Conductor: LLM Plans, Extract Call, Execute, Synthesize

You ask "What's Apple's stock price?" and the model calls `get_stock_price(ticker="APPL")`. a ticker that doesn't exist. Or it invents a function called `fetch_realtime_quote` that was never in the schema. Or it calls `get_stock_price` with `{"user_id": "12345"}` because it confused the parameters from a different function. When an LLM has direct API access, every hallucinated function name, wrong parameter type, or confused argument is a live production call. This example separates intent from execution: the LLM decides what to call, a validation layer checks it against the real function registry, and only then does execution happen. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Giving LLMs the Ability to Call Functions

An LLM can reason about what needs to happen ("I should look up the current weather") but can't actually call a weather API. Function calling bridges this gap: the LLM receives a list of available function definitions (name, description, parameters), decides which function to call for the user's query, and outputs a structured function call. A separate step executes the actual function and returns the result. The LLM then synthesizes the function output into a natural language response.

This four-step pattern separates intent (LLM decides what to call) from execution (worker actually calls the function). The LLM never has direct API access. It only specifies what it wants. The execution layer validates the call, applies rate limits, and handles errors. Without this separation, you'd give the LLM direct API access, which is both a security risk and makes debugging impossible.

## The Solution

**You write the LLM planning, function extraction, execution, and synthesis logic. Conductor handles the call chain, execution retries, and full audit of every function invocation.**

`LlmPlanWorker` sends the user query and function definitions to an LLM, which returns its decision about which function to call and why. `ExtractFunctionCallWorker` parses the LLM's output to extract the function name and arguments in a structured format. `ExecuteFunctionWorker` validates the extracted call against the function registry and executes the function with the specified arguments. `LlmSynthesizeWorker` feeds the function's result back to the LLM, which synthesizes it into a natural language response for the user. Conductor chains these steps, retries failed function executions, and records the full plan-extract-execute-synthesize chain for debugging.

### What You Write: Workers

Four workers implement function calling, the LLM plans which function to invoke, the call is extracted and validated, the function executes, and the result is synthesized into natural language.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteFunctionWorker** | `fc_execute_function` | Executes the specified function with the given arguments. Currently supports get_stock_price; returns real API results |
| **ExtractFunctionCallWorker** | `fc_extract_function_call` | Extracts a structured function call (name + arguments) from the LLM output and validates it against the available fun |
| **LlmPlanWorker** | `fc_llm_plan` | Llm Plan. Computes and returns llm response |
| **LlmSynthesizeWorker** | `fc_llm_synthesize` | LLM synthesis worker. Takes the function execution result and produces a natural-language answer for the user. |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode, the agent workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
fc_llm_plan
    │
    ▼
fc_extract_function_call
    │
    ▼
fc_execute_function
    │
    ▼
fc_llm_synthesize
```

## Example Output

```
=== Function Calling Demo ===

Step 1: Registering task definitions...
  Registered: fc_llm_plan, fc_extract_function_call, fc_execute_function, fc_llm_synthesize

Step 2: Registering workflow 'function_calling'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: 2bc402e2-160a-5755-7225-7c2db07fc296

  [fc_llm_plan] Planning function call for query: What's the current price of Apple stock?
  [fc_extract_function_call] Extracting function call from LLM output
  [fc_execute_function] Executing function: unknown with args: <functionCall.get("ar>
  [fc_llm_synthesize] Synthesizing answer for query: What's the current price of Apple stock?


  Status: COMPLETED
  Output: {answer=I was unable to retrieve the requested information for your query: , confidence=0.97, sourceFunctionUsed=unknown, executionStatus=error}

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
java -jar target/function-calling-1.0.0.jar
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
java -jar target/function-calling-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow function_calling \
  --version 1 \
  --input '{"userQuery": "What's the current price of Apple stock?", "functionDefinitions": [{"name": "get_stock_price", "description": "Get the current stock price for a given ticker symbol", "parameters": {"ticker": {"type": "string", "description": "Stock ticker symbol"}, "includeChange": {"type": "boolean", "description": "Include price change data"}}}, {"name": "get_weather", "description": "Get the current weather for a location", "parameters": {"location": {"type": "string", "description": "City name or coordinates"}, "units": {"type": "string", "description": "Temperature units: celsius or fahrenheit"}}}, {"name": "search_web", "description": "Search the web for information", "parameters": {"query": {"type": "string", "description": "Search query"}, "maxResults": {"type": "integer", "description": "Maximum number of results"}}}]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w function_calling -s COMPLETED -c 5
```

## How to Extend

Each worker owns one phase of the function-calling pattern. Connect OpenAI's function calling or Claude's tool use for planning, build a validated function registry for execution, and use an LLM for answer synthesis, and the plan-extract-execute-synthesize workflow runs unchanged.

- **LlmPlanWorker** (`fc_llm_plan`): use OpenAI's function calling API or Claude's tool use feature for native structured function call output instead of parsing free-text responses
- **ExecuteFunctionWorker** (`fc_execute_function`): build a real function registry with input validation, sandboxed execution, rate limiting, and audit logging per function call
- **LlmSynthesizeWorker** (`fc_llm_synthesize`): use streaming responses for real-time output, with citation of which function provided which data points in the answer

Wire in a real LLM and function registry; the plan-extract-execute-synthesize pipeline preserves the same interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
function-calling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/functioncalling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FunctionCallingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExecuteFunctionWorker.java
│       ├── ExtractFunctionCallWorker.java
│       ├── LlmPlanWorker.java
│       └── LlmSynthesizeWorker.java
└── src/test/java/functioncalling/workers/
    ├── ExecuteFunctionWorkerTest.java        # 9 tests
    ├── ExtractFunctionCallWorkerTest.java        # 9 tests
    ├── LlmPlanWorkerTest.java        # 8 tests
    └── LlmSynthesizeWorkerTest.java        # 9 tests
```
