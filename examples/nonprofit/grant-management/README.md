# Grant Management

Orchestrates grant management through a multi-stage Conductor workflow.

**Input:** `organizationName`, `grantProgram`, `requestedAmount` | **Timeout:** 60s

## Pipeline

```
gmt_apply
    │
gmt_review
    │
gmt_approve
    │
gmt_fund
    │
gmt_report
```

## Workers

**ApplyWorker** (`gmt_apply`)

Reads `organization`, `program`. Outputs `application`.

**ApproveWorker** (`gmt_approve`)

Reads `requestedAmount`, `reviewScore`. Outputs `approved`, `approvedAmount`.

**FundWorker** (`gmt_fund`)

Reads `approvedAmount`, `organization`. Outputs `grantId`, `funded`, `disbursedAt`.

**ReportWorker** (`gmt_report`)

Reads `grantId`, `organization`. Outputs `grant`.

**ReviewWorker** (`gmt_review`)

Reads `requestedAmount`. Outputs `score`, `recommendation`, `reviewer`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
