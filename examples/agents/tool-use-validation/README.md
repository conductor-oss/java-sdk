# Tool Use Validation in Java Using Conductor : Generate Call, Validate Input, Execute, Validate Output, Deliver

Tool Use Validation. generate tool call, validate input, execute tool, validate output, and deliver results through a sequential pipeline. ## Tool Calls Need Guardrails on Both Sides

An LLM generates a tool call: `search(query="DROP TABLE users")`. Without input validation, that malicious query goes straight to the search API. Or it generates `calculate(expression="1/0")`. Without output validation, the division-by-zero error crashes the pipeline.

Input validation catches bad parameters before execution. rejecting SQL injection attempts, enforcing value ranges, checking required fields, and ensuring type correctness. Output validation catches bad results after execution, verifying the response matches the expected schema, checking for error responses disguised as successes, and performing sanity checks on returned values. Both validations are separate from the tool execution itself, so adding new validation rules doesn't require modifying tool code.

## The Solution

**You write the call generation, input/output validation, and execution logic. Conductor handles the validation pipeline, security gating, and audit trail for every tool call.**

`GenerateToolCallWorker` creates the tool call specification from the user request. tool name, parameters, and expected return type. `ValidateInputWorker` checks the parameters against the tool's input schema, type correctness, required fields present, values within allowed ranges, and no injection patterns. `ExecuteToolWorker` runs the validated call. `ValidateOutputWorker` checks the response against the expected output schema, correct structure, reasonable values, no error responses. `DeliverWorker` formats and delivers the validated result. Conductor chains all five steps and records validation results for security auditing.

### What You Write: Workers

Five workers add guardrails to tool calls. Generating the call, validating input, executing the tool, validating output, and delivering the verified result.

| Worker | Task | What It Does |
|---|---|---|
| **DeliverWorker** | `tv_deliver` | Delivers the final formatted result to the user, combining the validated tool output with the validation report into ... |
| **ExecuteToolWorker** | `tv_execute_tool` | Executes the validated tool call and returns the raw output along with execution metadata. |
| **GenerateToolCallWorker** | `tv_generate_tool_call` | Generates a tool call from a user request, producing structured tool arguments along with input and output schemas fo... |
| **ValidateInputWorker** | `tv_validate_input` | Validates tool input arguments against the provided schema. Returns validation status, the validated arguments, a lis... |
| **ValidateOutputWorker** | `tv_validate_output` | Validates the raw tool output against the expected output schema. Returns validation status, the validated output, an... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
tv_generate_tool_call
 │
 ▼
tv_validate_input
 │
 ▼
tv_execute_tool
 │
 ▼
tv_validate_output
 │
 ▼
tv_deliver

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
