# Hello World

The simplest Conductor workflow — one task, one worker

**Input:** `name` | **Timeout:** 60s

**Output:** `greeting`

## Pipeline

```
greet
```

## Workers

**GreetWorker** (`greet`): Simple worker that greets a user by name.

Reads `name`. Outputs `greeting`.

## Workflow Output

- `greeting`: `${greet_ref.output.greeting}`

## Data Flow

**greet**: `name` = `${workflow.input.name}`

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

## Workflow Definition

**Name:** `hello_world_workflow` | **Tasks:** 1 | **Workers:** 1

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
