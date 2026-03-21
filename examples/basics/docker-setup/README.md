# Docker Setup Verification in Java with Conductor: One-Task Smoke Test

A minimal Java Conductor workflow with a single task that verifies your Docker-based Conductor setup is working correctly. If the workflow completes successfully, your Docker environment (Conductor server, worker connectivity, task polling) is properly configured. If it fails, the error tells you exactly what's wrong. Uses [Conductor](https://github.com/conductor-oss/conductor) running in Docker.

## Verifying Your Setup Before Building

Before building complex workflows, you need to confirm the basics work: Conductor is running in Docker, the health endpoint responds, workers can connect and poll for tasks, and workflow execution completes end-to-end. This single-task workflow is the quickest way to verify all of that.

A successful run means: Docker is running Conductor, the API is reachable, the worker registered and polled successfully, the task executed, and the result was recorded. If any of those steps fail, you get a specific error to debug.

## The Solution

**Run this first, build everything else second.**

One worker, one task, one workflow. If it completes, your setup is correct. If it fails, the error pinpoints whether the issue is Docker, Conductor, connectivity, or the worker.

### What You Write: Workers

A single smoke-test worker verifies your Docker and Conductor setup is working correctly before you build anything more complex.

| Worker | Task | What It Does |
|---|---|---|
| `DockerTestWorker` | `docker_test_task` | Accepts a `message` string input (defaults to "Docker setup test" if blank), echoes it back with an ISO-8601 timestamp to confirm Docker-based Conductor connectivity |

Workers in this example use in-memory simulation so you can run the full workflow without external dependencies. To move to production, swap the demo logic for your real service calls, the worker contract stays the same.

### The Workflow

```
docker_test_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
