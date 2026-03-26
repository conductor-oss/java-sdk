# Clickstream Analytics

A product analytics team captures raw click events from a web application, but the events arrive with inconsistent session IDs, missing page titles, and timestamps in different formats. Before any dashboard can render, the raw stream needs sessionization, enrichment with page metadata, and aggregation into per-session metrics.

## Pipeline

```
[ck_ingest_clicks]
     |
     v
[ck_sessionize]
     |
     v
[ck_analyze_journeys]
     |
     v
[ck_generate_report]
```

**Workflow inputs:** `clickData`, `sessionTimeout`, `analysisType`

## Workers

**AnalyzeJourneysWorker** (task: `ck_analyze_journeys`)

Analyzes user journeys from session data.

- Formats output strings
- Reads `sessions`. Writes `analysis`, `topJourney`, `conversionRate`

**GenerateReportWorker** (task: `ck_generate_report`)

Generates a clickstream analytics report.

- Writes `report`

**IngestClicksWorker** (task: `ck_ingest_clicks`)

Ingests click events from a tracking source.

- Uses java streams
- Writes `events`, `clickCount`

**SessionizeWorker** (task: `ck_sessionize`)

Groups click events into user sessions.

- Rounds with `math.round()`, uses java streams, computes sums
- Reads `events`. Writes `sessions`, `sessionCount`, `avgDuration`

---

**14 tests** | Workflow: `clickstream_analytics` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
