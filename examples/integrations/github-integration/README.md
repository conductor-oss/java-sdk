# Github Integration

Orchestrates github integration through a multi-stage Conductor workflow.

**Input:** `repo`, `branch`, `baseBranch`, `commitMessage` | **Timeout:** 60s

## Pipeline

```
gh_receive_webhook
    │
gh_create_pr
    │
gh_run_checks
    │
gh_merge_pr
```

## Workers

**CreatePrWorker** (`gh_create_pr`): Creates a pull request on GitHub.

```java
this.liveMode = githubToken != null && !githubToken.isBlank();
```

Reads `base`, `head`, `repo`, `title`. Outputs `prNumber`, `headSha`, `url`.
Returns `FAILED` on validation errors.

**MergePrWorker** (`gh_merge_pr`): Merges a pull request.

```java
this.liveMode = githubToken != null && !githubToken.isBlank();
```

Reads `checksPass`, `prNumber`, `repo`. Outputs `merged`, `mergedAt`.
Returns `FAILED` on validation errors.

**ReceiveWebhookWorker** (`gh_receive_webhook`): Receives a GitHub webhook push event.

Reads `branch`, `commitMessage`, `repo`. Outputs `prTitle`, `eventType`, `receivedAt`.

**RunChecksWorker** (`gh_run_checks`): Runs CI checks on a pull request.

Reads `prNumber`. Outputs `checks`, `allPassed`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
