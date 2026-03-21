# Test Generation in Java with Conductor

## Writing Tests Nobody Wants to Write

Developers know they should write tests, but doing it manually is tedious. especially for existing code with many functions and edge cases. You need to parse the source code to understand function signatures and behavior, generate meaningful test cases that cover normal and edge scenarios, validate that the generated tests actually compile, and report on coverage. Doing this by hand for every function in a codebase does not scale.

This workflow automates test generation for a single source file. The code analyzer parses the file to extract function signatures and metadata. The test generator creates test cases for each discovered function. The validator checks that generated tests are syntactically correct. The reporter summarizes coverage and results. Each step feeds the next. discovered functions drive test generation, generated tests feed validation, and validation results feed the report.

## The Solution

**You just write the code-analysis, test-generation, validation, and reporting workers. Conductor handles the test-gen pipeline and coverage data flow.**

Each worker handles one CRM operation. Conductor manages the customer lifecycle pipeline, assignment routing, follow-up scheduling, and activity tracking.

### What You Write: Workers

AnalyzeCodeWorker parses source files to discover function signatures and metadata, then ReportWorker summarizes coverage and pass rates, each step in the test generation pipeline operates independently.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeCodeWorker** | `tge_analyze_code` | Parses the source file to discover functions, their signatures, parameters, and return types. |
| **ReportWorker** | `tge_report` | Generates a coverage report summarizing test counts, pass rates, and per-function results. |

### The Workflow

```
tge_analyze_code
 │
 ▼
tge_generate_tests
 │
 ▼
tge_validate_tests
 │
 ▼
tge_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
