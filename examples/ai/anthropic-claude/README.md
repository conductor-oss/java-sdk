# Anthropic Claude Integration in Java Using Conductor: Build Messages, Call API, Process Response

You need Claude for long-context analysis in a production pipeline: security audits, document review, code analysis; but calling the Messages API directly means no retries on 429 rate limits, no fallback when the model is overloaded, and no centralized record of prompts, responses, or token spend. A single timeout loses your carefully constructed system prompt. This example builds a three-step Claude integration using [Conductor](https://github.com/conductor-oss/conductor), message construction, API invocation, and response processing, so every call is durable, observable, and independently retryable.

## Integrating Claude into Production Pipelines

Claude's Messages API is straightforward for a single call. Construct the messages, POST to the endpoint, read the response. But production usage means managing the system prompt separately from user messages, handling rate limits (429 errors) with exponential backoff, extracting the assistant's response from the structured JSON output, tracking token usage for cost allocation, and logging every prompt/response pair for evaluation and debugging.

Without orchestration, prompt construction, API calling, error handling, and response parsing are all mixed together. When you want to add prompt caching, swap models (claude-3-opus to claude-3-haiku for cost savings), or add response post-processing, you're editing a monolithic method.

## The Solution

**You write the message construction and Claude API call logic. Conductor handles the pipeline, durability, and observability.**

`ClaudeBuildMessagesWorker` constructs the messages array from the system prompt and user message, applying the correct format for the Claude Messages API (role-based messages with system as a top-level parameter). `ClaudeCallApiWorker` sends the request to the Claude API and surfaces live-mode 4xx/429 failures immediately. `ClaudeProcessResponseWorker` extracts the generated text from the response, along with token usage (input_tokens, output_tokens) and stop reason. Conductor preserves each task boundary, records the prompt and response payloads, and lets you raise the retry count later if you want automatic replays in production.

### What You Write: Workers

Three workers isolate each phase of the Claude integration. Message construction with system prompts, API invocation with rate-limit handling, and content-block response processing with token tracking.

| Worker | Task | What It Does |
|---|---|---|
| **ClaudeBuildMessagesWorker** | `claude_build_messages` | Builds the Claude Messages API request body, including Claude's top-level `system` field plus the user message array. |
| **ClaudeCallApiWorker** | `claude_call_api` | Calls the Anthropic Claude Messages API. When `CONDUCTOR_ANTHROPIC_API_KEY` is set, makes a real HTTP call using `java.net.http.HttpClient`; otherwise returns a deterministic demo response. |
| **ClaudeProcessResponseWorker** | `claude_process_response` | Processes Claude's content-block response, filters `text` blocks, and returns the final analysis plus usage metadata. |

**Live vs Demo mode:** The `ClaudeCallApiWorker` auto-detects the `CONDUCTOR_ANTHROPIC_API_KEY` environment variable at startup. When the key is present and has access to the configured model (`ANTHROPIC_MODEL`, default `claude-sonnet-4-20250514`), it makes real HTTP calls to `https://api.anthropic.com/v1/messages` using `java.net.http.HttpClient` (built into Java 21) and parses the response with Jackson. When the key is absent, it returns a deterministic demo response so the workflow runs end-to-end without credentials. Live-mode 4xx failures are surfaced immediately with the failed task name plus a truncated response body so you can diagnose model-access and request-shape issues quickly.

### The Workflow

```
claude_build_messages
 │
 ▼
claude_call_api
 │
 ▼
claude_process_response

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
