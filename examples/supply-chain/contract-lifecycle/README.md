# Contract Lifecycle

Contract lifecycle: draft, review, approve, execute, and renewal.

**Input:** `vendor`, `contractType`, `value`, `termMonths` | **Timeout:** 60s

## Pipeline

```
clf_draft
    │
clf_review
    │
clf_approve
    │
clf_execute
    │
clf_renew
```

## Workers

**ApproveWorker** (`clf_approve`): Approves a contract. Real threshold-based approval routing.

- `amount > 100000` &rarr; `"CEO"`
- `amount > 50000` &rarr; `"VP-Procurement"`

```java
boolean reviewPassed = Boolean.TRUE.equals(reviewPassedObj);
```

Reads `amount`, `reviewPassed`. Outputs `contractState`, `approved`, `approver`.

**DraftWorker** (`clf_draft`): Drafts a new contract. Real contract state machine - DRAFT state.

Reads `amount`, `vendorName`. Outputs `contractId`, `contractState`, `draftedAt`.

**ExecuteWorker** (`clf_execute`): Executes an approved contract.

```java
boolean approved = Boolean.TRUE.equals(approvedObj);
```

Reads `approved`. Outputs `contractState`, `executed`, `effectiveDate`, `expirationDate`.

**RenewWorker** (`clf_renew`): Renews an active contract.

```java
boolean wasExecuted = Boolean.TRUE.equals(executedObj);
```

Reads `executed`. Outputs `contractState`, `renewed`, `newExpirationDate`.

**ReviewWorker** (`clf_review`): Reviews a contract. Real review checklist validation.

```java
if (amount > 100000) reviewItems.add("Legal review: required");
```

Reads `amount`, `contractId`. Outputs `contractState`, `reviewPassed`, `reviewItems`.

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
