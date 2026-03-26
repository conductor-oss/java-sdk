# Handing Off On-Call with Active Incident Transfer

The rotation timer fires and Alice's shift ends, but she has 2 active incidents. If the
handoff is not explicit, Bob starts his shift without knowing about them and the incidents
go unattended. This workflow checks the schedule, transfers 2 active incidents from Alice
to Bob, updates PagerDuty routing, and confirms the new on-call has acknowledged.

## Workflow

```
team, rotationType
      |
      v
+---------------------+     +----------------+     +----------------------+     +---------------+
| oc_check_schedule   | --> | oc_handoff     | --> | oc_update_routing    | --> | oc_confirm    |
+---------------------+     +----------------+     +----------------------+     +---------------+
  CHECK_SCHEDULE-1524        2 active incidents     PagerDuty routing           new on-call
  platform-eng:              handed off              updated                     confirmed
  Alice -> Bob
```

## Workers

**CheckScheduleWorker** -- Checks the rotation schedule for the `team`. Finds that
platform-eng is due for rotation: Alice -> Bob. Returns
`check_scheduleId: "CHECK_SCHEDULE-1524"`.

**HandoffWorker** -- Transfers 2 active incidents to the incoming on-call. Returns
`handoff: true`.

**UpdateRoutingWorker** -- Updates PagerDuty routing rules to point to the new on-call.
Returns `update_routing: true`.

**ConfirmWorker** -- Confirms the new on-call has acknowledged their shift. Returns
`confirm: true`.

## Tests

2 unit tests cover the on-call rotation pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
