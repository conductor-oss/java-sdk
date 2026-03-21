# Prompt Templates in Java Using Conductor :  Versioned Templates, Variable Resolution, and LLM Invocation

A Java Conductor workflow that manages prompt engineering as a first-class concern. resolving a versioned prompt template by ID, substituting variables into the template, calling an LLM with the rendered prompt, and collecting the result with template metadata. This separates prompt management from LLM integration, so you can iterate on prompts without touching API code. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate template resolution, LLM invocation, and result collection as independent workers,  you write the template registry and LLM logic, Conductor handles sequencing, retries, durability, and observability.

## Prompt Engineering Needs Version Control

When prompts are hardcoded in application code, changing a prompt means a code deploy. You can't A/B test prompts, roll back to a previous version, or track which prompt version produced which results. And when multiple applications share the same prompt patterns (summarization, classification, extraction), each one maintains its own copy.

A template system solves this: prompts are stored with IDs and version numbers, variables are resolved at runtime, and every LLM call records which template version was used. You can deploy a new prompt version without touching application code, and you can trace any output back to the exact prompt that produced it.

## The Solution

**You write the template resolution and LLM invocation logic. Conductor handles the versioned pipeline, retries, and observability.**

Each concern is an independent worker. template resolution (looking up the template by ID and version, substituting variables), LLM invocation (calling the model with the rendered prompt), and result collection (packaging the response with template metadata for auditability). Conductor chains them, retries the LLM call on transient errors, and records the template ID, version, rendered prompt, and response for every execution.

### What You Write: Workers

Three workers manage templated LLM calls. resolving a versioned template with variable substitution, calling the LLM with the resolved prompt, and collecting the result with template metadata for tracking.

| Worker | Task | What It Does |
|---|---|---|
| **CallLlmWorker** | `pt_call_llm` | Calls an LLM with a prompt. Uses OpenAI API in live mode, returns deterministic output in demo mode. |
| **CollectWorker** | `pt_collect` | Collects and logs the results of the prompt template pipeline. | Processing only |
| **ResolveTemplateWorker** | `pt_resolve_template` | Resolves a prompt template by substituting variables into placeholders. Template store contains versioned prompt templates. | Processing only |

**Live vs Demo mode:** When `CONDUCTOR_OPENAI_API_KEY` is set, `CallLlmWorker` calls the OpenAI Chat Completions API (model: `gpt-4o-mini`). Without the key, it runs in demo mode with deterministic output prefixed with `[DEMO]`. Non-LLM workers (template resolution, collection) always run their real logic.

### The Workflow

```
pt_resolve_template
    │
    ▼
pt_call_llm
    │
    ▼
pt_collect

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
java -jar target/prompt-templates-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key. When set, `CallLlmWorker` calls the real API. When absent, runs in demo mode. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/prompt-templates-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow prompt_templates_workflow \
  --version 1 \
  --input '{"templateId": "TEST-001", "templateVersion": "1.0", "variables": "sample-variables", "model": "gpt-4o-mini"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w prompt_templates_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one prompt management concern. swap in a database-backed template store for versioning, connect any LLM provider for invocation, log to Weights & Biases for prompt analytics, and the resolve-invoke-collect pipeline runs unchanged.

- **CallLlmWorker** (`pt_call_llm`): call OpenAI, Anthropic Claude, or Google Gemini APIs with the resolved prompt, routing to different models based on template metadata
- **CollectWorker** (`pt_collect`): log prompt version, model response, latency, and token usage to an analytics store (e.g., Datadog, Weights & Biases) for prompt performance tracking
- **ResolveTemplateWorker** (`pt_resolve_template`): load versioned templates from a database or config service (e.g., PostgreSQL, AWS Parameter Store), supporting A/B testing of prompt versions

The template/result contract stays fixed. add new templates, update variable schemas, or swap LLM providers without changing the pipeline structure.

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
prompt-templates/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/prompttemplates/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PromptTemplatesExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CallLlmWorker.java
│       ├── CollectWorker.java
│       └── ResolveTemplateWorker.java
└── src/test/java/prompttemplates/workers/
    ├── CallLlmWorkerTest.java        # 4 tests
    ├── CollectWorkerTest.java        # 3 tests
    └── ResolveTemplateWorkerTest.java        # 6 tests

```
