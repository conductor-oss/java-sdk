# Four Eyes Approval

Four-Eyes Approval -- two independent approvals required via parallel WAIT tasks in a FORK/JOIN.

**Input:** `requestId` | **Timeout:** 120s

**Output:** `submitted`, `finalized`

## Pipeline

```
fep_submit
    │
    ┌────────────┬────────────┐
    │ approver_1 │ approver_2 │
    └────────────┴────────────┘
fep_finalize
```

## Workers

**FinalizeWorker** (`fep_finalize`): Worker for fep_finalize -- finalizes the request after both approvals.

- finalized: true
- bothApproved: true if both approvers approved, false otherwise
- approval1: first approver's decision
- approval2: second approver's decision

```java
boolean approved1 = Boolean.TRUE.equals(a1) || "true".equals(String.valueOf(a1));
```

Reads `approval1`, `approval2`. Outputs `finalized`, `bothApproved`, `approval1`, `approval2`.

**SubmitWorker** (`fep_submit`): Submits a request for dual (four-eyes) approval.

```java
String tier2Role = amount > 50000 ? "Director" : "Manager";
```

Reads `amount`, `requestId`. Outputs `submitted`, `requestId`, `tier1Role`, `tier2Role`, `submittedAt`.

## Workflow Output

- `submitted`: `${fep_submit_ref.output.submitted}`
- `finalized`: `${fep_finalize_ref.output.finalized}`

## Data Flow

**fep_submit**: `requestId` = `${workflow.input.requestId}`
**fep_finalize**: `approval1` = `${approver_1_ref.output.approval}`, `approval2` = `${approver_2_ref.output.approval}`

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
