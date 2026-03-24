# Timezone Handling in Java Using Conductor : Zone Detection, Time Conversion, and Timezone-Aware Scheduling

## The Problem

Your users are in different timezones. When a user in Tokyo schedules a job for "9:00 AM", that's 9:00 AM JST, not UTC. You need to detect the user's timezone, convert the requested time to UTC for storage and scheduling, and execute the job at the correct moment regardless of where your servers are. Getting timezone conversion wrong means jobs run at the wrong time for every user.

Without orchestration, timezone handling is scattered across the codebase. one method converts to UTC, another assumes local time, and a third ignores timezones entirely. Daylight saving time transitions cause jobs to run an hour early or late. Nobody can tell what timezone a scheduled job is stored in.

## The Solution

**You just write the timezone detection and UTC conversion logic. Conductor handles the detect-convert-schedule-execute sequence, retries when timezone lookups or scheduling APIs are unavailable, and a record of every conversion showing original time, detected zone, and UTC result.**

Each timezone concern is an independent worker. zone detection, time conversion, and job scheduling. Conductor runs them in sequence: detect the user's timezone, convert the requested time to UTC, then schedule the job. Every timezone conversion is tracked with the original time, detected zone, and UTC result.

### What You Write: Workers

DetectZoneWorker identifies the user's IANA timezone, ConvertTimeWorker translates the requested time to UTC, ScheduleJobWorker queues the job at the correct UTC instant, and ExecuteJobWorker runs it when the moment arrives.

| Worker | Task | What It Does |
|---|---|---|
| **ConvertTimeWorker** | `tz_convert_time` | Converts a requested time from the user's source timezone to the target timezone, returning the offset |
| **DetectZoneWorker** | `tz_detect_zone` | Detects a user's timezone and returns the IANA zone name, UTC offset, and DST status |
| **ExecuteJobWorker** | `tz_execute_job` | Executes the scheduled job at the correct UTC time, returning completion timestamp and duration |
| **ScheduleJobWorker** | `tz_schedule_job` | Schedules a job at the converted UTC time and returns a job ID for tracking |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
tz_detect_zone
 │
 ▼
tz_convert_time
 │
 ▼
tz_schedule_job
 │
 ▼
tz_execute_job

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
