# Hotel Booking in Java with Conductor

Hotel booking: search, filter, book, confirm, reminder. ## The Problem

You need to book a hotel for a business traveler. Searching available hotels in the destination city for the travel dates, filtering results by company travel policy (maximum nightly rate, preferred chains, required amenities), reserving the selected hotel, confirming the booking with the hotel, and scheduling a check-in reminder for the traveler. Each step depends on the previous one's output.

If the booking succeeds but confirmation fails, the hotel may release the room and the traveler arrives with no reservation. If filtering removes all results because the policy rate cap is too low for that city, the traveler needs to know immediately so they can request a policy exception. Without orchestration, you'd build a monolithic hotel handler that mixes hotel API queries, policy rule evaluation, reservation management, and notification scheduling. Making it impossible to update policy rules, swap hotel providers, or track which policy filters drove which booking decisions.

## The Solution

**You just write the hotel search, policy filtering, reservation, confirmation, and reminder scheduling logic. Conductor handles rate comparison retries, reservation sequencing, and booking audit trails.**

SearchWorker queries hotel availability in the destination city for the check-in and check-out dates. FilterWorker applies the company's travel policy. Maximum nightly rate, preferred hotel chains, required amenities (Wi-Fi, breakfast), and removes non-compliant options. BookWorker reserves the best matching hotel and returns a reservation ID. ConfirmWorker finalizes the booking with the hotel and obtains the confirmation number. ReminderWorker schedules a check-in reminder notification for the traveler before their arrival date. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

Availability search, rate comparison, reservation, and confirmation workers each handle one step of securing hotel accommodations.

| Worker | Task | What It Does |
|---|---|---|
| **BookWorker** | `htl_book` | Books the input and computes reservation id, confirmation code |
| **ConfirmWorker** | `htl_confirm` | Confirm. Computes and returns confirmed |
| **FilterWorker** | `htl_filter` | Filtered hotels by policy compliance |
| **ReminderWorker** | `htl_reminder` | Schedules a check-in reminder notification for the guest |
| **SearchWorker** | `htl_search` | Searching hotels in |

### The Workflow

```
htl_search
 │
 ▼
htl_filter
 │
 ▼
htl_book
 │
 ▼
htl_confirm
 │
 ▼
htl_reminder

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
