# Event Replay Testing in Java Using Conductor

Event Replay Testing. loads recorded events, sets up a sandbox environment, replays each event in a DO_WHILE loop comparing results, then generates a test report. ## The Problem

You need to test your event processing logic by replaying recorded production events in a sandbox environment. The workflow loads a set of recorded events, sets up an isolated test environment, replays each event through the processing pipeline while comparing actual results to expected results, and generates a test report summarizing pass/fail outcomes. Without replay testing, you only discover processing bugs when they corrupt production data.

Without orchestration, you'd build a custom test harness that loads event fixtures, manages sandbox setup/teardown, replays events in a loop, diffs results, and generates reports. manually handling test environment cleanup, ensuring sandbox isolation, and managing the test event corpus.

## The Solution

**You just write the event-load, sandbox-setup, replay, and result-comparison workers. Conductor handles DO_WHILE test iteration, sandbox lifecycle management, and a durable record of every replay test run.**

Each replay-testing concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of loading events, setting up the sandbox, replaying each event in a DO_WHILE loop with result comparison, and generating the test report, retrying flaky tests and tracking every replay run. ### What You Write: Workers

Four workers drive replay testing: LoadEventsWorker reads recorded production events, SetupSandboxWorker provisions an isolated test environment, ReplayEventWorker processes each event in a DO_WHILE loop, and CompareResultWorker diffs actual vs expected output.

| Worker | Task | What It Does |
|---|---|---|
| **CompareResultWorker** | `rt_compare_result` | Compares the actual replay result against the expected result. |
| **LoadEventsWorker** | `rt_load_events` | Loads recorded events for replay testing. |
| **ReplayEventWorker** | `rt_replay_event` | Replays a single recorded event in the sandbox environment. |
| **SetupSandboxWorker** | `rt_setup_sandbox` | Sets up an isolated sandbox environment for replay testing. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
rt_load_events
 │
 ▼
rt_setup_sandbox
 │
 ▼
DO_WHILE
 └── rt_replay_event
 └── rt_compare_result
 │
 ▼
rt_test_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
