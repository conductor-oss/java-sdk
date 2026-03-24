# Registering Workflows

Workflow registration demo — echoes a message through a single task

**Input:** `message` | **Timeout:** 60s

**Output:** `result`

## Pipeline

```
echo_task
```

## Workers

**EchoWorker** (`echo_task`): Simple worker that echoes an input message back as output.

Reads `message`. Outputs `result`.

## Workflow Output

- `result`: `${echo_task_ref.output.result}`

## Data Flow

**echo_task**: `message` = `${workflow.input.message}`

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

## Workflow Definition

**Name:** `registration_demo` | **Tasks:** 1 | **Workers:** 1

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
