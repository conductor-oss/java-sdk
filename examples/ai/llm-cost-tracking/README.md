# LLM Cost Tracking in Java Using Conductor: Multi-Model Calls with Per-Provider Cost Aggregation

End of month AWS bill: $12,000 in OpenAI API calls. Nobody knows which feature consumed what, or that the summarization pipeline was running GPT-4 on 10-word inputs that Gemini could have handled for pennies. Every provider bills differently. Per-token with separate input/output rates, usage metadata buried in different response fields, and without centralized tracking you're flying blind. This example builds a multi-provider cost tracking pipeline using [Conductor](https://github.com/conductor-oss/conductor) that sends the same prompt to GPT-4, Claude, and Gemini, captures per-call token usage, and aggregates a side-by-side cost breakdown so you know exactly where every dollar goes.

## Knowing What Your LLM Calls Actually Cost

When your application calls multiple LLM providers, cost tracking becomes fragmented. GPT-4 charges per token with different rates for input vs, output. Claude has its own token pricing. Gemini uses a different pricing model entirely. Without centralized tracking, you get a surprise bill at the end of the month with no breakdown of which provider, which prompt, or which feature drove the cost.

This workflow makes cost visible per call: each provider returns its token usage (prompt tokens, completion tokens), and an aggregation step applies each provider's pricing to compute per-model costs and a total. Over many executions, you build a dataset showing exactly how much each provider costs for your specific workloads. Enabling data-driven decisions about which model to use for which tasks.

## The Solution

**You write the provider API calls and pricing aggregation logic. Conductor handles the sequencing, retries, and observability.**

Each provider call is an independent worker. GPT-4, Claude, Gemini, each returning token usage alongside its response. An aggregation worker applies per-provider pricing rates to compute costs. Conductor sequences the calls, retries if any provider's API is temporarily unavailable, and tracks every execution with full token usage data. Over time, the execution history becomes your cost analytics dataset.

### What You Write: Workers

Four workers track costs across providers. Calling GPT-4, Claude, and Gemini sequentially with per-call token and cost tracking, then aggregating total spend, token usage, and per-model breakdowns in a final report.

| Worker | Task | What It Does |
|---|---|---|
| **CallGpt4Worker** | `ct_call_gpt4` | Calls GPT-4 with the prompt and returns token usage alongside the response text | **Live** when `CONDUCTOR_OPENAI_API_KEY` is set (calls OpenAI API, extracts `usage.prompt_tokens` and `usage.completion_tokens`). **Simulated** otherwise (returns fixed tokens with `` prefix) |
| **CallClaudeWorker** | `ct_call_claude` | Calls Claude with the prompt and returns token usage alongside the response text | **Live** when `CONDUCTOR_ANTHROPIC_API_KEY` is set (calls Anthropic API, extracts `usage.input_tokens` and `usage.output_tokens`). **Simulated** otherwise |
| **CallGeminiWorker** | `ct_call_gemini` | Calls Gemini with the prompt and returns token usage alongside the response text | **Live** when `GOOGLE_API_KEY` is set (calls Gemini API, extracts `usageMetadata`). **Simulated** otherwise |
| **AggregateCostsWorker** | `ct_aggregate_costs` | Aggregate Costs. Computes and returns breakdown, total cost, total tokens | Pricing-based aggregation of real or HMAC-signed JWT counts |

Each worker auto-detects its API key from the environment. Set one, two, or all three keys to mix live and simulated providers in the same workflow run. Without any keys, everything runs in simulated mode with realistic output shapes.

### The Workflow

```
ct_call_gpt4
    |
    v
ct_call_claude
    |
    v
ct_call_gemini
    |
    v
ct_aggregate_costs

```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### API Keys (optional)

Set any combination of API keys to enable live mode for individual providers:

```bash
export CONDUCTOR_OPENAI_API_KEY="sk-..."        # Enables live GPT-4 calls
export CONDUCTOR_ANTHROPIC_API_KEY="sk-ant-..." # Enables live Claude calls
export GOOGLE_API_KEY="AIza..."       # Enables live Gemini calls

```

Without keys, workers run in simulated mode with fixed token counts and `` response prefix.

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build

```

Starts Conductor on port 8080 and runs the example automatically.

To enable live API calls via Docker Compose, set the keys before running:

```bash
CONDUCTOR_OPENAI_API_KEY="sk-..." CONDUCTOR_ANTHROPIC_API_KEY="sk-ant-..." GOOGLE_API_KEY="AIza..." docker compose up --build

```

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
java -jar target/llm-cost-tracking-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

# With live API calls:
CONDUCTOR_OPENAI_API_KEY="sk-..." CONDUCTOR_ANTHROPIC_API_KEY="sk-ant-..." GOOGLE_API_KEY="AIza..." ./run.sh

```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |
| `CONDUCTOR_OPENAI_API_KEY` | _(unset)_ | OpenAI API key. Enables live GPT-4 calls |
| `CONDUCTOR_ANTHROPIC_API_KEY` | _(unset)_ | Anthropic API key. Enables live Claude calls |
| `GOOGLE_API_KEY` | _(unset)_ | Google API key. Enables live Gemini calls |

## Example Output

### Simulated mode (no API keys)

```
=== LLM Cost Tracking: Multi-Model Cost Aggregation ===

Step 1: Registering task definitions...
  Registered: ct_call_gpt4, ct_call_claude, ct_call_gemini, ct_aggregate_costs

Step 2: Registering workflow 'llm_cost_tracking_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: e5f6a7b8-...

Step 5: Waiting for completion...
  Status: COMPLETED
  Output: {breakdown=[
    {model=gpt-4, inputTokens=120, outputTokens=350, cost=$0.0246},
    {model=claude-3, inputTokens=120, outputTokens=280, cost=$0.0228},
    {model=gemini, inputTokens=120, outputTokens=400, cost=$0.0007}
  ], totalCost=$0.0481, totalTokens=1390}

Result: PASSED

```

### Live mode (with API keys)

When API keys are set, workers call the real provider APIs and extract actual token usage. The cost breakdown reflects real token counts from each provider for the given prompt.

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/llm-cost-tracking-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow llm_cost_tracking_workflow \
  --version 1 \
  --input '{"prompt": "Summarize the key benefits of microservices architecture"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w llm_cost_tracking_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker calls one LLM provider and reports token usage. The same worker interface supports both live and simulated modes. Set the corresponding API key to switch.

- **AggregateCostsWorker** (`ct_aggregate_costs`): update pricing rates as providers change their pricing, or write cost data to a time-series database (InfluxDB, TimescaleDB) or observability platform (Datadog, Grafana) for dashboarding
- **Run providers in parallel**: replace the sequential task chain with a FORK/JOIN to call all three providers simultaneously, reducing total latency from 3x to 1x
- **Add a cost-based router**: use the historical cost data to build a SWITCH-based router that sends simple prompts to cheaper models (Gemini) and complex prompts to more capable models (GPT-4, Claude)

Each provider worker returns the same cost/token shape, so adding providers or changing pricing models requires no changes to the aggregation logic.

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
llm-cost-tracking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/llmcosttracking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LlmCostTrackingExample.java  # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateCostsWorker.java # Per-model cost calculation with pricing rates
│       ├── CallClaudeWorker.java     # Claude call (live via Anthropic API or simulated)
│       ├── CallGeminiWorker.java     # Gemini call (live via Google API or simulated)
│       └── CallGpt4Worker.java       # GPT-4 call (live via OpenAI API or simulated)
└── src/test/java/llmcosttracking/workers/
    ├── AggregateCostsWorkerTest.java # 3 tests. Full cost calculation, small token counts
    ├── CallClaudeWorkerTest.java
    ├── CallGeminiWorkerTest.java
    └── CallGpt4WorkerTest.java

```
