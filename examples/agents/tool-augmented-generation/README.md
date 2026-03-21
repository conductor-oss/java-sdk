# Tool-Augmented Generation in Java Using Conductor :  Generate, Detect Gaps, Call Tools, Incorporate, Complete

Tool-Augmented Generation .  detect knowledge gaps during text generation, invoke external tools to fill them, and produce enriched output. Uses [Conductor](https://github.

## LLMs Need Tools When Their Knowledge Runs Out

An LLM generating a response about current stock prices will hallucinate numbers because its training data is months old. Tool-augmented generation detects these knowledge gaps mid-generation and pauses to call external tools .  a stock API for prices, a calculator for computations, a database for customer data ,  then resumes generation with real data incorporated.

This five-step pipeline separates concern: start the generation, detect where the model would hallucinate (gap detection), call the right tool to fill the gap, splice the tool result back into the generation context, and complete the response with factual data. Without this separation, the LLM confidently generates wrong numbers, and there's no record of which facts were grounded in tool calls versus model knowledge.

## The Solution

**You write the generation, gap detection, tool invocation, and incorporation logic. Conductor handles the enrichment pipeline, tool retries, and provenance tracking.**

`StartGenerationWorker` begins the LLM generation and produces an initial partial response. `DetectGapWorker` analyzes the partial response for knowledge gaps .  places where the model needs real-time data (prices, dates, statistics) it doesn't have. `CallToolWorker` invokes the appropriate tool based on the gap type and returns factual data. `IncorporateResultWorker` merges the tool's output back into the generation context. `CompleteGenerationWorker` finishes the response with the tool data incorporated. Conductor chains these five steps and records which tools were called and what data they provided.

### What You Write: Workers

Five workers enrich generation with real data. Starting the draft, detecting knowledge gaps, calling external tools, incorporating the results, and completing the response.

| Worker | Task | What It Does |
|---|---|---|
| **CallToolWorker** | `tg_call_tool` | Invokes the external tool identified by the gap detector and returns the factual result along with its source. |
| **CompleteGenerationWorker** | `tg_complete_generation` | Finalises text generation by appending remaining content to the enriched text and reporting total token count. |
| **DetectGapWorker** | `tg_detect_gap` | Analyses partial text to detect the knowledge gap and determines which external tool should be called and what query ... |
| **IncorporateResultWorker** | `tg_incorporate_result` | Merges the tool result into the partial text, producing enriched text that fills the previously detected knowledge gap. |
| **StartGenerationWorker** | `tg_start_generation` | Begins text generation from a prompt and produces partial text with a knowledge gap that requires an external tool to... |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

### The Workflow

```
tg_start_generation
    │
    ▼
tg_detect_gap
    │
    ▼
tg_call_tool
    │
    ▼
tg_incorporate_result
    │
    ▼
tg_complete_generation

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
java -jar target/tool-augmented-generation-1.0.0.jar

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
java -jar target/tool-augmented-generation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tool_augmented_generation \
  --version 1 \
  --input '{"prompt": "What is workflow orchestration?"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tool_augmented_generation -s COMPLETED -c 5

```

## How to Extend

Each worker handles one stage of the tool-augmented pipeline. Use an LLM for gap detection, connect real data APIs (Alpha Vantage, OpenWeatherMap, Wolfram Alpha) for factual grounding, and splice tool results back into generation context, and the generate-detect-call-incorporate-complete workflow runs unchanged.

- **DetectGapWorker** (`tg_detect_gap`): use an LLM to identify placeholders, uncertain statements, or temporal references in the partial generation that indicate a need for external data
- **CallToolWorker** (`tg_call_tool`): build a tool registry with real APIs: financial data (Alpha Vantage, Yahoo Finance), weather (OpenWeatherMap), calculations (Wolfram Alpha), and databases (JDBC queries)
- **IncorporateResultWorker** (`tg_incorporate_result`): use structured injection to replace placeholders with tool results while maintaining narrative coherence and citation tracking

Wire in real LLMs and tool APIs; the generation-enrichment pipeline preserves the same gap-detection-and-fill interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
tool-augmented-generation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/toolaugmentedgeneration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ToolAugmentedGenerationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CallToolWorker.java
│       ├── CompleteGenerationWorker.java
│       ├── DetectGapWorker.java
│       ├── IncorporateResultWorker.java
│       └── StartGenerationWorker.java
└── src/test/java/toolaugmentedgeneration/workers/
    ├── CallToolWorkerTest.java        # 8 tests
    ├── CompleteGenerationWorkerTest.java        # 8 tests
    ├── DetectGapWorkerTest.java        # 8 tests
    ├── IncorporateResultWorkerTest.java        # 8 tests
    └── StartGenerationWorkerTest.java        # 8 tests

```
