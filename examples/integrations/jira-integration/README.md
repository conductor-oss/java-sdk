# Jira Integration

Jira integration: create issue, track status, transition, notify

**Input:** `project`, `summary`, `description`, `assignee` | **Timeout:** 1800s

## Pipeline

```
jra_create_issue
    │
jra_track_status
    │
jra_transition
    │
jra_notify
```

## Workers

**CreateIssueWorker** (`jra_create_issue`): Creates a Jira issue via the Jira REST API.

```java
.header("Authorization", "Basic " + auth)
```

Reads `description`, `project`, `summary`. Outputs `issueKey`, `createdAt`, `demoMode`.
Returns `FAILED` on validation errors.

**NotifyWorker** (`jra_notify`): Notifies the assignee about a Jira issue status change.

Reads `assignee`, `issueKey`, `newStatus`. Outputs `notified`, `status`.

**TrackStatusWorker** (`jra_track_status`): Tracks the current status of a Jira issue.

Reads `issueKey`. Outputs `currentStatus`, `lastUpdated`.

**TransitionWorker** (`jra_transition`): Transitions a Jira issue from one status to another.

Reads `fromStatus`, `issueKey`, `toStatus`. Outputs `newStatus`, `transitionedAt`.

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
