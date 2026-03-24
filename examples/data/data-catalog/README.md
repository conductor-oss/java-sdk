# Data Catalog in Java Using Conductor : Asset Discovery, Classification, Tagging, and Search Indexing

## The Problem

Your organization has data scattered across databases, data lakes, APIs, and file stores. Nobody knows what data exists, where it lives, whether it contains PII, or who owns it. You need to build a catalog that answers these questions. That means crawling data sources to discover tables, columns, and datasets at a configurable scan depth, classifying each asset by category and PII sensitivity (using rule-based patterns for emails, SSNs, phone numbers), tagging assets with metadata labels (owner, domain, freshness, quality tier), and indexing everything into a searchable catalog so analysts and engineers can find what they need.

Without orchestration, you'd write a monolithic crawler that connects to every data source, scans schemas inline, runs classification rules, generates tags, and writes to a search index in one pass. If the scan of a large database times out, the entire catalog build fails. If the process crashes after discovering 500 assets but before classifying them, you'd restart the full crawl. Adding a new data source type or classification rule means modifying deeply coupled code with no visibility into which step failed or how long each phase took.

## The Solution

**You just write the asset discovery, classification, metadata tagging, and catalog indexing workers. Conductor handles the discovery-to-index pipeline sequencing, retries when data source connections time out, and tracking of how many assets were discovered, classified, and indexed.**

Each stage of catalog building is a simple, independent worker. The discovery worker crawls the configured data source at the requested scan depth and returns a list of assets (tables, columns, datasets). The classifier applies rules to determine each asset's category and PII sensitivity level. The tagger generates metadata labels based on classification results (data domain, owner, sensitivity tier). The indexer writes the fully tagged assets into a searchable catalog index. Conductor executes them in sequence, passes the evolving asset list between steps, retries if a data source connection times out, and tracks exactly how many assets were discovered, classified, tagged, and indexed.

### What You Write: Workers

Four workers build the catalog end-to-end: discovering data assets across schemas, classifying them by PII sensitivity and content type, tagging with metadata labels, and indexing into a searchable catalog.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyDataWorker** | `cg_classify_data` | Classifies data assets by PII and category. |
| **DiscoverAssetsWorker** | `cg_discover_assets` | Discovers data assets across schemas. |
| **IndexCatalogWorker** | `cg_index_catalog` | Indexes tagged assets into the catalog. |
| **TagMetadataWorker** | `cg_tag_metadata` | Tags data assets with metadata labels. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
cg_discover_assets
 │
 ▼
cg_classify_data
 │
 ▼
cg_tag_metadata
 │
 ▼
cg_index_catalog

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
