# Healthcare Appointment Scheduling in Java Using Conductor : Provider Availability, Booking, Confirmation, and Reminders

## The Problem

You need to schedule patient appointments across a healthcare organization. A scheduling request comes in with a provider ID, preferred date, and visit type. The system must query the provider's calendar for open slots (and suggest alternates if the preferred time is taken), reserve the chosen slot, send the patient a confirmation with the appointment details, and schedule a reminder notification before the visit. Each step depends on the previous one. you cannot book without an available slot, and you cannot confirm without a booking.

Without orchestration, you'd build a monolithic scheduling service that queries the EHR calendar, writes to the booking database, calls the notification API, and sets up the reminder. all in a single class with inline error handling. If the EHR is briefly unavailable, you'd need retry logic. If the system crashes after booking but before confirming, the patient never receives their appointment details, and the front desk has no record that the confirmation failed.

## The Solution

**You just write the scheduling workers. Availability checks, slot booking, patient confirmation, and reminder setup. Conductor handles step sequencing, automatic retries when the EHR is briefly unavailable, and a complete scheduling audit trail.**

Each step of the scheduling process is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of checking availability before booking, sending confirmations only after a successful booking, retrying if the EHR calendar API is temporarily unavailable, and maintaining a complete record of every scheduling attempt. ### What You Write: Workers

Four workers divide the scheduling lifecycle: CheckAvailabilityWorker queries provider calendars, BookWorker reserves the slot, ConfirmWorker notifies the patient, and RemindWorker sets up pre-visit reminders.

| Worker | Task | What It Does |
|---|---|---|
| **CheckAvailabilityWorker** | `apt_check_availability` | Queries the provider's calendar for the preferred date, returns the best available slot and alternate options |
| **BookWorker** | `apt_book` | Reserves the selected time slot on the provider's calendar for the patient |
| **ConfirmWorker** | `apt_confirm` | Sends appointment confirmation (date, time, location, provider) to the patient via SMS or email |
| **RemindWorker** | `apt_remind` | Schedules a reminder notification to be sent before the appointment (e.g., 24 hours prior) |

the workflow and compliance logic stay the same.

### The Workflow

```
Input -> BookWorker -> CheckAvailabilityWorker -> ConfirmWorker -> RemindWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
