# Amazon Bedrock Integration in Java Using Conductor :  Build Payload, Invoke Model, Parse Output

A Java Conductor workflow example for orchestrating Amazon Bedrock model invocations .  building the request payload with the prompt and use-case-specific parameters, invoking the Bedrock model, and parsing the response into a structured output. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Calling Bedrock Models Reliably in Production

Amazon Bedrock provides access to foundation models from AI21, Anthropic, Cohere, Meta, and Stability AI. But calling `InvokeModel` in production means more than a single API call .  you need to construct the model-specific payload format (Claude uses `messages`, Titan uses `inputText`), handle throttling and quota errors with retry logic, parse the model-specific response format, and log the prompt, response, and latency for cost tracking and debugging.

Without orchestration, the payload construction, API call, and response parsing get tangled in a single method. When you switch from Claude to Titan, you change the payload format and break the parser. When Bedrock throttles you, the retry logic is mixed in with the business logic.

## The Solution

**You write the Bedrock payload construction and response parsing. Conductor handles the invocation pipeline, retries, and observability.**

`BedrockBuildPayloadWorker` constructs the model-specific request body based on the prompt and use case .  selecting the right payload format, temperature, and max tokens for the target model. `BedrockInvokeModelWorker` calls the Bedrock `InvokeModel` API and handles throttling/quota responses. `BedrockParseOutputWorker` extracts the generated text from the model-specific response format and structures it for downstream use. Conductor retries throttled invocations with backoff, and records every prompt, model response, and latency for cost analysis and debugging.

### What You Write: Workers

Three workers separate the Bedrock integration into payload construction, model invocation, and response parsing .  isolating the model-specific formats from the orchestration logic.

| Worker | Task | What It Does |
|---|---|---|
| **BedrockBuildPayloadWorker** | `bedrock_build_payload` | Builds the Bedrock InvokeModel payload for Claude on Bedrock. |
| **BedrockInvokeModelWorker** | `bedrock_invoke_model` | Simulates calling Amazon Bedrock InvokeModel API. In production, this would use the AWS SDK BedrockRuntimeClient to c... |
| **BedrockParseOutputWorker** | `bedrock_parse_output` | Parses the Bedrock response to extract the classification text. |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
bedrock_build_payload
    │
    ▼
bedrock_invoke_model
    │
    ▼
bedrock_parse_output
```

## Example Output

```
=== Example 115: Orchestrating Amazon Bedrock ===

Step 1: Registering task definitions...
  Registered: bedrock_build_payload, bedrock_invoke_model, bedrock_parse_output

Step 2: Registering workflow 'amazon_bedrock_workflow'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [build] Bedrock payload: modelId=
  [bedrock_invoke_model] AWS credentials detected, but live Bedrock calls
  [parse] Classification extracted:

  Status: COMPLETED
  Output: {payload=..., modelId=..., region=..., responseBody=...}

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
java -jar target/amazon-bedrock-1.0.0.jar
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
| `AWS_ACCESS_KEY_ID` | _(none)_ | AWS access key for Bedrock. Live Bedrock calls require AWS SDK v2 with SigV4 signing (not included). Currently always runs in `[SIMULATED]` mode. |
| `AWS_SECRET_ACCESS_KEY` | _(none)_ | AWS secret key for Bedrock. See above. |
| `AWS_REGION` | `us-east-1` | AWS region for Bedrock model invocation. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/amazon-bedrock-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow amazon_bedrock_workflow \
  --version 1 \
  --input '{"prompt": "sample-prompt", "data viewed by unknown party.'": "sample-data viewed by unknown party.'", "useCase": "sample-useCase"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w amazon_bedrock_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one phase of the Bedrock integration .  swap in real AWS SDK `BedrockRuntimeClient` calls with IAM auth, build model-specific payloads for Claude or Titan, and the orchestration pipeline runs unchanged.

- **BedrockBuildPayloadWorker** (`bedrock_build_payload`): build real model-specific payloads: Claude Messages API format for `anthropic.claude-3`, Titan `inputText` format, or Llama 2 chat template with system/user roles
- **BedrockInvokeModelWorker** (`bedrock_invoke_model`): call the real Bedrock API using AWS SDK (`BedrockRuntimeClient.invokeModel()`) with IAM authentication, region selection, and model ID configuration
- **BedrockParseOutputWorker** (`bedrock_parse_output`): parse real model responses: extract `content[0].text` from Claude, `results[0].outputText` from Titan, or `generation` from Llama, handling model-specific response schemas

Each worker's input/output contract stays fixed .  switch between Claude, Titan, or Llama on Bedrock by changing only the payload builder and parser implementations.

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
amazon-bedrock/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/amazonbedrock/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AmazonBedrockExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BedrockBuildPayloadWorker.java
│       ├── BedrockInvokeModelWorker.java
│       └── BedrockParseOutputWorker.java
└── src/test/java/amazonbedrock/workers/
    ├── BedrockBuildPayloadWorkerTest.java        # 4 tests
    ├── BedrockInvokeModelWorkerTest.java        # 5 tests
    └── BedrockParseOutputWorkerTest.java        # 4 tests
```
