# Tracking Per-Provider LLM Costs Across GPT-4, Claude, and Gemini

Your monthly AI bill is $12,000 but you don't know which feature consumed what. This workflow sends the same prompt to three providers in parallel, captures per-call token counts, and aggregates a side-by-side cost breakdown using fixed pricing constants.

## Workflow

```
prompt
  │
  ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ ct_call_gpt4    │  │ ct_call_claude  │  │ ct_call_gemini  │
│ (OpenAI GPT-4)  │  │ (Anthropic)     │  │ (Google)        │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │
         └────────────┬───────┘────────────────────┘
                      ▼
            ┌──────────────────────┐
            │ ct_aggregate_costs   │  Calculate per-model + total cost
            └──────────────────────┘
                      │
                      ▼
              breakdown, totalCost, totalTokens
```

## Workers

**CallGpt4Worker** (`ct_call_gpt4`) -- Requires `CONDUCTOR_OPENAI_API_KEY`. Calls OpenAI Chat Completions with `gpt-4o-mini`, `max_tokens: 512`. Extracts `usage.prompt_tokens` and `usage.completion_tokens` from the response. Maps the pricing model to `"gpt-4"`. Returns `usage: {model, inputTokens, outputTokens}`.

**CallClaudeWorker** (`ct_call_claude`) -- Requires `CONDUCTOR_ANTHROPIC_API_KEY`. Calls the Anthropic Messages API at `https://api.anthropic.com/v1/messages` with model `claude-sonnet-4-6`, `max_tokens: 512`, `anthropic-version: 2023-06-01`. Extracts text blocks from `content` array filtering by `type == "text"`. Reads `usage.input_tokens` and `usage.output_tokens`. Maps the model name to `"claude-3"` for pricing via `model.startsWith("claude-3")`. Distinguishes retryable (429/503) from terminal errors.

**CallGeminiWorker** (`ct_call_gemini`) -- Requires `GOOGLE_API_KEY`. Calls `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent`. Reads `usageMetadata.promptTokenCount` and `usageMetadata.candidatesTokenCount`.

**AggregateCostsWorker** (`ct_aggregate_costs`) -- Applies fixed per-1K-token pricing: GPT-4 at `[$0.03 input, $0.06 output]`, Claude-3 at `[$0.015, $0.075]`, Gemini at `[$0.0005, $0.0015]`. Computes `(inputTokens / 1000.0) * inputPrice + (outputTokens / 1000.0) * outputPrice` for each provider. Formats costs as `String.format("$%.4f", cost)`. Sums all three into `totalCost` and `totalTokens`.

## Tests

15 tests cover all three provider workers and the cost aggregation math.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
