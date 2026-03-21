# Idempotent Start in Java with Conductor

Idempotent start demo. demonstrates correlationId-based dedup and search-based idempotency. ## The Problem

You need to ensure that processing the same order twice does not charge the customer twice. When a webhook fires or a message is redelivered, the same orderId and amount can arrive multiple times. Starting a new workflow for each duplicate request means the order gets processed repeatedly. double charges, duplicate shipments, or inconsistent state. You need a way to guarantee that the second (and third, and fourth) request for the same order is recognized as a duplicate and returns the existing result instead of processing again.

Without orchestration, you'd build your own deduplication layer. checking a database for existing orderId records before processing, handling race conditions when two requests arrive simultaneously, and managing the gap between "check" and "insert" where duplicates can slip through. That logic is error-prone, and there is no built-in way to search for running or completed workflows by correlation ID.

## The Solution

**You just write the order processing worker. Conductor handles the deduplication, correlation tracking, and duplicate detection.**

This example demonstrates two idempotency strategies using Conductor's built-in capabilities. First, correlationId-based dedup. when starting a workflow, you pass the orderId as the correlationId, and Conductor can detect if a workflow with that correlationId is already running. Second, search-based idempotency, before starting a new workflow, you search for existing executions with the same orderId to check if it has already been processed. The IdemProcessWorker handles the actual order processing (taking orderId and amount), and its deterministic output ensures that even if the same order is processed, the result is consistent. Conductor tracks every start attempt with its correlationId, making duplicate detection trivial.

### What You Write: Workers

A single IdemProcessWorker handles order processing, taking an orderId and amount and returning a deterministic result. Conductor's correlationId and search APIs handle the deduplication logic externally.

| Worker | Task | What It Does |
|---|---|---|
| **IdemProcessWorker** | `idem_process` | Idempotent process worker. Takes an orderId and amount, returns a deterministic result based on orderId. This worker ... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
idem_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
