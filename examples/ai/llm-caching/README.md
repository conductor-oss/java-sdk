# LLM Caching in Java Using Conductor :  Hash Prompts, Cache Responses, Track Savings

A Java Conductor workflow that wraps LLM calls with a caching layer .  hashing each prompt to create a deterministic cache key, checking a cache before calling the model, storing new responses, and reporting cache hit rates and cost savings. Identical prompts return cached responses in milliseconds instead of waiting seconds for an LLM round-trip. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate hashing, cache-aware LLM calls, and savings reporting as independent workers ,  you write the caching and LLM logic, Conductor handles sequencing, retries, durability, and observability.

## Paying for the Same Answer Twice

LLM API calls are slow (1-10 seconds) and expensive ($0.01-$0.10+ per call). In production, many prompts are repeated .  the same support question, the same product description request, the same summarization of a document that hasn't changed. Without caching, every duplicate prompt makes a full round-trip to the LLM API, burning tokens and adding latency.

The caching pipeline needs three steps: hash the prompt (with the model name) to create a stable cache key, check the cache and either return the cached response or call the LLM and store the result, then report whether it was a hit or miss so you can track savings over time. If the LLM call fails, you need to retry it without re-hashing .  the cache key is still valid. And you want visibility into cache hit rates and estimated cost savings across all calls.

## The Solution

**You write the prompt hashing, cache lookup, and LLM call logic. Conductor handles the cache-aware pipeline, retries, and observability.**

Each concern is an independent worker .  prompt hashing, cache-aware LLM invocation, savings reporting. Conductor chains them so the cache key feeds into the LLM call worker (which checks the cache first), and the hit/miss result feeds into the reporter. If the LLM API times out on a cache miss, Conductor retries automatically. Every call records whether it was a cache hit or miss, the response latency, and the estimated cost savings.

### What You Write: Workers

Three workers implement the caching layer .  hashing the prompt and model into a deterministic cache key, performing a cache-aware LLM call that returns cached responses on hit, and reporting cache hit rates with estimated cost savings.

| Worker | Task | What It Does |
|---|---|---|
| **CacheHashPromptWorker** | `cache_hash_prompt` | Creates a deterministic cache key by concatenating model and prompt, normalizing whitespace, and truncating to 64 characters | Processing only |
| **CacheLlmCallWorker** | `cache_llm_call` | Checks an in-memory cache for the key .  on hit returns the cached response instantly, on miss calls OpenAI API (live) or returns a fixed response (simulated), stores the result, and reports latency |
| **CacheReportWorker** | `cache_report` | Reports whether the call was a cache hit or miss and estimates cost savings (~$0.02 per cache hit) | Processing only |

**Live vs Simulated mode:** When `CONDUCTOR_OPENAI_API_KEY` is set, `CacheLlmCallWorker` calls the OpenAI Chat Completions API (model: `gpt-4o-mini`) on cache miss. Without the key, it runs in simulated mode with deterministic output prefixed with `[SIMULATED]`. Non-LLM workers (hashing, reporting) always run their real logic.

### The Workflow

```
cache_hash_prompt
    │
    ▼
cache_llm_call
    │
    ▼
cache_report

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
java -jar target/llm-caching-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key. When set, `CacheLlmCallWorker` calls the real API on cache miss. When absent, runs in simulated mode. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/llm-caching-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow llm_caching \
  --version 1 \
  --input '{"prompt": "What is workflow orchestration?", "model": "gpt-4o-mini"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w llm_caching -s COMPLETED -c 5

```

## How to Extend

Each worker handles one caching concern .  swap in Redis or GPTCache for real cache lookups, connect any LLM provider for cache misses, track hit rates and cost savings, and the hash-check-call-store pipeline runs unchanged.

- **CacheHashPromptWorker** (`cache_hash_prompt`): use a cryptographic hash (SHA-256) for cache keys to handle long prompts, and include model version and temperature in the key for cache correctness
- **CacheLlmCallWorker** (`cache_llm_call`): replace the in-memory ConcurrentHashMap with Redis or Memcached for distributed caching across instances, and call the real OpenAI/Anthropic/Cohere API on cache misses
- **CacheReportWorker** (`cache_report`): push cache hit rates, latency savings, and estimated cost savings to a metrics backend (Datadog, Prometheus) for monitoring caching effectiveness

The hash/call/report contract is fixed .  swap the in-memory cache for Redis, connect a real LLM provider, or push metrics to Prometheus without changing the workflow.

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
llm-caching/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/llmcaching/
│   ├── CacheHashPromptWorker.java   # Worker 1: hashes prompt into cache key
│   ├── CacheLlmCallWorker.java      # Worker 2: cache-aware LLM call
│   ├── CacheReportWorker.java       # Worker 3: reports cache savings
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   └── LlmCachingExample.java      # Main entry point (supports --workers mode)
└── src/test/java/llmcaching/
    └── LlmCachingTest.java          # 7 tests

```
