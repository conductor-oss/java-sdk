# Calendar Integration in Java Using Conductor : Event Sync, Schedule Comparison, Change Propagation, and Notification

## The Problem

You need to keep calendars in sync. when a meeting is added to Google Calendar, it needs to appear in your internal scheduling system, and vice versa. Events must be fetched from the source, compared against the target to find additions/deletions/changes, synced with conflict resolution, and stakeholders notified of any changes. If the sync step fails, changes are lost. If notifications fail, people miss schedule updates.

Without orchestration, calendar sync is a fragile cron job that overwrites one calendar with another. Conflict detection is minimal, notification is an afterthought, and a failure in the sync step leaves calendars permanently out of sync with no record of what went wrong.

## The Solution

**You just write the calendar API calls and conflict resolution rules. Conductor handles the fetch-compare-sync-notify sequence, retries when calendar APIs are temporarily unavailable, and a complete record of every sync operation and conflict resolved.**

Each sync concern is an independent worker. event fetching, schedule comparison, change sync, and notification. Conductor runs them in sequence with retry logic, ensuring a temporary API failure doesn't cause permanent sync drift. Every sync operation is tracked, you can see what changed, what was synced, and who was notified. ### What You Write: Workers

Four workers handle bidirectional sync: FetchEventsWorker pulls events from the source calendar, CompareSchedulesWorker diffs additions/deletions/conflicts, SyncChangesWorker applies the reconciled changes, and NotifyStakeholdersWorker alerts affected participants.

| Worker | Task | What It Does |
|---|---|---|
| **CompareSchedulesWorker** | `cal_compare_schedules` | Compares fetched external events against the internal schedule, counting additions, updates, deletions, and conflicts |
| **FetchEventsWorker** | `cal_fetch_events` | Fetches calendar events from a specified calendar source, returning event details and total count |
| **NotifyStakeholdersWorker** | `cal_notify_stakeholders` | Notifies stakeholders about synced schedule changes, returning the recipient count |
| **SyncChangesWorker** | `cal_sync_changes` | Applies additions, updates, and deletions to synchronize the target calendar with the source |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
cal_fetch_events
 │
 ▼
cal_compare_schedules
 │
 ▼
cal_sync_changes
 │
 ▼
cal_notify_stakeholders

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
