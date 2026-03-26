# Event Management

An event management platform handles the lifecycle of events from creation through scheduling, execution, and archival. Each event needs validation, resource allocation, scheduling against a calendar, and status tracking through its complete lifecycle.

## Pipeline

```
[evt_plan]
     |
     v
[evt_register]
     |
     v
[evt_execute]
     |
     v
[evt_followup]
```

**Workflow inputs:** `eventName`, `date`, `capacity`

## Workers

**ExecuteEventWorker** (task: `evt_execute`)

- Applies `math.floor()`
- Reads `attendees`. Writes `actualAttendees`, `sessions`, `speakersPresent`

**FollowupWorker** (task: `evt_followup`)

- Reads `attendees`. Writes `surveySent`, `satisfaction`, `nps`, `followUpEmails`

**PlanWorker** (task: `evt_plan`)

- Uppercases strings, records wall-clock milliseconds
- Reads `eventName`, `date`. Writes `eventId`, `venue`, `logistics`

**RegisterAttendeesWorker** (task: `evt_register`)

- Clamps with `math.min()`
- Reads `capacity`. Writes `registeredCount`, `waitlist`

---

**8 tests** | Workflow: `evt_event_management` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
