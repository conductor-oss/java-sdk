# Email Campaign

Orchestrates email campaign through a multi-stage Conductor workflow.

**Input:** `campaignId`, `subject`, `templateId`, `listId` | **Timeout:** 60s

## Pipeline

```
eml_segment_audience
    │
eml_personalize
    │
eml_send_campaign
    │
eml_track_engagement
    │
eml_analyze_results
```

## Workers

**AnalyzeResultsWorker** (`eml_analyze_results`)

Reads `industryBenchmark`. Outputs `industryBenchmark`, `clickRate`.

**PersonalizeWorker** (`eml_personalize`)

Reads `personalizedCount`, `recipientCount`. Outputs `personalizedCount`, `variantsCreated`, `mergeFieldsUsed`.

**SegmentAudienceWorker** (`eml_segment_audience`)

Reads `segments`. Outputs `segments`, `recipientCount`, `suppressedCount`.

**SendCampaignWorker** (`eml_send_campaign`)

Reads `personalizedCount`, `sendId`. Outputs `sendId`, `sentCount`, `bouncedCount`, `sentAt`.

**TrackEngagementWorker** (`eml_track_engagement`)

Reads `openRate`. Outputs `openRate`, `clickRate`, `unsubscribeRate`, `uniqueOpens`, `uniqueClicks`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
