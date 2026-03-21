# Loyalty Rewards in Java with Conductor

Processes restaurant loyalty rewards: calculating points earned, evaluating tier status, applying redemptions, and updating the customer's loyalty account. ## The Problem

You need to process loyalty rewards for a restaurant customer. The workflow calculates points earned from the current order, checks the customer's loyalty tier (bronze, silver, gold, platinum), processes any point redemptions the customer wants to apply, and updates their loyalty account. Earning points without checking the tier means missing tier-specific bonuses; redeeming without validating the balance allows overdrafts.

Without orchestration, you'd embed loyalty logic in the checkout flow. manually calculating points, querying tier status, validating redemption amounts, and updating balances in a single transaction, hoping the database update does not fail after the redemption has already been applied to the order.

## The Solution

**You just write the points calculation, tier evaluation, redemption processing, and account update logic. Conductor handles points calculation retries, tier evaluation, and reward redemption audit trails.**

Each loyalty concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (earn points, check tier, redeem, track), retrying if the loyalty database is unavailable, tracking every loyalty transaction, and resuming from the last step if the process crashes. ### What You Write: Workers

Purchase tracking, points calculation, tier evaluation, and reward redemption workers each manage one dimension of the restaurant loyalty program.

| Worker | Task | What It Does |
|---|---|---|
| **CheckTierWorker** | `lyr_check_tier` | Evaluates the customer's loyalty tier (Silver, Gold, or Platinum) based on total points |
| **EarnPointsWorker** | `lyr_earn_points` | Calculates points earned from the order total and adds them to the customer's balance |
| **RedeemWorker** | `lyr_redeem` | Redeems the requested points at the customer's tier, calculates the discount, and deducts from their balance |
| **TrackWorker** | `lyr_track` | Updates the customer's loyalty account with earned and redeemed points and records the transaction |

### The Workflow

```
lyr_earn_points
 │
 ▼
lyr_check_tier
 │
 ▼
lyr_redeem
 │
 ▼
lyr_track

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
