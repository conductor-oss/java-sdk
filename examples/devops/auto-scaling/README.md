# Threshold-Based Auto-Scaling with Verification

Your platform runs multiple services at varying load levels. When CPU crosses 80%, you need
to scale up; when it drops below 30%, you should scale down to save cost. But scaling without
verification risks silent failures -- pods crashing, load not actually decreasing. This
workflow analyzes metrics, plans the action, executes it, and confirms the result.

## Workflow

```
service, metric, threshold
         |
         v
  +-------------+     +-----------+     +-------------+     +-------------+
  | as_analyze  | --> | as_plan   | --> | as_execute  | --> | as_verify   |
  +-------------+     +-----------+     +-------------+     +-------------+
    currentLoad         action:          newInstanceCount     verified=true
    per service         scale-up/down    5 (up), 2 (down)    newLoad checked
```

## Workers

**Analyze** -- Takes `service` and `metric` (defaults to `"cpu"`). Returns deterministic
`currentLoad` values by service name: `"api-server"` = 85%, `"web-frontend"` = 45%,
`"batch-worker"` = 92%, `"cache-service"` = 30%, default = 60%. Also computes `avgLoad15m`
(currentLoad - 5), `peakLoad1h` (currentLoad + 8), and `currentInstances` = 3.

**Plan** -- Reads `currentLoad` as an integer. If >= 80 the action is `"scale-up"` (from 3 to
5 instances). If <= 30 the action is `"scale-down"` (from 3 to max(1, 2)). Otherwise
`"no-change"`. Outputs a human-readable `reason` string.

**Execute** -- Receives the `action` string. For `"scale-up"`, sets `newInstanceCount` to 5.
For `"scale-down"`, sets it to 2. For anything else, keeps 3 and marks `scaled: false`.
Stamps an `executedAt` ISO timestamp.

**Verify** -- Confirms the scaling worked. After `"scale-up"`, reports CPU dropped to 52%.
After `"scale-down"`, reports load stable at 45%. No-change reports 50%. Always sets
`verified: true`.

## Tests

37 unit tests cover metric analysis, planning thresholds, execution paths, and verification.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
