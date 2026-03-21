# LLM Fallback Chain in Java Using Conductor: GPT-4, Claude, Gemini with Automatic Provider Failover

GPT-4 returns a 429 and your entire AI feature goes dark. Because you bet everything on a single provider. Claude has a maintenance window the same week. Your users see error pages while three other perfectly capable models sit idle. This example builds an automatic fallback chain using [Conductor](https://github.com/conductor-oss/conductor) that tries GPT-4 first, falls back to Claude on failure, then to Gemini, and reports which model actually served the request, so a single provider outage never takes down your AI feature again.

## LLM Providers Go Down

No single LLM provider has 100% uptime. GPT-4 rate-limits under heavy load. Claude has maintenance windows. Gemini returns 503s during capacity crunches. If your application depends on a single provider, an outage means your users get errors. Even though two other perfectly capable models are available.

A fallback chain tries GPT-4 first (your preferred model). If GPT-4 returns a failure status, the workflow calls Claude. If Claude also fails, it tries Gemini. The response comes from whichever provider succeeds first. A formatting step at the end normalizes the output and records which model was used and how many fallbacks were triggered.

This creates nested conditional logic. Try A, check status, try B on failure, check status, try C on failure. Without orchestration, this becomes a deeply nested try/catch chain where you lose visibility into which provider failed, why it failed, and how often each fallback is triggered.

## The Solution

**You write the API integration for each LLM provider. Conductor handles the failover routing, retries, and observability.**

Each provider is an independent worker. GPT-4, Claude, Gemini, each returning a status and response. Conductor's nested `SWITCH` tasks inspect each status and route to the next provider only when the previous one failed. Every execution records the full fallback path: which providers were tried, which failed, and which ultimately served the request.

### What You Write: Workers

Four workers implement the multi-provider fallback. One per LLM provider (GPT-4, Claude, Gemini) plus a formatter that reports which model served the request and how many failovers occurred.

| Worker | Task | What It Does |
|---|---|---|
| **FbCallGpt4Worker** | `fb_call_gpt4` | Calls GPT-4 (the preferred model). In live mode, calls the OpenAI Chat Completions API. In demo mode, returns a `503 Service Unavailable` failure to trigger the fallback chain |
| **FbCallClaudeWorker** | `fb_call_claude` | Calls Claude (first fallback). In live mode, calls the Anthropic Messages API. In demo mode, returns a `429 Too Many Requests` failure to trigger the next fallback |
| **FbCallGeminiWorker** | `fb_call_gemini` | Calls Gemini (last resort). In live mode, calls the Google Generative Language API. In demo mode, returns a `` success response completing the fallback chain |
| **FbFormatResultWorker** | `fb_format_result` | Inspects the status of each model's response, selects the first successful response, and reports which model was used and how many fallbacks were triggered (0 = GPT-4 succeeded, 1 = Claude, 2 = Gemini) | Always runs locally |

Each worker auto-detects whether to make live API calls or return demo responses based on the corresponding environment variable. No code changes are needed to switch between modes. Just set or unset the API key. All API calls use `java.net.http.HttpClient` (built into Java 21) with Jackson for JSON serialization.

### The Workflow

```
fb_call_gpt4
 │
 ▼
SWITCH (check_gpt4_ref)
 ├── failed: fb_call_claude -> check_claude_status
 │
 ▼
fb_format_result

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
