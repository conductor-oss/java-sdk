# Digital Asset Management

Digital asset management: ingest, AI auto-tag, version control, store, distribute via CDN

**Input:** `assetId`, `assetType`, `sourceUrl`, `projectId` | **Timeout:** 1800s

## Pipeline

```
dam_ingest_asset
    │
dam_auto_tag
    │
dam_version_control
    │
dam_store_asset
    │
dam_distribute_asset
```

## Workers

**AutoTagWorker** (`dam_auto_tag`): Auto-tags an asset using AI-based analysis.

Reads `assetType`. Outputs `tags`, `aiConfidence`, `colorPalette`.

**DistributeAssetWorker** (`dam_distribute_asset`): Distributes the asset to configured channels (website, mobile, portal).

Reads `projectId`. Outputs `distributedTo`, `cdnUrls`.

**IngestAssetWorker** (`dam_ingest_asset`): Ingests a digital asset from the source URL and stores the original.

Reads `assetId`, `assetType`. Outputs `storagePath`, `fileSizeMb`, `checksum`, `dimensions`.

**StoreAssetWorker** (`dam_store_asset`): Stores the tagged and versioned asset in the DAM repository.

Reads `tags`, `version`. Outputs `assetUrl`, `storedAt`, `indexed`.

**VersionControlWorker** (`dam_version_control`): Creates a version entry for the asset.

Reads `projectId`. Outputs `version`, `previousVersion`, `changeType`, `versionId`.

## Tests

**40 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
