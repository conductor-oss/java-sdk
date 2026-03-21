# Event Management in Java with Conductor : Plan, Register, Execute, and Follow Up on Events

## Running Events Without Dropping the Ball

Events have a strict lifecycle: plan the logistics, open and manage registrations, execute the event itself, then follow up with attendees. Each phase must complete before the next begins. you cannot register attendees for an unplanned event, and you cannot follow up before the event happens. Missing a phase or running them out of order results in confused attendees, overbooked venues, or missed follow-ups that waste the event's value.

This workflow enforces the event lifecycle. The planner sets up the event with venue, schedule, and capacity. The registration worker processes attendee sign-ups. The execution worker manages the live event. The follow-up worker sends thank-you emails, surveys, and next-step communications to attendees. Each step's output feeds the next. the event plan feeds registration, registration counts feed execution, and attendee lists feed follow-up.

## The Solution

**You just write the event-planning, registration, execution, and follow-up workers. Conductor handles the phase sequencing and attendee data flow.**

Four workers handle the event lifecycle. planning, registration, execution, and follow-up. The planner creates the event with venue and capacity. The registration worker processes attendees. The executor runs the event. The follow-up worker sends post-event communications. Conductor sequences the four phases and passes event details, attendee lists, and execution data between them automatically.

### What You Write: Workers

PlanWorker sets up venue and capacity, RegisterAttendeesWorker processes sign-ups, ExecuteEventWorker manages the live event, and FollowupWorker sends post-event surveys and communications.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteEventWorker** | `evt_execute` | Manages the live event and records actual attendance numbers. |
| **FollowupWorker** | `evt_followup` | Sends post-event surveys, thank-you emails, and next-step communications to attendees. |
| **PlanWorker** | `evt_plan` | Sets up the event with venue, schedule, and capacity details. |
| **RegisterAttendeesWorker** | `evt_register` | Processes attendee registrations up to the event's capacity limit. |

### The Workflow

```
evt_plan
 │
 ▼
evt_register
 │
 ▼
evt_execute
 │
 ▼
evt_followup

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
