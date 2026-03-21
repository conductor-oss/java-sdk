# LLM Retry in Java Using Conductor :  Exponential Backoff for Transient API Failures

A Java Conductor workflow that demonstrates Conductor's built-in retry mechanism for LLM API calls. The LLM call task is configured with `retryCount: 3` and `EXPONENTIAL_BACKOFF` retry logic .  if the LLM API returns a rate-limit error or times out, Conductor automatically retries with increasing delays (1s, 2s, 4s) without any retry code in the worker. A reporting step tracks how many attempts were needed. Uses [Conductor](https://github.com/conductor-oss/conductor) to handle retries declaratively ,  you write the LLM call, Conductor retries it with backoff.

## LLM APIs Fail Transiently :  A Lot

LLM APIs are notorious for transient failures. OpenAI returns 429 (rate limited) when you exceed your tokens-per-minute quota. Anthropic returns 529 (overloaded) during peak usage. Google Gemini times out when model loading takes too long. These failures are temporary .  the same request succeeds seconds later.

Writing retry logic by hand means wrapping every API call in a loop with exponential backoff, tracking attempt counts, handling different error codes, and deciding when to give up. That logic gets duplicated across every LLM call in your codebase, is easy to get wrong (forgetting to cap the backoff, retrying non-retryable errors), and obscures the actual business logic.

## The Solution

The LLM call worker contains only the API call logic .  no retry loops, no backoff calculations, no attempt counting. Retry behavior is declared in the workflow definition: `retryCount: 3`, `retryLogic: EXPONENTIAL_BACKOFF`, `retryDelaySeconds: 1`. Conductor handles the rest. Every attempt is tracked with its inputs, outputs, and timing, so you can see exactly how many retries a specific call needed and why each attempt failed.

### What You Write: Workers

Two workers demonstrate retry resilience .  an LLM call worker that may fail transiently, and a report worker that records the outcome ,  with Conductor's built-in exponential backoff handling the retries automatically.

| Worker | Task | What It Does |
|---|---|---|
| **RetryLlmCallWorker** | `retry_llm_call` | LLM API call that fails with 429 rate-limit errors on the first two attempts, then succeeds on the third. Calls OpenAI API in live mode on success attempt. |
| **RetryReportWorker** | `retry_report` | Summarises the retry outcome. Receives the LLM response and the number of attempts it took to succeed. | Processing only |

**Live vs Simulated mode:** When `CONDUCTOR_OPENAI_API_KEY` is set, the successful attempt (3rd+) of `RetryLlmCallWorker` calls the OpenAI Chat Completions API (model: `gpt-4o-mini`). Without the key, it runs in simulated mode with deterministic output prefixed with `[SIMULATED]`. The retry simulation (first 2 attempts fail with 429) always runs regardless of mode.

### The Workflow

```
retry_llm_call
    │
    ▼
retry_report

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
java -jar target/llm-retry-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key. When set, successful LLM attempts call the real API. When absent, runs in simulated mode. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/llm-retry-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow llm_retry_wf \
  --version 1 \
  --input '{"prompt": "What is workflow orchestration?", "model": "gpt-4o-mini"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w llm_retry_wf -s COMPLETED -c 5

```

## How to Extend

Each worker contains only the API call logic with zero retry code .  swap in a real OpenAI, Claude, or Gemini API call, and Conductor's declarative retry configuration handles exponential backoff automatically with the workflow unchanged.

- **RetryLlmCallWorker** (`retry_llm_call`): swap in a real LLM API call (OpenAI, Claude, Gemini); throw `RuntimeException` on transient errors so Conductor triggers its retry logic
- **RetryReportWorker** (`retry_report`): write retry metrics to a monitoring system (Datadog, Prometheus) to track retry rates over time

The call/report contract stays fixed .  replace the simulated LLM call with a real API client and Conductor's retry configuration handles transient failures without any worker code changes.

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
llm-retry/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/llmretry/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LlmRetryExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── RetryLlmCallWorker.java
│       └── RetryReportWorker.java
└── src/test/java/llmretry/workers/
    ├── RetryLlmCallWorkerTest.java        # 5 tests
    └── RetryReportWorkerTest.java        # 3 tests

```
