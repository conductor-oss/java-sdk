# Kyc Aml

KYC/AML workflow that verifies customer identity, screens against watchlists, assesses risk, and makes a compliance decision.

**Input:** `customerId`, `name`, `nationality`, `documentType` | **Timeout:** 60s

## Pipeline

```
kyc_verify_identity
    │
kyc_screen_watchlists
    │
kyc_assess_risk
    │
kyc_decide
```

## Workers

**AssessRiskWorker** (`kyc_assess_risk`): Assesses overall KYC risk based on identity verification and watchlist results.

```java
String riskLevel = riskScore >= 60 ? "high" : riskScore >= 30 ? "medium" : "low";
```

Reads `identityVerified`, `watchlistHits`. Outputs `riskScore`, `riskLevel`, `factors`.

**DecideWorker** (`kyc_decide`): Makes a compliance decision based on the assessed risk level.

Reads `riskLevel`. Outputs `decision`, `reviewRequired`, `decidedAt`.

**ScreenWatchlistsWorker** (`kyc_screen_watchlists`): Screens customer against OFAC, PEP, and adverse media watchlists.

Reads `name`. Outputs `hits`, `listsChecked`, `clearanceStatus`.

**VerifyIdentityWorker** (`kyc_verify_identity`): Verifies customer identity using the provided document type.

Reads `documentType`, `name`. Outputs `verified`, `documentAuthentic`, `matchScore`, `verifiedAt`.

## Tests

**21 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
