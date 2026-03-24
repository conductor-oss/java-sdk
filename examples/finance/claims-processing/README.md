# Claims Processing

Insurance claims workflow: submit claim, verify details, assess damage, settle amount, and close claim.

**Input:** `claimId`, `policyId`, `claimType`, `description`, `amount` | **Timeout:** 60s

## Pipeline

```
clp_submit_claim
    │
clp_verify_details
    │
clp_assess_damage
    │
clp_settle_amount
    │
clp_close_claim
```

## Workers

**AssessDamageWorker** (`clp_assess_damage`): Assesses damage for a claim. Real damage assessment.

- Categorizes damage based on amount thresholds
- Applies claim-type-specific assessment factors

- `requested > 50000` &rarr; `"severe"`
- `requested > 10000` &rarr; `"major"`
- `requested > 2000` &rarr; `"moderate"`


Reads `claimType`, `requestedAmount`. Outputs `assessedAmount`, `damageCategory`, `assessmentFactor`, `assessorNotes`.

**CloseClaimWorker** (`clp_close_claim`): Closes a claim after settlement.

Reads `claimId`, `paymentMethod`, `settledAmount`. Outputs `claimStatus`, `closedDate`, `satisfactionSurveyId`, `totalPaid`.

**SettleAmountWorker** (`clp_settle_amount`): Settles claim amount. Real computation.

- Subtracts deductible based on damage category
- Determines payment method based on amount

- `settled > 10000` &rarr; `"wire_transfer"`
- `settled > 1000` &rarr; `"direct_deposit"`


Reads `assessedAmount`, `damageCategory`. Outputs `settledAmount`, `deductible`, `paymentMethod`, `estimatedPaymentDate`.

**SubmitClaimWorker** (`clp_submit_claim`): Submits an insurance claim with real validation.


Reads `amount`, `claimId`, `claimType`, `policyId`. Outputs `policyStatus`, `submissionDate`, `referenceNumber`, `validType`, `validAmount`.

**VerifyDetailsWorker** (`clp_verify_details`): Verifies policy details for a claim.


Reads `policyId`, `policyStatus`. Outputs `verified`, `policyHolder`, `coverageLimit`, `deductible`.

## Tests

**34 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
