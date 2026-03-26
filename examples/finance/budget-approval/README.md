# Budget Approval

Budget approval with SWITCH for approve/revise/reject decisions.

**Input:** `budgetId`, `department`, `amount`, `justification` | **Timeout:** 60s

## Pipeline

```
bgt_submit_budget
    │
bgt_review_budget
    │
bgt_decision [SWITCH]
  ├─ approve: bgt_approve_budget
  ├─ revise: bgt_revise_budget
  └─ reject: bgt_reject_budget
    │
bgt_allocate_funds
```

## Workers

**AllocateFundsWorker** (`bgt_allocate_funds`)

```java
boolean allocated = "approve".equals(decision) || "revise".equals(decision);
```

Reads `decision`, `department`. Outputs `allocationStatus`.

**ApproveBudgetWorker** (`bgt_approve_budget`)

Reads `approver`, `budgetId`. Outputs `approved`, `approvalId`.

**RejectBudgetWorker** (`bgt_reject_budget`)

Reads `budgetId`, `reason`. Outputs `rejected`, `rejectionId`.

**ReviewBudgetWorker** (`bgt_review_budget`)

```java
String decision = amount <= 50000 ? "approve" : "revise";
```

Reads `amount`. Outputs `decision`, `reviewer`, `revisedAmount`, `comments`.

**ReviseBudgetWorker** (`bgt_revise_budget`)

Reads `budgetId`, `revisedAmount`. Outputs `revised`, `revisedAmount`, `revisionId`.

**SubmitBudgetWorker** (`bgt_submit_budget`)

Reads `amount`, `budgetId`, `department`. Outputs `submissionId`, `submittedAt`, `fiscalQuarter`.

## Tests

**6 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
