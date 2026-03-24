# Loyalty Program

Loyalty program: earn points, check tier, upgrade, deliver rewards

**Input:** `customerId`, `purchaseAmount`, `currentTier` | **Timeout:** 60s

## Pipeline

```
loy_earn_points
    │
loy_check_tier
    │
loy_upgrade_tier
    │
loy_reward
```

## Workers

**CheckTierWorker** (`loy_check_tier`)

```java
if (total >= 5000 && "Silver".equals(current)) { newTier = "Gold"; upgradeEligible = true; }
else if (total >= 2000 && "Bronze".equals(current)) { newTier = "Silver"; upgradeEligible = true; }
```

**EarnPointsWorker** (`loy_earn_points`)

```java
int pointsEarned = (int) Math.round(amount * multiplier);
```

**RewardWorker** (`loy_reward`)

**UpgradeTierWorker** (`loy_upgrade_tier`)

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
