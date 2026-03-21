# Google Gemini in Java Using Conductor: Structured Gemini API Calls with Content Preparation and Output Formatting

Your team picks Gemini for multimodal tasks, but the API latency varies wildly: sometimes 2 seconds, sometimes 30, and your users stare at a spinner while you have no idea if it's a quota issue, a cold start, or a malformed request. A rate-limit 429 loses the parts-based content you just assembled, and there's zero visibility into token consumption across calls. This example builds a structured Gemini pipeline using [Conductor](https://github.com/conductor-oss/conductor), content preparation, API invocation, and response formatting, so every call is retryable, observable, and decoupled from the request-building logic.

## Calling Gemini in Production

Google Gemini's API has a specific request format. Content must be structured as parts, generation config specifies temperature/topK/topP/maxOutputTokens, and safety settings control content filtering. The response comes back as candidates with safety ratings and usage metadata that need to be extracted and formatted for downstream consumers.

When you embed all of this in a single method: building the request body, making the HTTP call, parsing the nested candidate structure, you end up with tightly coupled code where changing the prompt format means touching API call logic, a rate-limit error loses the prepared request, and there's no record of token consumption across calls. Retrying a failed API call means re-building the entire request from scratch.

## The Solution

**You write the Gemini content preparation and output formatting logic. Conductor handles the API invocation pipeline, durability, and observability.**

Each concern is an independent worker. Content preparation (building Gemini's parts-based request with generation config), API invocation (the actual generateContent call), and output formatting (extracting text from the candidate structure with usage metadata). Conductor chains them so the prepared request body feeds into the API call, and the raw candidate response feeds into the formatter. In this example the task defs are configured to fail fast on provider errors, which makes quota and model-access problems obvious during local validation. Every call records the prompt, model, token usage, and formatted output.

### What You Write: Workers

Three workers handle the Gemini integration. Preparing parts-based content with safety settings, calling the Gemini API, and formatting the candidate response with token counts and finish reason.

| Worker | Task | What It Does |
|---|---|---|
| **GeminiFormatOutputWorker** | `gemini_format_output` | Extracts the generated text from Gemini's candidate structure and returns it as formattedResult. |
| **GeminiGenerateWorker** | `gemini_generate` | Calls the Google Gemini generateContent REST API using the configured model (`GEMINI_MODEL`, default `gemini-2.5-flash`). Falls back to a demo response when GOOGLE_API_KEY is not set. |
| **GeminiPrepareContentWorker** | `gemini_prepare_content` | Prepares a Gemini-style request body with parts-based content structure, generation config, and safety settings. |

When `GOOGLE_API_KEY` is set and has active billing/quota for the Gemini API, `GeminiGenerateWorker` makes a real HTTP call to the Gemini REST API using `java.net.http.HttpClient` (built into Java 21). The model comes from `GEMINI_MODEL` by default and can also be overridden via workflow input. If your API key lacks quota (HTTP 429), the worker fails fast with a clear terminal message and includes the `Retry-After` value when Google returns one. When the key is absent, it falls back to a demo response (prefixed with ``) so the workflow runs end-to-end without credentials.

### The Workflow

```
gemini_prepare_content
 │
 ▼
gemini_generate
 │
 ▼
gemini_format_output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
