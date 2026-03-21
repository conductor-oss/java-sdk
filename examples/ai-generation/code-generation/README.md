# Code Generation in Java with Conductor : From Requirements to Validated, Tested Code

## Generating Code That Actually Works

Generating code from requirements is not just about producing text that looks like code. The output needs to be syntactically valid and functionally correct. This means parsing the requirements into actionable specs, generating code in the right language with the right framework conventions, checking that it compiles, and running test cases to verify behavior. Each step depends on the previous one. you cannot validate code that has not been generated, and you cannot run tests without knowing the expected behavior from the requirements.

This workflow takes natural language requirements and produces tested code. The parser extracts structured specs from the requirements. The generator creates source code (e.g., REST API routes) along with test cases in the specified language and framework. The validator checks syntax. The test runner executes the generated test cases against the generated code. The workflow outputs the code, line count, syntax validity, and test results.

## The Solution

**You just write the requirement-parsing, code-generation, validation, and test-execution workers. Conductor handles the pipeline sequencing and data routing.**

Four workers form the code generation pipeline. requirement parsing, code generation, syntax validation, and test execution. The parser converts free-text requirements into structured specs. The generator produces source files (like `routes/users.js`, `routes/orders.js`) with accompanying test cases. The validator checks the generated code for syntax errors. The test runner executes the test cases and reports pass/fail. Conductor sequences the pipeline and routes parsed specs, generated code, and test cases between steps via JSONPath.

### What You Write: Workers

ParseRequirementsWorker converts natural language to structured specs, GenerateCodeWorker produces source files with test cases, and ValidateWorker checks syntax, each step in the code generation pipeline runs independently.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateCodeWorker** | `cdg_generate_code` | Generates source code and test files in the specified language and framework from parsed requirements. |
| **ParseRequirementsWorker** | `cdg_parse_requirements` | Converts natural-language requirements into structured specs (entities, endpoints, operations). |
| **ValidateWorker** | `cdg_validate` | Checks the generated code for syntax errors and lint violations. |

### The Workflow

```
cdg_parse_requirements
 │
 ▼
cdg_generate_code
 │
 ▼
cdg_validate
 │
 ▼
cdg_test

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
