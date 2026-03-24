# Reservation System in Java with Conductor

Manages restaurant reservations end-to-end: checking table availability, booking, sending confirmation and reminder, and seating the party on arrival.

## The Problem

You need to manage restaurant reservations from booking to seating. The workflow checks table availability for the requested date, time, and party size, creates the reservation, sends a confirmation to the guest, sends a reminder before the reservation, and seats the party when they arrive. Double-booking a table ruins the dining experience; forgetting to send reminders leads to no-shows.

Without orchestration, you'd build a single reservation service that queries availability, inserts bookings, sends confirmation emails, schedules reminder jobs, and updates table status. manually handling overlapping reservations, cancellations, waitlist management, and the timing of reminder notifications.

## The Solution

**You just write the availability check, booking, confirmation, reminder, and seating logic. Conductor handles availability retries, table assignment, and reservation lifecycle tracking.**

Each reservation concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (check availability, book, confirm, remind, seat), retrying if the notification service is unavailable, tracking every reservation from booking to seating, and resuming from the last step if the process crashes.

### What You Write: Workers

Availability checking, table assignment, confirmation, and reminder workers handle restaurant reservations as a sequence of independent steps.

| Worker | Task | What It Does |
|---|---|---|
| **BookWorker** | `rsv_book` | Creates the reservation for the guest and returns a reservation ID |
| **CheckAvailabilityWorker** | `rsv_check_availability` | Checks table availability for the requested date, time, and party size, returning the table ID and section |
| **ConfirmWorker** | `rsv_confirm` | Sends a confirmation to the guest and returns a confirmation code |
| **RemindWorker** | `rsv_remind` | Sends a reminder to the guest before the reservation via SMS |
| **SeatWorker** | `rsv_seat` | Seats the party at the assigned table and marks the reservation as seated |

### The Workflow

```
rsv_check_availability
 │
 ▼
rsv_book
 │
 ▼
rsv_confirm
 │
 ▼
rsv_remind
 │
 ▼
rsv_seat

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
