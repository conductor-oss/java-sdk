# SDK Setup Verification in Java with Conductor: Smoke Test for Your Maven/Java Configuration

A minimal Java Conductor workflow that verifies your SDK setup is correct, the Maven dependency is properly declared, the `conductor-client` JAR resolves, the `ConductorClient` can connect to the server, and a worker can poll and execute tasks. If this runs, your Java development environment is ready for Conductor development. Uses [Conductor](https://github.com/conductor-oss/conductor) to validate the end-to-end SDK setup.

## Verifying Your Java SDK Configuration

Setting up the Conductor Java SDK involves several pieces: adding the `conductor-client` Maven dependency, configuring the server URL, creating a `ConductorClient` instance, and registering workers. If any piece is misconfigured, you'll get cryptic errors when you try to build a real workflow. This smoke test catches setup issues early.

A successful run confirms: Maven resolves the dependency, the client connects to Conductor, the worker registers and polls, and task execution completes.

## The Solution

**Run this smoke test first.**

One worker, one task. Just enough to verify your entire SDK setup. If it passes, start building. If it fails, the error tells you exactly which part of your setup needs fixing.

### What You Write: Workers

A minimal worker runs a round-trip through the SDK to confirm that your Maven dependencies, Java version, and Conductor connection are all configured correctly.

| Worker | Task | What It Does |
|---|---|---|
| `SdkTestWorker` | `sdk_test_task` | Accepts a `check` string input (defaults to "default" if blank), returns a confirmation message that conductor-client 5.0.1 is working |

Workers in this example use in-memory simulation so you can run the full workflow without external dependencies. To move to production, swap the demo logic for your real service calls, the worker contract stays the same.

### The Workflow

```
sdk_test_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
