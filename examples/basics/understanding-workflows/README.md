# Understanding Workflows

A 3-step order pipeline: validate, calculate total, send confirmation

**Input:** `orderId`, `customerEmail`, `items` | **Timeout:** 60s

## Pipeline

```
validate_order
    │
calculate_total
    │
send_confirmation
```

## Workers

**CalculateTotalWorker** (`calculate_total`): Calculates order totals from validated items.

- `TAX_RATE` = `0.08`

```java
subtotal += price * qty;
double tax = Math.round(subtotal * TAX_RATE * 100.0) / 100.0;
```

Reads `validatedItems`. Outputs `subtotal`, `tax`, `total`.

**SendConfirmationWorker** (`send_confirmation`): Sends an order confirmation email.

Reads `customerEmail`, `orderId`, `total`. Outputs `emailSent`, `recipient`.

**ValidateOrderWorker** (`validate_order`): Validates an order by checking each item and marking it as validated and in stock.

Reads `items`, `orderId`. Outputs `validatedItems`, `itemCount`.

## Tests

**12 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
