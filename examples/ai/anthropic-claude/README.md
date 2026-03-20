# Anthropic Claude Integration in Java Using Conductor: Build Messages, Call API, Process Response

You need Claude for long-context analysis in a production pipeline: security audits, document review, code analysis; but calling the Messages API directly means no retries on 429 rate limits, no fallback when the model is overloaded, and no centralized record of prompts, responses, or token spend. A single timeout loses your carefully constructed system prompt. This example builds a three-step Claude integration using [Conductor](https://github.com/conductor-oss/conductor), message construction, API invocation, and response processing, so every call is durable, observable, and independently retryable.

## Integrating Claude into Production Pipelines

Claude's Messages API is straightforward for a single call. Construct the messages, POST to the endpoint, read the response. But production usage means managing the system prompt separately from user messages, handling rate limits (429 errors) with exponential backoff, extracting the assistant's response from the structured JSON output, tracking token usage for cost allocation, and logging every prompt/response pair for evaluation and debugging.

Without orchestration, prompt construction, API calling, error handling, and response parsing are all mixed together. When you want to add prompt caching, swap models (claude-3-opus to claude-3-haiku for cost savings), or add response post-processing, you're editing a monolithic method.

## The Solution

**You write the message construction and Claude API call logic. Conductor handles the pipeline, durability, and observability.**

`ClaudeBuildMessagesWorker` constructs the messages array from the system prompt and user message, applying the correct format for the Claude Messages API (role-based messages with system as a top-level parameter). `ClaudeCallApiWorker` sends the request to the Claude API and surfaces live-mode 4xx/429 failures immediately. `ClaudeProcessResponseWorker` extracts the generated text from the response, along with token usage (input_tokens, output_tokens) and stop reason. Conductor preserves each task boundary, records the prompt and response payloads, and lets you raise the retry count later if you want automatic replays in production.

### What You Write: Workers

Three workers isolate each phase of the Claude integration. Message construction with system prompts, API invocation with rate-limit handling, and content-block response processing with token tracking.

| Worker | Task | What It Does |
|---|---|---|
| **ClaudeBuildMessagesWorker** | `claude_build_messages` | Builds the Claude Messages API request body, including Claude's top-level `system` field plus the user message array. |
| **ClaudeCallApiWorker** | `claude_call_api` | Calls the Anthropic Claude Messages API. When `CONDUCTOR_ANTHROPIC_API_KEY` is set, makes a real HTTP call using `java.net.http.HttpClient`; otherwise returns a deterministic simulated response. |
| **ClaudeProcessResponseWorker** | `claude_process_response` | Processes Claude's content-block response, filters `text` blocks, and returns the final analysis plus usage metadata. |

**Live vs Simulated mode:** The `ClaudeCallApiWorker` auto-detects the `CONDUCTOR_ANTHROPIC_API_KEY` environment variable at startup. When the key is present and has access to the configured model (`ANTHROPIC_MODEL`, default `claude-sonnet-4-20250514`), it makes real HTTP calls to `https://api.anthropic.com/v1/messages` using `java.net.http.HttpClient` (built into Java 21) and parses the response with Jackson. When the key is absent, it returns a deterministic simulated response so the workflow runs end-to-end without credentials. Live-mode 4xx failures are surfaced immediately with the failed task name plus a truncated response body so you can diagnose model-access and request-shape issues quickly.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Configurable retry policy** | These example task definitions intentionally use `retryCount=0` so provider 4xx/429 failures fail fast during development. Increase retries per task when you want automatic replay behavior. |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
claude_build_messages
    │
    ▼
claude_call_api
    │
    ▼
claude_process_response
```

## Example Output

```
=== Example 113: Orchestrating Anthropic Claude ===

Mode: LIVE (CONDUCTOR_ANTHROPIC_API_KEY detected)
Model: claude-sonnet-4-20250514

Step 1: Registering task definitions...
  Registered: claude_build_messages, claude_call_api, claude_process_response

Step 2: Registering workflow 'anthropic_claude_workflow'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: c7f90776-de16-9031-ac4d-309dfec055f1

  [build] Claude request: model=claude-sonnet-4-20250514, system prompt set
  [api] Calling Anthropic Claude API (LIVE, model=claude-sonnet-4-20250514)...
  [process] Extracted 1 text block(s), 412 chars

  Status: COMPLETED
  Model: claude-sonnet-4-20250514
  Stop reason: end_turn
  Analysis: Critical: JWT tokens lack expiration claims. High: password reset tokens use predictable IDs...
  Usage: {input_tokens=92, output_tokens=118}

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
java -jar target/anthropic-claude-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

`run.sh` auto-loads the nearest `.env` file it finds while walking up parent directories, so a repo-root `.env` works without manually exporting variables each time.

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_ANTHROPIC_API_KEY` | *(none)* | Anthropic API key with access to the configured model. When set, the worker makes real Claude API calls. When absent, responses are simulated. |
| `ANTHROPIC_MODEL` | `claude-sonnet-4-20250514` | Claude model to send in the Messages API request. You can also override it per workflow input with `model`. |
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

### Running with a real API key

```bash
export CONDUCTOR_ANTHROPIC_API_KEY=sk-ant-api03-...
export ANTHROPIC_MODEL=claude-sonnet-4-20250514
./run.sh
```

The example will print `Mode: LIVE` plus the configured model at startup and make real calls to the Claude Messages API. If the key lacks access to that model, the example fails fast and prints the failed task plus Anthropic's error body.

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/anthropic-claude-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow anthropic_claude_workflow \
  --version 1 \
  --input '{"userMessage":"Perform a security audit of our authentication module and list vulnerabilities by severity.","systemPrompt":"You are a senior application security engineer. Provide structured audit findings with severity levels and remediation steps.","model":"claude-sonnet-4-20250514"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w anthropic_claude_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker owns one concern of the Claude integration. Swap in real Anthropic Messages API calls with prompt caching and token tracking, and the three-step pipeline runs unchanged.

- **ClaudeCallApiWorker** (`claude_call_api`): already supports real API calls via `java.net.http.HttpClient` when `CONDUCTOR_ANTHROPIC_API_KEY` is set. Extend with prompt caching, streaming, or custom retry logic
- **ClaudeBuildMessagesWorker** (`claude_build_messages`): implement real prompt engineering: load system prompts from a template store, inject few-shot examples, or apply prompt caching with the `cache_control` parameter
- **ClaudeProcessResponseWorker** (`claude_process_response`): extract structured data from Claude's response: parse JSON outputs from tool use, handle streaming responses, or apply content filtering before returning to the caller

The message-in, text-out contract stays fixed at each boundary. Upgrade Claude models, add prompt caching, or switch to streaming without changing the workflow.

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
anthropic-claude/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/anthropicclaude/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AnthropicClaudeExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClaudeBuildMessagesWorker.java
│       ├── ClaudeCallApiWorker.java
│       └── ClaudeProcessResponseWorker.java
└── src/test/java/anthropicclaude/workers/
    ├── ClaudeBuildMessagesWorkerTest.java        # 5 tests
    ├── ClaudeCallApiWorkerTest.java        # 8 tests
    └── ClaudeProcessResponseWorkerTest.java        # 6 tests
```
