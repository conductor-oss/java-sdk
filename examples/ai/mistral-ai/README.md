# Mistral AI in Java Using Conductor :  Document Q&A via Mistral Chat Completions

A Java Conductor workflow that orchestrates Mistral AI chat completion calls for document-based question answering .  composing a chat request with a document as context and a user question, calling the Mistral chat API (with configurable model, temperature, max tokens, and safe prompt settings), and extracting the answer from the response. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate request composition, API invocation, and answer extraction as independent workers ,  you write the Mistral-specific logic, Conductor handles retries, durability, and observability.

## Structured Mistral API Integration

Mistral's chat completion API uses a messages-based format with system and user roles, supports safe prompt mode for content moderation, and returns responses with usage metadata. When you embed document context into a chat completion call, the request composition (building the messages array with the document as context and the question as the user message) should be separate from the API call itself, and the answer extraction (pulling text from the choices array) should be decoupled from both.

Bundling request composition, API calls, and response parsing into a single method means changing the prompt format requires touching API code, a rate-limit error loses the composed request, and there's no record of which model/temperature combination produced which answer.

## The Solution

**You write the Mistral request composition and answer extraction logic. Conductor handles the API pipeline, retries, and observability.**

Each concern is an independent worker .  composing the Mistral chat request (messages array, generation config, safety settings), calling the chat completion API, and extracting the answer from the response. Conductor chains them so the composed request feeds into the API call, and the raw response feeds into the extractor. If Mistral rate-limits the call, Conductor retries it without re-composing the request.

### What You Write: Workers

Three workers manage the Mistral integration .  composing the chat request with system and user messages, calling the Mistral Chat API, and extracting the answer from the choices array.

| Worker | Task | What It Does |
|---|---|---|
| **MistralChatWorker** | `mistral_chat` | Simulates calling the Mistral chat completion API. In production this would make an HTTP call to the Mistral API. Her... |
| **MistralComposeRequestWorker** | `mistral_compose_request` | Composes a Mistral chat completion request body from workflow inputs. Inputs: document, question, model, temperature,... |
| **MistralExtractAnswerWorker** | `mistral_extract_answer` | Extracts the assistant's answer from a Mistral chat completion response. |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### The Workflow

```
mistral_compose_request
    │
    ▼
mistral_chat
    │
    ▼
mistral_extract_answer

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
java -jar target/mistral-ai-1.0.0.jar

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
| `MISTRAL_API_KEY` | _(none)_ | Mistral API key. When set, `MistralChatWorker` calls the real Mistral Chat Completions API. When unset, returns simulated responses with `[SIMULATED]` prefix. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/mistral-ai-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow mistral_ai_workflow \
  --version 1 \
  --input '{"input": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w mistral_ai_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one phase of the Mistral integration .  swap in real HTTP calls to `api.mistral.ai/v1/chat/completions` with your API key, customize system prompts and safety settings, and the compose-call-extract pipeline runs unchanged.

- **MistralChatWorker** (`mistral_chat`): swap in a real HTTP call to `https://api.mistral.ai/v1/chat/completions` with your API key
- **MistralComposeRequestWorker** (`mistral_compose_request`): customize the system prompt, add few-shot examples, or support multi-turn conversations
- **MistralExtractAnswerWorker** (`mistral_extract_answer`): add structured output parsing, confidence scoring, or citation extraction from the response

The request-in, answer-out contract is fixed at each stage .  switch Mistral models, adjust temperature, or add tool use without modifying the workflow.

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
mistral-ai/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/mistralai/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MistralAiExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MistralChatWorker.java
│       ├── MistralComposeRequestWorker.java
│       └── MistralExtractAnswerWorker.java
└── src/test/java/mistralai/workers/
    ├── MistralChatWorkerTest.java        # 4 tests
    ├── MistralComposeRequestWorkerTest.java        # 4 tests
    └── MistralExtractAnswerWorkerTest.java        # 4 tests

```
