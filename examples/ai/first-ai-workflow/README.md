# First AI Workflow in Java Using Conductor: Prompt, Call, Parse in Three Steps

Your LLM feature works beautifully in a Jupyter notebook. Then you deploy it. The first OpenAI rate-limit error crashes the process and loses the carefully constructed prompt. Nobody knows how many tokens the batch job burned overnight until the invoice arrives. When you swap from GPT-4 to Claude, you have to rewrite everything because the prompt formatting, API call, and response parsing are tangled in a single function. And when the CEO asks "can we see every question our users asked last week and what it cost?" you realize you have no audit trail at all. This example builds the simplest possible production AI pipeline with Conductor: prepare the prompt, call the LLM (with automatic retries on rate limits), and parse the response: each step independent, observable, and swappable. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the three-step chain as independent workers, you write the prompt formatting, LLM call, and response parsing, Conductor handles retries, durability, and observability for free.

## The Simplest AI Pipeline

Calling an LLM from application code sounds simple. Format a prompt, make an API call, use the response. But even this minimal chain has operational concerns. The prompt template needs to be decoupled from the LLM call so you can change formatting without touching the API integration. The LLM call can fail due to rate limits, timeouts, or transient API errors, and you want automatic retries with backoff, not a crashed process. The raw response needs parsing and validation before it reaches your application. And you want to track token usage across every call for cost monitoring.

When these three steps are tangled in a single method, swapping the LLM provider means rewriting everything, a rate-limit error loses the prepared prompt, and there's no record of which calls consumed how many tokens.

## The Solution

**You write the prompt template, LLM call, and response parser. Conductor handles the chaining, retries, and observability.**

Each step is an independent worker. Prompt preparation, LLM invocation, response parsing. Conductor chains them so the formatted prompt feeds into the LLM call, and the raw response feeds into the parser. If the LLM call is rate-limited, Conductor retries automatically. Every execution records the question, the formatted prompt, the raw response, the parsed answer, and the token usage, giving you a complete audit trail for cost tracking and debugging.

### What You Write: Workers

Three workers form the simplest possible LLM pipeline. Preparing the prompt with system and user messages, calling the model, and parsing the structured response into application-ready fields.

| Worker | Task | What It Does |
|---|---|---|
| **AiCallLlmWorker** | `ai_call_llm` | Worker that simulates an LLM call and returns a fixed response. |
| **AiParseResponseWorker** | `ai_parse_response` | Worker that parses the raw LLM response and validates it. |
| **AiPreparePromptWorker** | `ai_prepare_prompt` | Worker that prepares a formatted prompt from a question and model name. |

Workers run in simulated mode by default, returning realistic outputs so you can run the full pipeline without API keys. Set `CONDUCTOR_OPENAI_API_KEY` to switch to live mode (see Configuration below). The workflow and worker interfaces stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
ai_prepare_prompt
    │
    ▼
ai_call_llm
    │
    ▼
ai_parse_response
```

## Example Output

```
=== First AI Workflow: Chain of AI Workers ===

Step 1: Registering task definitions...
  Registered: ai_prepare_prompt, ai_call_llm, ai_parse_response

Step 2: Registering workflow 'first_ai_workflow'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: 56bd0c02-ae75-9ec5-1c62-19b122dafe72

  [ai_prepare_prompt worker] Prepared prompt for model gpt-4
  [ai_call_llm] Live mode: CONDUCTOR_OPENAI_API_KEY detected
  [ai_call_llm] Simulated mode: set CONDUCTOR_OPENAI_API_KEY for live API calls
  [ai_call_llm worker] Calling gpt-4 (live) with prompt length 35
  [ai_call_llm worker] OpenAI API call completed successfully
  [ai_call_llm worker] Calling gpt-4 with prompt length 35
  [ai_parse_response worker] Parsing response of length 80

  Status: COMPLETED
  Output: {question=What is Orkes Conductor?, answer=Orkes Conductor is a platform for building distributed applications , model=gpt-4, tokens={promptTokens=45, completionTokens=38, totalTokens=83}}

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
java -jar target/first-ai-workflow-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key. When set, `AiCallLlmWorker` calls the real OpenAI Chat Completions API. When unset, returns simulated responses with `` prefix. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/first-ai-workflow-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow first_ai_workflow \
  --version 1 \
  --input '{"question": "What is Orkes Conductor?", "model": "gpt-4"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w first_ai_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker isolates one concern of the LLM call. Swap in any provider (OpenAI, Claude, Gemini, or Ollama) for the API call, customize the prompt formatter, and the three-step workflow runs unchanged.

- **AiPreparePromptWorker** (`ai_prepare_prompt`): add template rendering with Mustache/Jinja-style variables, few-shot examples, or system prompts
- **AiCallLlmWorker** (`ai_call_llm`): swap in a real API call to OpenAI, Anthropic Claude, Google Gemini, or a local Ollama instance
- **AiParseResponseWorker** (`ai_parse_response`): add structured output parsing (JSON extraction, field validation) for your specific use case

The prompt-in, parsed-result-out contract stays fixed. Upgrade to any LLM provider or add response post-processing without changing the workflow definition.

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
first-ai-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/firstaiworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FirstAiWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AiCallLlmWorker.java
│       ├── AiParseResponseWorker.java
│       └── AiPreparePromptWorker.java
└── src/test/java/firstaiworkflow/workers/
    ├── AiCallLlmWorkerTest.java        # 4 tests
    ├── AiParseResponseWorkerTest.java        # 4 tests
    └── AiPreparePromptWorkerTest.java        # 5 tests
```
