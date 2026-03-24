# Webinar Registration

A conference platform handles webinar registrations. Each registration needs attendee validation (email format, capacity check), confirmation email dispatch, calendar invite generation, and waitlist management when the session reaches capacity.

## Pipeline

```
[wbr_register]
     |
     v
[wbr_confirm]
     |
     v
[wbr_remind]
     |
     v
[wbr_followup]
```

**Workflow inputs:** `attendeeName`, `email`, `webinarId`

## Workers

**ConfirmWorker** (task: `wbr_confirm`)

- Reads `email`. Writes `confirmed`, `calendarInvite`

**FollowupWorker** (task: `wbr_followup`)

- Writes `sent`, `content`

**RegisterWorker** (task: `wbr_register`)

- Uppercases strings, records wall-clock milliseconds
- Reads `name`, `webinarId`. Writes `registrationId`, `joinLink`, `calendarAdded`

**RemindWorker** (task: `wbr_remind`)

- Writes `reminders`

---

**8 tests** | Workflow: `wbr_webinar_registration` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
