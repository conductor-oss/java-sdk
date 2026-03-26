# Tax Calculation

Tax calculation: determine jurisdiction, calculate rates, apply tax, report

**Input:** `orderId`, `subtotal`, `shippingAddress` | **Timeout:** 60s

## Pipeline

```
tax_determine_jurisdiction
    │
tax_calculate_rates
    │
tax_apply
    │
tax_report
```

## Workers

**ApplyTaxWorker** (`tax_apply`)

```java
double taxAmount = Math.round(subtotal * rate * 100.0) / 100.0;
double total = Math.round((subtotal + taxAmount) * 100.0) / 100.0;
```

**CalculateRatesWorker** (`tax_calculate_rates`)

```java
double combinedRate = stateRate + countyRate + cityRate;
```

**DetermineJurisdictionWorker** (`tax_determine_jurisdiction`)

**TaxReportWorker** (`tax_report`)

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
