# Fork Join in Java with Conductor

FORK_JOIN demo: fetch product details, inventory status, and customer reviews in parallel, then merge into a unified product page. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to assemble a product detail page by fetching data from three independent microservices simultaneously. Product catalog (name, description, price), inventory service (stock level, warehouse location), and reviews service (ratings, review text). Calling them sequentially triples the page load time. All three queries use the same productId and are completely independent of each other, but the merge step cannot run until all three have responded.

Without orchestration, you'd fire three async HTTP calls, manage CompletableFutures or callbacks for each, write barrier logic to wait for all three, and handle the case where the reviews service is slow while the other two have already returned. If the inventory service times out, you need to decide whether to retry it independently or fail the entire page assembly. There is no record of what each service returned or how long each took.

## The Solution

**You just write the product, inventory, reviews, and merge workers. Conductor handles running all three fetches in parallel via FORK_JOIN and waiting for completion.**

This example demonstrates Conductor's FORK_JOIN task for parallel data aggregation. Three workers run simultaneously. GetProductWorker fetches catalog data (name, description, price), GetInventoryWorker queries stock levels and warehouse locations, and GetReviewsWorker retrieves ratings and review text. All three receive the same productId and execute concurrently. A JOIN task waits until all three branches complete, then MergeResultsWorker combines the product, inventory, and reviews data into a single product page object. If the reviews service is slow, Conductor waits for it while keeping the product and inventory results safe. If the inventory call fails, Conductor retries just that branch, the other two results are preserved.

### What You Write: Workers

Four workers demonstrate parallel data aggregation: GetProductWorker, GetInventoryWorker, and GetReviewsWorker each fetch from their respective microservice concurrently, then MergeResultsWorker combines all three into a unified product page.

| Worker | Task | What It Does |
|---|---|---|
| **GetProductWorker** | `fj_get_product` | Returns product catalog data (id, name, price, category) for the given productId. Defaults to "UNKNOWN" if productId is missing or blank. |
| **GetInventoryWorker** | `fj_get_inventory` | Returns inventory data (productId, inStock flag, quantity, warehouse location) for the given productId. Defaults to "UNKNOWN" if productId is missing or blank. |
| **GetReviewsWorker** | `fj_get_reviews` | Returns review data (productId, averageRating, totalReviews, topReview text) for the given productId. Defaults to "UNKNOWN" if productId is missing or blank. |
| **MergeResultsWorker** | `fj_merge_results` | Combines product, inventory, and review data into a single product page with name, price, availability, stock count, rating, and review count. Handles null inputs gracefully with safe defaults. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic, the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
FORK_JOIN
 ├── fj_get_product
 ├── fj_get_inventory
 └── fj_get_reviews
 │
 ▼
JOIN (wait for all branches)
fj_merge_results

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
