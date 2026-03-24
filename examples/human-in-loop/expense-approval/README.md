# Expense Approval

Expense approval with policy validation and conditional human approval

**Output:** `approvalRequired`, `processed`

## Pipeline

```
exp_validate_policy
    │
approval_switch [SWITCH]
  └─ true: wait_for_approval
    │
exp_process
```

## Workers

**ProcessWorker** (`exp_process`): Processes the approved expense. Real processing.

```java
int daysToReimburse = amount > 5000 ? 14 : amount > 500 ? 7 : 3;
```

Reads `amount`, `category`. Outputs `processed`, `transactionId`, `reimbursementDays`, `processedAt`.

**ValidatePolicyWorker** (`exp_validate_policy`): Worker for exp_validate_policy task -- validates expense against policy rules.

- If amount > 100 OR category equals "travel", approvalRequired = "true"
- Otherwise, approvalRequired = "false"

```java
String approvalRequired = needsApproval ? "true" : "false";
```

Reads `amount`, `category`. Outputs `approvalRequired`.

## Workflow Output

- `approvalRequired`: `${validate_policy.output.approvalRequired}`
- `processed`: `${process.output.processed}`

## Data Flow

**exp_validate_policy**: `amount` = `${workflow.input.amount}`, `category` = `${workflow.input.category}`
**approval_switch** [SWITCH]: `approvalRequired` = `${validate_policy.output.approvalRequired}`

## Tests

**6 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
