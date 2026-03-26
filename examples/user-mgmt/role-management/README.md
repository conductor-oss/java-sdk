# Role Management

Orchestrates role management through a multi-stage Conductor workflow.

**Input:** `userId`, `requestedRole`, `requestedBy` | **Timeout:** 60s

## Pipeline

```
rom_request_role
    │
rom_validate
    │
rom_assign
    │
rom_sync_permissions
```

## Workers

**AssignRoleWorker** (`rom_assign`)

Reads `role`. Outputs `assigned`, `permissions`.

**RequestRoleWorker** (`rom_request_role`)

Reads `requestedBy`, `role`, `userId`. Outputs `requestId`, `logged`.

**SyncPermissionsWorker** (`rom_sync_permissions`)

Outputs `synced`, `targets`.

**ValidateRoleWorker** (`rom_validate`)

Reads `role`, `userId`. Outputs `approved`, `conflictingRoles`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
