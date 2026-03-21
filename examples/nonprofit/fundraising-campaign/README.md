# Fundraising Campaign in Java with Conductor

## The Problem

Your nonprofit is launching an end-of-year fundraising campaign with a $100,000 goal. The development team needs to plan the campaign by selecting outreach channels (email, social, direct mail), launch it on the target date, track donations against the goal throughout the campaign, close the campaign at the end date, and produce a final report showing total raised, donor count, and whether the goal was met. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the campaign planning, donor outreach, donation tracking, and results reporting logic. Conductor handles outreach retries, pledge tracking, and campaign performance audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Campaign setup, outreach, pledge tracking, and results reporting workers each handle one phase of the fundraising drive.

| Worker | Task | What It Does |
|---|---|---|
| **CloseWorker** | `frc_close` | Closes the campaign on the end date and records the final close timestamp |
| **LaunchWorker** | `frc_launch` | Launches the campaign and records the launch date |
| **PlanWorker** | `frc_plan` | Plans the campaign by setting the name, goal amount, and outreach channels (email, social, direct mail) |
| **ReportWorker** | `frc_report` | Generates the final campaign report with total raised, goal-met status, and campaign ID |
| **TrackWorker** | `frc_track` | Tracks donations against the goal, computing total raised, donor count, and average donation |

### The Workflow

```
frc_plan
 │
 ▼
frc_launch
 │
 ▼
frc_track
 │
 ▼
frc_close
 │
 ▼
frc_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
