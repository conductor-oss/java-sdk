# Webhook Callback in Java Using Conductor

Webhook Callback Workflow. receive an incoming webhook request, process the data, and notify the caller via callback URL. ## The Problem

You need to process an incoming webhook request and notify the caller of the result via a callback URL. The workflow receives the webhook payload, processes the data according to your business logic, and sends the result back to the caller's callback endpoint. If the callback fails, the caller never learns the outcome; if processing fails, you must still notify the caller of the failure.

Without orchestration, you'd handle the webhook in a servlet or controller, process the data inline, and make an HTTP POST to the callback URL. manually retrying failed callbacks, handling timeout and connection errors, and logging every callback attempt to debug delivery failures.

## The Solution

**You just write the request-receive, data-processing, and callback-notification workers. Conductor handles ordered request processing, callback delivery retry with backoff, and a durable record of every webhook callback attempt.**

Each webhook-callback concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of receiving the request, processing the data, and calling back the caller, retrying failed callbacks with backoff, tracking every webhook's full lifecycle, and resuming if the process crashes after processing but before the callback. ### What You Write: Workers

Three workers handle the callback lifecycle: ReceiveRequestWorker parses the incoming webhook, ProcessDataWorker applies business logic to the payload, and NotifyCallbackWorker posts the result back to the caller's URL.

| Worker | Task | What It Does |
|---|---|---|
| **NotifyCallbackWorker** | `wc_notify_callback` | Sends a callback notification to the partner's webhook URL with the processing result and completion status. |
| **ProcessDataWorker** | `wc_process_data` | Processes the parsed data from the received webhook request. Produces a processing result with record counts and status. |
| **ReceiveRequestWorker** | `wc_receive_request` | Receives an incoming webhook request, validates it, and parses the payload into a structured format for downstream pr... |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
wc_receive_request
 │
 ▼
wc_process_data
 │
 ▼
wc_notify_callback

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
