# Webinar Registration in Java with Conductor

## Getting Attendees Registered, Reminded, and Followed Up

A webinar registration workflow is more than just recording a name. the attendee needs to be registered, sent a confirmation email, reminded before the event (24 hours and 1 hour out), and followed up afterward with a recording link and next steps. Missing any step means lower attendance rates and lost engagement opportunities. Each step depends on the previous one, you cannot send a confirmation until registration succeeds, and follow-up only makes sense after the event.

This workflow manages a single attendee's webinar journey. The registrar records the attendee and generates a registration ID. The confirmer sends a confirmation email with event details and a calendar link. The reminder sends timed notifications before the webinar starts. The follow-up worker sends a post-event email with the recording link and related resources.

## The Solution

**You just write the registration, confirmation, reminder, and follow-up workers. Conductor handles the attendee lifecycle and timed notification sequencing.**

Each worker handles one CRM operation. Conductor manages the customer lifecycle pipeline, assignment routing, follow-up scheduling, and activity tracking.

### What You Write: Workers

RegisterWorker records the attendee, ConfirmWorker sends event details with a calendar invite, RemindWorker delivers timed notifications at 24h and 1h before, and FollowupWorker sends the recording link afterward.

| Worker | Task | What It Does |
|---|---|---|
| **ConfirmWorker** | `wbr_confirm` | Sends a confirmation email with event details and a calendar invite link. |
| **FollowupWorker** | `wbr_followup` | Sends a post-webinar email with the recording link and related resources. |
| **RegisterWorker** | `wbr_register` | Registers the attendee for the webinar and generates a unique registration ID. |
| **RemindWorker** | `wbr_remind` | Sends reminder notifications at 24 hours and 1 hour before the event. |

### The Workflow

```
wbr_register
 │
 ▼
wbr_confirm
 │
 ▼
wbr_remind
 │
 ▼
wbr_followup

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
