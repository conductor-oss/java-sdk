# API Test Generation in Java with Conductor : Auto-Generate and Run Tests from an OpenAPI Spec

## From API Spec to Test Report in Four Steps

Every API needs tests, but writing them by hand is tedious and falls behind as endpoints change. An OpenAPI spec already describes your endpoints, parameters, and expected responses. the information needed to generate tests automatically. The challenge is coordinating the pipeline: parse the spec into a list of endpoints, generate test cases for each one, execute the suite, and compile the results into a report.

This workflow takes a `specUrl` and `format` as input, parses the spec to extract endpoints (paths, methods, parameters), generates a test suite covering each endpoint, runs those tests, and produces a report with pass rates. Each step's output feeds the next via JSONPath. the parsed endpoints feed test generation, the test suite feeds execution, and the execution results feed the report.

## The Solution

**You just write the spec-parsing, test-generation, execution, and reporting workers. Conductor handles the pipeline sequencing and endpoint data flow.**

Four workers form the test generation pipeline. spec parsing, test generation, test execution, and reporting. The spec parser extracts endpoint metadata (paths like `/users`, `/orders`, methods like GET/POST, and their parameters). The test generator creates test cases for each endpoint. The runner executes them and calculates pass rates. The reporter compiles everything into a summary. Conductor sequences the four steps and passes each output to the next input automatically.

### What You Write: Workers

ParseSpecWorker extracts endpoints and methods from the OpenAPI spec, and ReportWorker compiles pass/fail counts and pass rates, each step in the API test pipeline operates independently.

| Worker | Task | What It Does |
|---|---|---|
| **ParseSpecWorker** | `atg_parse_spec` | Parses the input and computes endpoints, endpoint count |
| **ReportWorker** | `atg_report` | Compiles test execution results into a summary report with pass/fail counts and pass rate. |

### The Workflow

```
atg_parse_spec
 │
 ▼
atg_generate_tests
 │
 ▼
atg_run_tests
 │
 ▼
atg_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
