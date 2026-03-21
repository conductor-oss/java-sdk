# Email Campaign Pipeline in Java Using Conductor : Audience Segmentation, Personalization, Sending, Engagement Tracking, and Performance Analysis

## Why Email Campaigns Need Orchestration

Running an email campaign involves a strict pipeline where sending before segmentation or tracking before sending produces incorrect results. You segment your subscriber list into targeted cohorts, suppress unsubscribed users, and count recipients. You personalize the email template with merge fields (first name, purchase history, recommended products) and create A/B variants. You send the campaign in batches, handling bounces in real time. You track engagement. open rates, click-through rates, unique opens, unique clicks, unsubscribe rates. Finally, you analyze the results against industry benchmarks to measure campaign effectiveness.

If segmentation produces an empty list, you must not send. If the send fails partway through, you need to know which batch completed so you can resume without double-sending. Without orchestration, you'd build a monolithic email system that mixes list management, template rendering, SMTP delivery, event tracking, and analytics. making it impossible to swap your ESP, test personalization independently, or trace which segment received which variant.

## How This Workflow Solves It

**You just write the campaign workers. Audience segmentation, content personalization, batch sending, engagement tracking, and results analysis. Conductor handles batch send sequencing, ESP retries, and a full audit trail from segmentation through delivery metrics.**

Each campaign stage is an independent worker. segment audience, personalize, send, track engagement, analyze results. Conductor sequences them, passes recipient counts and send IDs between stages, retries if an ESP API times out, and provides a complete audit trail from segmentation through delivery metrics.

### What You Write: Workers

Five workers power the campaign pipeline: SegmentAudienceWorker builds targeted cohorts, PersonalizeWorker merges recipient data into templates, SendCampaignWorker delivers in batches, TrackEngagementWorker monitors opens and clicks, and AnalyzeResultsWorker benchmarks performance.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeResultsWorker** | `eml_analyze_results` | Analyzes results |
| **PersonalizeWorker** | `eml_personalize` | Personalizes the content and computes personalized count, variants created, merge fields used |
| **SegmentAudienceWorker** | `eml_segment_audience` | Segments the audience |
| **SendCampaignWorker** | `eml_send_campaign` | Sends the campaign |
| **TrackEngagementWorker** | `eml_track_engagement` | Tracks the engagement |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction, with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
eml_segment_audience
 │
 ▼
eml_personalize
 │
 ▼
eml_send_campaign
 │
 ▼
eml_track_engagement
 │
 ▼
eml_analyze_results

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
