# Feature Environment in Java with Conductor

Automates on-demand feature environment provisioning using [Conductor](https://github.com/conductor-oss/conductor). This workflow provisions an isolated Kubernetes namespace for a feature branch, deploys the branch code, configures a preview DNS URL, and posts the preview link back to the pull request.

## Preview Environments for Every PR

A developer opens a pull request for `feature-auth-v2`. Reviewers want to click a link and see it running. Not read diffs. The workflow provisions a namespace, deploys the branch, sets up a preview URL like `feature-auth-v2.preview.example.com`, and comments the link on the PR. When the PR is merged, the environment gets torn down.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the provisioning and deployment logic. Conductor handles namespace-to-notification sequencing, retries, and environment lifecycle tracking.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. your workers call the infrastructure APIs.

### What You Write: Workers

Four workers provision the preview environment. Creating the namespace, deploying the branch, configuring DNS, and notifying the pull request.

| Worker | Task | What It Does |
|---|---|---|
| **ConfigureDnsWorker** | `fe_configure_dns` | Sets up a preview DNS record (e.g., `branch-name.preview.example.com`) pointing to the deployed environment |
| **DeployBranchWorker** | `fe_deploy_branch` | Builds and deploys the feature branch code to the provisioned preview environment |
| **NotifyWorker** | `fe_notify` | Posts the preview URL as a comment on the pull request so reviewers can access it |
| **ProvisionWorker** | `fe_provision` | Creates an isolated Kubernetes namespace (or VM/container) for the feature branch |

the workflow and rollback logic stay the same.

### The Workflow

```
fe_provision
 │
 ▼
fe_deploy_branch
 │
 ▼
fe_configure_dns
 │
 ▼
fe_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
