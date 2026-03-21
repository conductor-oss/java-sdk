# Role Management in Java Using Conductor

## The Problem

An admin requests to assign the "editor" role to a team member. The system needs to log the role request with the requester's identity, validate that the assignment complies with access policies and doesn't conflict with existing roles, assign the role with its associated permissions (read, write), and sync those permissions to the IAM provider, API gateway, and database layer. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the request-logging, policy-validation, role-assignment, and permission-sync workers. Conductor handles the role lifecycle and cross-system propagation.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

RequestRoleWorker logs the assignment request, ValidateRoleWorker checks policy compliance, AssignRoleWorker grants permissions, and SyncPermissionsWorker propagates to IAM, API gateway, and database targets.

| Worker | Task | What It Does |
|---|---|---|
| **AssignRoleWorker** | `rom_assign` | Assigns the role to the user with its associated permissions (e.g., admin gets read, write, delete, manage_users) |
| **RequestRoleWorker** | `rom_request_role` | Logs the role assignment request with the requester's identity and assigns a unique request ID |
| **SyncPermissionsWorker** | `rom_sync_permissions` | Propagates the role's permissions to IAM, API gateway, and database targets |
| **ValidateRoleWorker** | `rom_validate` | Validates that the requested role assignment complies with access policies and checks for conflicting roles |

Replace with real identity provider and database calls and ### The Workflow

```
rom_request_role
 │
 ▼
rom_validate
 │
 ▼
rom_assign
 │
 ▼
rom_sync_permissions

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
