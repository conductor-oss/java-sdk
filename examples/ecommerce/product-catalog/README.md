# Product Catalog Management in Java Using Conductor : Add, Validate, Enrich, Publish, Index

Product catalog management: add, validate, enrich, publish, and index products. ## Product Listings Need Validation, Enrichment, and Indexing

A merchant adds a product: "Blue Widget, $24.99, Gadgets category." Before this listing goes live, it needs validation (price within category range, required images present, description meets minimum length), enrichment (generate SEO-optimized title and meta description, suggest related products, extract keywords for search), publication (make available on the storefront with correct routing and category placement), and search indexing (update the search engine so customers can find it).

Each step transforms the product data. Validation catches errors (negative price, missing category). Enrichment adds value (SEO metadata, related products). Publication makes it visible. Indexing makes it findable. If the search index update fails, the product is still published. customers can browse to it but can't search for it, so the indexing step should be retried independently.

## The Solution

**You just write the product validation, SEO enrichment, publishing, and search indexing logic. Conductor handles enrichment retries, publication sequencing, and catalog change tracking.**

`AddProductWorker` creates the product entry with SKU, name, price, category, description, and images. `ValidateWorker` checks data quality. required fields present, price within category norms, image dimensions and format, description length, and category validity. `EnrichWorker` generates SEO-optimized content, meta title, meta description, keyword tags, and related product suggestions. `PublishWorker` makes the product available on the storefront with proper category placement and URL routing. `IndexWorker` updates the search index so the product appears in customer searches. Conductor chains these five steps and records each transformation for catalog audit.

### What You Write: Workers

Catalog workers handle product ingestion, enrichment, categorization, and publishing as discrete pipeline stages.

| Worker | Task | What It Does |
|---|---|---|
| **AddProductWorker** | `prd_add_product` | Adds a new product to the catalog and assigns a product ID. |
| **EnrichProductWorker** | `prd_enrich` | Enriches product metadata with SEO title, tags, and slug. |
| **IndexProductWorker** | `prd_index` | Indexes a product for search. |
| **PublishProductWorker** | `prd_publish` | Publishes a product to the storefront. |
| **ValidateProductWorker** | `prd_validate` | Validates product SKU and price. |

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
