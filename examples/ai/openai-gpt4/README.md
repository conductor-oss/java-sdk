# OpenAI GPT-4 Chat Completion Pipeline

You need GPT-4 in a production workflow, but prompt construction, HTTP calls, and response parsing are tangled in one method. This three-step pipeline separates request building (with system message support), API invocation (via a shared `OpenAiService`), and result extraction.

## Workflow

```
prompt, systemMessage, model
       │
       ▼
┌──────────────────────┐
│ gpt4_build_request   │  Build messages array with system + user
└──────────┬───────────┘
           │  requestBody {model, messages, max_tokens, temperature}
           ▼
┌──────────────────────┐
│ gpt4_call_api        │  Call OpenAI Chat Completions
└──────────┬───────────┘
           │  apiResponse {choices, usage}
           ▼
┌──────────────────────┐
│ gpt4_extract_result  │  Extract text from choices[0]
└──────────────────────┘
           │
           ▼
     result, model, usage
```

## Workers

**Gpt4BuildRequestWorker** (`gpt4_build_request`) -- Constructs a `messages` array with a system message and user message as `List.of(systemMsg, userMsg)`. Defaults model to `gpt-4o-mini` via `configuredDefaultModel()` which checks the `OPENAI_MODEL` env var. Coerces `max_tokens` and `temperature` from workflow input.

**Gpt4CallApiWorker** (`gpt4_call_api`) -- Uses a shared `OpenAiService` class for the HTTP call. Extracts the model name from `requestBody.getOrDefault("model", DEFAULT_MODEL)`. Constructs the `apiResponse` with a `choices` list containing the response.

**Gpt4ExtractResultWorker** (`gpt4_extract_result`) -- Pulls the generated text from the API response structure and returns it as the final result.

## Tests

13 tests cover request building with system messages, API calling, and result extraction.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
