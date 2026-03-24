# Release Management in Java with Conductor : Preparation, Approval Gates, Deployment, and Announcements

Orchestrates the software release lifecycle using [Conductor](https://github.com/conductor-oss/conductor). This workflow prepares a release by tagging the version and building artifacts, gates it through an approval step before anything goes to production, deploys the approved release, and announces the release to stakeholders via configured channels.

## Shipping Without the Chaos

Version 2.4.0 is ready to ship. Someone needs to tag the commit, build release artifacts, get sign-off from the release manager, deploy to production, and announce the release to customers and internal teams. If deployment fails after approval, the release is in limbo. Approved but not deployed. If the announcement goes out before deployment actually succeeds, customers expect features that aren't live yet. Every step must happen in the right order, and every step's success must be confirmed before moving on.

Without orchestration, you'd manage releases via a checklist in a Confluence page and a sequence of manual commands. Deployments happen in someone's terminal with no audit trail. Approvals live in Slack threads that get lost. There's no record of which version was deployed when, whether it was approved, or who announced it.

## The Solution

**You write the release preparation and deployment logic. Conductor handles approval gating, deploy-before-announce sequencing, and the full release audit trail.**

Each stage of the release pipeline is a simple, independent worker. The preparer tags the version, builds release artifacts, and compiles the changelog. The approver gates the release: checking that all tests pass, the change log is complete, and the release manager has signed off. The deployer pushes the approved artifacts to production infrastructure. The announcer notifies stakeholders, posting release notes to Slack, updating the status page, and sending customer-facing changelogs. Conductor executes them in strict sequence, ensures deployment only happens after approval passes, retries if the deployment target is temporarily unavailable, and provides a complete audit trail of every release.

### What You Write: Workers

Four workers manage the release lifecycle. Preparing the version, gating through approval, deploying to production, and announcing to stakeholders.

| Worker | Task | What It Does |
|---|---|---|
| **AnnounceWorker** | `rm_announce` | Publishes release notes and notifies stakeholders (email, Slack, status page) |
| **ApproveWorker** | `rm_approve` | Gates the release with release-manager approval before production deployment |
| **DeployWorker** | `rm_deploy` | Deploys the approved version to the production environment |
| **PrepareWorker** | `rm_prepare` | Gathers changelogs, feature counts, and bug fixes into a release summary (e.g., "12 changes, 3 fixes") |

the workflow and rollback logic stay the same.

### The Workflow

```
rm_prepare
 │
 ▼
rm_approve
 │
 ▼
rm_deploy
 │
 ▼
rm_announce

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
