# LLM Fallback Chain in Java Using Conductor -- GPT-4, Claude, Gemini with Automatic Provider Failover

GPT-4 returns a 429 and your entire AI feature goes dark -- because you bet everything on a single provider. Claude has a maintenance window the same week. Your users see error pages while three other perfectly capable models sit idle. This example builds an automatic fallback chain using [Conductor](https://github.com/conductor-oss/conductor) that tries GPT-4 first, falls back to Claude on failure, then to Gemini, and reports which model actually served the request -- so a single provider outage never takes down your AI feature again.

## LLM Providers Go Down

No single LLM provider has 100% uptime. GPT-4 rate-limits under heavy load. Claude has maintenance windows. Gemini returns 503s during capacity crunches. If your application depends on a single provider, an outage means your users get errors -- even though two other perfectly capable models are available.

A fallback chain tries GPT-4 first (your preferred model). If GPT-4 returns a failure status, the workflow calls Claude. If Claude also fails, it tries Gemini. The response comes from whichever provider succeeds first. A formatting step at the end normalizes the output and records which model was used and how many fallbacks were triggered.

This creates nested conditional logic -- try A, check status, try B on failure, check status, try C on failure. Without orchestration, this becomes a deeply nested try/catch chain where you lose visibility into which provider failed, why it failed, and how often each fallback is triggered.

## The Solution

**You write the API integration for each LLM provider. Conductor handles the failover routing, retries, and observability.**

Each provider is an independent worker -- GPT-4, Claude, Gemini -- each returning a status and response. Conductor's nested `SWITCH` tasks inspect each status and route to the next provider only when the previous one failed. Every execution records the full fallback path: which providers were tried, which failed, and which ultimately served the request.

### What You Write: Workers

Four workers implement the multi-provider fallback -- one per LLM provider (GPT-4, Claude, Gemini) plus a formatter that reports which model served the request and how many failovers occurred.

| Worker | Task | What It Does | Live / Simulated |
|---|---|---|---|
| **FbCallGpt4Worker** | `fb_call_gpt4` | Calls GPT-4 (the preferred model). In live mode, calls the OpenAI Chat Completions API. In simulated mode, returns a `503 Service Unavailable` failure to trigger the fallback chain | Live when `CONDUCTOR_OPENAI_API_KEY` is set; simulated otherwise |
| **FbCallClaudeWorker** | `fb_call_claude` | Calls Claude (first fallback). In live mode, calls the Anthropic Messages API. In simulated mode, returns a `429 Too Many Requests` failure to trigger the next fallback | Live when `CONDUCTOR_ANTHROPIC_API_KEY` is set; simulated otherwise |
| **FbCallGeminiWorker** | `fb_call_gemini` | Calls Gemini (last resort). In live mode, calls the Google Generative Language API. In simulated mode, returns a `` success response completing the fallback chain | Live when `GOOGLE_API_KEY` is set; simulated otherwise |
| **FbFormatResultWorker** | `fb_format_result` | Inspects the status of each model's response, selects the first successful response, and reports which model was used and how many fallbacks were triggered (0 = GPT-4 succeeded, 1 = Claude, 2 = Gemini) | Always runs locally |

Each worker auto-detects whether to make live API calls or return simulated responses based on the corresponding environment variable. No code changes are needed to switch between modes -- just set or unset the API key. All API calls use `java.net.http.HttpClient` (built into Java 21) with Jackson for JSON serialization.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically -- configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status -- no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
fb_call_gpt4
    │
    ▼
SWITCH (check_gpt4_ref)
    ├── failed: fb_call_claude -> check_claude_status
    │
    ▼
fb_format_result
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

Starts Conductor on port 8080 and runs the example automatically in simulated mode.

To run with live API calls, pass your API keys:

```bash
CONDUCTOR_OPENAI_API_KEY=sk-... CONDUCTOR_ANTHROPIC_API_KEY=sk-ant-... GOOGLE_API_KEY=AI... docker compose up --build
```

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

# Build and run (simulated mode)
mvn package -DskipTests
java -jar target/llm-fallback-chain-1.0.0.jar

# Or with live API calls (set any combination of keys):
export CONDUCTOR_OPENAI_API_KEY=sk-...
export CONDUCTOR_ANTHROPIC_API_KEY=sk-ant-...
export GOOGLE_API_KEY=AI...
java -jar target/llm-fallback-chain-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

# With live API calls:
CONDUCTOR_OPENAI_API_KEY=sk-... CONDUCTOR_ANTHROPIC_API_KEY=sk-ant-... GOOGLE_API_KEY=AI... ./run.sh
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |
| `CONDUCTOR_OPENAI_API_KEY` | _(not set)_ | OpenAI API key. When set, `FbCallGpt4Worker` makes live API calls to GPT-4 |
| `CONDUCTOR_ANTHROPIC_API_KEY` | _(not set)_ | Anthropic API key. When set, `FbCallClaudeWorker` makes live API calls to Claude |
| `GOOGLE_API_KEY` | _(not set)_ | Google API key. When set, `FbCallGeminiWorker` makes live API calls to Gemini |

Each LLM worker independently checks for its API key. You can set any combination -- for example, set only `GOOGLE_API_KEY` to make live Gemini calls while GPT-4 and Claude run in simulated mode.

## Example Output

```
=== LLM Fallback Chain: Multi-Model Fallback ===

Step 1: Registering task definitions...
  Registered: fb_call_gpt4, fb_call_claude, fb_call_gemini, fb_format_result

Step 2: Registering workflow 'llm_fallback_chain_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  [fb_call_gpt4] Calling OpenAI GPT-4 API with prompt: Explain how Conductor handles workflow orchestration
  [fb_call_gpt4] GPT-4 returned success
  [fb_call_gpt4] GPT-4 returned error: error-value
  [fb_call_gpt4] GPT-4 call failed: error-value
  [fb_call_gpt4] Attempting GPT-4 with prompt: Explain how Conductor handles workflow orchestration
  [fb_call_gpt4] GPT-4 returned 503 Service Unavailable
  [fb_call_claude] Calling Anthropic Claude API with prompt: Explain how Conductor handles workflow orchestration
  [fb_call_claude] Claude returned success
  [fb_call_claude] Claude returned error: error-value
  [fb_call_claude] Claude call failed: error-value
  [fb_call_claude] Attempting Claude with prompt: Explain how Conductor handles workflow orchestration
  [fb_call_claude] Claude returned 429 Too Many Requests
  [fb_call_gemini] Calling Google Gemini API with prompt: Explain how Conductor handles workflow orchestration
  [fb_call_gemini] Gemini returned success
  [fb_call_gemini] Gemini returned error: error-value
  [fb_call_gemini] Gemini call failed: error-value
  [fb_call_gemini] Attempting Gemini with prompt: Explain how Conductor handles workflow orchestration
  [fb_call_gemini] Gemini returned success
  [fb_format_result] Model used: primary-model, fallbacks triggered: 0

  Workflow ID: d4e5f6a7-...

Step 5: Waiting for completion...

  Status: COMPLETED
  Output: {response=Success, modelUsed=primary-model, fallbacksTriggered=0}

Result: PASSED
```
## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/llm-fallback-chain-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow llm_fallback_chain_workflow \
  --version 1 \
  --input '{"prompt": "Explain the benefits of workflow orchestration for microservices"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w llm_fallback_chain_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker wraps one LLM provider call with live/simulated dual-mode. The real API integrations are already built in -- just set the corresponding environment variable to enable live calls.

- **FbCallGpt4Worker** (`fb_call_gpt4`) -- calls OpenAI Chat Completions API (`gpt-4`) when `CONDUCTOR_OPENAI_API_KEY` is set. Customize the model, max_tokens, or add system prompts by editing the request body
- **FbCallClaudeWorker** (`fb_call_claude`) -- calls Anthropic Messages API (`claude-sonnet-4-6`) when `CONDUCTOR_ANTHROPIC_API_KEY` is set. Handles rate limits (429) and server errors as `status: "failed"`
- **FbCallGeminiWorker** (`fb_call_gemini`) -- calls Google Generative Language API (`gemini-2.0-flash`) when `GOOGLE_API_KEY` is set
- **FbFormatResultWorker** (`fb_format_result`) -- customize the output normalization to match your application's expected response format. Add token usage tracking from each provider's response to compare costs
- **Add more providers** -- extend the SWITCH chain with additional providers (Mistral, Llama via Together AI, Cohere) for deeper fallback coverage
- **Add per-provider retries** -- configure Conductor task-level retries (e.g., retry each provider 2x with exponential backoff before falling back to the next provider)

Each provider worker returns the same status/response shape, so adding new providers or reordering the fallback chain requires no changes to the existing workers.

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
llm-fallback-chain/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/llmfallbackchain/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LlmFallbackChainExample.java # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FbCallClaudeWorker.java   # Claude call (live Anthropic API / simulated 429)
│       ├── FbCallGeminiWorker.java   # Gemini call (live Google API / simulated success)
│       ├── FbCallGpt4Worker.java     # GPT-4 call (live OpenAI API / simulated 503)
│       └── FbFormatResultWorker.java # Picks first success, reports fallback count
└── src/test/java/llmfallbackchain/workers/
    ├── FbCallClaudeWorkerTest.java
    ├── FbCallGeminiWorkerTest.java
    ├── FbCallGpt4WorkerTest.java
    └── FbFormatResultWorkerTest.java # 4 tests -- GPT-4 success, Claude fallback, Gemini fallback, all fail
```
