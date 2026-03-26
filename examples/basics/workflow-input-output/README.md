# Workflow Input Output

4-step price calculator pipeline demonstrating data flow between tasks using JSONPath expressions

**Input:** `productId`, `quantity`, `couponCode` | **Timeout:** 120s

## Pipeline

```
lookup_price
    │
apply_discount
    │
calculate_tax
    │
format_invoice
```

## Workers

**ApplyDiscountWorker** (`apply_discount`): Applies a coupon discount to the subtotal.

```java
double discountAmount = subtotal * discountRate;
```

Reads `couponCode`, `subtotal`. Outputs `couponCode`, `discountRate`, `discountAmount`, `discountedTotal`.

**CalculateTaxWorker** (`calculate_tax`): Calculates sales tax on the discounted total.

- `TAX_RATE` = `0.0825`

```java
double taxAmount = discountedTotal * TAX_RATE;
```

Reads `discountedTotal`. Outputs `taxRate`, `taxAmount`, `finalTotal`.

**FormatInvoiceWorker** (`format_invoice`): Formats a text invoice from all prior task outputs.

```java
sb.append(String.format("Coupon:     %s (%.0f%% off)%n", couponCode, discountRate * 100));
```

Reads `couponCode`, `discountAmount`, `discountRate`, `discountedTotal`, `finalTotal`. Outputs `invoice`.

**LookupPriceWorker** (`lookup_price`): Looks up product price from a built-in catalog and calculates subtotal.

```java
double subtotal = product.price() * quantity;
```

Reads `productId`, `quantity`. Outputs `productName`, `unitPrice`, `quantity`, `subtotal`.

## Tests

**27 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
