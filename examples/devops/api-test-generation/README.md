# Auto-Generating API Tests from an OpenAPI Spec

Your team maintains a growing REST API. Every new endpoint needs tests for the happy path,
invalid inputs, and missing authentication -- but writing them by hand falls behind the pace
of development. This workflow parses an API spec, generates a test matrix, executes it, and
produces a pass/fail report automatically.

## Workflow

```
specUrl, format
       |
       v
 +--------------+     +------------------+     +--------------+     +------------+
 | atg_parse_   | --> | atg_generate_    | --> | atg_run_     | --> | atg_report |
 | spec         |     | tests            |     | tests        |     |            |
 +--------------+     +------------------+     +--------------+     +------------+
   4 endpoints          3 cases/endpoint       total-1 passed        status:
   (openapi3)           (happy_path,           passRate computed     "complete"
                         invalid_input,
                         auth_required)
```

## Workers

**ParseSpecWorker** -- Reads the `specUrl` input and defaults `format` to `"openapi3"`. Returns
a hard-coded catalogue of 4 endpoints: `GET /users`, `POST /users` (params `name`, `email`),
`GET /users/{id}` (param `id`), and `POST /orders` (params `userId`, `items`). Outputs
`endpoints` list and `endpointCount`.

**GenerateTestsWorker** -- Iterates over the `endpoints` list. For every endpoint it builds a
test descriptor with three cases: `"happy_path"`, `"invalid_input"`, and `"auth_required"`.
`testCount` equals `endpoints.size() * 3` (12 for the default spec).

**RunTestsWorker** -- Receives `testSuite`, computes `total = suite.size() * 3`, then sets
`passed = total - 1` (exactly one failure). Calculates `passRate` as a rounded percentage
string (e.g. `"92%"`).

**ReportWorker** -- Copies the `results` map, stamps it with `"status" -> "complete"`, and
outputs the final `report`.

## Tests

8 unit tests verify parsing, generation, execution, and reporting.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
