# Tool Use Basics in Java Using Conductor: Analyze Request, Select Tool, Execute, Format Result

Your AI chatbot can eloquently explain how to check the weather in Tokyo. It just can't actually check the weather in Tokyo. It can describe the steps to calculate 15% of 230, but it can't call a calculator. It reasons beautifully about what should happen, then hands back a paragraph of text instead of a result. The gap between "knowing what to do" and "doing it" is the tool-use problem. This example wires an LLM to real tools through a four-step Conductor pipeline: analyze the request, select the right tool, execute it, and format the result into a natural language answer. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Foundation of Tool-Using Agents

An AI agent with access to tools (calculator, web search, calendar, database) needs a systematic way to determine which tool to use for a given request. "What's 15% of 230?" needs the calculator. "What's the weather in Tokyo?" needs the weather API. "Who won the Super Bowl?" needs web search.

The tool-use pattern has four steps: understand what the user wants (intent extraction), pick the right tool (tool selection from the available set), call the tool with the right parameters (execution), and present the result in natural language (formatting). Each step is independent. You can swap the tool selection logic without changing execution, or add new tools without modifying the request analyzer.

## The Solution

**You write the request analysis, tool selection, execution, and formatting logic. Conductor handles the tool-use pipeline, execution retries, and usage pattern tracking.**

`AnalyzeRequestWorker` parses the user request to extract intent, entities, and parameters. `SelectToolWorker` matches the request intent against available tool descriptions and selects the best fit with a confidence score. `ExecuteToolWorker` calls the selected tool with the extracted parameters and returns the raw result. `FormatResultWorker` converts the tool's output into a natural language response. Conductor chains these four steps and records which tool was selected and why, building a dataset of tool usage patterns.

### What You Write: Workers

Four workers implement the tool-use pattern. Analyzing the request, selecting the right tool, executing it, and formatting the result into natural language.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeRequestWorker** | `tu_analyze_request` | Analyzes a user request to determine intent, entities, and complexity. Returns an analysis object with entities, inte |
| **ExecuteToolWorker** | `tu_execute_tool` | Executes the selected tool and returns its output. For weather_api, returns a deterministic weather report. |
| **FormatResultWorker** | `tu_format_result` | Formats tool execution results into a natural language answer. Builds a human-readable string based on the tool output. |
| **SelectToolWorker** | `tu_select_tool` | Selects the appropriate tool based on the analyzed intent. Maps intent to tool name, produces a description, and buil |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode, the agent workflow stays the same.

### The Workflow

```
tu_analyze_request
 │
 ▼
tu_select_tool
 │
 ▼
tu_execute_tool
 │
 ▼
tu_format_result

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
