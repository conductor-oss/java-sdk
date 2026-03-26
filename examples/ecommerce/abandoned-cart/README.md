# Abandoned Cart

Abandoned cart recovery: detect, wait, remind, offer discount, convert

**Input:** `cartId`, `customerId`, `cartTotal` | **Timeout:** 60s

## Pipeline

```
abc_detect_abandonment
    │
abc_wait_period [WAIT]
    │
abc_send_reminder
    │
abc_offer_discount
    │
abc_convert
```

## Workers

**ConvertWorker** (`abc_convert`)

**DetectAbandonmentWorker** (`abc_detect_abandonment`)

**OfferDiscountWorker** (`abc_offer_discount`)

```java
int discount = total > 50 ? 15 : 10;
```

**SendReminderWorker** (`abc_send_reminder`)

## Tests

**7 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
