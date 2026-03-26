# Generating Unit Tests from Source Code Analysis

Writing tests for an existing codebase is tedious. You need to find every function, figure
out its parameters, generate test cases with edge cases, validate the syntax, and produce a
coverage report. This workflow analyzes a source file to extract functions, generates test
cases for each, validates them, and produces a report.

## Workflow

```
sourceFile, language, framework
             |
             v
+---------------------+     +-----------------------+     +-----------------------+     +---------------+
| tge_analyze_code    | --> | tge_generate_tests    | --> | tge_validate_tests    | --> | tge_report    |
+---------------------+     +-----------------------+     +-----------------------+     +---------------+
  functions extracted         test cases generated         all tests validated          coverage: "87%"
  functionCount returned      testCount computed           syntactically correct        status: "ready"
```

## Workers

**AnalyzeCodeWorker** -- Parses the `sourceFile` and extracts functions. Returns
`functions` list and `functionCount`.

**GenerateTestsWorker** -- Creates test cases for each function. Returns `tests` list and
`testCount` (3 cases per function).

**ValidateTestsWorker** -- Validates all generated tests are syntactically correct. Returns
`validatedTests` and `validCount`.

**ReportWorker** -- Generates a report with `coverage: "87%"` and `status: "ready"` for
the source file.

## Tests

8 unit tests cover code analysis, test generation, validation, and reporting.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
