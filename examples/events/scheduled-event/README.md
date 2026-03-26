# Scheduled Event

A billing system needs to generate invoices on the first of each month, send payment reminders on the 15th, and run reconciliation on the last day. Each scheduled event needs creation with execution time, persistence, execution when the time arrives, and outcome recording.

## Pipeline

```
[se_queue_event]
     |
     v
[se_check_schedule]
     |
     v
[se_wait_until_ready]
     |
     v
[se_execute_event]
     |
     v
[se_confirm]
```

**Workflow inputs:** `eventId`, `payload`, `scheduledTime`

## Workers

**CheckScheduleWorker** (task: `se_check_schedule`)

Checks the schedule and determines the delay until execution.

- Reads `scheduledTime`. Writes `delayMs`, `ready`

**ConfirmWorker** (task: `se_confirm`)

Confirms that the scheduled event was executed successfully.

- Reads `eventId`, `executedAt`. Writes `confirmed`

**ExecuteEventWorker** (task: `se_execute_event`)

Executes the scheduled event.

- Sets `result` = `"success"`
- Reads `eventId`, `payload`. Writes `executedAt`, `result`

**QueueEventWorker** (task: `se_queue_event`)

Queues an event for scheduled execution.

- Reads `eventId`, `payload`, `scheduledTime`. Writes `queued`, `eventId`, `scheduledTime`

**WaitUntilReadyWorker** (task: `se_wait_until_ready`)

Waits until the scheduled event is ready for execution.

- Reads `delayMs`, `eventId`. Writes `waited`

---

**41 tests** | Workflow: `scheduled_event_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
