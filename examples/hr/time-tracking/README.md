# Time Tracking

Time tracking workflow: submit, validate, approve, process.

**Input:** `employeeId`, `weekEnding`, `entries` | **Timeout:** 60s

## Pipeline

```
ttk_submit
    │
ttk_validate
    │
ttk_approve
    │
ttk_process
```

## Workers

**ApproveWorker** (`ttk_approve`)

Reads `timesheetId`. Outputs `approved`, `approvedBy`.

**ProcessWorker** (`ttk_process`)

Reads `totalHours`. Outputs `processed`, `payrollBatch`.

**SubmitWorker** (`ttk_submit`)

Reads `employeeId`, `weekEnding`. Outputs `timesheetId`, `submitted`.

**ValidateWorker** (`ttk_validate`)

Reads `timesheetId`. Outputs `totalHours`, `valid`, `overtime`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
