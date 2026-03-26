# Inventory Optimization

Inventory optimization: analyze stock, calculate reorder, optimize, execute.

**Input:** `warehouse`, `skuList` | **Timeout:** 60s

## Pipeline

```
io_analyze_stock
    │
io_calculate_reorder
    │
io_optimize
    │
io_execute
```

## Workers

**AnalyzeStockWorker** (`io_analyze_stock`)

```java
.map(sku -> Map.<String, Object>of("sku", sku, "current", rand.nextInt(500) + 50, "dailyUsage", rand.nextInt(30) + 5))
```

Reads `skuList`, `warehouse`. Outputs `stockLevels`, `skuCount`.

**CalculateReorderWorker** (`io_calculate_reorder`)

Reads `stockLevels`. Outputs `reorderPlan`, `reorderCount`.

**ExecuteWorker** (`io_execute`)

```java
int orders = optimizedPlan != null ? optimizedPlan.size() : 0;
```

Reads `optimizedPlan`. Outputs `ordersPlaced`, `status`.

**OptimizeWorker** (`io_optimize`)

Reads `reorderPlan`. Outputs `optimizedPlan`, `costSavings`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
