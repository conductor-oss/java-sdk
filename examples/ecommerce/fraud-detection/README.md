# Real-Time Fraud Detection with Parallel Risk Scoring

A legitimate customer buys a $5,000 camera for a trip leaving in 2 hours. Your fraud system flags it: high amount, new merchant category, first purchase over $1,000. Card frozen. Customer calls support, furious, while their flight boards without them.

Meanwhile, an actual fraudster running $80 test charges across stolen cards sails through because no single transaction crosses your threshold.

The problem isn't the rules or the ML model. It's that rule checks, ML scoring, and velocity analysis run sequentially, so you either add latency to every transaction or skip checks to stay fast.

This example runs all three risk signals in parallel using Conductor's `FORK_JOIN`, combines them into a deterministic decision, and records every signal for audit compliance.

## How It Works

A transaction arrives. Conductor dispatches three independent risk assessments simultaneously:

```
analyze_transaction
        |
    FORK_JOIN ──────────────────────────
    |               |                  |
rule_check      ml_score       velocity_check
    |               |                  |
    └───────────── JOIN ───────────────┘
                    |
                 decide
```

**Total latency = slowest check, not sum of all three.** A 200ms rule check, 150ms ML score, and 180ms velocity check complete in 200ms total, not 530ms.

## The Five Workers

### AnalyzeTransactionWorker

Examines the raw transaction and builds a feature vector:

- **Amount deviation**: how far this purchase is from the customer's historical average
- **Merchant category**: maps merchant ID prefix to category (electronics, travel, crypto)
- **Distance from home**: estimates geographic anomaly from merchant location
- **New-merchant flag**: first time this customer has used this merchant

```java
// From AnalyzeTransactionWorker.java
double amountDeviation = Math.abs(amount - historicalAvg) / Math.max(historicalAvg, 1.0);
String category = MERCHANT_CATEGORIES.getOrDefault(merchantPrefix, "general");
```

### RuleCheckWorker

Evaluates 8 configurable fraud rules against the transaction:

| Rule | Weight | Trigger |
|------|--------|---------|
| High amount | 1.0 | amount > $1,000 |
| Very high amount | 2.0 | amount > $5,000 |
| New merchant + high value | 1.5 | new merchant AND amount > $500 |
| Round amount | 0.5 | amount is exact multiple of $100 |
| New account | 1.5 | account age < 30 days |
| Crypto merchant | 1.0 | merchant category is cryptocurrency |
| Off-hours | 0.5 | transaction between 1am-5am local time |
| Velocity flag | 1.0 | > 3 transactions in 10 minutes |

**Risk levels**: `low_risk` (weight < 1.5), `medium_risk` (1.5-3.0), `high_risk` (> 3.0)

Thresholds are configurable via environment variables `FRAUD_RULE_MEDIUM_RISK_WEIGHT` and `FRAUD_RULE_HIGH_RISK_WEIGHT`.

### MlScoreWorker

Scores the transaction using a weighted feature model:

```
fraudScore = 0.35 * amountDeviation
           + 0.25 * distanceFromHome
           + 0.20 * newMerchantFlag
           + 0.10 * timeOfDayRisk
           + 0.10 * merchantCategoryRisk
```

Returns a score in `[0.0, 1.0]`, model version, confidence level, and the number of features evaluated. The score threshold is configurable: `FRAUD_BLOCK_THRESHOLD` (default 0.8) and `FRAUD_REVIEW_THRESHOLD` (default 0.5).

### VelocityCheckWorker

Analyzes transaction frequency patterns:

- **Rapid succession**: multiple transactions within 5 minutes
- **Unusual volume**: more than the customer's normal hourly rate
- **Geographic anomaly**: transactions from different regions within a short window

Returns `normal`, `elevated`, or `suspicious` with per-flag details.

### DecideWorker

Combines all three signals into a deterministic decision:

| Decision | When | Risk Score |
|----------|------|-----------|
| **BLOCK** | ML score > 0.8, OR rules = high_risk, OR velocity = suspicious | max(score, 0.85) |
| **REVIEW** | ML score > 0.5, OR rules = medium_risk | max(score, 0.55) |
| **APPROVE** | Everything else | score as-is |

Evaluated top-down: BLOCK is checked first, then REVIEW, then APPROVE (default).

## Error Handling

Every worker validates inputs and classifies errors:

- **Terminal errors** (will never succeed on retry): missing transaction ID, negative amount, invalid merchant ID format, ML score outside [0, 1]
- **Retryable errors** (Conductor will retry automatically): network timeouts to ML model, temporary database unavailability

```java
if (amount < 0) {
    result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
    result.setReasonForIncompletion("Transaction amount cannot be negative: " + amount);
    return result;
}
```

## Testing

**74 tests** covering:

- **Unit tests**: each worker with valid inputs, boundary conditions, null/invalid inputs
- **Integration tests**: full pipeline data flow (analyze → fork → rule+ml+velocity → join → decide)
- **Failure paths**: negative amounts, missing merchant IDs, ML score out of range, velocity with no history
- **Decision logic**: verify BLOCK/REVIEW/APPROVE thresholds with exact boundary values

Run tests:
```bash
mvn test
```

## Configuration

| Variable | Default | What it controls |
|----------|---------|-----------------|
| `FRAUD_BLOCK_THRESHOLD` | 0.8 | ML score above this → BLOCK |
| `FRAUD_REVIEW_THRESHOLD` | 0.5 | ML score above this → REVIEW |
| `FRAUD_RULE_HIGH_RISK_WEIGHT` | 3.0 | Rule weight above this → high_risk |
| `FRAUD_RULE_MEDIUM_RISK_WEIGHT` | 1.5 | Rule weight above this → medium_risk |

## Production Notes

See [PRODUCTION.md](PRODUCTION.md) for deployment guidance, monitoring expectations, and security considerations.

---

> **How to run this example:** See [RUNNING.md](../../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
