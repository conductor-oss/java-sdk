# GPT-4 vs Claude vs Gemini: Side-by-Side Evaluation

You need to choose between GPT-4, Claude, and Gemini for a task but have no data to compare them. This workflow sends the same prompt to all three providers in parallel via FORK_JOIN, collects their responses and token usage, and produces a side-by-side comparison.

## Workflow

```
prompt
  │
  ▼
┌─── FORK_JOIN ────────────────────────────────┐
│                  │                            │
│ ┌──────────────┐ │ ┌──────────────┐          │ ┌──────────────┐
│ │mc_call_gpt4  │ │ │mc_call_claude│          │ │mc_call_gemini│
│ └──────────────┘ │ └──────────────┘          │ └──────────────┘
└────────┬─────────┴──────────┬────────────────┘
         ▼                    ▼
    ┌──────────┐
    │   JOIN   │
    └────┬─────┘
         ▼
  ┌──────────────┐
  │ mc_compare   │  Side-by-side comparison
  └──────────────┘
         │
         ▼
   comparison, winner
```

## Workers

**McCallGpt4Worker** (`mc_call_gpt4`) -- Calls OpenAI Chat Completions with `gpt-4o-mini`, `max_tokens: 512`. Returns response text, model name, and token usage. In fallback mode, returns a deterministic response.

**McCallClaudeWorker** (`mc_call_claude`) -- Calls Anthropic Messages API with `claude-sonnet-4-6`. Returns response text, model name, and token usage. In fallback mode, returns a deterministic response.

**McCallGeminiWorker** (`mc_call_gemini`) -- Calls Gemini generateContent with `gemini-2.0-flash`. Uses `contents[0].parts[0].text` format. Returns response text, model name, and token usage. In fallback mode, returns a deterministic response.

**McCompareWorker** (`mc_compare`) -- Collects all three responses into a `List.of(gpt4, claude, gemini)` and produces a comparison summary.

## Tests

14 tests cover all three provider calls and the comparison logic.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
