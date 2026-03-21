# Function Calling in Java Using Conductor: LLM Plans, Extract Call, Execute, Synthesize

You ask "What's Apple's stock price?" and the model calls `get_stock_price(ticker="APPL")`. a ticker that doesn't exist. Or it invents a function called `fetch_realtime_quote` that was never in the schema. Or it calls `get_stock_price` with `{"user_id": "12345"}` because it confused the parameters from a different function. When an LLM has direct API access, every hallucinated function name, wrong parameter type, or confused argument is a live production call. This example separates intent from execution: the LLM decides what to call, a validation layer checks it against the real function registry, and only then does execution happen. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Giving LLMs the Ability to Call Functions

An LLM can reason about what needs to happen ("I should look up the current weather") but can't actually call a weather API. Function calling bridges this gap: the LLM receives a list of available function definitions (name, description, parameters), decides which function to call for the user's query, and outputs a structured function call. A separate step executes the actual function and returns the result. The LLM then synthesizes the function output into a natural language response.

This four-step pattern separates intent (LLM decides what to call) from execution (worker actually calls the function). The LLM never has direct API access. It only specifies what it wants. The execution layer validates the call, applies rate limits, and handles errors. Without this separation, you'd give the LLM direct API access, which is both a security risk and makes debugging impossible.

## The Solution

**You write the LLM planning, function extraction, execution, and synthesis logic. Conductor handles the call chain, execution retries, and full audit of every function invocation.**

`LlmPlanWorker` sends the user query and function definitions to an LLM, which returns its decision about which function to call and why. `ExtractFunctionCallWorker` parses the LLM's output to extract the function name and arguments in a structured format. `ExecuteFunctionWorker` validates the extracted call against the function registry and executes the function with the specified arguments. `LlmSynthesizeWorker` feeds the function's result back to the LLM, which synthesizes it into a natural language response for the user. Conductor chains these steps, retries failed function executions, and records the full plan-extract-execute-synthesize chain for debugging.

### What You Write: Workers

Four workers implement function calling, the LLM plans which function to invoke, the call is extracted and validated, the function executes, and the result is synthesized into natural language.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteFunctionWorker** | `fc_execute_function` | Executes the specified function with the given arguments. Currently supports get_stock_price; returns real API results |
| **ExtractFunctionCallWorker** | `fc_extract_function_call` | Extracts a structured function call (name + arguments) from the LLM output and validates it against the available fun |
| **LlmPlanWorker** | `fc_llm_plan` | Llm Plan. Computes and returns llm response |
| **LlmSynthesizeWorker** | `fc_llm_synthesize` | LLM synthesis worker. Takes the function execution result and produces a natural-language answer for the user. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode, the agent workflow stays the same.

### The Workflow

```
fc_llm_plan
 │
 ▼
fc_extract_function_call
 │
 ▼
fc_execute_function
 │
 ▼
fc_llm_synthesize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
