# Tool-Augmented Generation: Generate, Detect Gaps, Call Tools, Complete

The LLM starts generating but hits a knowledge gap (`gapType` identified). The agent detects what tool is needed (`toolName`, `toolQuery`), calls it (returning `toolResult` with `source`), incorporates the result into an `enrichedText`, and completes the generation with `totalTokens` tracking.

## Workflow

```
prompt -> tg_start_generation -> tg_detect_gap -> tg_call_tool -> tg_incorporate_result -> tg_complete_generation
```

## Tests

40 tests cover partial generation, gap detection, tool calling, incorporation, and completion.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
