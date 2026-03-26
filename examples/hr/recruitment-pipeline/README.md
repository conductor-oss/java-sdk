# Recruitment Pipeline

Recruitment pipeline: post, screen, interview, evaluate, offer.

**Input:** `jobTitle`, `department`, `candidateName` | **Timeout:** 60s

## Pipeline

```
rcp_post
    │
rcp_screen
    │
rcp_interview
    │
rcp_evaluate
    │
rcp_offer
```

## Workers

**EvaluateWorker** (`rcp_evaluate`)

Reads `interviewScore`, `screenScore`. Outputs `recommendation`, `overallScore`.

**InterviewWorker** (`rcp_interview`)

Reads `candidateName`. Outputs `score`, `rounds`, `feedback`.

**OfferWorker** (`rcp_offer`)

Reads `candidateName`. Outputs `offerId`, `salary`, `startDate`.

**PostWorker** (`rcp_post`)

Reads `department`, `jobTitle`. Outputs `jobId`, `platforms`.

**ScreenWorker** (`rcp_screen`)

Reads `candidateName`. Outputs `score`, `passedScreen`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
