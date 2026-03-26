# Supply Chain Mgmt

End-to-end supply chain flow: plan, source, make, deliver, and handle returns.

**Input:** `product`, `quantity`, `destination` | **Timeout:** 60s

## Pipeline

```
scm_plan
    │
scm_source
    │
scm_make
    │
scm_deliver
    │
scm_return
```

## Workers

**DeliverWorker** (`scm_deliver`): Ships the batch to the destination.

Reads `batchId`, `destination`. Outputs `deliveryId`, `eta`.

**MakeWorker** (`scm_make`): Manufactures the product.

Reads `product`, `quantity`. Outputs `batchId`, `unitsProduced`.

**PlanWorker** (`scm_plan`): Creates a production plan based on product and quantity.

```java
Map.of("name", "steel", "qty", quantity * 2),
```

Reads `product`, `quantity`. Outputs `planId`, `materials`.

**ReturnWorker** (`scm_return`): Configures the return policy for a delivery.

Reads `deliveryId`. Outputs `returnPolicy`, `active`.

**SourceWorker** (`scm_source`): Sources materials from suppliers.

Reads `materials`. Outputs `sourcedMaterials`, `suppliersUsed`.

## Tests

**17 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
