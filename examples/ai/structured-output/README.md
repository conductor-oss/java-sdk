# Structured Output in Java Using Conductor: Generate JSON, Validate Schema, Transform

The LLM returns beautiful prose when you need a JSON object. You parse it, it breaks, missing closing brace. You add "return JSON only" to the prompt, and it works 80% of the time; the other 20% it wraps the JSON in a markdown code fence, or omits a required field, or returns `"employees": "five hundred"` instead of a number. This example builds a generate-validate-transform pipeline using [Conductor](https://github.com/conductor-oss/conductor) that catches every class of structural failure, malformed JSON, missing required fields, wrong types, and separates each concern into an independently retryable step.

## Getting Reliable JSON from LLMs

LLMs generate text, but applications need structured data. JSON with specific fields, types, and constraints. Even with careful prompting, LLMs sometimes produce invalid JSON (missing closing braces), missing required fields, or fields with wrong types (string instead of number). A three-step pipeline catches these errors: generate the JSON, validate it against a schema, and transform it into the application's expected format.

If the LLM generates invalid JSON, the validation step catches it. If a required field is missing, schema validation fails. Separating generation from validation means you can retry the LLM call (which is non-deterministic) without re-running validation, or adjust the schema without changing the generation prompt.

## The Solution

**You write the JSON generation, schema validation, and transformation logic. Conductor handles the pipeline, retries, and observability.**

Each stage is an independent worker. JSON generation (LLM call with structured output instructions), schema validation (checking required fields, types, and constraints), and transformation (reshaping into the target format). Conductor sequences them, retries the LLM call if it produces invalid JSON, and tracks every execution with the raw output, validation results, and transformed data.

### What You Write: Workers

Three workers form the structured output pipeline. JSON generation from entity descriptions, schema validation against required fields and types, and transformation into the application's expected format.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateJsonWorker** | `so_generate_json` | Generates a structured JSON object for the given entity. Calls OpenAI API in live mode, returns deterministic output in demo mode. |
| **ValidateSchemaWorker** | `so_validate_schema` | Validates the generated JSON against the schema: checks that all required fields exist and that their types match (string, number). Returns a `valid` boolean, a list of errors, and the validated data | Processing only |
| **TransformWorker** | `so_transform` | Enriches the validated data by adding metadata fields (`_validated: true`, `_timestamp`) to produce the final application-ready JSON | Processing only |

**Live vs Demo mode:** When `CONDUCTOR_OPENAI_API_KEY` is set, `GenerateJsonWorker` calls the OpenAI Chat Completions API (model: `gpt-4o-mini`) to generate structured JSON. Without the key, it runs in demo mode with deterministic output where string fields are prefixed with ``. Non-LLM workers (schema validation, transformation) always run their real logic.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
