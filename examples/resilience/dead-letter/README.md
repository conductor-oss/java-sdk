# Implementing Dead Letter Queue Pattern in Java with Conductor: Capture Poison Messages Instead of Losing Them

## The Problem

You have a message processing pipeline where some messages are "poison". They will never succeed no matter how many times you retry. A malformed JSON payload, a reference to a deleted customer, a request for a discontinued product. After exhausting retries, those failed messages need to be captured somewhere durable (a dead letter queue) with full context about why they failed, instead of being silently dropped.

### What Goes Wrong Without Dead Letter Handling

Consider an order processing pipeline that ingests events from a queue:

1. Message arrives: `{orderId: "ORD-456", customerId: "DELETED-CUST"}`
2. Worker tries to process. **FAILS** (customer not found in database)
3. Conductor retries 3 times. **FAILS** each time (same error, customer is permanently deleted)
4. Message is exhausted and discarded

Without dead letter handling:
- The order is silently lost. Nobody knows it failed
- The customer may have already been charged but never received their order
- The operations team has no record of the failure to investigate
- The same class of error keeps happening without anyone noticing the pattern

With the dead letter pattern, the failed message and its full context (original input, error details, retry count, timestamps) are routed to a dead letter handler workflow. Operations gets an alert, the message is persisted for manual review, and patterns of failure become visible.

## The Solution

**You just write the message processor and dead letter handler. Conductor handles retry exhaustion detection, automatic routing of poison messages to the dead-letter handler workflow, and a complete record of every failed message with its error context and retry history.**

The processing worker handles the business logic, and when it fails with retries exhausted, Conductor's failure workflow mechanism captures the failed task with its full context. Original inputs, error details, retry history. A separate handler workflow receives the dead letter items and can log them, alert operators, or persist them for manual review. Every failed item is tracked with complete execution history, so you always know what failed, why, and how many times it was retried. ### What You Write: Workers

ProcessWorker handles message processing and reports success or failure, while HandleFailureWorker captures permanently failed messages with full context: original inputs, error details, and retry history, for investigation and manual review.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessWorker** | `dl_process` | Processes data based on mode input. When `mode="fail"`, returns FAILED with `{error: "Processing failed for data: order-456"}`. When mode is anything else (or missing), returns COMPLETED with `{result: "Processed: order-456"}`. Only accepts String inputs. Non-string mode defaults to "success", non-string data defaults to empty string. |
| **HandleFailureWorker** | `dl_handle_failure` | Handles dead letter entries by logging the failed task details. Receives `failedWorkflowId`, `failedTaskName`, and `error` as inputs. Returns `{handled: true, summary: "Failure handled for workflow wf-123, task dl_process: ..."}`. Always succeeds. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflows

**Main workflow** (`dead_letter_demo`):

```
dl_process
 |
 |-- success: workflow COMPLETED with {result: "Processed: order-123"}
 |-- failure: workflow FAILED (retries exhausted). Triggers dead letter handler

```

**Dead letter handler** (`dead_letter_handler`):

```
dl_handle_failure
 |
 v
(logs failure details, marks as handled, alerts operators)

```

The main workflow has `retryCount: 0` on the process task, so a failure immediately triggers the dead letter path. In production, you would set retries to 2-3 so that transient failures resolve before reaching the dead letter queue.

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
