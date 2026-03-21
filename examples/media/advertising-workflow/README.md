# Advertising Campaign Pipeline in Java Using Conductor : Creative Setup, Audience Targeting, Bid Strategy, Ad Serving, and Performance Reporting

## Why Ad Campaign Management Needs Orchestration

Launching a digital ad campaign involves a strict sequence where each step depends on the previous one. You create the campaign creative. specifying ad formats (banner 300x250, video pre-roll, native cards) and associating creative assets. You define the target audience, selecting interest segments (tech professionals), demographic filters (age, location), and reaching an estimated audience of 2.5 million. You configure the bid strategy, target CPA, daily budget derived from the total campaign budget, and maximum bid caps. You activate ad serving and collect performance data: 850K impressions, 12.7K clicks, 425 conversions, $8,500 spend. Finally, you generate a campaign report summarizing ROI and delivery metrics.

If audience targeting fails, you must not start serving ads to an undefined audience. If bid configuration returns an error, you cannot proceed to ad serving with uncapped spend. Without orchestration, you'd build a monolithic ad platform integration that mixes creative management, audience APIs, bidding logic, and reporting. making it impossible to swap your DSP, test bid strategies independently, or audit which targeting parameters drove which performance outcomes.

## How This Workflow Solves It

**You just write the campaign workers. Creative setup, audience targeting, bid configuration, ad serving, and performance reporting. Conductor handles creative-to-report sequencing, ad platform retries, and complete records linking targeting parameters to delivery metrics.**

Each campaign stage is an independent worker. create campaign, target audience, set bids, serve ads, generate report. Conductor sequences them, passes creative IDs and audience segments between steps, retries if an ad platform API times out, and maintains a complete audit trail linking every campaign configuration to its delivery metrics.

### What You Write: Workers

Five workers orchestrate the ad campaign: CreateCampaignWorker defines creatives, TargetAudienceWorker segments viewers, SetBidsWorker configures bidding strategy, ServeAdsWorker tracks impressions and clicks, and GenerateReportWorker summarizes ROI.

| Worker | Task | What It Does |
|---|---|---|
| **CreateCampaignWorker** | `adv_create_campaign` | Creates the campaign |
| **GenerateReportWorker** | `adv_generate_report` | Generates the report |
| **ServeAdsWorker** | `adv_serve_ads` | Handles serve ads |
| **SetBidsWorker** | `adv_set_bids` | Sets bids |
| **TargetAudienceWorker** | `adv_target_audience` | Targets the audience |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction, with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
adv_create_campaign
 │
 ▼
adv_target_audience
 │
 ▼
adv_set_bids
 │
 ▼
adv_serve_ads
 │
 ▼
adv_generate_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
