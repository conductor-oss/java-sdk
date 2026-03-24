# Conditional Tool Use in Java Using Conductor : Classify Query, Route to Calculator/Interpreter/Search

Tool Use Conditional. classifies a user query and routes to the appropriate tool (calculator, interpreter, or web search) via a SWITCH task.

## Different Questions Need Different Tools

"What's the square root of 144?" needs a calculator. "Write a Python function to sort a list" needs a code interpreter. "What happened at the G7 summit?" needs web search. A tool-using agent must first determine which type of question it's looking at, then route to the appropriate tool.

Classification determines the tool, and getting it wrong wastes resources and returns poor results. A math question sent to web search gets irrelevant links. A code question sent to the calculator gets an error. The `SWITCH` pattern makes this routing explicit and auditable. you can see which queries are classified into which categories, track accuracy, and add new categories (data analysis, translation, scheduling) without modifying existing tool workers.

## The Solution

**You write the query classifier and individual tool handlers. Conductor handles the conditional routing, per-tool retries, and classification analytics.**

`ClassifyQueryWorker` analyzes the user's query and determines the type. math (arithmetic, equations), code (programming, algorithms), or search (factual questions, current events), with a confidence score. Conductor's `SWITCH` routes to the matching tool: `CalculatorWorker` for math computations, `InterpreterWorker` for code execution, or `WebSearchWorker` for information retrieval. Each tool handles its domain and returns results in its own format. Conductor tracks which tool handles each query type, enabling classification accuracy analysis.

### What You Write: Workers

Four workers handle conditional routing. Classifying the query type, then routing to the calculator, code interpreter, or web search via SWITCH.

| Worker | Task | What It Does |
|---|---|---|
| **CalculatorWorker** | `tc_calculator` | Handles math-category queries by simulating a calculator tool. Returns an answer string, a calculation object with ex... |
| **ClassifyQueryWorker** | `tc_classify_query` | Classifies a user query into one of three categories (math, code, search) and routes it to the appropriate downstream... |
| **InterpreterWorker** | `tc_interpreter` | Handles code-category queries by simulating a code interpreter tool. Returns an answer with generated code, the code ... |
| **WebSearchWorker** | `tc_web_search` | Handles search-category queries by simulating a web search tool. Returns an answer string, a list of search results, ... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
tc_classify_query
 │
 ▼
SWITCH (route_to_tool_ref)
 ├── math: tc_calculator
 ├── code: tc_interpreter
 ├── search: tc_web_search
 └── default: tc_web_search

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
