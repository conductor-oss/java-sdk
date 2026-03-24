# Reservation System

Orchestrates reservation system through a multi-stage Conductor workflow.

**Input:** `guestName`, `date`, `time`, `partySize` | **Timeout:** 60s

## Pipeline

```
rsv_check_availability
    │
rsv_book
    │
rsv_confirm
    │
rsv_remind
    │
rsv_seat
```

## Workers

**BookWorker** (`rsv_book`)

Reads `guestName`. Outputs `reservationId`, `booked`.

**CheckAvailabilityWorker** (`rsv_check_availability`)

Reads `date`, `partySize`, `time`. Outputs `slot`.

**ConfirmWorker** (`rsv_confirm`)

Reads `guestName`. Outputs `confirmed`, `confirmationCode`.

**RemindWorker** (`rsv_remind`)

Reads `reservationId`. Outputs `reminded`, `channel`.

**SeatWorker** (`rsv_seat`)

```java
result.addOutputData("seated", Map.of("reservationId", reservationId != null ? reservationId : "RSV-736", "table", "T-8", "status", "SEATED"));
```

Reads `partySize`, `reservationId`. Outputs `seated`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
