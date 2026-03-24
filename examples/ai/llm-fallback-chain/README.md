# Automatic Provider Failover: GPT-4 to Claude to Gemini

GPT-4 returns a 429 and your AI feature goes dark -- because you bet everything on one provider. This workflow tries GPT-4 first, falls back to Claude on failure, then to Gemini, using nested SWITCH tasks that inspect each provider's `status` output.

## Workflow

```
prompt
  │
  ▼
┌──────────────────┐
│ fb_call_gpt4     │  Try GPT-4 first
└────────┬─────────┘
         │  status: "success"/"failed"
         ▼
    SWITCH (check_gpt4_status)
    ├── "failed":
    │       ┌──────────────────┐
    │       │ fb_call_claude   │  Fallback #1
    │       └────────┬─────────┘
    │                ▼
    │          SWITCH (check_claude_status)
    │          └── "failed":
    │                 ┌──────────────────┐
    │                 │ fb_call_gemini   │  Fallback #2
    │                 └──────────────────┘
    └── (success: skip fallbacks)
         │
         ▼
┌──────────────────┐
│ fb_format_result │  Report which model served
└──────────────────┘
         │
         ▼
   response, modelUsed, fallbacksTriggered
```

## Workers

**FbCallGpt4Worker** (`fb_call_gpt4`) -- Requires `CONDUCTOR_OPENAI_API_KEY`. Calls OpenAI Chat Completions with model `gpt-4`, `max_tokens: 512`. Returns `status: "success"` with the response and model name, or `status: "failed"` with an `error` field. Always sets status to COMPLETED (the SWITCH routes on the output `status` field, not the task status).

**FbCallClaudeWorker** (`fb_call_claude`) -- Requires `CONDUCTOR_ANTHROPIC_API_KEY`. Calls `https://api.anthropic.com/v1/messages` with model `claude-sonnet-4-6`, `anthropic-version: 2023-06-01`. Same `status`/`response`/`model` output contract as GPT-4.

**FbCallGeminiWorker** (`fb_call_gemini`) -- Requires `GOOGLE_API_KEY`. Calls `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent`. Uses `contents[0].parts[0].text` request format. Same output contract.

**FbFormatResultWorker** (`fb_format_result`) -- Collects the response from whichever provider succeeded, reports `modelUsed` and `fallbacksTriggered` count.

## Tests

11 tests cover each provider call, SWITCH routing for success/failure paths, and result formatting.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
