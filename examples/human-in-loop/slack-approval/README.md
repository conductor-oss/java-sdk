# Slack Approval

Slack interactive button approval workflow -- submit, post Slack message with approve/reject buttons, WAIT for response, finalize.

**Input:** `requestor`, `reason`, `channel` | **Timeout:** 3600s

## Pipeline

```
sa_submit
    │
sa_post_slack
    │
slack_response [WAIT]
    │
sa_finalize
```

## Workers

**FinalizeWorker** (`sa_finalize`): Worker for sa_finalize — processes the approval decision after the WAIT task.

Reads `decision`. Outputs `done`, `decision`.

**PostSlackWorker** (`sa_post_slack`): Worker for sa_post_slack — generates a Slack Block Kit message payload.

Reads `channel`, `reason`, `requestor`. Outputs `posted`, `slackPayload`.

**SubmitWorker** (`sa_submit`): Worker for sa_submit — handles the initial submission step.

Outputs `submitted`.

## Tests

**20 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
