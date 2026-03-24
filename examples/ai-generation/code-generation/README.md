# Code Generation: From Requirements to Validated, Tested Code

A developer describes what they need in plain English -- "build a REST API for users and orders with authentication and pagination" -- and expects working, tested code. Translating natural language requirements into structured specifications, generating code that follows framework conventions, validating syntax, and running test cases are four distinct concerns that benefit from separation.

This workflow parses requirements into structured entities, generates code with test cases, validates syntax, and runs the tests.

## Pipeline Architecture

```
requirements, language, framework
         |
         v
  cdg_parse_requirements (parsed: entities, operations, constraints)
         |
         v
  cdg_generate_code      (code, linesOfCode=85, testCases, filesGenerated)
         |
         v
  cdg_validate           (syntaxValid=true, lintErrors=0, warnings=1)
         |
         v
  cdg_test               (allPassed=true, testsRun)
```

## Worker: ParseRequirements (`cdg_parse_requirements`)

Extracts structured information from the natural language `requirements` string. Returns a `parsed` map containing `entities: ["User", "Order"]` (the domain objects), `operations: ["create", "read", "update"]` (the CRUD operations needed), and `constraints: ["auth required", "pagination"]` (cross-cutting requirements). This structured representation drives code generation.

## Worker: GenerateCode (`cdg_generate_code`)

Generates source code from the parsed specification, targeting the specified `language` and `framework`. Returns `code` as a string containing a REST API stub (e.g., `app.post('/users', auth, async (req, res) => { ... })`), `linesOfCode: 85`, `filesGenerated: ["routes/users.js", "routes/orders.js"]` (one route file per entity), and `testCases` as a list of maps with `name`, `input`, and `expected` fields (e.g., `{name: "create_user", input: {email: "test@ex.com"}, expected: 201}`).

## Worker: Validate (`cdg_validate`)

Checks the generated code for syntax correctness and lint compliance. Returns `syntaxValid: true`, `lintErrors: 0`, and `warnings: 1`. The single warning indicates a minor style issue that does not affect correctness.

## Worker: Test (`cdg_test`)

Runs the generated test cases against the code. Returns `allPassed: true` and `testsRun` equal to the number of test cases provided. The test count is derived from `tests.size()`.

## Tests

4 tests cover requirement parsing, code generation, syntax validation, and test execution.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
