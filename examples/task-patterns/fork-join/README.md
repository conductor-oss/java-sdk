# Parallel Data Aggregation with FORK_JOIN: Building a Product Page from Three Independent Services

A product detail page needs data from three backends: the product catalog (name, price, category), the inventory system (stock levels, warehouse location), and the reviews service (average rating, review count). Calling them sequentially takes 3x the latency of the slowest service. Calling them in parallel from the application means managing threads, error handling, and result aggregation in every controller that needs composite data.

This example uses Conductor's FORK_JOIN to dispatch three independent data fetches in parallel, wait for all three to complete, and merge their outputs into a single product page -- with strict validation that every branch contributed its result.

## Workflow Structure

```
FORK_JOIN ─┬─ fj_get_product    (catalog lookup by productId)
           ├─ fj_get_inventory  (hash-based stock levels + warehouse assignment)
           └─ fj_get_reviews    (hash-based ratings + review count)
           |
        JOIN (all three must complete)
           |
           v
fj_merge_results  (combine product + inventory + reviews -> productPage)
```

All three fork branches receive the same `productId` from workflow input. The JOIN waits for all three task references. The merge worker receives outputs wired as `${fj_get_product_ref.output.product}`, `${fj_get_inventory_ref.output.inventory}`, `${fj_get_reviews_ref.output.reviews}`.

## Branch 1: GetProductWorker (`fj_get_product`)

Looks up the product ID against a 5-item hardcoded catalog:

| ID | Name | Price | Category |
|---|---|---|---|
| PROD-001 | Wireless Headphones | $79.99 | Electronics |
| PROD-002 | Mechanical Keyboard | $129.99 | Electronics |
| PROD-003 | USB-C Hub | $49.99 | Accessories |
| PROD-004 | Standing Desk | $399.99 | Furniture |
| PROD-005 | Noise Cancelling Earbuds | $149.99 | Electronics |

Unknown product IDs return `FAILED_WITH_TERMINAL_ERROR` with a message naming the missing ID. This is the only branch where the data source is finite and failure is expected for invalid inputs.

## Branch 2: GetInventoryWorker (`fj_get_inventory`)

Generates deterministic inventory data from the product ID hash:

```java
int hash = Math.abs(productId.hashCode());
int quantity = hash % 500;
boolean inStock = quantity > 0;
String warehouse = WAREHOUSES[hash % WAREHOUSES.length];
```

Warehouse assignment cycles through `US-WEST-1`, `US-WEST-2`, `US-EAST-1`, `EU-WEST-1`, `AP-SOUTH-1`. The `inStock` flag is derived directly from `quantity > 0`, ensuring logical consistency. This branch never fails for valid product IDs -- it always produces an inventory record.

## Branch 3: GetReviewsWorker (`fj_get_reviews`)

Generates deterministic review metrics from the product ID hash:

```java
double averageRating = 1.0 + (hash % 41) / 10.0;  // range 1.0 - 5.0
int totalReviews = hash % 1000;
String topReview = SAMPLE_REVIEWS[hash % SAMPLE_REVIEWS.length];
```

The rating formula guarantees values between 1.0 and 5.0 inclusive. The top review is selected from 5 sample reviews.

## The Merge: MergeResultsWorker (`fj_merge_results`)

The merge worker is where branch independence meets strict assembly requirements. It validates that all three inputs are non-null before proceeding:

```java
if (product == null) {
    fail.setReasonForIncompletion(
        "Input 'product' is required -- the product fork branch must complete successfully");
}
```

Each null check names the specific branch that failed, making it immediately clear which parallel task caused the merge to fail.

The output `productPage` contains exactly 6 fields, each sourced from a specific branch:

| Field | Source Branch | Derivation |
|---|---|---|
| `name` | product | Direct from catalog |
| `price` | product | Direct from catalog |
| `available` | inventory | `inStock` boolean |
| `stock` | inventory | `quantity` integer |
| `rating` | reviews | `averageRating` double |
| `reviewCount` | reviews | `totalReviews` integer |

## Branch Independence

Every branch operates on immutable data derived solely from the `productId` input. There is no shared mutable state between branches. Running branches for `PROD-001` and `PROD-002` in any order produces the same results -- the tests explicitly verify this by comparing outputs across interleaved executions.

## What Happens When a Branch Fails

When the product branch fails (e.g., unknown `PROD-999`), Conductor's FORK_JOIN propagates the failure. The merge worker receives `null` for the failed branch's output and returns `FAILED_WITH_TERMINAL_ERROR` with a message identifying which branch is missing. The inventory and reviews branches may have already completed successfully -- their results are simply not used.

## Test Coverage

5 test classes, 36 tests:

**ForkJoinIntegrationTest (4 tests):** Full three-branch merge for `PROD-001`, branch independence across two products, failed branch causing merge failure, merge output containing all 6 fields from all 3 branches.

**GetProductWorkerTest (8 tests):** Known product lookup, unknown product failure with ID in error message, missing/blank/null productId, 4-field output, deterministic output across invocations.

**GetInventoryWorkerTest (8 tests):** Inventory field completeness, deterministic quantity/warehouse, different products getting different inventory, `inStock` matching `quantity > 0`, missing/blank/null productId.

**GetReviewsWorkerTest (8 tests):** Rating range validation (1.0-5.0), deterministic review data, different products getting different reviews, 4-field output, missing/blank/null productId.

**MergeResultsWorkerTest (8 tests):** Full merge, individual branch failures (product null, inventory null, reviews null), all branches null, missing input keys, out-of-stock handling, 6-field output.

---

> **How to run this example:** See [RUNNING.md](../../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
