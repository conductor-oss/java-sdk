# AI Prompt Engineering in Java Using Conductor :  Define Task, Generate Variants, Test, Evaluate, Select Best

A Java Conductor workflow that automates prompt optimization. defining the task and evaluation criteria, generating multiple prompt variants, testing each variant against a benchmark, evaluating results against criteria, and selecting the best-performing prompt. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the five-stage prompt engineering pipeline as independent workers,  you write the prompt generation and evaluation logic, Conductor handles sequencing, retries, durability, and observability.

## Finding the Best Prompt Through Systematic Testing

Prompt engineering by trial and error is slow and unreliable. "Summarize this document" might work, but "You are an expert technical writer. Read the following document carefully and produce a concise summary that captures the key findings, methodology, and conclusions in 3-4 sentences" might work much better. Without systematic testing, you'll never know.

Automated prompt engineering generates multiple variants (different system prompts, instruction styles, few-shot examples, output format specifications), tests each against a standardized benchmark with consistent inputs, evaluates the outputs on quality metrics (accuracy, relevance, format compliance), and selects the variant that scores highest. This is the scientific method applied to prompt design.

## The Solution

**You just write the task definition, prompt variant generation, benchmark testing, evaluation scoring, and best-prompt selection logic. Conductor handles variant testing orchestration, score aggregation, and complete prompt iteration history.**

`DefineTaskWorker` establishes the task description, evaluation criteria (accuracy, format compliance, relevance), and test inputs. `GeneratePromptsWorker` creates multiple prompt variants. varying instruction style, system prompt, few-shot examples, and output format. `TestVariantsWorker` runs each variant against the test inputs and collects outputs. `EvaluateWorker` scores each variant's outputs against the evaluation criteria. `SelectBestWorker` picks the highest-scoring prompt variant and produces the final prompt with its evaluation metrics. Conductor records every variant and its scores for prompt iteration history.

### What You Write: Workers

Workers for variant generation, benchmark testing, and scoring operate independently, letting you iterate on prompt strategies without touching the evaluation logic.

| Worker | Task | What It Does |
|---|---|---|
| **DefineTaskWorker** | `ape_define_task` | Establishes the task specification from the description. defines input format, expected output, and evaluation criteria |
| **EvaluateWorker** | `ape_evaluate` | Ranked by quality. P3 leads with 0.91 |
| **GeneratePromptsWorker** | `ape_generate_prompts` | 5 prompt variants generated |
| **SelectBestWorker** | `ape_select_best` | Best prompt: P3 (score: 0.91) |

Workers implement AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode. the generation workflow stays the same.

### The Workflow

```
ape_define_task
    │
    ▼
ape_generate_prompts
    │
    ▼
ape_test_variants
    │
    ▼
ape_evaluate
    │
    ▼
ape_select_best

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
java -jar target/ai-prompt-engineering-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for live prompt engineering (optional. falls back to demo) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/ai-prompt-engineering-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ape_prompt_engineering \
  --version 1 \
  --input '{"taskDescription": "sample-taskDescription", "modelId": "TEST-001", "evaluationCriteria": "sample-evaluationCriteria"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ape_prompt_engineering -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real prompt optimization stack. an LLM for variant generation, your benchmark dataset for testing, automated metrics (ROUGE, exact match) or LLM-as-judge for evaluation, and the workflow runs identically in production.

- **GeneratePromptsWorker** (`ape_generate_prompts`): use an LLM to generate diverse prompt variants, or implement DSPy-style automatic prompt optimization with gradient-free search
- **TestVariantsWorker** (`ape_test_variants`): run variants against a curated benchmark dataset with known-good answers for objective evaluation, or use A/B testing in production with real user queries
- **EvaluateWorker** (`ape_evaluate`): implement LLM-as-judge evaluation using a separate model, or use automated metrics (ROUGE for summarization, exact match for extraction, format regex for structured output)

Upgrade your LLM provider or scoring criteria and the pipeline adapts seamlessly.

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
ai-prompt-engineering-ai-prompt-engineering/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/aipromptengineering/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AiPromptEngineeringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DefineTaskWorker.java
│       ├── EvaluateWorker.java
│       ├── GeneratePromptsWorker.java
│       └── SelectBestWorker.java
└── src/test/java/aipromptengineering/workers/
    ├── DefineTaskWorkerTest.java        # 1 tests
    └── SelectBestWorkerTest.java        # 1 tests

```
