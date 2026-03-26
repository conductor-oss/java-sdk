# Data Catalog

A data engineering team maintains hundreds of datasets across S3 buckets, databases, and APIs, but nobody knows what half of them contain. Each dataset needs schema discovery, metadata tagging, freshness scoring, and registration in a central catalog so analysts can find and trust their sources.

## Pipeline

```
[cg_discover_assets]
     |
     v
[cg_classify_data]
     |
     v
[cg_tag_metadata]
     |
     v
[cg_index_catalog]
```

**Workflow inputs:** `dataSource`, `scanDepth`, `classificationRules`

## Workers

**ClassifyDataWorker** (task: `cg_classify_data`)

Classifies data assets by PII and category.

- `PII_COLUMNS` = List.of("email", "phone", "address", "ip_address")
- Reads `assets`. Writes `classified`, `classificationSummary`

**DiscoverAssetsWorker** (task: `cg_discover_assets`)

Discovers data assets across schemas.

- Writes `assets`, `assetCount`

**IndexCatalogWorker** (task: `cg_index_catalog`)

Indexes tagged assets into the catalog.

- Reads `taggedAssets`. Writes `indexedCount`, `catalogId`, `searchable`

**TagMetadataWorker** (task: `cg_tag_metadata`)

Tags data assets with metadata labels.

- Reads `classifiedAssets`. Writes `tagged`, `tagCount`

---

**32 tests** | Workflow: `data_catalog` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
