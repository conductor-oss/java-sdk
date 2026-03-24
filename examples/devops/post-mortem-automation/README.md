# Generating a Post-Mortem Document from Incident INC-2024-042

After an incident is resolved, the team needs a post-mortem -- but gathering the timeline,
collecting impact metrics, drafting the document, and scheduling the review meeting takes
days of follow-up. This workflow automates all four steps so the post-mortem is ready before
anyone forgets what happened.

## Workflow

```
incidentId, severity
         |
         v
+------------------------+     +---------------------+     +---------------------+     +----------------------+
| pm_gather_timeline     | --> | pm_collect_metrics  | --> | pm_draft_document   | --> | pm_schedule_review   |
+------------------------+     +---------------------+     +---------------------+     +----------------------+
  GATHER_TIMELINE-1337          1200 affected users         document generated          review meeting
  24 events for                 99.2% availability          with timeline and           scheduled for
  INC-2024-042                  during incident             action items                next Tuesday
```

## Workers

**GatherTimelineWorker** -- Collects 24 events for incident INC-2024-042. Returns
`gather_timelineId: "GATHER_TIMELINE-1337"`.

**CollectMetricsWorker** -- Measures incident impact: 1,200 affected users and 99.2%
availability during the incident. Returns `collect_metrics: true`.

**DraftDocumentWorker** -- Generates the post-mortem document with timeline and action items.
Returns `draft_document: true`.

**ScheduleReviewWorker** -- Schedules the review meeting for next Tuesday. Returns
`schedule_review: true`.

## Tests

2 unit tests cover the post-mortem pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
