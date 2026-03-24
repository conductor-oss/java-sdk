# Parallel Security, Performance, and Style Code Review

Code is parsed into an AST (functions: `["handleRequest", "validateInput", "processData", "sendResponse"]`, complexity score). Three reviewers run in parallel: security finds `SQL_INJECTION` (HIGH), performance finds `N_PLUS_1_QUERY` (HIGH), style finds `INCONSISTENT_NAMING` (LOW). A compiler merges all findings.

## Workflow

```
code, language -> cr_parse_code
  -> FORK_JOIN(cr_security_review | cr_performance_review | cr_style_review)
  -> cr_compile_review
```

## Workers

**ParseCodeWorker** (`cr_parse_code`) -- Returns AST with `functions`, imports, line count, complexity.

**SecurityReviewWorker** -- Finds `{severity: "HIGH", type: "SQL_INJECTION"}`.

**PerformanceReviewWorker** -- Finds `{severity: "HIGH", type: "N_PLUS_1_QUERY"}`.

**StyleReviewWorker** -- Finds `{severity: "LOW", type: "INCONSISTENT_NAMING"}`.

**CompileReviewWorker** -- Merges findings (null-safe via `List.of()` defaults).

## Tests

38 tests cover parsing, all three review types, and compilation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
