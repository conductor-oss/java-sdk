# OpenAI GPT-4 in Java Using Conductor: Chat Completion Pipeline with Request Building and Result Extraction

You want to call GPT-4 from a workflow, but the API times out sometimes, the response format varies between models, retries are manual, and there is no record of which calls consumed how many tokens. Prompt construction, HTTP calls, and response parsing get tangled into one brittle method where changing the system message means touching retry logic. This example builds a clean three-step GPT-4 pipeline using [Conductor](https://github.com/conductor-oss/conductor). Build the request, call the API, extract the result, so each concern is independently testable, retryable, and observable without writing a single line of retry or logging code.

## Structured GPT-4 Integration

OpenAI's chat completion API requires a specific request format. Messages with roles, model selection, temperature, max_tokens, and optional function calling. The response returns a choices array with finish reasons and a usage object tracking prompt and completion tokens. Building the request, making the API call, and parsing the response are three distinct concerns.

When they're combined in a single method, changing the prompt format (adding a system message, switching from GPT-4 to GPT-4 Turbo) means touching API call code. A 429 rate-limit error loses the built request. And there's no centralized record of token usage across calls for cost tracking.

## The Solution

**You write the request construction, API call, and result extraction logic. Conductor handles the pipeline, durability, and observability.**

Each concern is an independent worker. Request building (constructing the messages array with system and user messages, setting model parameters), API invocation (the actual HTTP call to OpenAI), and result extraction (pulling the response text and token usage from the choices array). Conductor chains them, records every call's prompt, response, and token usage, and lets you raise retries later if you want automatic replay behavior in production. In this example, task defs intentionally use `retryCount=0` so live API failures surface immediately while you validate the integration.

### What You Write: Workers

Three workers handle the OpenAI integration. Building the chat completion request with messages and parameters, calling the GPT-4 API, and extracting the response text with usage statistics.

| Worker | Task | What It Does |
|---|---|---|
| **Gpt4BuildRequestWorker** | `gpt4_build_request` | Builds an OpenAI chat completion request body from workflow input parameters. |
| **Gpt4CallApiWorker** | `gpt4_call_api` | Calls the OpenAI GPT-4 chat completion API, or returns a demo response when no API key is set. |
| **Gpt4ExtractResultWorker** | `gpt4_extract_result` | Extracts the assistant's response content from the GPT-4 API response. |

Without `CONDUCTOR_OPENAI_API_KEY`, the `Gpt4CallApiWorker` runs in demo mode with `` output. Set the environment variable to make real OpenAI API calls. The default chat model is `gpt-4o-mini`, configurable through `OPENAI_CHAT_MODEL` or workflow input `model`, and the worker interface stays the same.

### The Workflow

```
gpt4_build_request
 │
 ▼
gpt4_call_api
 │
 ▼
gpt4_extract_result

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
