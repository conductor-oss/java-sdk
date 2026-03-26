# Saga Pattern: Orchestrated Compensation with a ConcurrentHashMap Booking Store

A customer books a trip: flight confirmed, hotel reserved, then the payment is declined. Without compensation, the airline holds a seat and the hotel holds a room for a trip that will never happen. The customer sees "payment failed" but gets charged a no-show fee three weeks later. The saga pattern solves this by pairing every forward action with a compensating action and running them in reverse order on failure. This example implements an orchestrated saga using [Conductor](https://github.com/conductor-oss/conductor) with three `ConcurrentHashMap` stores that track exactly which bookings exist at any moment, so compensation workers can verify they actually removed the record they were supposed to undo.

## How the Workflow Uses SWITCH for Compensation

The workflow definition in `workflow.json` is not a simple linear sequence. After the three forward steps, a `SWITCH` task inspects the payment status:

```
saga_reserve_hotel  -->  saga_book_flight  -->  saga_charge_payment  -->  SWITCH
                                                                          |
                        "failed": saga_cancel_flight --> saga_cancel_hotel --> TERMINATE(ROLLED_BACK)
                        default:  workflow completes with booking details
```

The SWITCH uses `value-param` evaluation on `${charge_payment_ref.output.status}`. When the status is `"failed"`, it triggers the compensation branch: cancel flight first, then cancel hotel (reverse order of the forward steps), then terminate the workflow with `status: "ROLLED_BACK"`. When the payment succeeds, the default case passes through and the workflow completes normally.

Task definitions in `task-defs.json` configure `retryCount: 2` and `responseTimeoutSeconds: 30` for all six tasks -- forward and compensation -- ensuring that even cancellation operations are retried if they fail transiently.

## The BookingStore: Proving Compensation Actually Works

The key design insight is `BookingStore`, a shared in-memory store using three `ConcurrentHashMap<String, String>` instances:

```java
static final ConcurrentHashMap<String, String> HOTEL_RESERVATIONS = new ConcurrentHashMap<>();
static final ConcurrentHashMap<String, String> FLIGHT_BOOKINGS = new ConcurrentHashMap<>();
static final ConcurrentHashMap<String, String> PAYMENT_TRANSACTIONS = new ConcurrentHashMap<>();
```

Forward workers `put()` entries; compensation workers `remove()` them. The `remove()` return value tells you whether the booking actually existed: `removedFromStore: true` means the compensation undid a real booking, `false` means the booking was already gone (idempotent compensation).

A synchronized `ACTION_LOG` list records every operation in order: `"BOOK_FLIGHT:FLT-TRIP-100"`, `"RESERVE_HOTEL:HTL-TRIP-100"`, `"CHARGE_PAYMENT:TXN-TRIP-100"`. The integration tests use this log to verify that compensation runs in the correct reverse order.

## Forward Workers

**ReserveHotelWorker** (`saga_reserve_hotel`) -- Creates a reservation entry `"HTL-" + tripId` in the hotel store. Supports a `shouldFail` flag that simulates hotel unavailability by returning `FAILED_WITH_TERMINAL_ERROR` without creating a store entry -- meaning no compensation is needed for a reservation that was never made.

**BookFlightWorker** (`saga_book_flight`) -- Creates a booking entry `"FLT-" + tripId` in the flight store. Returns the `bookingId` and `bookedAt` timestamp.

**ChargePaymentWorker** (`saga_charge_payment`) -- Creates a transaction entry `"TXN-" + tripId` on success. Validates that if an `amount` is provided, it must be a positive number. When `shouldFail` is `true` (either as boolean or string `"true"`), the worker returns `FAILED_WITH_TERMINAL_ERROR` with `status: "failed"` -- this is what triggers the SWITCH compensation branch.

## Compensation Workers

**CancelFlightWorker** (`saga_cancel_flight`) -- Calls `BookingStore.FLIGHT_BOOKINGS.remove(bookingId)`. If no explicit `bookingId` is provided, defaults to `"FLT-" + tripId`. Returns `removedFromStore: true/false` to indicate whether the booking existed.

**CancelHotelWorker** (`saga_cancel_hotel`) -- Same pattern: removes from `HOTEL_RESERVATIONS`, defaults to `"HTL-" + tripId`.

**RefundPaymentWorker** (`saga_refund_payment`) -- Removes from `PAYMENT_TRANSACTIONS` using `"TXN-" + tripId`. This worker is registered in `task-defs.json` but not used in the current workflow's compensation branch (because when payment fails, no transaction was created). It exists for the case where you need to compensate after a successful payment -- for example, if a post-payment validation step fails.

## Idempotent Compensation

A critical property of saga compensation workers is idempotency. Every cancel/refund worker returns `removedFromStore: true` if the booking existed and was removed, or `removedFromStore: false` if it was already gone. This means running compensation twice doesn't fail -- it just reports that there was nothing to undo. The integration test `handlesNonExistentBooking` in `CancelFlightWorkerTest` verifies this: cancelling a flight that was never booked completes successfully with `removedFromStore: false`.

## Workflow Configuration

The `task-defs.json` defines retry and timeout behavior for all six tasks:

| Setting | Value | Purpose |
|---|---|---|
| `retryCount` | 2 | Each task retries twice before failing |
| `timeoutSeconds` | 60 | Task times out after 60 seconds |
| `responseTimeoutSeconds` | 30 | Worker must respond within 30 seconds |

These settings apply equally to forward and compensation tasks. If `saga_cancel_flight` fails on the first attempt (e.g., a transient network error), Conductor retries it automatically. The overall workflow timeout is 60 seconds.

## Test Coverage

**Unit tests** (per worker):
- **BookFlightWorkerTest**: 5 tests -- booking creation, ID format, store entry verification, missing/blank tripId, action log
- **CancelFlightWorkerTest**: 4 tests -- cancel existing (removedFromStore=true), cancel non-existent (removedFromStore=false), missing tripId, action log
- **ChargePaymentWorkerTest**: 9 tests -- success creates transaction, failure returns terminal error without transaction, `shouldFail` as string, missing tripId, negative/zero/non-numeric amount, valid positive amount, action log
- **RefundPaymentWorkerTest**: 4 tests -- refund existing, refund non-existent, missing tripId, action log
- (ReserveHotelWorkerTest and CancelHotelWorkerTest follow the same pattern)

**SagaIntegrationTest** (5 tests):
- **Happy path**: all three bookings created, action log shows `BOOK_FLIGHT -> RESERVE_HOTEL -> CHARGE_PAYMENT`
- **Compensation in reverse order**: after payment failure, hotel is cancelled before flight; the test asserts `cancelHotelIdx < cancelFlightIdx` in the action log
- **Partial failure**: hotel fails after flight succeeds; flight remains in store until explicitly compensated
- **Every forward worker has a matching cancel**: books all three, cancels all three, asserts `BookingStore.isEmpty()`
- **Full compensation with refund**: all three succeed, then full reverse compensation (refund -> cancel hotel -> cancel flight) leaves empty stores and correct ordering in the action log

**27+ tests total** with explicit verification of compensation ordering, store cleanup, and idempotent cancellation.

---

> **How to run:** See [RUNNING.md](../../RUNNING.md) | **Production guidance:** See [PRODUCTION.md](PRODUCTION.md)
