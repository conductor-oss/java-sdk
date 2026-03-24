# Code Interpreter: Analyze, Generate, Execute in Sandbox, Interpret

A data analysis question arrives. The agent analyzes the question to identify needed columns (`["id", "value"]`), generates Python code, executes it in a sandboxed environment (logging `code.split("\n").length` lines), and interprets the result with a confidence score.

## Workflow

```
question, dataset -> ci_analyze_question -> ci_generate_code -> ci_execute_sandbox -> ci_interpret_result
```

## Workers

**AnalyzeQuestionWorker** (`ci_analyze_question`) -- Identifies required columns and produces an analysis map.

**GenerateCodeWorker** (`ci_generate_code`) -- Generates Python code, counting `linesOfCode` via `code.split("\n").length`.

**ExecuteSandboxWorker** (`ci_execute_sandbox`) -- Executes code in a sandboxed environment. Logs the line count.

**InterpretResultWorker** (`ci_interpret_result`) -- Interprets execution results with a confidence score.

## Tests

36 tests cover question analysis, code generation, sandbox execution, and interpretation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
