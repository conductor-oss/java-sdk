# Post Mortem Automation in Java with Conductor

Automates post-incident post-mortem generation using [Conductor](https://github.com/conductor-oss/conductor). This workflow gathers the incident timeline from alerts and response events, collects impact metrics (affected users, availability), drafts a structured post-mortem document with action items, and schedules a blameless review meeting.

## Learning From Incidents

Incident INC-2024-042 is resolved. Now the real work starts: piecing together what happened, when, and why. You need to reconstruct a timeline from 24 scattered events across PagerDuty, Slack, and deploy logs. You need to know the blast radius. 1,200 affected users, availability dropped to 99.2%. You need a structured document with root cause, contributing factors, and action items. And you need a review meeting on the calendar before the details fade from memory.

Without orchestration, someone opens a blank Google Doc, spends two hours scrubbing through PagerDuty alerts and Slack threads to reconstruct what happened, guesses at the impact numbers, writes action items that nobody tracks, and forgets to schedule the review meeting. By the time the post-mortem lands, it is two weeks late, missing key details, and the action items never get completed. There's no consistent format across incidents, no systematic impact measurement, and no guarantee the review actually happens.

## The Solution

**You write the timeline reconstruction and impact analysis logic. Conductor handles data gathering sequencing, document assembly, and follow-through tracking.**

Each stage of the post-mortem pipeline is a simple, independent worker. The timeline gatherer reconstructs the incident chronology from PagerDuty alerts, Slack messages, and deploy events. Building an ordered sequence of the 24 events that made up INC-2024-042. The metrics collector measures the blast radius: affected user count, availability drop, error rate spike, and duration of impact. The document drafter assembles a structured post-mortem from a template, populating the timeline, impact data, root cause section, and placeholder action items for the team to refine. The review scheduler creates a calendar invite for the blameless review meeting with all responders. Conductor executes them in strict sequence, ensures the document is only drafted after timeline and metrics are collected, retries if PagerDuty or Slack APIs are temporarily unavailable, and tracks every post-mortem so you can audit incident follow-through.

### What You Write: Workers

Four workers assemble the post-mortem. Gathering the incident timeline, collecting impact metrics, drafting the document, and scheduling the review meeting.

| Worker | Task | What It Does |
|---|---|---|
| **CollectMetricsWorker** | `pm_collect_metrics` | Pulls impact metrics from the incident window (affected users, availability percentage, error rates) |
| **DraftDocumentWorker** | `pm_draft_document` | Creates a structured post-mortem document with timeline, impact summary, root cause, and action items |
| **GatherTimelineWorker** | `pm_gather_timeline` | Builds the incident timeline by collecting alert triggers, response actions, and resolution events |
| **ScheduleReviewWorker** | `pm_schedule_review` | Schedules a blameless post-mortem review meeting with the involved team |

the workflow and rollback logic stay the same.

### The Workflow

```
pm_gather_timeline
 │
 ▼
pm_collect_metrics
 │
 ▼
pm_draft_document
 │
 ▼
pm_schedule_review

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
