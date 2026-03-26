# Tax Filing

Tax filing: collect data, calculate tax, validate, file return, confirm.

**Input:** `taxpayerId`, `taxYear`, `filingType` | **Timeout:** 60s

## Pipeline

```
txf_collect_data
    │
txf_calculate_tax
    │
txf_validate_filing
    │
txf_file_return
    │
txf_confirm_submission
```

## Workers

**CalculateTaxWorker** (`txf_calculate_tax`)

```java
long taxLiability = Math.round(taxable * 0.22 - credits);
```

Reads `credits`, `deductions`, `grossIncome`. Outputs `taxLiability`, `taxableIncome`, `effectiveRate`.

**CollectDataWorker** (`txf_collect_data`)

Reads `taxYear`, `taxpayerId`. Outputs `grossIncome`, `deductions`, `credits`, `w2Count`, `form1099Count`.

**ConfirmSubmissionWorker** (`txf_confirm_submission`)

Reads `confirmationNumber`, `filingId`. Outputs `confirmed`, `expectedRefundDate`, `receiptId`.

**FileReturnWorker** (`txf_file_return`)

Reads `taxYear`, `taxpayerId`. Outputs `filingId`, `confirmationNumber`, `filedAt`, `status`.

**ValidateFilingWorker** (`txf_validate_filing`)

Reads `taxLiability`. Outputs `validated`, `validationChecks`, `warnings`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
