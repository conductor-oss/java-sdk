# Insurance Renewal

Orchestrates insurance renewal through a multi-stage Conductor workflow.

**Input:** `policyId`, `customerId`, `claimHistory` | **Timeout:** 60s

## Pipeline

```
irn_notify
    │
irn_review
    │
irn_reprice
    │
route_decision [SWITCH]
  ├─ renew: irn_process_renew
  └─ cancel: irn_process_cancel
```

## Workers

**NotifyWorker** (`irn_notify`)

Reads `policyId`. Outputs `notified`.

**ProcessCancelWorker** (`irn_process_cancel`)

Reads `policyId`. Outputs `cancelled`.

**ProcessRenewWorker** (`irn_process_renew`)

Reads `policyId`. Outputs `renewed`, `effectiveDate`.

**RepriceWorker** (`irn_reprice`)

Outputs `decision`, `newPremium`.

**ReviewWorker** (`irn_review`)

Reads `policyId`. Outputs `riskScore`, `claimsCount`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
