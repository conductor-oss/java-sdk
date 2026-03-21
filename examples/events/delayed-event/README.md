# Delayed Event in Java Using Conductor

Delayed event processing workflow that receives an event, computes a delay, applies it, processes the event, and logs completion. ## The Problem

You need to process events after a configurable delay. When an event arrives, the system must compute the appropriate wait time (based on business rules, priority, or a fixed delay), hold the event for that duration, and then process it. Use cases include scheduled notifications, rate-limited API calls, and time-delayed order confirmations. Processing an event before its delay expires violates business timing requirements.

Without orchestration, you'd build a delay queue with Thread.sleep() or scheduled executors, manually tracking which events are waiting, handling JVM restarts that lose in-memory timers, and logging everything to debug why a delayed notification fired immediately or not at all.

## The Solution

**You just write the event-receive, delay-compute, delay-apply, and event-processing workers. Conductor handles durable delay that survives restarts, ordered post-delay execution, and full lifecycle tracking per event.**

Each delayed-processing concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of receiving the event, computing the delay, waiting the specified duration (durably, surviving restarts), processing the event after the delay, and logging completion. ### What You Write: Workers

Five workers manage time-delayed processing: ReceiveEventWorker ingests the event, ComputeDelayWorker calculates the wait duration, ApplyDelayWorker holds execution, ProcessEventWorker handles the event after the delay, and LogCompletionWorker records the outcome.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyDelayWorker** | `de_apply_delay` | Applies the computed delay (demo, instant in mock). |
| **ComputeDelayWorker** | `de_compute_delay` | Computes the delay duration in milliseconds from delaySeconds. |
| **LogCompletionWorker** | `de_log_completion` | Logs the completion of the delayed event processing. |
| **ProcessEventWorker** | `de_process_event` | Processes the delayed event. |
| **ReceiveEventWorker** | `de_receive_event` | Receives an incoming event and marks it as received. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
de_receive_event
 │
 ▼
de_compute_delay
 │
 ▼
de_apply_delay
 │
 ▼
de_process_event
 │
 ▼
de_log_completion

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
