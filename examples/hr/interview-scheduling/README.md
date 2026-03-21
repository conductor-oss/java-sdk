# Interview Scheduling in Java with Conductor : Availability Check, Slot Selection, Candidate Invite, Confirmation, and Reminder

## The Problem

You need to coordinate interview scheduling across multiple busy interviewers and a candidate. Given an interview panel (e.g., hiring manager, two engineers, a product lead), you must find time slots where all interviewers are free. The best available slot is selected, and the candidate receives an invite with the date, time, role description, interviewer names, and a video conference link. Each interviewer must confirm their participation. On the day of the interview, all parties receive reminders with the agenda and join details. If any interviewer's calendar changes after booking, the system needs to detect the conflict and reschedule. Coordinating this manually across four or five calendars through back-and-forth emails regularly takes days and creates a poor candidate experience.

Without orchestration, you'd build a scheduling tool that queries each interviewer's calendar API, finds overlapping free slots, sends emails, polls for confirmations, and fires off reminders. If Google Calendar's API rate-limits you while checking availability for a large panel, you'd need retry logic. If the system crashes after sending the candidate invite but before the interviewers are confirmed, you have a scheduled interview that interviewers don't know about. Recruiters need visibility into which interviews are confirmed versus pending so they can follow up on stragglers.

## The Solution

**You just write the availability checking, slot selection, candidate invitation, confirmation, and reminder logic. Conductor handles calendar booking retries, availability matching, and scheduling audit trails.**

Each stage of interview scheduling is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of checking availability before scheduling, scheduling before inviting, confirming all participants after the invite is sent, sending reminders only after confirmation, retrying if a calendar API is temporarily unavailable, and giving recruiters complete visibility into every interview's scheduling status. ### What You Write: Workers

Availability collection, slot matching, calendar booking, and reminder workers coordinate interviews without coupling to the hiring decision logic.

| Worker | Task | What It Does |
|---|---|---|
| **AvailabilityWorker** | `ivs_availability` | Queries each interviewer's calendar and returns overlapping available slots (e.g., "2024-03-25 10:00", "2024-03-25 14:00", "2024-03-26 11:00") |
| **ScheduleWorker** | `ivs_schedule` | Selects the optimal time slot from available options, books the room or video link, and assigns an interview ID |
| **InviteWorker** | `ivs_invite` | Sends the candidate an interview invitation with the scheduled time, interviewer names, role details, and video join link |
| **ConfirmWorker** | `ivs_confirm` | Confirms all interviewers have accepted the calendar hold and flags any who have declined or not responded |
| **RemindWorker** | `ivs_remind` | Sends day-of reminders to the candidate and all interviewers with the agenda, scorecard link, and join instructions |

### The Workflow

```
ivs_availability
 │
 ▼
ivs_schedule
 │
 ▼
ivs_invite
 │
 ▼
ivs_confirm
 │
 ▼
ivs_remind

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
