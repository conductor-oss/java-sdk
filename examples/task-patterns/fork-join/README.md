# Fork Join in Java with Conductor

FORK_JOIN demo: fetch product details, inventory status, and customer reviews in parallel, then merge into a unified product page. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers. You write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

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

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic, the task pattern and Conductor orchestration remain unchanged.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Parallel execution** | FORK_JOIN runs multiple tasks simultaneously and waits for all to complete |

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

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build
```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build
```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/fork-join-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/fork-join-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow fork_join_demo \
  --version 1 \
  --input '{"productId": "PROD-001"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w fork_join_demo -s COMPLETED -c 5
```

## Example Output

```
=== FORK_JOIN Demo: Parallel Product Page Assembly ===

Step 1: Registering task definitions...
  Registered: fj_get_product, fj_get_inventory, fj_get_reviews, fj_merge_results

Step 2: Registering workflow 'fork_join_demo'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...

  Workflow ID: 3f8a1b2c-...

Step 5: Waiting for completion...
  [fj_get_product] Fetching product details for: PROD-001
  [fj_get_inventory] Checking inventory for: PROD-001
  [fj_get_reviews] Fetching reviews for: PROD-001
  [fj_merge_results] Building product page:
    -> Wireless Headphones | $79.99 |  | 0 stars (0 reviews)



  Status: COMPLETED
  Output: {productPage=productPage-value}

Result: PASSED
```
## How to Extend

Connect the data-fetching workers to your product catalog, inventory system (SAP, NetSuite), and reviews service (Bazaarvoice, Yotpo), and the parallel assembly works unchanged.

- **GetProductWorker** (`fj_get_product`): query your product catalog service or database (PostgreSQL, MongoDB, Elasticsearch) for the product name, description, price, images, and category
- **GetInventoryWorker** (`fj_get_inventory`): call your inventory management system (SAP, NetSuite, or a custom microservice) for real-time stock levels, warehouse locations, and reorder status
- **GetReviewsWorker** (`fj_get_reviews`): fetch customer reviews from your reviews service, Bazaarvoice, or Yotpo, returning average rating, review count, and recent review text
- **MergeResultsWorker** (`fj_merge_results`): add SEO metadata, compute pricing tiers (wholesale vs: retail), inject personalized recommendations, and cache the assembled page in Redis

Connecting each worker to a real microservice API does not change the parallel fetch-then-merge workflow, provided each returns the expected product, inventory, or review data structure.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>
```

## Project Structure

```
fork-join/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/forkjoin/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ForkJoinExample.java         # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GetInventoryWorker.java
│       ├── GetProductWorker.java
│       ├── GetReviewsWorker.java
│       └── MergeResultsWorker.java
└── src/test/java/forkjoin/workers/
    ├── GetInventoryWorkerTest.java   # 7 tests
    ├── GetProductWorkerTest.java     # 6 tests
    ├── GetReviewsWorkerTest.java     # 7 tests
    └── MergeResultsWorkerTest.java   # 9 tests
```
