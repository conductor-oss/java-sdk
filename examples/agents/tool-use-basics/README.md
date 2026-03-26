# Tool Use Basics: Analyze, Select, Execute, Format

An agent receives a free-text user request like "What's the weather in San Francisco?" and needs to figure out which tool to call, construct the right arguments, execute it, and present the result in natural language. The challenge is bridging the gap between unstructured human language and structured API calls.

This workflow implements a four-stage tool-use pipeline: intent analysis, tool selection, execution with real API calls, and human-readable formatting.

## Pipeline Architecture

```
userRequest, availableTools
         |
         v
  tu_analyze_request     (intent, entities, complexity, confidence=0.95)
         |
         v
  tu_select_tool         (selectedTool, toolDescription, toolArgs map)
         |
         v
  tu_execute_tool        (toolOutput map, executionTimeMs, success boolean)
         |
         v
  tu_format_result       (answer string, toolUsed, sourceData)
```

## Worker: AnalyzeRequest (`tu_analyze_request`)

Checks `userRequest.toLowerCase()` for keyword patterns: `"weather"` maps to `get_weather`, `"calculat"` or `"math"` maps to `calculate`, `"search"` or `"find"` maps to `search`, and anything else becomes `general`. Returns an analysis map with `entities`, `intent`, `complexity: "simple"`, and `requiresMultipleTools: false`. Confidence fixed at `0.95`.

## Worker: SelectTool (`tu_select_tool`)

Uses a `switch` on the intent string to pick tool name, description, and arguments. Weather: `toolArgs = Map.of("location", "San Francisco, CA", "units", "fahrenheit")`. Calculator: `expression` and `precision`. Web search: `query` and `maxResults`. Default: `general_handler`.

## Worker: ExecuteTool (`tu_execute_tool`)

Dispatches via `switch(toolName)` to one of three real implementations. **weather_api**: geocodes via Open-Meteo (`geocoding-api.open-meteo.com`), fetches current conditions and 3-day forecast, maps WMO weather codes to strings via a 15-case switch expression, converts wind degrees to cardinal directions using a 16-element `String[]` array. **calculator**: evaluates expressions via a recursive-descent parser supporting `+`, `-`, `*`, `/`, `^`, parentheses, and unary minus. **web_search**: calls the Wikipedia opensearch API. All network calls use `HttpClient` with 10-second timeouts; errors are caught and returned as an `error` field rather than thrown.

## Worker: FormatResult (`tu_format_result`)

Formats tool output into natural language based on `toolName`. Weather responses include temperature, unit symbol (F/C), condition, humidity, wind direction and speed. Calculator responses report the numeric result. Search responses report the total result count. Falls back to a generic message for unknown tools.

## Tests

4 tests cover intent analysis, tool selection, execution dispatch, and result formatting.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
