# Advertising Workflow

Orchestrates advertising workflow through a multi-stage Conductor workflow.

**Input:** `campaignId`, `advertiserId`, `budget`, `objective` | **Timeout:** 60s

## Pipeline

```
adv_create_campaign
    │
adv_target_audience
    │
adv_set_bids
    │
adv_serve_ads
    │
adv_generate_report
```

## Workers

**CreateCampaignWorker** (`adv_create_campaign`)

Reads `creativeId`. Outputs `creativeId`, `adFormats`, `createdAt`.

**GenerateReportWorker** (`adv_generate_report`)

Reads `reportUrl`. Outputs `reportUrl`.

**ServeAdsWorker** (`adv_serve_ads`)

Reads `impressions`. Outputs `impressions`, `clicks`, `conversions`, `spend`, `ctr`.

**SetBidsWorker** (`adv_set_bids`)

Reads `bidStrategy`. Outputs `bidStrategy`, `dailyBudget`, `maxBid`.

**TargetAudienceWorker** (`adv_target_audience`)

Reads `audienceSize`. Outputs `audienceSize`, `segments`, `demographics`, `interests`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
