# Calling the Claude Messages API with Durable Retries

A production system needs to call Anthropic's Claude API for long-context analysis -- security audits, document review, code analysis -- but a single HTTP timeout or 429 rate-limit response loses the entire request. The system prompt, user message, and API call need to be separated so each step can be retried independently.

## Workflow

```
userMessage, systemPrompt, model
       │
       ▼
┌──────────────────────┐
│ claude_build_messages │  Assemble Messages API request body
└──────────┬───────────┘
           │  requestBody
           ▼
┌──────────────────────┐
│ claude_call_api      │  POST to api.anthropic.com
└──────────┬───────────┘
           │  apiResponse
           ▼
┌──────────────────────┐
│ claude_process_response│  Extract text blocks
└──────────────────────┘
           │
           ▼
    analysis, model, usage, stopReason
```

## Workers

**ClaudeBuildMessagesWorker** (`claude_build_messages`) -- Constructs the Claude Messages API body. Claude requires `system` as a top-level field (not inside `messages`), so this worker builds a `Map.of("model", model, "max_tokens", maxTokens, "temperature", temperature, "system", systemPrompt, "messages", [...])` structure. Explicitly coerces `max_tokens` via `((Number) ...).intValue()` and `temperature` via `.doubleValue()` because Conductor's JSON round-trip can change Integer to Long or Double to BigDecimal, which the Anthropic API rejects with HTTP 400. Defaults to `claude-sonnet-4-20250514` via the `ANTHROPIC_MODEL` env var fallback.

**ClaudeCallApiWorker** (`claude_call_api`) -- Makes a real HTTP POST to `https://api.anthropic.com/v1/messages` using Java's built-in `HttpClient`. Sets headers `x-api-key`, `anthropic-version: 2023-06-01`, and `content-type: application/json`. Requires `CONDUCTOR_ANTHROPIC_API_KEY` at construction time (throws `IllegalStateException` if missing). On HTTP errors, distinguishes retryable failures (429 rate limits, 5xx) from terminal errors (4xx except 429) using `FAILED` vs `FAILED_WITH_TERMINAL_ERROR`. Truncates error bodies to 220 chars via `summarizeBody()`. The workflow sets `max_tokens` to 1024 and `temperature` to 0.5.

**ClaudeProcessResponseWorker** (`claude_process_response`) -- Filters Claude's content blocks by `"text".equals(block.get("type"))`, maps each to its `"text"` field, and joins them with `String.join("\n", textBlocks)`. Logs the count of text blocks and total character length.

## Tests

15 tests across 3 test files cover message construction with type coercion, API call error handling (HTTP 4xx/429/5xx), and response processing.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
