# OpenAI GPT-4 in Java Using Conductor: Chat Completion Pipeline with Request Building and Result Extraction

You want to call GPT-4 from a workflow, but the API times out sometimes, the response format varies between models, retries are manual, and there is no record of which calls consumed how many tokens. Prompt construction, HTTP calls, and response parsing get tangled into one brittle method where changing the system message means touching retry logic. This example builds a clean three-step GPT-4 pipeline using [Conductor](https://github.com/conductor-oss/conductor). Build the request, call the API, extract the result, so each concern is independently testable, retryable, and observable without writing a single line of retry or logging code.

## Structured GPT-4 Integration

OpenAI's chat completion API requires a specific request format. Messages with roles, model selection, temperature, max_tokens, and optional function calling. The response returns a choices array with finish reasons and a usage object tracking prompt and completion tokens. Building the request, making the API call, and parsing the response are three distinct concerns.

When they're combined in a single method, changing the prompt format (adding a system message, switching from GPT-4 to GPT-4 Turbo) means touching API call code. A 429 rate-limit error loses the built request. And there's no centralized record of token usage across calls for cost tracking.

## The Solution

**You write the request construction, API call, and result extraction logic. Conductor handles the pipeline, durability, and observability.**

Each concern is an independent worker. Request building (constructing the messages array with system and user messages, setting model parameters), API invocation (the actual HTTP call to OpenAI), and result extraction (pulling the response text and token usage from the choices array). Conductor chains them, records every call's prompt, response, and token usage, and lets you raise retries later if you want automatic replay behavior in production. In this example, task defs intentionally use `retryCount=0` so live API failures surface immediately while you validate the integration.

### What You Write: Workers

Three workers handle the OpenAI integration. Building the chat completion request with messages and parameters, calling the GPT-4 API, and extracting the response text with usage statistics.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **Gpt4BuildRequestWorker** | `gpt4_build_request` | Builds an OpenAI chat completion request body from workflow input parameters. | Real (no API needed) |
| **Gpt4CallApiWorker** | `gpt4_call_api` | Calls the OpenAI GPT-4 chat completion API, or returns a simulated response when no API key is set. | Real with `CONDUCTOR_OPENAI_API_KEY` / Simulated without |
| **Gpt4ExtractResultWorker** | `gpt4_extract_result` | Extracts the assistant's response content from the GPT-4 API response. | Real (no API needed) |

Without `CONDUCTOR_OPENAI_API_KEY`, the `Gpt4CallApiWorker` runs in simulated mode with `` output. Set the environment variable to make real OpenAI API calls. The default chat model is `gpt-4o-mini`, configurable through `OPENAI_CHAT_MODEL` or workflow input `model`, and the worker interface stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Configurable retry policy** | These example task definitions intentionally use `retryCount=0` so provider failures fail fast during development. Increase retries per task when you want automatic replay behavior. |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
gpt4_build_request
    │
    ▼
gpt4_call_api
    │
    ▼
gpt4_extract_result
```

## Example Output

```
=== OpenAI GPT-4 Integration Workflow ===

Mode: LIVE (CONDUCTOR_OPENAI_API_KEY detected)
Model: gpt-4o-mini

Step 1: Registering task definitions...
  Registered: gpt4_build_request, gpt4_call_api, gpt4_extract_result

Step 2: Registering workflow 'openai_gpt4_workflow'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: fc3c1826-3366-44c6-571c-5169f94c69e3

  [gpt4_build_request worker] Built request for model: gpt-4o-mini
  [gpt4_call_api worker] Response from OpenAI API (LIVE): North America and APAC drove growth while Europe contracted and SMB churn worsened...
  [gpt4_extract_result worker] Extracted summary (214 chars)

  Status: COMPLETED
  Output: {summary=North America and APAC drove growth while Europe contracted and SMB churn worsened...}

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
java -jar target/openai-gpt4-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

`run.sh` auto-loads the nearest `.env` file it finds while walking up parent directories, so a repo-root `.env` works without manual `export` commands.

## Configuration

| Environment Variable | Required For | Default | Description |
|---|---|---|---|
| `CONDUCTOR_OPENAI_API_KEY` | Real mode | _(none)_ | OpenAI API key for GPT-4 chat completion. Without this, workers run in simulated mode with `` output. |
| `OPENAI_CHAT_MODEL` | Optional | `gpt-4o-mini` | Default chat model for live runs. You can also override it per workflow input with `model`. |
| `CONDUCTOR_BASE_URL` | Conductor | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | Docker Compose | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/openai-gpt4-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow openai_gpt4_workflow \
  --version 1 \
  --input '{"model":"gpt-4o-mini","prompt":"Analyze these Q3 revenue figures: North America $12.4M->$14.1M, Europe $8.2M->$7.9M, APAC $5.1M->$6.8M, LATAM $2.3M->$2.5M. Total Q3 $31.3M vs Q2 $28.0M. Enterprise deals were 62% of new bookings and SMB churn rose from 4.2% to 5.1%.","systemMessage":"You are a senior financial analyst. Be specific with numbers and actionable recommendations."}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w openai_gpt4_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one phase of the GPT-4 integration. Swap in real OpenAI Chat Completions API calls with function calling or JSON mode, and the build-call-extract pipeline runs unchanged.

- **Gpt4BuildRequestWorker** (`gpt4_build_request`): add function calling definitions, response_format for JSON mode, or multi-turn conversation history to the request body
- **Gpt4CallApiWorker** (`gpt4_call_api`): already uses the official OpenAI Java SDK in live mode. Extend it with structured output, tool calls, or custom rate-limit handling while keeping the request/response contract stable
- **Gpt4ExtractResultWorker** (`gpt4_extract_result`): parse function call responses, handle multiple choices for best-of-N sampling, or extract structured data using JSON mode output

The request-in, result-out contract is stable. Switch between GPT-4, GPT-4o, or o1 models, or add function calling, without modifying the workflow.

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
openai-gpt4/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/openaigpt4/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── OpenaiGpt4Example.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── Gpt4BuildRequestWorker.java
│       ├── Gpt4CallApiWorker.java
│       └── Gpt4ExtractResultWorker.java
└── src/test/java/openaigpt4/workers/
    ├── Gpt4BuildRequestWorkerTest.java        # 5 tests
    ├── Gpt4CallApiWorkerTest.java        # 5 tests
    └── Gpt4ExtractResultWorkerTest.java        # 6 tests
```
