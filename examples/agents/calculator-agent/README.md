# Calculator Agent in Java Using Conductor : Parse Expressions, Compute Steps, Explain Results

Calculator Agent. parse a math expression, compute step-by-step following PEMDAS, and explain the result.

## Math Agents Need to Show Their Work

An LLM asked to compute "(15.7 + 3.3) * 2.5 / (1 + 0.1)" will often get the wrong answer. large language models are unreliable at arithmetic. A calculator agent separates understanding from computation: first parse the expression into structured operations (identify operands, operators, and precedence), then compute each step with proper floating-point precision, then explain the solution process so the user understands the reasoning.

Without separation, the LLM tries to do all three at once and often makes arithmetic errors that it confidently presents as correct. By externalizing computation to a dedicated worker, the math is always right. the LLM's role is limited to parsing intent and explaining results, which it does well.

## The Solution

**You write the parsing, computation, and explanation logic. Conductor handles the step-by-step pipeline and ensures each phase uses the prior phase's output.**

`ParseExpressionWorker` analyzes the mathematical expression and breaks it into an ordered list of operations with operands, operators, and evaluation order. `ComputeStepsWorker` evaluates each operation step-by-step with the specified precision, tracking intermediate results. `ExplainResultWorker` generates a natural language walkthrough of the computation, explaining each step and the final answer. Conductor chains these three steps, ensuring the computation uses the parser's output and the explanation uses the computation's intermediate results.

### What You Write: Workers

Three workers separate math reasoning from computation. Parsing the expression, computing each step with proper precision, and explaining the result.

| Worker | Task | What It Does |
|---|---|---|
| **ComputeStepsWorker** | `ca_compute_steps` | Computes the result of a parsed expression step-by-step, following the operation order from the parser. |
| **ExplainResultWorker** | `ca_explain_result` | Generates a human-readable explanation of how a mathematical expression was evaluated, referencing PEMDAS rules. |
| **ParseExpressionWorker** | `ca_parse_expression` | Parses a mathematical expression into tokens and determines the order of operations following PEMDAS rules. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
ca_parse_expression
 │
 ▼
ca_compute_steps
 │
 ▼
ca_explain_result

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
