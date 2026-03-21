# LLM Chain in Java Using Conductor: Prompt, Generate, Parse, Validate

First LLM call summarizes a customer email. Second one extracts product IDs. Third one validates against the catalog. If the second one fails with a rate limit, you've wasted the first call's tokens and the third one never runs, or worse, you retry the whole chain from scratch because everything was jammed into one function. When the final answer is wrong, you can't tell if the prompt was bad, the generation hallucinated, the parser choked, or the validator missed a rule. This example builds a four-step LLM chain using [Conductor](https://github.com/conductor-oss/conductor): prompt, generate, parse, validate, where each step is independently retryable and its inputs and outputs are recorded for debugging.

## LLM Output You Can Trust

LLMs generate text, but applications need structured, validated data. When a customer email asks about product recommendations, the LLM's response needs to be parsed into a structured format (product IDs, quantities, reasons) and validated against the actual product catalog. Does product X exist? Is it in stock? Is the price correct?

Each step in this chain depends on the previous one: the prompt must be formatted before generation, the raw text must be generated before parsing, and the parsed data must exist before validation. If the LLM generates malformed JSON, the parser catches it. If the parser succeeds but references a product ID that doesn't exist, the validator catches it. Without separating these concerns, you end up with a single method where prompt formatting, API calls, JSON parsing, and business validation are tangled together. Impossible to test individually and impossible to retry a failed LLM call without re-building the prompt.

## The Solution

**You write the prompt engineering, generation, parsing, and business validation logic. Conductor handles the chain sequencing, retries, and observability.**

Each stage of the chain is an independent worker. prompt construction (combining customer email with product catalog), LLM generation, response parsing (extracting structured JSON from raw text), and catalog validation (checking product IDs and prices). Conductor sequences them, retries the LLM call if it's rate-limited, and tracks every step so you can see exactly where in the chain a failure occurred, was it a bad prompt, a generation error, a parse failure, or a validation mismatch?

### What You Write: Workers

Four workers form a sequential processing chain: prompt construction from customer email context, LLM generation, structured response parsing, and output validation, each step refining the previous output.

| Worker | Task | What It Does |
|---|---|---|
| **ChainGenerateWorker** | `chain_generate` | LLM generation. Takes formattedPrompt, model, temperature, maxTokens. Calls OpenAI API in live mode, returns deterministic output in simulated mode. |
| **ChainParseWorker** | `chain_parse` | Worker 3: Parses rawText JSON string into a Map. Returns FAILED status if parsing fails. | Processing only |
| **ChainPromptWorker** | `chain_prompt` | Worker 1: Takes customerEmail and productCatalog, builds a structured prompt with few-shot examples and expected JSON format. | Processing only |
| **ChainValidateWorker** | `chain_validate` | Worker 4: Validates parsedData against business rules. Runs 4 checks: valid_intent, valid_sentiment, products_in_catalog, reply_length. | Processing only |

**Live vs Simulated mode:** When `CONDUCTOR_OPENAI_API_KEY` is set, `ChainGenerateWorker` calls the OpenAI Chat Completions API (model: `gpt-4o-mini`). Without the key, it runs in simulated mode with deterministic output prefixed with ``. Non-LLM workers (prompt building, parsing, validation) always run their real logic.

### The Workflow

```
chain_prompt
    │
    ▼
chain_generate
    │
    ▼
chain_parse
    │
    ▼
chain_validate

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
java -jar target/llm-chain-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key. When set, `ChainGenerateWorker` calls the real API. When absent, runs in simulated mode. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/llm-chain-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow llm_chain_workflow \
  --version 1 \
  --input '{"customerEmail": "Subject: Frustrated with recent service\n\nI have been a loyal customer for 3 years but the recent downtime has severely impacted our operations. We need well-structured reliability and dedicated support. Please advise on upgrade options.", "productCatalog": "PROD-BASIC-100,PROD-PRO-250,PROD-ENT-500,PROD-SUPPORT-PREM,PROD-SUPPORT-STD"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w llm_chain_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker owns one chain step. Swap in a real LLM for generation, connect your product catalog for validation, customize the prompt template for your domain, and the prompt-generate-parse-validate chain runs unchanged.

- **ChainPromptWorker** (`chain_prompt`): customize the prompt template for your specific domain (e.g., invoice processing, support ticket routing, lead qualification)
- **ChainGenerateWorker** (`chain_generate`): swap in a real LLM call to OpenAI, Claude, or Gemini with appropriate temperature and token settings
- **ChainParseWorker** (`chain_parse`): replace with real JSON/XML parsing with error recovery for malformed LLM output
- **ChainValidateWorker** (`chain_validate`): integrate with your product catalog, CRM, or inventory system for business rule validation

Each worker's input/output shape is fixed. Swap the LLM provider, add new validation rules, or change the prompt template without altering the chain structure.

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
llm-chain/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/llmchain/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LlmChainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ChainGenerateWorker.java
│       ├── ChainParseWorker.java
│       ├── ChainPromptWorker.java
│       └── ChainValidateWorker.java
└── src/test/java/llmchain/workers/
    ├── ChainGenerateWorkerTest.java        # 5 tests
    ├── ChainParseWorkerTest.java        # 4 tests
    ├── ChainPromptWorkerTest.java        # 5 tests
    └── ChainValidateWorkerTest.java        # 8 tests

```
