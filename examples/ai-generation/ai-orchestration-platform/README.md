# AI Orchestration Platform in Java Using Conductor :  Receive, Route to Model, Execute, Validate, Respond

A Java Conductor workflow that acts as an AI request gateway .  receiving incoming AI requests, routing each to the appropriate model based on request type and priority, executing the model inference, validating the response quality, and returning the result. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate request routing, model execution, and validation as independent workers ,  you write the routing and model integration logic, Conductor handles sequencing, retries, durability, and observability for free.

## Routing AI Requests to the Right Model

An organization running multiple AI models (GPT-4 for complex reasoning, Claude for long documents, a fine-tuned model for domain-specific tasks, a local model for sensitive data) needs a routing layer that sends each request to the right model. A summarization request goes to the model best at summarization. A coding request goes to the model best at code. A request involving PII goes to the on-premises model.

The routing decision depends on request type, data sensitivity, model availability, cost constraints, and priority level. After model execution, the response needs quality validation before returning to the caller .  catching model hallucinations, format violations, and incomplete responses. Without orchestration, this routing becomes hardcoded conditional logic that's impossible to update when you add new models or change routing rules.

## The Solution

**You just write the request intake, model routing, inference execution, response validation, and delivery logic. Conductor handles model routing, inference retries, and end-to-end request tracking across providers.**

`ReceiveRequestWorker` ingests the AI request and extracts its type, priority, and data sensitivity classification. `RouteModelWorker` selects the best model based on request type, sensitivity constraints, model availability, and cost/priority trade-offs. `ExecuteWorker` sends the request to the selected model and captures the response with usage metadata. `ValidateWorker` checks response quality .  format compliance, completeness, and confidence thresholds. `RespondWorker` returns the validated response to the caller with model selection metadata. Conductor tracks which model handled each request type for routing optimization.

### What You Write: Workers

Request routing, model execution, and response validation each run as isolated workers, letting you add new models without changing the orchestration layer.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteWorker** | `aop_execute` | Executes model inference at the selected endpoint, returning the result with latency and token usage metrics |
| **ReceiveRequestWorker** | `aop_receive_request` | Ingests the incoming AI request and assigns a request ID based on type and priority |
| **RespondWorker** | `aop_respond` | Sends the orchestrated response back to the requester with a status code and confirmation |
| **RouteModelWorker** | `aop_route_model` | Routes the request to the best model (e.g., text-model-v3) based on request type, selecting the model endpoint with load balancing |
| **ValidateWorker** | `aop_validate` | Validates the model response for coherence, relevance, and safety .  assigns a quality score and safety flag |

Workers simulate AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode .  the generation workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
aop_receive_request
    │
    ▼
aop_route_model
    │
    ▼
aop_execute
    │
    ▼
aop_validate
    │
    ▼
aop_respond
```

## Example Output

```
=== Example 800: AI Orchestration Platform. Receive Request, Route Model, Execute, Validate, Respond ===

Step 1: Registering task definitions...
  Registered: aop_receive_request, aop_route_model, aop_execute, aop_validate, aop_respond

Step 2: Registering workflow 'aop_ai_orchestration_platform'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [execute] Response from OpenAI (LIVE)
  [receive_request] Processing
  [respond] Processing
  [route_model] Processing
  [validate] Response from OpenAI (LIVE)

  Status: COMPLETED
  Output: {result=..., latencyMs=..., tokensUsed=..., requestId=...}

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
java -jar target/ai-orchestration-platform-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for live AI execution and validation (optional .  falls back to simulated) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/ai-orchestration-platform-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow aop_ai_orchestration_platform \
  --version 1 \
  --input '{"requestType": "standard", "summarization": "sample-summarization", "payload": "sample-payload", "A long article about renewable energy...": "sample-A long article about renewable energy...", "priority": "sample-priority"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w aop_ai_orchestration_platform -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real AI providers. OpenAI, Anthropic, Google, and AWS Bedrock for multi-model execution, your routing rules engine for model selection, custom validators for response quality gates, and the workflow runs identically in production.

- **RouteModelWorker** (`aop_route_model`): implement intelligent routing based on model benchmarks, current latency/availability metrics from health checks, and cost optimization across providers
- **ExecuteWorker** (`aop_execute`): integrate with multiple providers through a unified interface: OpenAI, Anthropic, Google, AWS Bedrock, and local models via Ollama/vLLM
- **ValidateWorker** (`aop_validate`): implement response quality gates: JSON schema validation for structured output, confidence thresholds for classification, and LLM-as-judge for open-ended generation quality

Add new model backends or validation rules and the routing layer picks them up without code changes.

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
ai-orchestration-platform-ai-orchestration-platform/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/aiorchestrationplatform/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AiOrchestrationPlatformExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExecuteWorker.java
│       ├── ReceiveRequestWorker.java
│       ├── RespondWorker.java
│       ├── RouteModelWorker.java
│       └── ValidateWorker.java
└── src/test/java/aiorchestrationplatform/workers/
    ├── ReceiveRequestWorkerTest.java        # 1 tests
    └── ValidateWorkerTest.java        # 1 tests
```
