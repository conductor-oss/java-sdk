# Customer Churn

Orchestrates customer churn through a multi-stage Conductor workflow.

**Input:** `customerId`, `accountAge`, `usageTrend` | **Timeout:** 60s

## Pipeline

```
ccn_detect_risk
    │
ccn_analyze_reasons
    │
ccn_create_offer
    │
ccn_deliver
    │
ccn_track
```

## Workers

**AnalyzeReasonsWorker** (`ccn_analyze_reasons`)

Outputs `reasons`, `primaryReason`.

**CreateOfferWorker** (`ccn_create_offer`)

Outputs `offerId`, `offer`.

**DeliverWorker** (`ccn_deliver`)

Reads `customerId`. Outputs `delivered`, `channels`.

**DetectRiskWorker** (`ccn_detect_risk`)

Reads `customerId`. Outputs `riskScore`, `riskLevel`.

**TrackWorker** (`ccn_track`)

Reads `customerId`. Outputs `retained`, `acceptedAt`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
