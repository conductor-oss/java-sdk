# Menu Management

Orchestrates menu management through a multi-stage Conductor workflow.

**Input:** `restaurantId`, `menuName` | **Timeout:** 60s

## Pipeline

```
mnu_create_items
    │
mnu_price
    │
mnu_categorize
    │
mnu_publish
    │
mnu_update
```

## Workers

**CategorizeWorker** (`mnu_categorize`)

Outputs `menu`.

**CreateItemsWorker** (`mnu_create_items`)

Reads `restaurantId`. Outputs `items`.

**PriceWorker** (`mnu_price`)

Outputs `pricedItems`.

**PublishWorker** (`mnu_publish`)

Reads `menuName`. Outputs `menuId`, `published`, `publishedAt`.

**UpdateWorker** (`mnu_update`)

```java
result.addOutputData("menu", Map.of("menuId", menuId != null ? menuId : "MENU-734", "status", "LIVE", "items", 3));
```

Reads `menuId`. Outputs `menu`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
