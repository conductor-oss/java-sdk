# Flash Sale

Flash sale: prepare inventory, open sale, process orders, close, report

**Input:** `saleId`, `saleName`, `durationMinutes` | **Timeout:** 60s

## Pipeline

```
fls_prepare_inventory
    │
fls_open_sale
    │
fls_process_orders
    │
fls_close_sale
    │
fls_report
```

## Workers

**CloseSaleWorker** (`fls_close_sale`)

**OpenSaleWorker** (`fls_open_sale`)

**PrepareInventoryWorker** (`fls_prepare_inventory`)

**ProcessOrdersWorker** (`fls_process_orders`)

```java
int ordersFilled = Math.min(available, 275);
double revenue = ordersFilled * 62.50;
```

**ReportWorker** (`fls_report`)

```java
int soldOutPercent = total > 0 ? Math.round((float) filled / total * 100) : 0;
```

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
