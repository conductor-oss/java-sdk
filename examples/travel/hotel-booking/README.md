# Hotel Booking

Hotel booking: search, filter, book, confirm, reminder.

**Input:** `travelerId`, `city`, `checkIn`, `checkOut` | **Timeout:** 60s

## Pipeline

```
htl_search
    │
htl_filter
    │
htl_book
    │
htl_confirm
    │
htl_reminder
```

## Workers

**BookWorker** (`htl_book`)

Reads `travelerId`. Outputs `reservationId`, `confirmationCode`.

**ConfirmWorker** (`htl_confirm`)

Reads `reservationId`. Outputs `confirmed`.

**FilterWorker** (`htl_filter`)

Outputs `bestMatch`.

**ReminderWorker** (`htl_reminder`)

Reads `travelerId`. Outputs `reminderSet`, `reminderDate`.

**SearchWorker** (`htl_search`)

Reads `city`. Outputs `hotels`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
