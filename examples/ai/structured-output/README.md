# Structured Output in Java Using Conductor -- Generate JSON, Validate Schema, Transform

The LLM returns beautiful prose when you need a JSON object. You parse it, it breaks -- missing closing brace. You add "return JSON only" to the prompt, and it works 80% of the time; the other 20% it wraps the JSON in a markdown code fence, or omits a required field, or returns `"employees": "five hundred"` instead of a number. This example builds a generate-validate-transform pipeline using [Conductor](https://github.com/conductor-oss/conductor) that catches every class of structural failure -- malformed JSON, missing required fields, wrong types -- and separates each concern into an independently retryable step.

## Getting Reliable JSON from LLMs

LLMs generate text, but applications need structured data -- JSON with specific fields, types, and constraints. Even with careful prompting, LLMs sometimes produce invalid JSON (missing closing braces), missing required fields, or fields with wrong types (string instead of number). A three-step pipeline catches these errors: generate the JSON, validate it against a schema, and transform it into the application's expected format.

If the LLM generates invalid JSON, the validation step catches it. If a required field is missing, schema validation fails. Separating generation from validation means you can retry the LLM call (which is non-deterministic) without re-running validation, or adjust the schema without changing the generation prompt.

## The Solution

**You write the JSON generation, schema validation, and transformation logic. Conductor handles the pipeline, retries, and observability.**

Each stage is an independent worker -- JSON generation (LLM call with structured output instructions), schema validation (checking required fields, types, and constraints), and transformation (reshaping into the target format). Conductor sequences them, retries the LLM call if it produces invalid JSON, and tracks every execution with the raw output, validation results, and transformed data.

### What You Write: Workers

Three workers form the structured output pipeline -- JSON generation from entity descriptions, schema validation against required fields and types, and transformation into the application's expected format.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **GenerateJsonWorker** | `so_generate_json` | Generates a structured JSON object for the given entity. Calls OpenAI API in live mode, returns deterministic output in simulated mode. | Live (with `CONDUCTOR_OPENAI_API_KEY`) / Simulated |
| **ValidateSchemaWorker** | `so_validate_schema` | Validates the generated JSON against the schema: checks that all required fields exist and that their types match (string, number). Returns a `valid` boolean, a list of errors, and the validated data | Processing only |
| **TransformWorker** | `so_transform` | Enriches the validated data by adding metadata fields (`_validated: true`, `_timestamp`) to produce the final application-ready JSON | Processing only |

**Live vs Simulated mode:** When `CONDUCTOR_OPENAI_API_KEY` is set, `GenerateJsonWorker` calls the OpenAI Chat Completions API (model: `gpt-4o-mini`) to generate structured JSON. Without the key, it runs in simulated mode with deterministic output where string fields are prefixed with ``. Non-LLM workers (schema validation, transformation) always run their real logic.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically -- configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status -- no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
so_generate_json
    │
    ▼
so_validate_schema
    │
    ▼
so_transform
```

## Running It

### Prerequisites

- **Java 21+** -- verify with `java -version`
- **Maven 3.8+** -- verify with `mvn -version`
- **Docker** -- to run Conductor

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
java -jar target/structured-output-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key. When set, `GenerateJsonWorker` calls the real API. When absent, runs in simulated mode. |

## Example Output

```
=== Structured Output Demo: Generate, Validate, Transform ===

Step 1: Registering task definitions...
  Registered: so_generate_json, so_validate_schema, so_transform

Step 2: Registering workflow 'structured_output_workflow'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  [so_generate_json] Generating JSON for entity: company, fields: [name, industry, founded, employees]
  [so_validate_schema] Validating data against schema...
  [so_transform] Transforming data, validated=true

  Workflow ID: c3d4e5f6-...

Step 5: Waiting for completion...

  Status: COMPLETED
  Output: {finalResult=Success}

Result: PASSED
```
## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/structured-output-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow structured_output_workflow \
  --version 1 \
  --input '{"entity": "company", "fields": "name,industry,founded,employees"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w structured_output_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker owns one stage of the structured output pipeline -- swap in GPT-4 JSON mode or Claude tool use for generation, add a full JSON Schema validator, customize the transformation for your data model, and the validation workflow runs unchanged.

- **GenerateJsonWorker** (`so_generate_json`) -- call Claude or GPT-4 with JSON-mode enabled (OpenAI `response_format: { type: "json_object" }` or Anthropic tool use with a JSON schema). Pass the entity description and field specifications in the prompt, and parse the structured JSON from the response
- **ValidateSchemaWorker** (`so_validate_schema`) -- the validation logic is already deterministic and production-ready. Enhance it with a full JSON Schema validator (everit-org/json-schema or networknt/json-schema-validator) for richer constraints (enums, min/max, regex patterns, nested objects)
- **TransformWorker** (`so_transform`) -- customize the transformation to match your application's expected format: map field names, convert types, flatten nested objects, or enrich with data from external APIs (Clearbit for company enrichment, FullContact for contact data)
- **Add a retry-on-invalid loop** -- if the schema validation fails, use a Conductor DO_WHILE task to re-generate with the validation errors included in the prompt, giving the LLM a chance to self-correct

The output contract is fixed at each stage -- switch from simulated generation to GPT-4 JSON mode, or swap in a full JSON Schema validator, without touching the workflow definition.

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
structured-output/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/structuredoutput/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── StructuredOutputExample.java # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GenerateJsonWorker.java      # LLM JSON generation with schema
│       ├── TransformWorker.java         # Enrich validated data with metadata
│       └── ValidateSchemaWorker.java    # Schema validation (required fields, types)
└── src/test/java/structuredoutput/workers/
    ├── GenerateJsonWorkerTest.java      # Tests -- JSON structure, schema shape
    ├── TransformWorkerTest.java         # Tests -- enrichment fields, data pass-through
    └── ValidateSchemaWorkerTest.java    # 4 tests -- valid data, missing fields, type mismatch
```
