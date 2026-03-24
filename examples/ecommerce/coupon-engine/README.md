# Coupon Engine

Coupon engine: validate code, check eligibility, apply discount, record usage

**Input:** `couponCode`, `customerId`, `cartTotal`, `cartItems` | **Timeout:** 60s

## Pipeline

```
cpn_validate_code
    │
cpn_check_eligibility
    │
cpn_apply_discount
    │
cpn_record_usage
```

## Workers

**ApplyDiscountWorker** (`cpn_apply_discount`)

```java
discountAmount = Math.round(cartTotal * discountValue / 100.0 * 100.0) / 100.0;
double newTotal = Math.round((cartTotal - discountAmount) * 100.0) / 100.0;
```

Reads `cartTotal`, `couponCode`, `discountType`, `discountValue`. Outputs `discountAmount`, `newTotal`, `appliedAt`.

**CheckEligibilityWorker** (`cpn_check_eligibility`)

```java
boolean meetsMinimum = cartTotal >= minCartTotal;
```

Reads `cartTotal`, `couponRules`, `customerId`. Outputs `eligible`, `meetsMinimum`, `notExpired`, `usesRemaining`.

**RecordUsageWorker** (`cpn_record_usage`)

Reads `couponCode`, `customerId`, `discountApplied`. Outputs `recorded`, `usageId`, `recordedAt`.

**ValidateCodeWorker** (`cpn_validate_code`)

Reads `couponCode`. Outputs `valid`, `discountType`, `discountValue`, `rules`.

## Tests

**9 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
