# Multi-Model Compare in Java Using Conductor :  GPT-4 vs Claude vs Gemini Side-by-Side Evaluation

A Java Conductor workflow that sends the same prompt to GPT-4, Claude, and Gemini in parallel, then compares their responses .  scoring each on quality metrics and picking a winner. Conductor's `FORK_JOIN` calls all three models simultaneously so the total latency is the speed of the slowest model, not the sum of all three. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate parallel model calls and comparison as independent workers ,  you write the API integrations and scoring logic, Conductor handles parallelism, retries, durability, and observability.

## Choosing the Right Model with Data

Picking an LLM provider for your application is usually based on vibes .  "GPT-4 feels smarter" or "Claude is better at code." A data-driven approach sends the same prompts to all three models, compares their responses on objective criteria (relevance, coherence, completeness), and builds a dataset showing which model performs best for your specific use cases.

Running this comparison manually means sequential API calls (3x the latency), ad-hoc comparison in a spreadsheet, and no historical record. If one provider's API is slow or rate-limited, it blocks the other comparisons.

## The Solution

**You write the per-model API calls and the comparison scoring logic. Conductor handles the parallel execution, retries, and observability.**

Each model call is an independent worker. GPT-4, Claude, Gemini. Conductor's `FORK_JOIN` runs all three in parallel and waits for all to complete. A comparison worker scores each response and picks a winner. If Claude's API is rate-limited, Conductor retries it independently without re-calling GPT-4 or Gemini. Every execution records all three responses, scores, and the winner .  building an evaluation dataset over time.

### What You Write: Workers

Four workers run a model comparison .  calling GPT-4, Claude, and Gemini in parallel via FORK_JOIN, then scoring and ranking all three responses for quality, speed, and cost in a single comparison worker.

| Worker | Task | What It Does |
|---|---|---|
| **McCallGpt4Worker** | `mc_call_gpt4` | Calls GPT-4 via OpenAI Chat Completions API. Uses `CONDUCTOR_OPENAI_API_KEY` env var; falls back to simulated when unset. | Both |
| **McCallClaudeWorker** | `mc_call_claude` | Calls Claude via Anthropic Messages API. Uses `CONDUCTOR_ANTHROPIC_API_KEY` env var; falls back to simulated when unset. | Both |
| **McCallGeminiWorker** | `mc_call_gemini` | Calls Gemini via Google Generative AI API. Uses `GOOGLE_API_KEY` env var; falls back to simulated when unset. | Both |
| **McCompareWorker** | `mc_compare` | Compares results from GPT-4, Claude, and Gemini model calls. Determines the winner (highest quality), fastest, cheapest. | N/A |

Each worker auto-detects its API key at startup. When a key is present, the worker makes real API calls; when absent, it returns a deterministic simulated response prefixed with `[SIMULATED]`. You can set any combination .  for example, set only `CONDUCTOR_OPENAI_API_KEY` to get live GPT-4 results alongside simulated Claude and Gemini.

### The Workflow

```
FORK_JOIN
    ├── mc_call_gpt4
    ├── mc_call_claude
    └── mc_call_gemini
    │
    ▼
JOIN (wait for all branches)
mc_compare
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
java -jar target/multi-model-compare-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(unset)_ | OpenAI API key. When set, `mc_call_gpt4` makes live API calls. |
| `CONDUCTOR_ANTHROPIC_API_KEY` | _(unset)_ | Anthropic API key. When set, `mc_call_claude` makes live API calls. |
| `GOOGLE_API_KEY` | _(unset)_ | Google API key. When set, `mc_call_gemini` makes live API calls. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/multi-model-compare-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow multi_model_compare \
  --version 1 \
  --input '{"input": "test"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w multi_model_compare -s COMPLETED -c 5
```

## How to Extend

Each worker already includes real API integration .  set the corresponding env var to enable live mode. Customize the scoring logic, add new models, or implement LLM-as-judge evaluation while the parallel compare-and-rank workflow runs unchanged.

- **McCallGpt4Worker** (`mc_call_gpt4`): calls OpenAI Chat Completions API (`gpt-4`); customize the model, max_tokens, or pricing in the worker
- **McCallClaudeWorker** (`mc_call_claude`): calls Anthropic Messages API (`claude-sonnet-4-6`); adjust model version or parameters as needed
- **McCallGeminiWorker** (`mc_call_gemini`): calls Google Generative AI API (`gemini-2.0-flash`); swap model or tune generation config
- **McCompareWorker** (`mc_compare`): implement custom scoring rubrics (factual accuracy, code quality, response length) or use an LLM-as-judge pattern

Each model worker returns the same response shape, so adding new models to the comparison requires only a new worker and fork branch .  the comparison logic stays unchanged.

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
multi-model-compare/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multimodelcompare/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultiModelCompareExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── McCallClaudeWorker.java
│       ├── McCallGeminiWorker.java
│       ├── McCallGpt4Worker.java
│       └── McCompareWorker.java
└── src/test/java/multimodelcompare/workers/
    ├── McCallClaudeWorkerTest.java        # 3 tests
    ├── McCallGeminiWorkerTest.java        # 3 tests
    ├── McCallGpt4WorkerTest.java        # 3 tests
    └── McCompareWorkerTest.java        # 5 tests
```
