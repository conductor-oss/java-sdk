# Code Interpreter Agent in Java Using Conductor : Analyze, Generate, Execute, Interpret

Code Interpreter Agent. analyzes a data question, generates Python code, executes in a sandbox, and interprets the results through a sequential pipeline.

## Answering Data Questions Requires Running Code, Not Just Generating It

"What's the correlation between marketing spend and revenue in Q3?" can't be answered by an LLM alone. it requires loading the dataset, computing a Pearson correlation coefficient, and possibly generating a scatter plot. The LLM can generate the code, but someone needs to run it, capture the output (including any charts), and explain what the results mean.

A code interpreter agent separates these concerns: analyze the question to determine what computation is needed (statistical analysis, data aggregation, visualization), generate the code, execute it in a sandbox where it can't access production systems, and interpret the raw output ("The correlation coefficient is 0.87, indicating a strong positive relationship between marketing spend and revenue"). Each step has different failure modes. the code might have a syntax error, the sandbox might timeout, the output might need re-interpretation.

## The Solution

**You write the question analysis, code generation, sandbox execution, and interpretation logic. Conductor handles the pipeline, retries on execution timeouts, and full reproducibility tracking.**

`AnalyzeQuestionWorker` examines the question and dataset to determine the analysis type (statistical, aggregation, visualization), required libraries, and output format. `GenerateCodeWorker` produces executable code (Python with pandas/matplotlib or SQL) based on the analysis plan. `ExecuteSandboxWorker` runs the code in an isolated environment, captures stdout, stderr, and any generated files (charts, CSVs). `InterpretResultWorker` translates the raw execution output into a natural language answer with key findings. Conductor chains these steps, retries code execution on timeout, and records the generated code and output for reproducibility.

### What You Write: Workers

Four workers form the code interpreter pipeline. Analyzing the question, generating executable code, running it in a sandbox, and interpreting the output.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeQuestionWorker** | `ci_analyze_question` | Analyzes a data question against a dataset schema to determine the type of analysis needed, the operations required, ... |
| **ExecuteSandboxWorker** | `ci_execute_sandbox` | Simulates executing generated code in a sandboxed environment. Returns the execution result including stdout with a t... |
| **GenerateCodeWorker** | `ci_generate_code` | Generates Python code based on the analysis plan and data schema. Produces a pandas-based script that performs group-... |
| **InterpretResultWorker** | `ci_interpret_result` | Interprets the execution results in the context of the original question. Provides a human-readable answer, a structu... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
ci_analyze_question
 │
 ▼
ci_generate_code
 │
 ▼
ci_execute_sandbox
 │
 ▼
ci_interpret_result

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
