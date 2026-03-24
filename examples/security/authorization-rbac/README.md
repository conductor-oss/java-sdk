# Authorization (RBAC)

A user requests access to a resource. The system must resolve their roles from the identity store, evaluate whether the role grants permission for the requested action, check contextual factors (business hours, corporate network), and enforce the final decision with an audit log entry.

## Workflow

```
rbac_resolve_roles ──> rbac_evaluate_permissions ──> rbac_check_context ──> rbac_enforce_decision
```

Workflow `authorization_rbac_workflow` accepts `userId`, `resource`, and `action`. All tasks have `retryCount` = `2`. Times out after `1800` seconds.

## Workers

**ResolveRolesWorker** (`rbac_resolve_roles`) -- reads `userId` (defaults to `"unknown"`). Reports `"roles = [admin, editor]"`. Returns `resolve_rolesId` = `"RESOLVE_ROLES-1372"`.

**EvaluatePermissionsWorker** (`rbac_evaluate_permissions`) -- reads `action` and `resource` (both default to `"unknown"`). Reports `action + " on " + resource + ": permitted"`. Returns `evaluate_permissions` = `true`.

**CheckContextWorker** (`rbac_check_context`) -- evaluates contextual factors. Reports `"Access within business hours, from corporate network"`. Returns `check_context` = `true`.

**EnforceDecisionWorker** (`rbac_enforce_decision`) -- enforces the access decision. Reports `"Access ALLOWED -- logged for audit"`. Returns `enforce_decision` = `true` and `completedAt` = `"2024-01-15T10:30:00Z"`.

## Workflow Output

The workflow produces `roles`, `permitted`, `contextAllowed`, `decision` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `rbac_resolve_roles`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `rbac_evaluate_permissions`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `rbac_check_context`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `rbac_enforce_decision`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `authorization_rbac_workflow` defines 4 tasks with input parameters `userId`, `resource`, `action` and a timeout of `1800` seconds.

## Tests

8 tests verify role resolution, permission evaluation, context checking, decision enforcement, and the full RBAC pipeline.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
