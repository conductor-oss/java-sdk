# Drip Campaign

Orchestrates drip campaign through a multi-stage Conductor workflow.

**Input:** `contactId`, `campaignId`, `email` | **Timeout:** 60s

## Pipeline

```
drp_enroll
    │
drp_send_series
    │
drp_track_engagement
    │
drp_graduate
```

## Workers

**EnrollWorker** (`drp_enroll`)

```java
String enrollId = "ENR-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
```

Reads `campaignId`, `contactId`. Outputs `enrollmentId`, `enrolledAt`.

**GraduateWorker** (`drp_graduate`)

```java
boolean graduated = engagement >= 60;
```

Reads `contactId`, `engagement`. Outputs `graduated`, `nextAction`.

**SendSeriesWorker** (`drp_send_series`)

Reads `email`. Outputs `emailsSent`, `series`.

**TrackEngagementWorker** (`drp_track_engagement`)

Outputs `engagementScore`, `opens`, `clicks`, `replies`.

## Tests

**9 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
