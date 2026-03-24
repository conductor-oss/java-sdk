# Scheduled Event in Java Using Conductor

Sequential scheduled-event workflow: queue_event -> check_schedule -> wait_until_ready -> execute_event -> confirm.

## The Problem

You need to process events at a scheduled future time. An event is queued with a target execution time, the system checks whether it is time to execute, waits until the scheduled moment, executes the event's action, and confirms completion. Use cases include scheduled notifications, time-delayed order cancellations, and appointment reminders. Executing before the scheduled time violates business requirements; losing the event during the wait period means it never fires.

Without orchestration, you'd store scheduled events in a database, poll for due events with a timer loop, execute them, and mark them complete. manually handling clock drift, managing the polling interval vs: precision tradeoff, and recovering events that were due during a service outage.

## The Solution

**You just write the event-queue, schedule-check, wait, execute, and confirmation workers. Conductor handles durable scheduling that survives restarts, ordered post-wait execution, and a complete record of every scheduled event lifecycle.**

Each scheduling concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of queuing the event, checking the schedule, waiting durably until the execution time (surviving restarts), executing the event action, and confirming completion.

### What You Write: Workers

Five workers manage scheduled execution: QueueEventWorker registers the event, CheckScheduleWorker calculates the delay, WaitUntilReadyWorker holds until the target time, ExecuteEventWorker runs the action, and ConfirmWorker stamps completion.

| Worker | Task | What It Does |
|---|---|---|
| **CheckScheduleWorker** | `se_check_schedule` | Checks the schedule and determines the delay until execution. |
| **ConfirmWorker** | `se_confirm` | Confirms that the scheduled event was executed successfully. |
| **ExecuteEventWorker** | `se_execute_event` | Executes the scheduled event. |
| **QueueEventWorker** | `se_queue_event` | Queues an event for scheduled execution. |
| **WaitUntilReadyWorker** | `se_wait_until_ready` | Waits until the scheduled event is ready for execution. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
se_queue_event
 │
 ▼
se_check_schedule
 │
 ▼
se_wait_until_ready
 │
 ▼
se_execute_event
 │
 ▼
se_confirm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
