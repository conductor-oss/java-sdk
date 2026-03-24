# Calendar Integration

An external calendar needs bi-directional synchronization. The pipeline fetches events from the calendar, compares them with the internal schedule, syncs changes in the configured direction, and notifies stakeholders of what changed.

## Workflow

```
cal_fetch_events ──> cal_compare_schedules ──> cal_sync_changes ──> cal_notify_stakeholders
```

Workflow `calendar_integration_406` accepts `calendarId`, `syncWindow`, and `direction`. Times out after `60` seconds.

## Workers

**FetchEventsWorker** (`cal_fetch_events`) -- fetches events from the specified calendar.

**CompareSchedulesWorker** (`cal_compare_schedules`) -- compares external events with the internal schedule.

**SyncChangesWorker** (`cal_sync_changes`) -- syncs the detected changes.

**NotifyStakeholdersWorker** (`cal_notify_stakeholders`) -- notifies stakeholders of synced changes.

## Workflow Output

The workflow produces `eventsFetched`, `changesSynced`, `notified` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `calendar_integration_406` defines 4 tasks with input parameters `calendarId`, `syncWindow`, `direction` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "Calendar sync workflow that fetches events, compares schedules, syncs changes, and notifies stakeholders.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the calendar sync pipeline from event fetching through stakeholder notification.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
