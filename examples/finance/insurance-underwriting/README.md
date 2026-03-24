# Insurance Underwriting

Insurance underwriting with SWITCH decision routing for accept/decline/refer.

**Input:** `applicationId`, `applicantName`, `coverageType`, `coverageAmount` | **Timeout:** 60s

## Pipeline

```
uw_collect_app
    │
uw_assess_risk
    │
uw_quote
    │
uw_route_decision [SWITCH]
  ├─ accept: uw_accept
  ├─ decline: uw_decline
  ├─ refer: uw_refer
  └─ default: uw_refer
    │
uw_bind
```

## Workers

**AcceptWorker** (`uw_accept`)

Reads `applicationId`, `premium`. Outputs `accepted`, `acceptedAt`.

**AssessRiskWorker** (`uw_assess_risk`)

```java
boolean smoker = Boolean.TRUE.equals(data.get("smoker"));
```

Reads `applicantData`, `coverageAmount`. Outputs `riskClass`, `decision`, `riskScore`, `declineReason`, `referReason`.

**BindWorker** (`uw_bind`)

```java
String policyNumber = "POL-" + String.valueOf(System.currentTimeMillis()).substring(5);
```

Reads `applicantName`, `decision`, `premium`. Outputs `policyNumber`, `bound`, `effectiveDate`, `premium`.

**CollectAppWorker** (`uw_collect_app`)

Reads `applicationId`, `coverageAmount`, `coverageType`. Outputs `applicantData`.

**DeclineWorker** (`uw_decline`)

Reads `applicationId`, `declineReason`. Outputs `declined`, `appealAvailable`.

**QuoteWorker** (`uw_quote`)

```java
double basePremium = (amount / 1000) * 1.5;
double premium = Math.round(basePremium * riskMultiplier * 100.0) / 100.0;
```

Reads `coverageAmount`, `riskClass`. Outputs `premium`, `premiumFrequency`, `quoteValidDays`.

**ReferWorker** (`uw_refer`)

Reads `applicationId`, `referReason`. Outputs `referred`, `assignedTo`.

## Tests

**6 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
