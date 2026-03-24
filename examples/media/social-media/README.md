# Social Media

Orchestrates social media through a multi-stage Conductor workflow.

**Input:** `campaignId`, `platform`, `message`, `mediaUrl` | **Timeout:** 60s

## Pipeline

```
soc_create_content
    │
soc_schedule_post
    │
soc_publish_post
    │
soc_monitor_engagement
    │
soc_engage_responses
```

## Workers

**CreateContentWorker** (`soc_create_content`)

Reads `contentId`, `message`. Outputs `contentId`, `formattedMessage`, `hashtags`, `optimalTime`.

**EngageResponsesWorker** (`soc_engage_responses`)

Reads `responsesHandled`. Outputs `responsesHandled`, `replied`, `liked`, `flaggedForReview`.

**MonitorEngagementWorker** (`soc_monitor_engagement`)

Reads `impressions`. Outputs `impressions`, `likes`, `shares`, `commentsCount`, `mentionsCount`.

**PublishPostWorker** (`soc_publish_post`)

Reads `postId`. Outputs `postId`, `postUrl`, `publishedAt`.

**SchedulePostWorker** (`soc_schedule_post`)

Reads `optimalTime`, `scheduledTime`. Outputs `scheduledTime`, `scheduleId`, `timezone`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
