# Deadline Management

A project task has a due date. The pipeline checks the deadline to determine urgency, then routes to the appropriate handler: normal processing, urgent escalation, or overdue remediation via a SWITCH task.

## Workflow

```
ded_check_deadlines ──> SWITCH(urgency)
                          ├── "normal" ──> ded_handle_normal
                          ├── "urgent" ──> ded_handle_urgent
                          └── "overdue" ──> ded_handle_overdue
```

Workflow `deadline_management_410` accepts `projectId`, `taskId`, and `dueDate`. Times out after `60` seconds.

## Workers

**CheckDeadlinesWorker** (`ded_check_deadlines`) -- evaluates the due date and assigns an urgency level.

**HandleNormalWorker** (`ded_handle_normal`) -- processes tasks with normal urgency.

**HandleUrgentWorker** (`ded_handle_urgent`) -- escalates urgent tasks.

**HandleOverdueWorker** (`ded_handle_overdue`) -- handles overdue tasks.

## Workflow Output

The workflow produces `urgency`, `hoursRemaining`, `action` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `deadline_management_410` defines 2 tasks with input parameters `projectId`, `taskId`, `dueDate` and a timeout of `60` seconds.

## Tests

5 tests verify deadline checking, urgency classification, and routing to the correct handler.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
