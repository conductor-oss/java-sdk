# Sync Execution in Java with Conductor

Simple workflow for demonstrating sync vs async execution.

## The Problem

You need to execute a workflow and get the result back immediately in the same API call. like a synchronous function call that happens to be a full workflow under the hood. For example, adding two numbers and returning the sum to the caller without them needing to poll for the result. The caller sends `{a: 5, b: 3}` and gets `{sum: 8}` back in the HTTP response, even though the computation ran through the full Conductor workflow engine with retries, durability, and tracking.

Without synchronous execution, the caller would start the workflow (getting back a workflow ID), then poll the status endpoint repeatedly until the workflow completes, then extract the output from the completed execution. That polling loop adds latency, complexity, and client-side retry logic for what is conceptually a simple request-response operation.

## The Solution

**You just write the computation worker. Conductor handles the synchronous blocking execution, durability, and result delivery.**

This example demonstrates synchronous workflow execution. starting a workflow and waiting for the result in a single blocking call. The AddWorker takes two numbers (`a` and `b`) and returns their sum. The example code shows both async execution (start the workflow, get a workflow ID, poll for the result) and sync execution (start the workflow and get the output directly). Under the hood, the workflow runs through the full Conductor engine, with task tracking, retry capability, and execution history; but the caller gets a synchronous request-response experience. This pattern is ideal for short-lived workflows that back API endpoints or real-time computations.

### What You Write: Workers

A single worker demonstrates the synchronous execution pattern: AddWorker takes two numbers and returns their sum, showing how a trivial computation becomes a durable, tracked, retryable operation behind a synchronous API call.

| Worker | Task | What It Does |
|---|---|---|
| **AddWorker** | `sync_add` | SIMPLE worker that adds two numbers. Takes input { a, b } and returns { sum: a + b }. Demonstrates the simplest possi... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
sync_add

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
