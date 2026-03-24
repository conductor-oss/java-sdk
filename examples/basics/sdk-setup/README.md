# Sdk Setup

Smoke test workflow for SDK setup verification

**Input:** `check` | **Timeout:** 60s

**Output:** `result`

## Pipeline

```
sdk_test_task
```

## Workers

**SdkTestWorker** (`sdk_test_task`): Simple worker for the SDK setup smoke test.

Reads `check`. Outputs `result`.

## Workflow Output

- `result`: `${sdk_test_task_ref.output.result}`

## Data Flow

**sdk_test_task**: `check` = `${workflow.input.check}`

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

## Workflow Definition

**Name:** `sdk_setup_test` | **Tasks:** 1 | **Workers:** 1

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
