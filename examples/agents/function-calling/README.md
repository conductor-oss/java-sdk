# LLM Function Calling: Plan, Extract Call, Execute, Synthesize

The LLM plans which function to call, the agent extracts the function call (checking `llmOutput.containsKey("functionCall")`), executes it via a `switch` statement mapping function names to results (e.g., price lookups), and the LLM synthesizes the answer from the function output.

## Workflow

```
userQuery, functionDefinitions
  -> fc_llm_plan -> fc_extract_function_call -> fc_execute_function -> fc_llm_synthesize
```

## Workers

**LlmPlanWorker** (`fc_llm_plan`) -- Uses `Pattern`/`Matcher` to plan the function call.

**ExtractFunctionCallWorker** (`fc_extract_function_call`) -- Validates `functionCall` key exists in LLM output; defaults to `"unknown"` if missing.

**ExecuteFunctionWorker** (`fc_execute_function`) -- Uses a `switch` expression to dispatch by `functionName`.

**LlmSynthesizeWorker** (`fc_llm_synthesize`) -- Checks for `functionResult.containsKey("price")` to build the answer.

## Tests

49 tests cover planning, extraction, function dispatch, and synthesis.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
