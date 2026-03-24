# Content Syndication

Orchestrates content syndication through a multi-stage Conductor workflow.

**Input:** `contentId`, `title`, `platforms`, `publishDate` | **Timeout:** 60s

## Pipeline

```
syn_select_content
    │
syn_format_per_platform
    │
syn_distribute
    │
syn_track_performance
```

## Workers

**DistributeWorker** (`syn_distribute`)

Reads `distributedPlatforms`. Outputs `distributedPlatforms`, `distributedCount`, `urls`, `medium`, `devto`.

**FormatPerPlatformWorker** (`syn_format_per_platform`)

Reads `formattedVersions`. Outputs `formattedVersions`.

**SelectContentWorker** (`syn_select_content`)

Reads `contentBody`. Outputs `contentBody`, `metadata`, `category`, `wordCount`.

**TrackPerformanceWorker** (`syn_track_performance`)

Reads `trackingId`. Outputs `trackingId`, `trackingPixels`, `utmCampaign`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
