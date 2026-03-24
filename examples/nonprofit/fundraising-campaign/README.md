# Fundraising Campaign

Orchestrates fundraising campaign through a multi-stage Conductor workflow.

**Input:** `campaignName`, `goalAmount`, `endDate` | **Timeout:** 60s

## Pipeline

```
frc_plan
    │
frc_launch
    │
frc_track
    │
frc_close
    │
frc_report
```

## Workers

**CloseWorker** (`frc_close`)

Reads `campaignId`, `endDate`. Outputs `closed`, `closedAt`.

**LaunchWorker** (`frc_launch`)

Reads `campaignId`. Outputs `launched`, `launchDate`.

**PlanWorker** (`frc_plan`)

Reads `campaignName`, `goalAmount`. Outputs `campaignId`, `channels`.

**ReportWorker** (`frc_report`)

Reads `campaignId`, `raised`. Outputs `campaign`.

**TrackWorker** (`frc_track`)

```java
int raised = (int)(goal * 1.12);
r.addOutputData("raised", raised); r.addOutputData("donors", 342); r.addOutputData("avgDonation", raised / 342); return r;
```

Reads `goalAmount`. Outputs `raised`, `donors`, `avgDonation`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
