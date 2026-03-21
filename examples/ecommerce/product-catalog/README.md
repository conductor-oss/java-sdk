# Product Catalog Management in Java Using Conductor :  Add, Validate, Enrich, Publish, Index

Product catalog management: add, validate, enrich, publish, and index products. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Product Listings Need Validation, Enrichment, and Indexing

A merchant adds a product: "Blue Widget, $24.99, Gadgets category." Before this listing goes live, it needs validation (price within category range, required images present, description meets minimum length), enrichment (generate SEO-optimized title and meta description, suggest related products, extract keywords for search), publication (make available on the storefront with correct routing and category placement), and search indexing (update the search engine so customers can find it).

Each step transforms the product data. Validation catches errors (negative price, missing category). Enrichment adds value (SEO metadata, related products). Publication makes it visible. Indexing makes it findable. If the search index update fails, the product is still published. customers can browse to it but can't search for it, so the indexing step should be retried independently.

## The Solution

**You just write the product validation, SEO enrichment, publishing, and search indexing logic. Conductor handles enrichment retries, publication sequencing, and catalog change tracking.**

`AddProductWorker` creates the product entry with SKU, name, price, category, description, and images. `ValidateWorker` checks data quality. required fields present, price within category norms, image dimensions and format, description length, and category validity. `EnrichWorker` generates SEO-optimized content,  meta title, meta description, keyword tags, and related product suggestions. `PublishWorker` makes the product available on the storefront with proper category placement and URL routing. `IndexWorker` updates the search index so the product appears in customer searches. Conductor chains these five steps and records each transformation for catalog audit.

### What You Write: Workers

Catalog workers handle product ingestion, enrichment, categorization, and publishing as discrete pipeline stages.

| Worker | Task | What It Does |
|---|---|---|
| **AddProductWorker** | `prd_add_product` | Adds a new product to the catalog and assigns a product ID. |
| **EnrichProductWorker** | `prd_enrich` | Enriches product metadata with SEO title, tags, and slug. |
| **IndexProductWorker** | `prd_index` | Indexes a product for search. |
| **PublishProductWorker** | `prd_publish` | Publishes a product to the storefront. |
| **ValidateProductWorker** | `prd_validate` | Validates product SKU and price. |

Workers implement e-commerce operations. payment processing, inventory checks, shipping,  with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
prd_add_product
    │
    ▼
prd_validate
    │
    ▼
prd_enrich
    │
    ▼
prd_publish
    │
    ▼
prd_index

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/product-catalog-1.0.0.jar

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
java -jar target/product-catalog-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow product_catalog \
  --version 1 \
  --input '{"sku": "sample-sku", "name": "test", "price": 100, "category": "general", "description": "sample-description"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w product_catalog -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real catalog stack. your PIM for validation, GPT-4 for SEO enrichment, Algolia or Elasticsearch for indexing, and the workflow runs identically in production.

- **EnrichWorker** (`prd_enrich`): use GPT-4 to generate SEO-optimized product descriptions from specifications, or integrate with Google Shopping Content API for rich product data
- **IndexWorker** (`prd_index`): update Algolia, Elasticsearch, or Typesense search indices for instant product discoverability with faceted search support
- **PublishWorker** (`prd_publish`): publish to multiple channels simultaneously: Shopify, Amazon (via SP-API), Google Shopping (via Merchant Center API), and your own storefront

Change your enrichment source or categorization taxonomy and the catalog pipeline structure remains the same.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
product-catalog/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/productcatalog/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ProductCatalogExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AddProductWorker.java
│       ├── EnrichProductWorker.java
│       ├── IndexProductWorker.java
│       ├── PublishProductWorker.java
│       └── ValidateProductWorker.java
└── src/test/java/productcatalog/workers/
    ├── AddProductWorkerTest.java        # 7 tests
    ├── EnrichProductWorkerTest.java        # 8 tests
    ├── IndexProductWorkerTest.java        # 8 tests
    ├── PublishProductWorkerTest.java        # 8 tests
    └── ValidateProductWorkerTest.java        # 8 tests

```
