# Kitchen Workflow

Orchestrates kitchen workflow through a multi-stage Conductor workflow.

**Input:** `orderId`, `tableId`, `items` | **Timeout:** 60s

## Pipeline

```
kit_receive_order
    │
kit_prep
    │
kit_cook
    │
kit_plate
    │
kit_serve
```

## Workers

**CookWorker** (`kit_cook`)

Reads `orderId`. Outputs `cooked`, `cookTime`, `temp`.

**PlateWorker** (`kit_plate`)

Reads `orderId`. Outputs `plated`, `presentation`.

**PrepWorker** (`kit_prep`)

Reads `orderId`. Outputs `prepped`, `prepTime`.

**ReceiveOrderWorker** (`kit_receive_order`)

Reads `orderId`. Outputs `parsedItems`, `station`.

**ServeWorker** (`kit_serve`)

```java
result.addOutputData("service", Map.of("orderId", orderId != null ? orderId : "ORD-735",
```

Reads `orderId`, `tableId`. Outputs `service`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
