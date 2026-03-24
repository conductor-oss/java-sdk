# Tutoring Match in Java with Conductor : Request Intake, Tutor Matching, Session Scheduling, and Confirmation

## The Problem

You need to connect students who need help with qualified tutors. A student requests tutoring in a specific subject at a preferred time, the system must find a tutor who is both qualified in that subject and available during that time slot, a session is scheduled, and both parties need confirmation with the session details. Matching a student with a tutor who is unavailable wastes everyone's time; scheduling without confirming availability leads to no-shows.

Without orchestration, you'd build a single matching service that queries the tutor database, checks availability calendars, creates calendar events, and sends confirmation emails. manually handling the case where a tutor's availability changes between matching and scheduling, retrying failed calendar API calls, and logging every step to investigate complaints about missed or double-booked sessions.

## The Solution

**You just write the request intake, tutor matching, session scheduling, and booking confirmation logic. Conductor handles availability retries, match scoring, and session scheduling audit trails.**

Each tutoring-match concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (request, match, schedule, confirm), retrying if the calendar service is temporarily unavailable, tracking every tutoring request from intake to confirmed session, and resuming from the last successful step if the process crashes.

### What You Write: Workers

Student profiling, tutor availability checking, compatibility scoring, and session scheduling workers each contribute to finding the right tutor match.

| Worker | Task | What It Does |
|---|---|---|
| **StudentRequestWorker** | `tut_student_request` | Records the student's tutoring request with subject and time preference |
| **MatchTutorWorker** | `tut_match_tutor` | Finds an available tutor qualified in the requested subject at the preferred time |
| **ScheduleWorker** | `tut_schedule` | Creates a tutoring session at the agreed time for the matched student-tutor pair |
| **ConfirmWorker** | `tut_confirm` | Sends session confirmation to both the student and the tutor |

### The Workflow

```
tut_student_request
 │
 ▼
tut_match_tutor
 │
 ▼
tut_schedule
 │
 ▼
tut_confirm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
