# Coupon Engine in Java Using Conductor : Validate Code, Check Eligibility, Apply Discount, Record Usage

Coupon engine: validate code, check eligibility, apply discount, record usage.

## Coupons Have Complex Validation Rules

A customer enters coupon code "SAVE20" at checkout. Before applying a 20% discount, the system must verify the code exists and is currently active (not expired, not past its redemption limit), check that the customer is eligible (hasn't used this code before, meets the minimum cart total of $50, cart contains items from eligible categories), apply the correct discount type (percentage off, fixed dollar amount, free shipping, buy-one-get-one), and record the usage atomically so the same code can't be reused.

Each validation step is independent: code validation doesn't depend on eligibility checking, but both must pass before the discount is applied. If the usage recording step fails, the discount should not be applied. otherwise the coupon appears used but the customer didn't get the discount. And every coupon redemption needs an audit trail for financial reconciliation.

## The Solution

**You just write the code validation, eligibility checking, discount calculation, and usage recording logic. Conductor handles validation retries, eligibility routing, and usage tracking for every coupon redemption.**

`ValidateCodeWorker` checks that the coupon code exists, is within its active date range, and hasn't exceeded its redemption limit. `CheckEligibilityWorker` verifies the customer meets the coupon's requirements. minimum cart total, eligible product categories, first-time or repeat customer restrictions, and per-customer usage limits. `ApplyDiscountWorker` calculates and applies the correct discount based on the coupon type (percentage, fixed, free shipping) and any caps (maximum discount amount). `RecordUsageWorker` atomically records the redemption to prevent reuse and updates the coupon's remaining redemption count. Conductor chains these four steps and records every redemption attempt (successful or rejected) for coupon analytics.

### What You Write: Workers

Validation, eligibility, discount calculation, and usage recording workers each handle one aspect of coupon redemption in complete isolation.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyDiscountWorker** | `cpn_apply_discount` | Applies the discount |
| **CheckEligibilityWorker** | `cpn_check_eligibility` | Performs the check eligibility operation |
| **RecordUsageWorker** | `cpn_record_usage` | Performs the record usage operation |
| **ValidateCodeWorker** | `cpn_validate_code` | Performs the validate code operation |

### The Workflow

```
cpn_validate_code
 │
 ▼
cpn_check_eligibility
 │
 ▼
cpn_apply_discount
 │
 ▼
cpn_record_usage

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
