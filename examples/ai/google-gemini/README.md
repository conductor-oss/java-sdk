# Calling Google Gemini with Structured Content and Safety Settings

Your team picks Gemini for a content generation task, but the API has its own request format: parts-based content structure, generation config with `topK`/`topP` controls, and safety settings per harm category. A rate-limit 429 loses the assembled request body, and without tracking `usageMetadata.totalTokenCount`, you have no cost visibility. This pipeline separates content preparation, API invocation, and output formatting.

## Workflow

```
prompt, context, model
       │
       ▼
┌──────────────────────────┐
│ gemini_prepare_content   │  Build parts + generationConfig + safetySettings
└───────────┬──────────────┘
            │  requestBody, model
            ▼
┌──────────────────────────┐
│ gemini_generate          │  POST to generativelanguage.googleapis.com
└───────────┬──────────────┘
            │  apiResponse (candidates, usageMetadata)
            ▼
┌──────────────────────────┐
│ gemini_format_output     │  Extract text from candidates[0].content.parts[0]
└──────────────────────────┘
            │
            ▼
     result, model, tokens
```

## Workers

**GeminiPrepareContentWorker** (`gemini_prepare_content`) -- Builds the Gemini-specific request body. Combines `context` and `prompt` as `"Context: " + context + "\n\n" + prompt` inside a `contents[0].parts[0].text` structure. Sets `generationConfig` with explicit type coercion: `temperature` via `((Number) ...).doubleValue()`, `topK` via `.intValue()`, `topP` via `.doubleValue()`, `maxOutputTokens` via `.intValue()` -- necessary because Conductor's JSON round-trip can change Integer to Long or Double to BigDecimal. Workflow defaults: `temperature: 0.4`, `topK: 40`, `topP: 0.95`, `maxOutputTokens: 1024`. Adds two `safetySettings` entries blocking `HARM_CATEGORY_HARASSMENT` and `HARM_CATEGORY_DANGEROUS_CONTENT` at `BLOCK_MEDIUM_AND_ABOVE`. Model defaults to `gemini-2.5-flash` via `GEMINI_MODEL` env var fallback.

**GeminiGenerateWorker** (`gemini_generate`) -- Requires `GOOGLE_API_KEY` (throws `IllegalStateException` at construction if missing). POSTs to `https://generativelanguage.googleapis.com/v1beta/models/<model>:generateContent?key=<apiKey>`. On 429 errors, checks the `Retry-After` response header and reports it in the failure reason; marks as `FAILED_WITH_TERMINAL_ERROR`. On 5xx errors, marks as `FAILED` (retryable). Logs `usageMetadata.totalTokenCount` from the response. Truncates error bodies to 220 chars.

**GeminiFormatOutputWorker** (`gemini_format_output`) -- Navigates Gemini's candidate structure: `candidates.get(0).get("content")` -> `content.get("parts")` -> `parts.get(0).get("text")`. Logs the first 50 characters of the formatted result.

## Tests

12 tests cover content preparation with type coercion, API invocation with error handling, and output extraction.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
