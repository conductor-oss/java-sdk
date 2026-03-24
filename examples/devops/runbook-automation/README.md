# Executing Runbooks with Real Command Execution and Outcome Logging

When an incident hits, the on-call engineer opens a wiki page and runs commands by hand,
forgetting steps and mistyping arguments. This workflow loads a runbook from a YAML file
on disk (or creates a default one), executes each step via `ProcessBuilder`, verifies step
completion and system health, and logs the outcome to a JSON file.

## Workflow

```
runbookName, trigger
         |
         v
+---------------------+     +--------------------+     +--------------------+     +--------------------+
| ra_load_runbook     | --> | ra_execute_step    | --> | ra_verify_step     | --> | ra_log_outcome     |
+---------------------+     +--------------------+     +--------------------+     +--------------------+
  loads YAML from             runs each step via        checks: no failures,      writes JSON log to
  /tmp/runbooks/              ProcessBuilder            no timeouts, health ok,   /tmp/runbook-logs/
  returns runbookId           tracks success/fail       execution time            with outcome/duration
```

## Workers

**LoadRunbookWorker** -- Reads a YAML runbook from `/tmp/runbooks/{runbookName}.yaml`. If it
does not exist, creates a default runbook. Returns `runbookId`, `runbookName`, `version`,
and the parsed step list.

**ExecuteStepWorker** -- Iterates over steps, executing each command via `ProcessBuilder`.
Tracks `successCount`, `failureCount`, and `totalDurationMs` for each step result.

**VerifyStepWorker** -- Runs four checks: no step failures, no timeouts, system health
(via `uptime` command), and execution time within 300,000ms. Returns `verified` boolean
and `checksTotal`.

**LogOutcomeWorker** -- Writes the execution outcome (pass/fail), duration, and runbook ID
to a JSON file in `/tmp/runbook-logs/`. Returns `outcome`, `logFile` path, and `loggedAt`.

## Tests

30 unit tests cover runbook loading, step execution, verification, and outcome logging.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
