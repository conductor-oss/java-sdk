# Implementing Saga with Parallel Booking via FORK/JOIN in Java with Conductor : Hotel, Flight, and Car in Parallel

## The Problem

You need to book a trip. hotel, flight, and car rental, and all three must succeed for the trip to be valid. The bookings are independent and can run in parallel for speed, but if any one fails after the others succeed, the successful ones must be cancelled. This is the saga pattern with parallel branches: fan out to book all three, collect results, and confirm or roll back.

Without orchestration, parallel booking means managing threads or async calls, collecting results, and implementing cancellation logic for each combination of partial successes. The code for "flight succeeded but car failed, so cancel flight and hotel" becomes a combinatorial nightmare.

## The Solution

**You just write the booking and cancellation logic per service. Conductor handles FORK/JOIN parallel execution across all booking services, result collection, confirm-or-compensate routing, retries per booking, and independent tracking of each parallel branch with timing and outcomes.**

Conductor's FORK/JOIN runs hotel, flight, and car booking workers in parallel. A check-results worker examines all outcomes. If all succeeded, a confirm-all worker finalizes the bookings. If any failed, the workflow routes to cancellation. Every parallel branch is tracked independently. you can see exactly which booking succeeded, which failed, and how long each took.

### What You Write: Workers

BookHotelWorker, BookFlightWorker, and BookCarWorker run simultaneously via FORK/JOIN for maximum speed, CheckResultsWorker verifies all bookings succeeded, and ConfirmAllWorker finalizes the trip or triggers cancellation.

| Worker | Task | What It Does |
|---|---|---|
| **BookCarWorker** | `sfj_book_car` | Worker for sfj_book_car. Books a rental car and returns a deterministic booking ID. |
| **BookFlightWorker** | `sfj_book_flight` | Worker for sfj_book_flight. Books a flight and returns a deterministic booking ID. |
| **BookHotelWorker** | `sfj_book_hotel` | Worker for sfj_book_hotel. Books a hotel and returns a deterministic booking ID. |
| **CheckResultsWorker** | `sfj_check_results` | Worker for sfj_check_results. Verifies all booking IDs are present. |
| **ConfirmAllWorker** | `sfj_confirm_all` | Worker for sfj_confirm_all. Confirms the entire trip if all bookings are valid. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
FORK_JOIN
 ├── sfj_book_hotel
 ├── sfj_book_flight
 └── sfj_book_car
 │
 ▼
JOIN (wait for all branches)
sfj_check_results
 │
 ▼
sfj_confirm_all

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
