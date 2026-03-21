# Streaming LLM in Java Using Conductor :  Prepare, Collect Chunks, Post-Process

A Java Conductor workflow that handles LLM streaming responses .  preparing the request, collecting server-sent event (SSE) chunks into a complete response, and post-processing the assembled output. While Conductor tasks are request-response based, this pattern lets you integrate streaming LLM APIs by collecting all chunks in a worker and producing the complete response as output. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate stream preparation, chunk collection, and post-processing as independent workers ,  you write the streaming integration, Conductor handles sequencing, retries, durability, and observability.

## Integrating Streaming LLMs into Workflows

LLM streaming APIs (SSE/WebSocket) deliver responses token by token for lower perceived latency. But workflow tasks need complete inputs and outputs. The solution: a worker that opens a streaming connection, collects all chunks into a buffer, and returns the complete response when the stream ends. This gives you streaming's benefits (early token delivery, chunked processing) while fitting into Conductor's request-response model.

If the stream breaks mid-generation, the worker can signal failure and Conductor retries the entire stream collection automatically.

## The Solution

**You write the stream collection and chunk assembly logic. Conductor handles the pipeline sequencing, retries, and observability.**

Each concern is an independent worker .  stream preparation (building the request with streaming enabled), chunk collection (consuming SSE events and assembling the complete response), and post-processing (formatting, token counting, or content filtering). Conductor sequences them and retries the chunk collector if the stream disconnects mid-generation.

### What You Write: Workers

Three workers handle LLM streaming .  preparing the SSE connection parameters, collecting streamed chunks into a complete response, and post-processing the assembled text with token counts and timing.

| Worker | Task | What It Does |
|---|---|---|
| **StreamCollectChunksWorker** | `stream_collect_chunks` | Simulates collecting streamed chunks from an LLM and assembling them into a full response. |
| **StreamPostProcessWorker** | `stream_post_process` | Post-processes the assembled LLM response: counts words and marks the result as processed. |
| **StreamPrepareWorker** | `stream_prepare` | Formats a raw prompt into a system/user prompt pair for the LLM. |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### The Workflow

```
stream_prepare
    │
    ▼
stream_collect_chunks
    │
    ▼
stream_post_process

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
java -jar target/streaming-llm-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key. When set, `StreamCollectChunksWorker` calls the real OpenAI streaming endpoint (SSE). When unset, returns simulated chunked responses with `[SIMULATED]` prefix. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/streaming-llm-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow streaming_llm_wf \
  --version 1 \
  --input '{"prompt": "What is workflow orchestration?", "model": "gpt-4o-mini", "maxTokens": "sample-maxTokens"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w streaming_llm_wf -s COMPLETED -c 5

```

## How to Extend

Each worker handles one streaming concern .  swap in a real SSE connection to OpenAI or Claude's streaming API, collect chunks into a complete response, and the prepare-collect-postprocess pipeline runs unchanged.

- **StreamCollectChunksWorker** (`stream_collect_chunks`): call an LLM streaming endpoint (OpenAI streaming, Anthropic streaming, Ollama streaming) and collect SSE chunks into a complete response
- **StreamPostProcessWorker** (`stream_post_process`): apply post-processing to the assembled response: Markdown rendering, token counting, content filtering, or structured extraction
- **StreamPrepareWorker** (`stream_prepare`): format the raw user prompt into system/user message pairs with model-specific templates (ChatML, Llama format, Claude XML)

The prepare/collect/post-process contract is fixed .  switch from OpenAI streaming to Claude streaming or change the chunk assembly strategy without modifying the workflow.

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
streaming-llm/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/streamingllm/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── StreamingLlmExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── StreamCollectChunksWorker.java
│       ├── StreamPostProcessWorker.java
│       └── StreamPrepareWorker.java
└── src/test/java/streamingllm/workers/
    ├── StreamCollectChunksWorkerTest.java        # 5 tests
    ├── StreamPostProcessWorkerTest.java        # 5 tests
    └── StreamPrepareWorkerTest.java        # 4 tests

```
