# User Survey

Orchestrates user survey through a multi-stage Conductor workflow.

**Input:** `surveyTitle`, `questions`, `targetAudience` | **Timeout:** 60s

## Pipeline

```
usv_create
    │
usv_distribute
    │
usv_collect
    │
usv_analyze
    │
usv_report
```

## Workers

**AnalyzeSurveyWorker** (`usv_analyze`)

Outputs `analysis`.

**CollectResponsesWorker** (`usv_collect`)

Outputs `responses`, `responseCount`, `responseRate`.

**CreateSurveyWorker** (`usv_create`)

Reads `questions`, `title`. Outputs `surveyId`, `questionCount`.

**DistributeSurveyWorker** (`usv_distribute`)

Reads `audience`. Outputs `sentTo`, `channels`.

**SurveyReportWorker** (`usv_report`)

Reads `surveyId`. Outputs `reportUrl`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
