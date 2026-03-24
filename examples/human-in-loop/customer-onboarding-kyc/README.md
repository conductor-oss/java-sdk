# Customer Onboarding Kyc

Customer Onboarding KYC -- auto-approves low-risk customers, routes high-risk to manual review via WAIT task.

**Input:** `customerId`, `customerName`, `riskLevel` | **Timeout:** 3600s

**Output:** `activated`, `customerId`, `needsReview`

## Pipeline

```
kyc_check
    │
kyc_review_decision [SWITCH]
  ├─ true: manual_kyc_review
  └─ default: kyc_auto_approved
    │
kyc_activate
```

## Workers

**KycActivateWorker** (`kyc_activate`): Worker for kyc_activate task -- activates the customer account after KYC approval.

Outputs `activated`, `customerId`, `customerName`.

**KycCheckWorker** (`kyc_check`): Worker for kyc_check task -- performs automated KYC risk assessment.

Outputs `customerId`, `customerName`, `riskLevel`, `needsReview`, `flags`.

## Workflow Output

- `activated`: `${kyc_activate_ref.output.activated}`
- `customerId`: `${workflow.input.customerId}`
- `needsReview`: `${kyc_check_ref.output.needsReview}`

## Data Flow

**kyc_check**: `customerId` = `${workflow.input.customerId}`, `customerName` = `${workflow.input.customerName}`, `riskLevel` = `${workflow.input.riskLevel}`
**kyc_review_decision** [SWITCH]: `needsReview` = `${kyc_check_ref.output.needsReview}`
**kyc_activate**: `customerId` = `${workflow.input.customerId}`, `customerName` = `${workflow.input.customerName}`

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
