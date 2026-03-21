# User-Generated Content Pipeline in Java Using Conductor : Submission, Moderation, Approval, Enrichment, and Publishing

## Why UGC Pipelines Need Orchestration

Processing user-generated content requires a pipeline that balances speed (users expect fast publication) with safety (you must not publish harmful content). You receive the submission and assign a queue position. You run automated moderation. spam detection, adult content classification, toxicity scoring, to catch clearly violating content. Content that passes moderation is approved (automatically or by a human moderator, depending on your confidence threshold). Approved content is enriched with auto-generated tags, sentiment analysis, readability scores, and language detection to improve discoverability. Finally, the enriched content is published with a public URL.

Each stage gates the next. you must not enrich or publish unmoderated content. If moderation flags borderline content, you need a different approval path (human review) than content that passes cleanly (auto-approve). Without orchestration, you'd build a monolithic UGC processor that mixes upload handling, ML inference for moderation, NLP enrichment, and database writes, making it impossible to tune moderation thresholds without risking the enrichment pipeline, add new enrichment types, or audit which moderation step rejected a specific submission.

## How This Workflow Solves It

**You just write the UGC workers. Submission intake, automated moderation, approval, metadata enrichment, and publishing. Conductor handles safety-gated sequencing, ML service retries, and a full audit trail from submission through publication.**

Each UGC stage is an independent worker. submit, moderate, approve, enrich, publish. Conductor sequences them, passes moderation scores and approval flags between stages, retries if an ML service times out, and maintains a complete audit trail from submission through publication for every piece of user content.

### What You Write: Workers

Five workers process user submissions: SubmitWorker queues incoming content, ModerateWorker runs automated safety checks, ApproveWorker gates publication, EnrichWorker adds auto-generated tags and sentiment analysis, and PublishWorker makes content live.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveWorker** | `ugc_approve` | Evaluates approval criteria and computes approval method, approved at |
| **EnrichWorker** | `ugc_enrich` | Enriches the data and computes metadata, auto tags, sentiment, readability |
| **ModerateWorker** | `ugc_moderate` | Moderates the content and computes moderation score, flagged, categories, spam |
| **PublishWorker** | `ugc_publish` | Publishes the content and computes published, publish url, published at |
| **SubmitWorker** | `ugc_submit` | Receives the user-submitted content, records the submission timestamp, and assigns a queue position for moderation review |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction, with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
ugc_submit
 │
 ▼
ugc_moderate
 │
 ▼
ugc_approve
 │
 ▼
ugc_enrich
 │
 ▼
ugc_publish

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
