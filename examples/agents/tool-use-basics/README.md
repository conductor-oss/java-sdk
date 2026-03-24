# Tool Use Basics: Analyze Intent, Select Tool by Switch, Execute, Format

The analyzer checks `userRequest.toLowerCase().contains("weather")` (and similar) to detect intent with confidence. The selector uses a `switch` on intent to pick the tool and build `toolArgs` as `Map.of(...)`. The executor dispatches via another `switch(toolName)` producing `toolOutput`. The formatter produces the `answer` with `toolUsed`.

## Workflow

```
userRequest, availableTools -> tu_analyze_request -> tu_select_tool -> tu_execute_tool -> tu_format_result
```

## Tests

41 tests cover intent analysis, tool selection, execution dispatch, and formatting.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
