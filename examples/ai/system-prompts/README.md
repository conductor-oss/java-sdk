# System Prompts in Java Using Conductor :  A/B Test Formal vs Casual Tone with Side-by-Side Comparison

A Java Conductor workflow that runs the same user prompt through two different system prompts .  formal and casual ,  then compares the outputs side by side. This lets you evaluate how system prompt tone affects response quality, length, and style for your specific use case. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate prompt building, sequential LLM calls (formal then casual), and output comparison as independent workers ,  you write the prompt engineering and comparison logic, Conductor handles sequencing, retries, durability, and observability.

## Testing System Prompt Variations

The system prompt dramatically affects LLM output .  formal prompts produce longer, more structured responses while casual prompts produce shorter, more conversational ones. But the effect varies by model and use case. To choose the right system prompt, you need to run the same user prompt through multiple variations and compare the outputs on metrics that matter (tone, length, helpfulness, accuracy).

This workflow builds the prompt with each system prompt style, calls the LLM for each, and compares the outputs .  recording both responses and the comparison metrics.

## The Solution

**You write the prompt variations and comparison scoring logic. Conductor handles the A/B test sequencing, retries, and observability.**

Each step is an independent worker .  prompt building (combining system prompt with user message), LLM invocation, and output comparison. Conductor sequences the two LLM calls and the comparison step, retries if either call is rate-limited, and tracks every A/B test with both responses and the comparison results.

### What You Write: Workers

Three workers enable system prompt experimentation .  building the full prompt with a system persona and user message, calling the LLM, and comparing outputs from different system prompts (formal vs, casual) side by side.

| Worker | Task | What It Does |
|---|---|---|
| **SpBuildPromptWorker** | `sp_build_prompt` | Builds a full prompt by combining a system prompt, few-shot examples, and the user's prompt based on the requested style. | Processing only |
| **SpCallLlmWorker** | `sp_call_llm` | Calls an LLM with the assembled prompt. Uses OpenAI API in live mode, returns style-based deterministic output in simulated mode. |
| **SpCompareOutputsWorker** | `sp_compare_outputs` | Compares the formal and casual LLM responses, reporting length differences and tone insights. | Processing only |

**Live vs Simulated mode:** When `CONDUCTOR_OPENAI_API_KEY` is set, `SpCallLlmWorker` calls the OpenAI Chat Completions API (model: `gpt-4o-mini`) with system prompt, few-shot examples, and user message. Without the key, it runs in simulated mode with style-based deterministic output prefixed with `[SIMULATED]`. Non-LLM workers (prompt building, comparison) always run their real logic.

### The Workflow

```
sp_build_prompt
    │
    ▼
sp_call_llm
    │
    ▼
sp_build_prompt
    │
    ▼
sp_call_llm
    │
    ▼
sp_compare_outputs
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
java -jar target/system-prompts-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key. When set, `SpCallLlmWorker` calls the real API. When absent, runs in simulated mode. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/system-prompts-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow system_prompts_workflow \
  --version 1 \
  --input '{"userPrompt": "test-value", "model": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w system_prompts_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one A/B testing step .  swap in real LLM calls to compare system prompt tones, add custom scoring metrics for your use case, and the build-call-compare workflow runs unchanged.

- **SpBuildPromptWorker** (`sp_build_prompt`): load system prompts and few-shot examples from a prompt registry (database, config service), supporting A/B testing of different prompt styles
- **SpCallLlmWorker** (`sp_call_llm`): call an LLM API (OpenAI GPT-4, Anthropic Claude, Google Gemini) with the assembled system prompt, few-shot examples, and user message
- **SpCompareOutputsWorker** (`sp_compare_outputs`): use an LLM-as-judge or automated metrics (BLEU, ROUGE, BERTScore) to compare outputs from different system prompt styles and recommend the best variant

The prompt-in, comparison-out contract stays fixed .  add new persona variants, swap LLM providers, or change scoring criteria without altering the workflow structure.

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
system-prompts/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/systemprompts/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SystemPromptsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── SpBuildPromptWorker.java
│       ├── SpCallLlmWorker.java
│       └── SpCompareOutputsWorker.java
└── src/test/java/systemprompts/workers/
    ├── SpBuildPromptWorkerTest.java        # 6 tests
    ├── SpCallLlmWorkerTest.java        # 6 tests
    └── SpCompareOutputsWorkerTest.java        # 6 tests
```
