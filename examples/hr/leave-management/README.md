# Leave Management

Leave management workflow: request, check balance, approve, update, notify.

**Input:** `employeeId`, `leaveType`, `startDate`, `days` | **Timeout:** 60s

## Pipeline

```
lvm_request
    │
lvm_check_balance
    │
lvm_approve
    │
lvm_update
    │
lvm_notify
```

## Workers

**ApproveWorker** (`lvm_approve`)

Reads `requestId`. Outputs `approved`, `approvedBy`.

**CheckBalanceWorker** (`lvm_check_balance`)

Reads `leaveType`, `requested`. Outputs `balance`, `sufficient`.

**NotifyWorker** (`lvm_notify`)

Reads `approved`, `employeeId`. Outputs `notified`.

**RequestWorker** (`lvm_request`)

Reads `days`, `employeeId`, `leaveType`. Outputs `requestId`, `submitted`.

**UpdateWorker** (`lvm_update`)

```java
int remaining = 15 - days;
```

Reads `days`. Outputs `remainingBalance`, `updated`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
