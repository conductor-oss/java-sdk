# Github Integration in Java Using Conductor

A critical bug fix gets merged to main at 3 PM. The deployment pipeline should have triggered automatically, but GitHub's webhook delivery failed, a transient 500 from your CI server that GitHub retried once and then gave up on. Nobody notices. The fix sits undeployed for 4 hours while customers keep hitting the bug, until an engineer happens to check the deploy dashboard and sees the last deploy was from yesterday. The webhook payload is gone, the CI job never ran, and there's no record of why. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the push-to-merge pipeline with durable webhook handling, CI retries, and full audit trails.

## Automating the PR Lifecycle from Push to Merge

When a developer pushes code, the resulting pull request needs to be created, CI checks need to run against it, and if all checks pass, the PR should be merged automatically. Each step depends on the previous one. You cannot run checks without a PR number, and you cannot merge without knowing the check results. If checks fail, the merge should be skipped.

Without orchestration, you would chain GitHub API calls manually, poll for check status, and manage PR numbers and SHA references between steps. Conductor sequences the pipeline and routes webhook data, PR metadata, and check results between workers automatically.

## The Solution

**You just write the GitHub workers. Webhook reception, PR creation, CI check execution, and merge handling. Conductor handles push-to-merge sequencing, CI check retries, and PR metadata routing between pipeline stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers automate the PR lifecycle: ReceiveWebhookWorker parses push events, CreatePrWorker opens pull requests, RunChecksWorker triggers CI validation, and MergePrWorker merges when all checks pass.

| Worker | Task | What It Does |
|---|---|---|
| **ReceiveWebhookWorker** | `gh_receive_webhook` | Receives a GitHub webhook push event. |
| **CreatePrWorker** | `gh_create_pr` | Creates a pull request on GitHub. |
| **RunChecksWorker** | `gh_run_checks` | Runs CI checks on the PR. |
| **MergePrWorker** | `gh_merge_pr` | Merges the pull request. |

The workers auto-detect GitHub credentials at startup. When `GITHUB_TOKEN` is set, CreatePrWorker and MergePrWorker use the real GitHub REST API (via `java.net.http`) to create and merge pull requests. Without the token, they fall back to demo mode with realistic output shapes so the workflow runs end-to-end without a GitHub token.

### The Workflow

```
gh_receive_webhook
 │
 ▼
gh_create_pr
 │
 ▼
gh_run_checks
 │
 ▼
gh_merge_pr

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
