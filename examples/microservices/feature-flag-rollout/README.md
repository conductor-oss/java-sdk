# Graduated Feature Flag Rollout with Impact Monitoring

Enabling a new feature for all users at once is risky -- if conversions drop, you cannot
tell if it was the feature or something else. This workflow creates the flag, enables it for
a target segment at a specified percentage, monitors the impact (conversion +2.3%, errors
stable), and either completes the full rollout or kills it based on health.

## Workflow

```
flagName, targetSegment, rolloutPercentage
                  |
                  v
+------------------+     +---------------------+     +---------------------+     +---------------------+
| ff_create_flag   | --> | ff_enable_segment   | --> | ff_monitor_impact   | --> | ff_full_rollout     |
+------------------+     +---------------------+     +---------------------+     +---------------------+
  FLAG-20260115            enabled for segment          conversionDelta: +2.3%   active based on
  created: true            at rolloutPercentage         errorDelta: 0             healthy flag
                                                        healthy: true
```

## Workers

**CreateFlagWorker** -- Creates flag `flagName`. Returns `flagId: "FLAG-20260115"`,
`created: true`.

**EnableSegmentWorker** -- Enables the flag for `segment` at `percentage`. Returns
`enabled: true`.

**MonitorImpactWorker** -- Monitors metrics: `conversionDelta: 2.3`, `errorDelta: 0`,
`healthy: true`.

**FullRolloutWorker** -- Completes or kills the rollout based on health. Returns
`active` flag and `flagName`.

## Tests

No unit tests for this example.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
