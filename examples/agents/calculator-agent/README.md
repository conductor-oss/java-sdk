# Calculator Agent: Parse Expressions, Compute with Steps, Explain

A math expression comes in and the agent must parse it into tokens (e.g., `{type: "number", value: "15"}`), compute step-by-step showing intermediate results, and explain the final answer with the step count.

## Workflow

```
expression, precision -> ca_parse_expression -> ca_compute_steps -> ca_explain_result
```

## Workers

**ParseExpressionWorker** (`ca_parse_expression`) -- Tokenizes the expression into `[{type: "number", value: "15"}, ...]`.

**ComputeStepsWorker** (`ca_compute_steps`) -- Executes computation step by step, producing a list of intermediate results.

**ExplainResultWorker** (`ca_explain_result`) -- Produces a human-readable explanation with `stepCount`.

## Tests

27 tests cover expression parsing, multi-step computation, and explanation generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
