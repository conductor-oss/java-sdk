# AI Guardrails in Java Using Conductor :  Input Check, Content Filter, Generate, Output Check, Deliver

A Java Conductor workflow that wraps AI generation with safety guardrails .  checking the user's prompt for policy violations, filtering content based on sensitivity rules, generating the response, checking the output for harmful or inappropriate content, and delivering only safe, compliant responses. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the five-stage guarded generation pipeline as independent workers ,  you write the safety logic, Conductor handles sequencing, retries, durability, and observability for free.

## AI Models Need Safety Boundaries on Input and Output

An LLM without guardrails can be prompted to generate harmful content (instructions for dangerous activities), leak sensitive information (system prompts, training data), produce biased or discriminatory text, or violate content policies (explicit content, copyright infringement). Guardrails on input catch malicious prompts before they reach the model. Guardrails on output catch harmful responses before they reach the user.

Input guardrails detect prompt injection, jailbreak attempts, and policy-violating requests. Content filtering applies sensitivity rules (PII detection, topic restrictions). Output guardrails check for harmful content, factual claims that need verification, and compliance violations. Both sides need independent checking because a safe-looking prompt can produce harmful output, and a suspicious-looking prompt might produce perfectly safe output.

## The Solution

**You just write the input safety checking, content filtering, AI generation, output validation, and safe delivery logic. Conductor handles safety check sequencing, generation retries, and complete content audit trails.**

`InputCheckWorker` scans the user prompt for policy violations .  jailbreak patterns, prompt injection attempts, restricted topics, and PII in the input. `ContentFilterWorker` applies sensitivity rules ,  topic restrictions, user-tier-based access controls, and content category filtering. `GenerateWorker` produces the AI response from the filtered prompt. `OutputCheckWorker` scans the generated response for harmful content, PII leakage, bias indicators, and compliance violations. `DeliverWorker` delivers the response only if both input and output checks pass. Conductor records every guardrail decision for safety auditing.

### What You Write: Workers

Safety checks run as independent workers before and after generation, so guardrail logic stays decoupled from the AI model itself.

| Worker | Task | What It Does |
|---|---|---|
| **ContentFilterWorker** | `grl_content_filter` | Applies content sensitivity rules .  checks for harmful topics, restricted categories, and policy violations |
| **DeliverWorker** | `grl_deliver` | Delivers the safe, validated response to the user after all guardrail checks pass |
| **GenerateWorker** | `grl_generate` | Generates the AI response from the filtered prompt using the specified model |
| **InputCheckWorker** | `grl_input_check` | Scans the user prompt for safety .  checks for PII, prompt injection attempts, and policy violations |
| **OutputCheckWorker** | `grl_output_check` | Validates the generated response .  checks toxicity (0.01), hallucination (0.03), and content safety |

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
grl_input_check
    │
    ▼
grl_content_filter
    │
    ▼
grl_generate
    │
    ▼
grl_output_check
    │
    ▼
grl_deliver
```

## Example Output

```
=== Example 803: AI Guardrails. Input Check, Content Filter, Generate, Output Check, Deliver ===

Step 1: Registering task definitions...
  Registered: grl_input_check, grl_content_filter, grl_generate, grl_output_check, grl_deliver

Step 2: Registering workflow 'grl_ai_guardrails'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [filter] Response from OpenAI (LIVE)
  [deliver] Processing
  [generate] Response from OpenAI (LIVE)
  [input] Response from OpenAI (LIVE)
  [output] Response from OpenAI (LIVE)

  Status: COMPLETED
  Output: {filteredPrompt=..., flagged=..., filterDetails=..., delivered=...}

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
java -jar target/ai-guardrails-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for live AI guardrails (optional .  falls back to simulated) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/ai-guardrails-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow grl_ai_guardrails \
  --version 1 \
  --input '{"userPrompt": "Explain what machine learning is", "Explain what machine learning is": "userId", "userId": "USR-803", "USR-803": "modelId", "modelId": "gpt-4", "gpt-4": "sample-gpt-4"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w grl_ai_guardrails -s COMPLETED -c 5
```

## How to Extend

Swap each worker for your real safety tools. Guardrails AI or LlamaGuard for input checking, Perspective API for toxicity scoring, Presidio for PII detection, and the workflow runs identically in production.

- **InputCheckWorker** (`grl_input_check`): integrate with Guardrails AI, LlamaGuard, or OpenAI's Moderation API for prompt injection and jailbreak detection
- **OutputCheckWorker** (`grl_output_check`): use Perspective API for toxicity scoring, custom classifiers for domain-specific content policies, and PII detection models (Presidio, AWS Comprehend) for data leakage prevention
- **ContentFilterWorker** (`grl_content_filter`): implement role-based content policies: different safety thresholds for internal vs: external users, and topic restrictions configurable per deployment

Update safety filters or generation models independently, each worker's contract remains stable.

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
ai-guardrails-ai-guardrails/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/aiguardrails/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AiGuardrailsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ContentFilterWorker.java
│       ├── DeliverWorker.java
│       ├── GenerateWorker.java
│       ├── InputCheckWorker.java
│       └── OutputCheckWorker.java
└── src/test/java/aiguardrails/workers/
    ├── InputCheckWorkerTest.java        # 1 tests
    └── OutputCheckWorkerTest.java        # 1 tests
```
