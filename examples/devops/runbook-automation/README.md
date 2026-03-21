# Runbook Automation in Java with Conductor

Your runbooks live in a Confluence wiki that was last updated eight months ago. When the database failover alert fires at 3 AM, the on-call engineer opens the page, squints at step 4 ("promote the replica: see Jira ticket DB-247 for details"), guesses at the parameters, runs the commands in the wrong order, and spends 20 minutes cleaning up the mess before starting over. The next engineer who gets this alert will have the same experience, because nobody updated the wiki after the schema changed in January. Every incident is a fresh adventure, and MTTR is a function of who happens to be on call. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to turn runbook steps into executable, versioned automation, load the procedure, execute remediation, verify the fix, and log the outcome, so incident response is identical regardless of who's holding the pager.

## The 3 AM Runbook Problem

A database failover alert fires. The on-call engineer opens the wiki, finds the "database-failover" runbook, and manually executes each step: promote the replica, verify the new primary accepts connections, update connection strings. Each step depends on the previous one, and skipping or re-ordering them risks data loss. Automating this sequence means incidents get resolved in seconds instead of the 20 minutes it takes a sleep-deprived human to follow a checklist.

Without orchestration, you'd wire all of this together in a single monolithic class. Managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the remediation steps. Conductor handles runbook sequencing, verification gates, and execution logging.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. Your workers call the infrastructure APIs.

### What You Write: Workers

Each worker handles one runbook stage. Loading the procedure, executing remediation, verifying the fix, and logging the outcome with timing data.

| Worker | Task | What It Does |
|---|---|---|
| `LoadRunbookWorker` | `ra_load_runbook` | Looks up the versioned runbook definition by name and returns its ID and version (e.g., "database-failover v3") |
| `ExecuteStepWorker` | `ra_execute_step` | Runs the primary remediation action from the runbook (e.g., promotes a database replica to primary) |
| `VerifyStepWorker` | `ra_verify_step` | Validates that the remediation succeeded (e.g., confirms the new primary is accepting connections) |
| `LogOutcomeWorker` | `ra_log_outcome` | Records the final execution outcome and duration (45s) for audit and post-mortem review |

### The Workflow

```
ra_load_runbook
 |
 v
ra_execute_step
 |
 v
ra_verify_step
 |
 v
ra_log_outcome

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
