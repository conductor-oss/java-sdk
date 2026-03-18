# Tool Use Basics in Java Using Conductor -- Analyze Request, Select Tool, Execute, Format Result

Your AI chatbot can eloquently explain how to check the weather in Tokyo. It just can't actually check the weather in Tokyo. It can describe the steps to calculate 15% of 230, but it can't call a calculator. It reasons beautifully about what should happen, then hands back a paragraph of text instead of a result. The gap between "knowing what to do" and "doing it" is the tool-use problem. This example wires an LLM to real tools through a four-step Conductor pipeline: analyze the request, select the right tool, execute it, and format the result into a natural language answer. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers -- you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Foundation of Tool-Using Agents

An AI agent with access to tools (calculator, web search, calendar, database) needs a systematic way to determine which tool to use for a given request. "What's 15% of 230?" needs the calculator. "What's the weather in Tokyo?" needs the weather API. "Who won the Super Bowl?" needs web search.

The tool-use pattern has four steps: understand what the user wants (intent extraction), pick the right tool (tool selection from the available set), call the tool with the right parameters (execution), and present the result in natural language (formatting). Each step is independent -- you can swap the tool selection logic without changing execution, or add new tools without modifying the request analyzer.

## The Solution

**You write the request analysis, tool selection, execution, and formatting logic. Conductor handles the tool-use pipeline, execution retries, and usage pattern tracking.**

`AnalyzeRequestWorker` parses the user request to extract intent, entities, and parameters. `SelectToolWorker` matches the request intent against available tool descriptions and selects the best fit with a confidence score. `ExecuteToolWorker` calls the selected tool with the extracted parameters and returns the raw result. `FormatResultWorker` converts the tool's output into a natural language response. Conductor chains these four steps and records which tool was selected and why, building a dataset of tool usage patterns.

### What You Write: Workers

Four workers implement the tool-use pattern -- analyzing the request, selecting the right tool, executing it, and formatting the result into natural language.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **AnalyzeRequestWorker** | `tu_analyze_request` | Analyzes a user request to determine intent, entities, and complexity. Returns an analysis object with entities, inte... | Simulated |
| **ExecuteToolWorker** | `tu_execute_tool` | Simulates executing the selected tool and returns its output. For weather_api, returns a deterministic weather report. | Simulated |
| **FormatResultWorker** | `tu_format_result` | Formats tool execution results into a natural language answer. Builds a human-readable string based on the tool output. | Simulated |
| **SelectToolWorker** | `tu_select_tool` | Selects the appropriate tool based on the analyzed intent. Maps intent to tool name, produces a description, and buil... | Simulated |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode -- the agent workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically -- configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status -- no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
tu_analyze_request
    │
    ▼
tu_select_tool
    │
    ▼
tu_execute_tool
    │
    ▼
tu_format_result
```

## Example Output

```
=== Tool Use Basics Demo ===

Step 1: Registering task definitions...
  Registered: tu_analyze_request, tu_select_tool, tu_execute_tool, tu_format_result

Step 2: Registering workflow 'tool_use_basics'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: 4081f851-e3a5-4528-4ccd-fe88d1d58cd0

  [tu_analyze_request] Analyzing request: What's the weather like in San Francisco?
  [tu_select_tool] Selecting tool for intent: get_weather
  [tu_execute_tool] Executing tool: weather_api
  [tu_format_result] Formatting result from tool: weather_api


  Status: COMPLETED
  Output: {answer=Success, toolUsed=weather_api, sourceData={location=location, temperature=62, units=units, condition=Partly Cloudy, humidity=72, windSpeed=12, windDirection=W, forecast=[{day=Tomorrow, high=65, low=54, condition=Sunny}, {day=Day After, high=63, low=52, condition=Cloudy}]}}

Result: PASSED
```
## Running It

### Prerequisites

- **Java 21+** -- verify with `java -version`
- **Maven 3.8+** -- verify with `mvn -version`
- **Docker** -- to run Conductor

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
java -jar target/tool-use-basics-1.0.0.jar
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
java -jar target/tool-use-basics-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tool_use_basics \
  --version 1 \
  --input '{"userRequest": "What's the weather like in San Francisco?", "availableTools": [{"name": "weather_api", "description": "Fetches current weather data", "parameters": ["location", "units"]}, {"name": "calculator", "description": "Performs mathematical calculations", "parameters": ["expression", "precision"]}, {"name": "web_search", "description": "Searches the web for information", "parameters": ["query", "maxResults"]}]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tool_use_basics -s COMPLETED -c 5
```

## How to Extend

Each worker wraps one tool-use step -- plug in real APIs (weather services, calculators, search engines), LLM-based tool selection, and natural language formatting, and the analyze-select-execute-format workflow runs unchanged.

- **SelectToolWorker** (`tu_select_tool`) -- use GPT-4's function calling or Claude's tool use feature for schema-aware tool selection, or implement embedding-based tool matching for large tool registries
- **ExecuteToolWorker** (`tu_execute_tool`) -- build a real tool registry with input validation, sandboxed execution, rate limiting per tool, and structured output schemas
- **AnalyzeRequestWorker** (`tu_analyze_request`) -- use an LLM with the tool catalog as context for intent classification and parameter extraction, with fallback to clarifying questions when intent is ambiguous

Replace with real tool implementations; the analyze-select-execute-format pipeline keeps the same interface for any tool.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
tool-use-basics/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/tooluse/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ToolUseBasicsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeRequestWorker.java
│       ├── ExecuteToolWorker.java
│       ├── FormatResultWorker.java
│       └── SelectToolWorker.java
└── src/test/java/tooluse/workers/
    ├── AnalyzeRequestWorkerTest.java        # 9 tests
    ├── ExecuteToolWorkerTest.java        # 9 tests
    ├── FormatResultWorkerTest.java        # 9 tests
    └── SelectToolWorkerTest.java        # 9 tests
```
