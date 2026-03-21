# Creating Workers in Java with Conductor: Three Common Worker Patterns

You've defined a workflow in JSON, registered it with Conductor, and hit "start"; but nothing happens. The workflow sits in RUNNING state forever because Conductor workflows don't execute code; workers do. And right now you have zero workers polling for tasks. This example builds three workers from scratch, a synchronous transform, a demo API fetch, and an error-handling processor, so you can see the `Worker` interface in action and understand how Conductor delegates real work to your Java code.

## Learning to Write Conductor Workers

A Conductor worker is a Java class that implements the `Worker` interface: it receives a `Task`, does work, and returns a `TaskResult`. But there are different patterns for different situations. Synchronous workers transform data in-place. Async-style workers implement external calls (API requests, database queries) that might be slow. Error-handling workers demonstrate how to set `FAILED` status with error messages so Conductor knows to retry or route to failure handling.

This example chains all three patterns in a single workflow so you can see how each type of worker looks, how Conductor passes data between them, and how error handling works.

## The Solution

**You just write the transform, fetch, and processing logic in each worker class. Conductor handles sequencing, retries, and error recovery.**

Three workers demonstrate three patterns, a transform worker that processes text synchronously, a fetch worker that simulates an external data call, and a process worker that shows proper error handling. Conductor chains them, demonstrating how outputs from one worker become inputs to the next.

### What You Write: Workers

This example demonstrates three common patterns for writing Conductor workers: stateless transformations, external API calls, and conditional logic.

| Worker | Task | What It Does |
|---|---|---|
| **SimpleTransformWorker** | `simple_transform` | Takes a text string and returns four fields: `upper` (uppercased), `lower` (lowercased), `length` (character count), and `original` (unchanged input). Pure logic, no side effects. |
| **FetchDataWorker** | `fetch_data` | Takes a `source` name and returns 3 deterministic records prefixed with the source name (e.g., `"my-api-record-1"`). Includes a 100ms sleep to simulate API latency. |
| **SafeProcessWorker** | `safe_process` | Takes a list of records and scores each one using a deterministic hash-based algorithm (PASS if score >= 50, FAIL otherwise). Wraps all logic in try/catch. On exception, returns `FAILED` status so Conductor can retry. |

### The Workflow

```
simple_transform
 │
 ▼
fetch_data
 │
 ▼
safe_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
