# Cohere Marketing Copy Generation in Java Using Conductor :  Build Prompt, Generate Candidates, Select Best

A Java Conductor workflow example for generating marketing copy using Cohere .  building a prompt tailored for marketing content, generating multiple text candidates via the Cohere Generate API, and selecting the best candidate based on quality criteria (engagement, clarity, brand voice). Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## One Generation Is Not Enough for Production Copy

Asking an LLM to write marketing copy once gives you one option, and it might be mediocre. The best practice is to generate multiple candidates with different temperature settings or prompt variations, then select the one that best matches your criteria: engagement score, reading level, brand voice alignment, and call-to-action strength. But generating multiple candidates, scoring each one, and selecting the winner requires coordinating three distinct steps.

Without orchestration, you'd loop through generations in a single function, mix scoring logic with API calls, and have no record of which candidates were generated and why one was selected over the others. When the marketing team asks "why did we pick that version?", you can't answer.

## The Solution

**You write the prompt construction, Cohere API call, and candidate selection logic. Conductor handles the pipeline, retries, and observability.**

`CohereBuildPromptWorker` constructs a prompt tailored for marketing content .  specifying tone, audience, product details, and desired call-to-action. `CohereGenerateWorker` calls the Cohere Generate API to produce multiple text candidates. `CohereSelectBestWorker` evaluates each candidate against quality criteria and selects the winner. Conductor records the prompt, all generated candidates, and the selection rationale ,  so the marketing team can review alternatives and understand why one version was chosen.

### What You Write: Workers

Three workers cover the full copy generation pipeline .  prompt construction with tone and audience parameters, multi-candidate generation via Cohere, and quality-based selection of the best candidate.

| Worker | Task | What It Does |
|---|---|---|
| **CohereBuildPromptWorker** | `cohere_build_prompt` | Builds a Cohere generate request body from workflow input parameters. |
| **CohereGenerateWorker** | `cohere_generate` | Simulates a Cohere API generate call, returning a fixed response with 3 generations. |
| **CohereSelectBestWorker** | `cohere_select_best` | Selects the generation with the highest likelihood (least negative value). |

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
cohere_build_prompt
    │
    ▼
cohere_generate
    │
    ▼
cohere_select_best
```

## Example Output

```
=== Cohere Text Generation Workflow ===

Step 1: Registering task definitions...
  Registered: cohere_build_prompt, cohere_generate, cohere_select_best

Step 2: Registering workflow 'cohere_text_generation'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [cohere_build_prompt worker] Built request for model:
  [cohere_generate] Live mode: COHERE_API_KEY detected
  [cohere_select_best worker] Selected best generation with likelihood:

  Status: COMPLETED
  Output: {model=..., prompt=..., max_tokens=..., temperature=...}

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
java -jar target/cohere-1.0.0.jar
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
| `COHERE_API_KEY` | _(none)_ | Cohere API key. When set, `CohereGenerateWorker` calls the real Cohere Generate API. When unset, returns simulated responses with `[SIMULATED]` prefix. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/cohere-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cohere_text_generation \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cohere_text_generation -s COMPLETED -c 5
```

## How to Extend

Each worker handles one stage of the copy generation pipeline .  swap in the real Cohere Generate API for multi-candidate generation, add readability scoring or Cohere Rerank for selection, and the workflow runs unchanged.

- **CohereGenerateWorker** (`cohere_generate`): call the real Cohere Generate API (`co.generate()`) or Chat API (`co.chat()`) with your API key, generating multiple candidates with different temperature/p values
- **CohereSelectBestWorker** (`cohere_select_best`): implement real selection: use Cohere's Rerank API to score candidates, apply readability metrics (Flesch-Kincaid), or call a separate LLM to judge quality
- **CohereBuildPromptWorker** (`cohere_build_prompt`): load real prompt templates from a database, inject product details from your catalog API, and apply brand voice guidelines from a style configuration

Each worker maintains its output shape, so swapping in the real Cohere API or adding a Rerank-based selection step requires no workflow changes.

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
cohere/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/cohere/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CohereExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CohereBuildPromptWorker.java
│       ├── CohereGenerateWorker.java
│       └── CohereSelectBestWorker.java
└── src/test/java/cohere/workers/
    ├── CohereBuildPromptWorkerTest.java        # 4 tests
    ├── CohereGenerateWorkerTest.java        # 5 tests
    └── CohereSelectBestWorkerTest.java        # 6 tests
```
