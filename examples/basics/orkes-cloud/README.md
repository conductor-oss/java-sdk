# Orkes Cloud Connection Test in Java with Conductor: Verify Your Cloud Environment

A minimal Java Conductor workflow that verifies your connection to Orkes Cloud, the managed Conductor service. A single greet task confirms that your API key, secret, and server URL are correctly configured, and that your local workers can poll tasks from the Orkes Cloud environment. Uses [Orkes Cloud](https://orkes.io/) as the managed Conductor server.

## Connecting to Orkes Cloud

Orkes Cloud runs Conductor as a managed service.; no Docker, no infrastructure. But you need to verify three things before building workflows: your API credentials are valid, your worker can reach the Orkes Cloud server, and task polling works end-to-end. This single-task workflow confirms all three.

If the workflow completes, your Orkes Cloud connection is working. If it fails, the error tells you whether the issue is authentication, connectivity, or worker configuration.

## The Solution

**One worker, one task, one verification.**

A single greet worker polls the Orkes Cloud Conductor server, executes a task, and returns the result. A successful run means your entire Orkes Cloud setup: credentials, connectivity, and worker registration, is correctly configured.

### What You Write: Workers

One verification worker confirms connectivity to your Orkes Cloud environment, validating credentials and endpoint configuration.

| Worker | Task | What It Does |
|---|---|---|
| **CloudGreetWorker** | `` | Worker that greets a user with a cloud/local indicator. In cloud mode, the task name is "cloud_greet" and the greetin |

Workers in this example use in-memory simulation so you can run the full workflow without external dependencies. To move to production, swap the demo logic for your real service calls, the worker contract stays the same.

### The Workflow

```
cloud_greet

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
