# Virtual Economy

Orchestrates virtual economy through a multi-stage Conductor workflow.

**Input:** `playerId`, `type`, `amount`, `currency` | **Timeout:** 60s

## Pipeline

```
vec_transaction
    │
vec_validate
    │
vec_update_balance
    │
vec_audit
    │
vec_report
```

## Workers

**AuditWorker** (`vec_audit`)

Reads `newBalance`, `transactionId`. Outputs `audited`, `auditId`.

**ReportWorker** (`vec_report`)

Reads `playerId`, `transactionId`. Outputs `summary`.

**TransactionWorker** (`vec_transaction`)

Reads `amount`, `playerId`, `type`. Outputs `transactionId`, `transaction`.

**UpdateBalanceWorker** (`vec_update_balance`)

```java
r.addOutputData("newBalance", 5200 + amount); r.addOutputData("previousBalance", 5200);
```

Reads `amount`. Outputs `newBalance`, `previousBalance`.

**ValidateWorker** (`vec_validate`)

Reads `currency`. Outputs `valid`, `rateCheck`, `limitCheck`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
