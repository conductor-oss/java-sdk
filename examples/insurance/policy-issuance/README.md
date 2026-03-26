# Policy Issuance

Orchestrates policy issuance through a multi-stage Conductor workflow.

**Input:** `applicantId`, `coverageType`, `requestedAmount` | **Timeout:** 60s

## Pipeline

```
pis_underwrite
    │
pis_approve
    │
pis_generate_policy
    │
pis_issue
    │
pis_deliver
```

## Workers

**ApproveWorker** (`pis_approve`)

Outputs `approved`, `premium`.

**DeliverWorker** (`pis_deliver`)

Reads `policyId`. Outputs `delivered`, `method`.

**GeneratePolicyWorker** (`pis_generate_policy`)

Outputs `policyId`, `documentUrl`.

**IssueWorker** (`pis_issue`)

Reads `policyId`. Outputs `issued`, `effectiveDate`.

**UnderwriteWorker** (`pis_underwrite`)

Reads `applicantId`. Outputs `result`, `riskClass`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
