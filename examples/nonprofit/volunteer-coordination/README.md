# Volunteer Coordination in Java with Conductor

## The Problem

A new volunteer signs up to help at your nonprofit. The volunteer coordination team needs to register the volunteer with their skills and availability, match them to an appropriate opportunity (e.g., food bank sorting), schedule their shift at a specific location and time, track their hours worked and events attended, and send a personalized thank-you acknowledging their contribution. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the volunteer registration, skill matching, shift scheduling, and engagement tracking logic. Conductor handles screening retries, assignment routing, and volunteer engagement audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Recruitment, screening, assignment, and hour tracking workers each manage one aspect of the volunteer engagement lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **MatchWorker** | `vol_match` | Matches the volunteer to an opportunity based on their skills, returning the opportunity name and location |
| **RegisterWorker** | `vol_register` | Registers the volunteer by name, assigning a unique volunteer ID |
| **ScheduleWorker** | `vol_schedule` | Schedules the volunteer for a specific date and shift at the matched opportunity |
| **ThankWorker** | `vol_thank` | Sends a personalized thank-you to the volunteer acknowledging their hours contributed |
| **TrackWorker** | `vol_track` | Logs the volunteer's hours for the session and updates their cumulative total hours and events attended |

### The Workflow

```
vol_register
 │
 ▼
vol_match
 │
 ▼
vol_schedule
 │
 ▼
vol_track
 │
 ▼
vol_thank

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
