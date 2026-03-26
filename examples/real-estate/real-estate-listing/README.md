# Real Estate Listing

Orchestrates real estate listing through a multi-stage Conductor workflow.

**Input:** `address`, `price`, `agentId` | **Timeout:** 60s

## Pipeline

```
rel_create
    │
rel_verify
    │
rel_enrich
    │
rel_publish
    │
rel_distribute
```

## Workers

**CreateListingWorker** (`rel_create`)

Reads `address`, `price`. Outputs `listing`.

**DistributeListingWorker** (`rel_distribute`)

Outputs `channels`, `distributed`.

**EnrichListingWorker** (`rel_enrich`)

Outputs `enrichedListing`.

**PublishListingWorker** (`rel_publish`)

Outputs `listingId`, `published`.

**VerifyListingWorker** (`rel_verify`)

Outputs `verifiedListing`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
