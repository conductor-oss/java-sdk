# Event Replay Testing

A QA team wants to verify that a new version of an event handler produces identical results to the old version for a known set of test events. The pipeline captures events, replays them through the new handler, compares outputs field-by-field, and reports any regressions.

## Pipeline

```
[rt_load_events]
     |
     v
[rt_setup_sandbox]
     |
     v
     +── loop ──────────────+
     |  [rt_replay_event]
     |  [rt_compare_result]
     +───────────────────────+
     |
     v
[rt_test_report]
```

**Workflow inputs:** `testSuiteId`, `eventSource`

## Workers

**CompareResultWorker** (task: `rt_compare_result`)

Compares the actual replay result against the expected result.

- Reads `actual`, `expected`, `iteration`. Writes `match`, `actual`, `expected`

**LoadEventsWorker** (task: `rt_load_events`)

Loads recorded events for replay testing.

- Reads `testSuiteId`, `eventSource`. Writes `events`, `count`

**ReplayEventWorker** (task: `rt_replay_event`)

Replays a single recorded event in the sandbox environment.

- Sets `result` = `"processed"`
- Reads `events`, `sandboxId`, `iteration`. Writes `result`, `expectedResult`, `eventId`

**SetupSandboxWorker** (task: `rt_setup_sandbox`)

Sets up an isolated sandbox environment for replay testing.

- Reads `testSuiteId`. Writes `sandboxId`, `ready`

**TestReportWorker** (task: `rt_test_report`)

Generates a final test report after all replay iterations.

- Sets `status` = `"all_passed"`
- Reads `testSuiteId`, `totalReplayed`. Writes `status`, `passCount`, `failCount`

---

**46 tests** | Workflow: `event_replay_testing` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
