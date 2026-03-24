# Saga with Parallel Booking (FORK/JOIN)

A trip booking system must reserve a hotel, flight, and rental car. The three bookings are independent and can run in parallel for speed, but all three must succeed for the trip to be valid. After the parallel phase, a verification step checks all booking IDs are present before the final confirmation.

## Workflow

```
FORK ──┬── sfj_book_hotel ───┐
       ├── sfj_book_flight ──┤
       └── sfj_book_car ─────┤
                              JOIN
                               │
                        sfj_check_results ──> sfj_confirm_all
```

Workflow `saga_fork_join_demo` accepts `tripId` as input. The `FORK_JOIN` runs three booking branches in parallel. `JOIN` waits on `sfj_book_hotel_ref`, `sfj_book_flight_ref`, and `sfj_book_car_ref`.

## Workers

**BookHotelWorker** (`sfj_book_hotel`) -- reads `tripId` from input (defaults to `"unknown"`). Returns `bookingId` = `"HTL-001"`, `type` = `"hotel"`, and the `tripId`.

**BookFlightWorker** (`sfj_book_flight`) -- reads `tripId` from input. Returns `bookingId` = `"FLT-001"`, `type` = `"flight"`, and the `tripId`.

**BookCarWorker** (`sfj_book_car`) -- reads `tripId` from input. Returns `bookingId` = `"CAR-001"`, `type` = `"car"`, and the `tripId`.

**CheckResultsWorker** (`sfj_check_results`) -- receives `hotelBookingId`, `flightBookingId`, and `carBookingId`. Checks each via the `isPresent()` helper (non-null and non-empty). Sets `allValid` = `true` only if all three are present. Returns `FAILED` with `error` = `"One or more bookings missing"` if any ID is absent.

**ConfirmAllWorker** (`sfj_confirm_all`) -- reads `tripId` and `allValid` from input. When `allValid` is `true`, returns `confirmed` = `true` with `message` = `"Trip " + tripId + " confirmed with all bookings"`. When `false`, returns `FAILED` with `confirmed` = `false` and `message` = `"Trip " + tripId + " rolled back -- not all bookings valid"`.

## Workflow Output

The workflow produces `tripId`, `hotelBookingId`, `flightBookingId`, `carBookingId`, `confirmed` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 5 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `saga_fork_join_demo` defines 4 tasks with input parameters `tripId` and a timeout of `120` seconds.

## Tests

5 tests verify parallel booking execution, result collection after join, validation of all booking IDs, successful confirmation, and the failure path when a booking is missing.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
