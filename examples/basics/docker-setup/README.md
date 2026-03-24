# Docker Setup

Simple one-task workflow to verify Docker setup is working

**Input:** `message` | **Timeout:** 60s

**Output:** `message`, `timestamp`

## Pipeline

```
docker_test_task
```

## Workers

**DockerTestWorker** (`docker_test_task`): Simple worker used to verify a Conductor Docker setup is working.

Reads `message`. Outputs `message`, `timestamp`.

## Workflow Output

- `message`: `${docker_test_task_ref.output.message}`
- `timestamp`: `${docker_test_task_ref.output.timestamp}`

## Data Flow

**docker_test_task**: `message` = `${workflow.input.message}`

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
