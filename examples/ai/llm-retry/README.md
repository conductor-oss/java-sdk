# LLM Retry in Java Using Conductor : Exponential Backoff for Transient API Failures

## LLM APIs Fail Transiently : A Lot

LLM APIs are notorious for transient failures. OpenAI returns 429 (rate limited) when you exceed your tokens-per-minute quota. Anthropic returns 529 (overloaded) during peak usage. Google Gemini times out when model loading takes too long. These failures are temporary. the same request succeeds seconds later.

Writing retry logic by hand means wrapping every API call in a loop with exponential backoff, tracking attempt counts, handling different error codes, and deciding when to give up. That logic gets duplicated across every LLM call in your codebase, is easy to get wrong (forgetting to cap the backoff, retrying non-retryable errors), and obscures the actual business logic.

## The Solution

The LLM call worker contains only the API call logic. no retry loops, no backoff calculations, no attempt counting. Retry behavior is declared in the workflow definition: `retryCount: 3`, `retryLogic: EXPONENTIAL_BACKOFF`, `retryDelaySeconds: 1`. Conductor handles the rest. Every attempt is tracked with its inputs, outputs, and timing, so you can see exactly how many retries a specific call needed and why each attempt failed.

### What You Write: Workers

Two workers demonstrate retry resilience. an LLM call worker that may fail transiently, and a report worker that records the outcome, with Conductor's built-in exponential backoff handling the retries automatically.

| Worker | Task | What It Does |
|---|---|---|
| **RetryLlmCallWorker** | `retry_llm_call` | LLM API call that fails with 429 rate-limit errors on the first two attempts, then succeeds on the third. Calls OpenAI API in live mode on success attempt. |
| **RetryReportWorker** | `retry_report` | Summarises the retry outcome. Receives the LLM response and the number of attempts it took to succeed. | Processing only |

**Live vs Demo mode:** When `CONDUCTOR_OPENAI_API_KEY` is set, the successful attempt (3rd+) of `RetryLlmCallWorker` calls the OpenAI Chat Completions API (model: `gpt-4o-mini`). Without the key, it runs in demo mode with deterministic output prefixed with `[DEMO]`. The retry simulation (first 2 attempts fail with 429) always runs regardless of mode.

### The Workflow

```
retry_llm_call
 │
 ▼
retry_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
