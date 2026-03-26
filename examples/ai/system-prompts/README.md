# A/B Testing System Prompts: Formal vs Casual Tone Comparison

Which system prompt produces better responses -- formal or casual? This workflow runs the same user prompt through two different system prompts (with few-shot examples), then compares the outputs side by side. The workflow calls `sp_build_prompt` and `sp_call_llm` twice with different tone settings.

## Workflow

```
userPrompt, model
       │
       ▼
┌──────────────────┐     ┌──────────────────┐
│ sp_build_prompt  │ --> │ sp_call_llm      │  (formal tone)
└──────────────────┘     └──────────────────┘
       │
       ▼
┌──────────────────┐     ┌──────────────────┐
│ sp_build_prompt  │ --> │ sp_call_llm      │  (casual tone)
└──────────────────┘     └──────────────────┘
       │
       ▼
┌──────────────────────┐
│ sp_compare_outputs   │  Side-by-side comparison
└──────────────────────┘
```

## Workers

**SpBuildPromptWorker** (`sp_build_prompt`) -- Uses a `SYSTEM_PROMPTS` map with tone-specific system prompts and `FEW_SHOT_EXAMPLES` with tone-specific examples. The `"formal"` tone has its own few-shot examples in `List.of(...)` format. Builds the complete messages array with system prompt, few-shot examples, and user message.

**SpCallLlmWorker** (`sp_call_llm`) -- Uses a `RESPONSES` map for deterministic mode with tone-specific responses. Builds messages as `[{role: "system", content: ...}, {role: ..., content: ...}]`.

**SpCompareOutputsWorker** (`sp_compare_outputs`) -- Produces a comparison map with both responses and analysis.

## Tests

18 tests cover prompt building with different tones, LLM calling, and output comparison.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
