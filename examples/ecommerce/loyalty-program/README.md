# Loyalty Program in Java Using Conductor : Earn Points, Check Tier, Upgrade, Reward

Loyalty program: earn points, check tier, upgrade, deliver rewards. ## Loyalty Programs Drive Repeat Purchases When They Work Right

A customer spends $85 and should earn 85 points (1 point per dollar). Their total reaches 950 points, putting them past the 900-point Gold tier threshold. They should be upgraded to Gold and receive the Gold welcome reward (10% off next purchase + free shipping for 30 days). Getting any of these steps wrong. miscounted points, missed upgrade, wrong reward, erodes trust in the program.

The loyalty pipeline must be sequential: points are earned before the tier check, the tier check happens before the upgrade, and the upgrade happens before the reward. If the upgrade step fails, the points should still be credited. the customer earned them regardless. And every loyalty interaction needs tracking for program analytics: points earned, tiers achieved, rewards delivered, and redemption rates.

## The Solution

**You just write the points calculation, tier evaluation, upgrade, and reward delivery logic. Conductor handles tier evaluation sequencing, reward delivery retries, and points ledger audit trails.**

`EarnPointsWorker` calculates and credits points based on the purchase amount and any multipliers (double points on certain categories, bonus points during promotions). `CheckTierWorker` compares the customer's updated point total against tier thresholds (Silver at 500, Gold at 900, Platinum at 2000) and determines if an upgrade is warranted. `UpgradeTierWorker` processes the tier change. updating the customer's tier, setting the upgrade date, and triggering welcome communications. `RewardWorker` delivers tier-appropriate rewards, discount codes, free shipping, early access to sales, or birthday bonuses. Conductor records every loyalty interaction for program analytics and ROI tracking.

### What You Write: Workers

Points calculation, tier evaluation, upgrade processing, and reward delivery workers each manage one facet of the loyalty lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **CheckTierWorker** | `loy_check_tier` | Checks the tier |
| **EarnPointsWorker** | `loy_earn_points` | Awards points |
| **RewardWorker** | `loy_reward` | Distributes rewards and returns reward, tier |
| **UpgradeTierWorker** | `loy_upgrade_tier` | Evaluates upgrade eligibility the tier |

### The Workflow

```
loy_earn_points
 │
 ▼
loy_check_tier
 │
 ▼
loy_upgrade_tier
 │
 ▼
loy_reward

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
