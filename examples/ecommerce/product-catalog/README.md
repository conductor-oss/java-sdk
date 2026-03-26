# Product Catalog

Product catalog management: add, validate, enrich, publish, and index products

**Input:** `sku`, `name`, `price`, `category`, `description` | **Timeout:** 60s

## Pipeline

```
prd_add_product
    │
prd_validate
    │
prd_enrich
    │
prd_publish
    │
prd_index
```

## Workers

**AddProductWorker** (`prd_add_product`): Adds a new product to the catalog and assigns a product ID.

Reads `name`, `sku`. Outputs `productId`, `createdAt`.

**EnrichProductWorker** (`prd_enrich`): Enriches product metadata with SEO title, tags, and slug.

Reads `category`, `name`, `productId`. Outputs `enrichedData`.

**IndexProductWorker** (`prd_index`): Indexes a product for search.

Reads `category`, `name`, `productId`. Outputs `indexed`, `indexedAt`.

**PublishProductWorker** (`prd_publish`): Publishes a product to the storefront.

Reads `productId`. Outputs `published`, `publishedAt`, `url`.

**ValidateProductWorker** (`prd_validate`): Validates product SKU and price.

```java
boolean valid = sku != null && !sku.isBlank() && price > 0;
```

Reads `price`, `productId`, `sku`. Outputs `valid`, `validatedAt`.

## Tests

**39 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
