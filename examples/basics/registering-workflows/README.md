# Workflow Registration in Java with Conductor: How to Register Definitions via the SDK

A Java example that demonstrates how to register workflow and task definitions with Conductor using the Java SDK, the essential first step before running any workflow. The example registers a simple echo workflow, runs it, and shows the output. This teaches the registration API: how workflow JSON gets loaded, how task definitions get created, and how the SDK submits them to the Conductor server. Uses [Conductor](https://github.com/conductor-oss/conductor) to accept and manage the registered definitions.

## Understanding Workflow Registration

Before a workflow can run, its definition must be registered with the Conductor server. This is a one-time operation (per version) that tells Conductor the workflow's structure, which tasks it contains, how they connect, and what inputs they expect. Task definitions must also be registered so Conductor knows about retry policies, timeouts, and rate limits.

This example walks through the registration process end-to-end: load a workflow definition from JSON, register it via the SDK, start an execution, and verify the result. The echo task simply passes a message through, so you can focus on the registration mechanics.

## The Solution

**Register once, run many times.**

The example shows the complete registration lifecycle. Creating task definitions, registering the workflow, starting an execution, and reading the result. Once you understand this pattern, you can register any workflow definition.

### What You Write: Workers

Workers here illustrate how task definitions and workflow registrations connect through the SDK, showing the registration lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **EchoWorker** | `echo_task` | Simple worker that echoes an input message back as output. |

Workers in this example use in-memory simulation so you can run the full workflow without external dependencies. To move to production, swap the demo logic for your real service calls, the worker contract stays the same.

### The Workflow

```
echo_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
