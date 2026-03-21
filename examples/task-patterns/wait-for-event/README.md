# Wait-for-Event in Java with Conductor

WAIT task demo. pauses a workflow durably until an external system sends a signal (approval, webhook callback, or third-party notification), then processes the signal data. The workflow survives process restarts while waiting. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need a workflow to pause and wait for an external event: a human approval, a webhook callback from a payment processor, or a notification from a third-party system, before continuing. The wait could last seconds, hours, or days. If the process hosting the workflow restarts while waiting, the workflow state must not be lost. Once the external signal arrives, the workflow must resume exactly where it left off, with the signal data available to downstream tasks.

Without orchestration, you'd poll a database for approval status in a loop, manage timeout logic manually, and lose the workflow context if the process restarts while waiting. You'd need to build durable state persistence, timeout handling, and a mechanism for external systems to resume the flow. Essentially re-implementing what a workflow engine already provides.

## The Solution

**You just write the request preparation and signal processing workers. Conductor handles the durable pause via WAIT, surviving restarts until the external signal arrives.**

This example demonstrates the WAIT task for pausing a workflow until an external system sends a signal. PrepareWorker takes a `requestId` and `requester`, logs the pending request, and returns preparation metadata (prepared=true, requestId, requester). The workflow then hits a WAIT task (`wait_for_signal`) that durably pauses execution: surviving process restarts, until an external system completes the task via Conductor's API with signal data (e.g., `{"decision": "approved", "signalData": "manager-notes"}`). Once the signal arrives, ProcessSignalWorker picks up the decision and signal data to finalize the request, returning processed=true, the requestId, and the decision. The WAIT task is a system task with no worker. Conductor manages the durable pause internally.

### What You Write: Workers

Two workers bookend the durable WAIT pause: PrepareWorker readies the request and returns preparation metadata before the workflow pauses, and ProcessSignalWorker acts on the external signal data (decision, notes) after the WAIT task resumes.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareWorker** | `we_prepare` | Prepares a request before the workflow pauses. Takes requestId and requester from workflow input, returns prepared=true along with both values echoed back. Passes through null values without error. |
| **ProcessSignalWorker** | `we_process_signal` | Processes the signal after the WAIT task completes. Takes requestId, signalData, and decision from the WAIT task output, returns processed=true with the requestId and decision. Handles null values gracefully. |

The WAIT task (`wait_for_signal`) is a Conductor system task.; no worker is needed. It pauses the workflow until an external system completes it via the Conductor API.

### The Workflow

```
we_prepare
 │
 ▼
wait_for_signal [WAIT] ← pauses here until external API call
 │
 ▼
we_process_signal

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
