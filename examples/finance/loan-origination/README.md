# Loan Origination

Loan origination: application intake, credit check, underwriting, approval, and funding.

**Input:** `applicationId`, `applicantId`, `loanAmount`, `loanType` | **Timeout:** 60s

## Pipeline

```
lnr_application
    │
lnr_credit_check
    │
lnr_underwrite
    │
lnr_approve
    │
lnr_fund
```

## Workers

**ApplicationWorker** (`lnr_application`): Receives a loan application and records intake details.

Reads `applicationId`, `loanAmount`, `loanType`. Outputs `received`, `employment`, `receivedAt`.

**ApproveWorker** (`lnr_approve`): Approves the loan after underwriting.

Reads `applicationId`, `approvedRate`, `underwritingDecision`. Outputs `approved`, `approvalNumber`.

**CreditCheckWorker** (`lnr_credit_check`): Runs a credit check for the loan applicant.

Reads `applicantId`. Outputs `creditScore`, `bureau`, `dti`, `delinquencies`, `openAccounts`.

**FundWorker** (`lnr_fund`): Disburses the loan funds to the applicant.

Reads `applicantId`, `loanAmount`. Outputs `funded`, `disbursementId`, `fundedAt`, `firstPaymentDate`.

**UnderwriteWorker** (`lnr_underwrite`): Underwrites the loan based on credit score and DTI ratio.

```java
String decision = (score >= 680 && dti <= 43) ? "approved" : "declined";
```

Reads `creditScore`, `dti`. Outputs `decision`, `interestRate`, `term`, `conditions`.

## Tests

**43 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
