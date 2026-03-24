# Loyalty Rewards

Orchestrates loyalty rewards through a multi-stage Conductor workflow.

**Input:** `customerId`, `orderTotal`, `redeemPoints` | **Timeout:** 60s

## Pipeline

```
lyr_earn_points
    │
lyr_check_tier
    │
lyr_redeem
    │
lyr_track
```

## Workers

**CheckTierWorker** (`lyr_check_tier`)

```java
String tier = total >= 2000 ? "Platinum" : total >= 1000 ? "Gold" : "Silver";
```

Reads `totalPoints`. Outputs `tier`, `totalPoints`.

**EarnPointsWorker** (`lyr_earn_points`)

```java
result.addOutputData("totalPoints", 1250 + earned);
```

Reads `customerId`, `orderTotal`. Outputs `earned`, `totalPoints`.

**RedeemWorker** (`lyr_redeem`)

Reads `redeemPoints`, `tier`. Outputs `redeemed`, `discount`, `success`.

**TrackWorker** (`lyr_track`)

Reads `customerId`, `earned`, `redeemed`. Outputs `loyalty`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
