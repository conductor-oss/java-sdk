# Jira Integration in Java Using Conductor

Your sprint board shows 47 tickets "In Progress." The standup takes 25 minutes because nobody can tell which ones are actually being worked on. Three of those bugs were fixed last week: the engineers pushed the code, closed the PRs, and forgot to update Jira. Eight more are blocked but still show "In Progress" because the blocked status requires a manual transition that nobody remembers to do. The PM pulls a velocity report for the stakeholder meeting and it says the team completed 4 story points this sprint, when they actually shipped 22. The data is useless because ticket status and actual work diverged weeks ago. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the Jira issue lifecycle, creation, transitions, tracking, and notifications, as a durable pipeline.

## Automating Jira Issue Lifecycle

When an event triggers issue creation (e.g., a deployment, a bug report, a feature request), the issue needs to be created in Jira, transitioned through workflow statuses, tracked for updates, and the assignee notified of each change. Each step depends on the previous one. You cannot transition an issue before creating it, and you cannot notify about a status change before the transition happens.

Without orchestration, you would chain Jira REST API calls manually, manage issue keys between steps, and build custom notification logic. Conductor sequences the pipeline and routes issue keys, statuses, and assignee information between workers automatically.

## The Solution

**You just write the Jira workers. Issue creation, status transitions, tracking, and assignee notification. Conductor handles create-to-notify sequencing, Jira API retries, and issue key routing between transition and tracking stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers manage the issue lifecycle: CreateIssueWorker opens tickets, TransitionWorker moves issues through statuses, TrackStatusWorker monitors current state, and NotifyWorker alerts assignees of changes.

| Worker | Task | What It Does |
|---|---|---|
| **CreateIssueWorker** | `jra_create_issue` | Creates a Jira issue via the Jira REST API (POST /rest/api/3/issue). When `JIRA_URL` and `JIRA_API_TOKEN` are set, calls the real Jira API. When unset, runs in demo mode with deterministic issue keys. |
| **TransitionWorker** | `jra_transition` | Transitions the issue. moves the issue through workflow statuses (To Do -> In Progress -> Done) and returns the new status |
| **TrackStatusWorker** | `jra_track_status` | Tracks the issue status: queries the current status, assignee, and last update timestamp for the issue |
| **NotifyWorker** | `jra_notify` | Notifies the assignee. sends a notification about the status change with the issue key and new status |

### The Workflow

```
Input -> CreateIssueWorker -> NotifyWorker -> TrackStatusWorker -> TransitionWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
