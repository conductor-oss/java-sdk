# Approval Dashboard React

Dashboard API for Pending Tasks -- dash_task (SIMPLE) followed by WAIT (pending_approval) with title, priority, and assignee inputs.

**Input:** `title`, `priority`, `assignee` | **Timeout:** 600s

**Output:** `processed`, `approvalStatus`

## Pipeline

```
dash_task
    │
pending_approval [WAIT]
```

## Workers

**DashTaskWorker** (`dash_task`): Worker for dash_task — processes the initial task before the approval wait step.

Outputs `processed`.

## Workflow Output

- `processed`: `${dash_task_ref.output.processed}`
- `approvalStatus`: `${pending_approval_ref.output.approvalStatus}`

## Data Flow

**dash_task**: `title` = `${workflow.input.title}`, `priority` = `${workflow.input.priority}`, `assignee` = `${workflow.input.assignee}`
**pending_approval** [WAIT]: `title` = `${workflow.input.title}`, `priority` = `${workflow.input.priority}`, `assignee` = `${workflow.input.assignee}`

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
