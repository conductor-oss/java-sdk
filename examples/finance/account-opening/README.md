# Account Opening: SSN Validation, ChexSystems Scoring, and Compliance Audit Trails

Opening a bank account requires validating a dozen things before a single dollar moves: the applicant's name matches a legal format, their SSN isn't all-zeros in any group, they're at least 18, the account type is one you actually offer, and the initial deposit meets the minimum for that product. Then you run a ChexSystems check for prior banking fraud, verify identity documents, and only then provision the account with a routing number. Every step must produce a compliance audit trail. This example implements the full KYC onboarding pipeline using [Conductor](https://github.com/conductor-oss/conductor), from regex-validated SSNs through deterministic credit scoring to account-type-specific welcome packages.

## The Workflow

```
acc_collect_info  -->  acc_verify_identity  -->  acc_credit_check  -->  acc_open_account  -->  acc_welcome
```

The `documents` output from collect flows into identity verification. The `chexScore` from credit check flows into account opening along with the `verified` flag from identity. The `accountNumber` from opening flows into the welcome package.

## Deep Dive: The Validation Layer in CollectInfoWorker

`CollectInfoWorker` performs five distinct validations using compiled regex patterns:

```java
private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z' .-]{1,99}$");
private static final Pattern SSN_PATTERN  = Pattern.compile("^\\d{3}-\\d{2}-\\d{4}$");
```

SSN validation goes beyond format -- after the regex passes, the worker splits on `-` and rejects SSNs where any group is all zeros (`"000"`, `"00"`, `"0000"`), matching actual Social Security Administration rules.

Date of birth validation parses ISO dates, rejects future dates and dates older than 120 years, and enforces a minimum age of 18 using `Period.between(birthDate, today).getYears()`.

Minimum deposit requirements vary by account type using a Java 17+ switch expression:

| Account Type | Minimum Deposit | Routing Number |
|---|---|---|
| `checking` | $25 | 021000089 |
| `savings` | $100 | 021000090 |
| `money_market` | $2,500 | 021000091 |
| `cd` | $1,000 | 021000092 |

Every validation result is captured in a compliance `auditTrail` map with timestamp, action (`"collect_applicant_info"`), and individual validation outcomes (`nameValidated`, `ssnFormatValidated`, `dobValidated`, etc.).

## ChexSystems Credit Check Algorithm

`CreditCheckWorker` computes a deterministic ChexSystems score from the applicant name hash:

```java
int chexScore = nameHash % 1000;          // 0-999 range
int bankruptcies = (nameHash / 1000) % 3; // 0-2
int closedForCause = (nameHash / 3000) % 2; // 0-1
boolean eligible = chexScore < 700 && bankruptcies == 0 && closedForCause == 0;
```

The eligibility threshold is a ChexSystems score below 700, zero bankruptcies, and zero accounts closed for cause. The same applicant name always produces the same score, making tests deterministic.

## Identity Verification Logic

`VerifyIdentityWorker` checks three document flags from the collect step: `driversLicense`, `ssn`, and `proofOfAddress`. All three must be `true` for verification to pass. The worker builds a list of verification methods used (`document_verification`, `ssn_trace`, `knowledge_based_auth`) and records failures with specific messages like `"Missing driver's license"`.

## Account Provisioning and Welcome Package

`OpenAccountWorker` generates account numbers using `SecureRandom`: `"ACCT-" + String.format("%010d", ...)`. The account opens only if `identityVerified == true` AND `chexScore < 700`. Rejection reasons are recorded in the audit trail: `"Identity not verified"` or `"ChexSystems score too high (800)"`.

`WelcomeWorker` assembles account-type-specific packages:
- **Checking**: online banking, mobile app, debit card, checks
- **Savings**: online banking, mobile app, savings goal tools
- **Money market**: online banking, mobile app, debit card, investment overview
- **CD**: online banking, mobile app, maturity tracker

The worker generates a real email body addressed to the applicant with their account number and package contents.

## Test Coverage

- **CollectInfoWorkerTest**: 10 tests -- valid application, SSN all-zeros rejection, wrong SSN pattern, missing SSN, underage DOB, future DOB, missing name, invalid account type, insufficient deposit, audit trail field verification
- **OpenAccountWorkerTest**: 8 tests -- successful opening, identity not verified, ChexSystems too high, routing number per account type, missing fields, audit trail decision tracking
- **WelcomeWorkerTest**: 4 tests -- welcome sent, checking includes debit card + checks, savings includes goal tools, email body contains name and account number
- **AccountOpeningIntegrationTest**: 3 tests -- full approved pipeline, rejection path with invalid SSN, audit trail present at every step

**25 tests total** covering KYC validation, credit decisions, and the complete audit chain.

## The Integration Test: Emily Davis Gets a Checking Account

The full pipeline test in `AccountOpeningIntegrationTest` walks "Emily Davis" through account opening with SSN `123-45-6789`, DOB `1990-05-15`, a checking account, and $1,000 deposit. The test verifies documents are collected (driver's license, SSN, proof of address), identity is verified using those documents, a ChexSystems credit check runs, the account opens with an `ACCT-` prefixed number, and a welcome package is sent with `welcomeSent: true`.

The rejection path test uses an invalid SSN (`"invalid-ssn"`) to demonstrate cascading failure: CollectInfoWorker marks `documents.ssn = false`, VerifyIdentityWorker finds SSN missing and sets `verified = false`, and OpenAccountWorker rejects the application with `FAILED_WITH_TERMINAL_ERROR` because `identityVerified` is false. The audit trail at each step records the specific failure reason.

---

> **How to run:** See [RUNNING.md](../../RUNNING.md) | **Production guidance:** See [PRODUCTION.md](PRODUCTION.md)
