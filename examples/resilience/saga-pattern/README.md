# Implementing Saga Pattern in Java with Conductor: Orchestrated Compensation for Distributed Trip Booking

## The Problem

You need to book a trip as a distributed transaction across three independent services. Flight booking, hotel reservation, and payment. If the payment charge fails after the flight and hotel are booked, both must be cancelled. If the hotel reservation fails after the flight is booked, the flight must be cancelled. Each service has its own compensating action that must run in reverse order.

Without orchestration, saga compensation is implemented as deeply nested try/catch blocks. Each forward step must know about every previous step's undo operation. Adding a new step (e.g., travel insurance) means updating the compensation logic for every existing step. Testing all compensation paths requires simulating failures at each step.

### What Goes Wrong Without a Saga

Consider what happens when the payment step fails midway through a trip booking:

1. Hotel is reserved (HTL-TRIP-001 confirmed)
2. Flight is booked (FLT-TRIP-001 confirmed)
3. Payment is charged. **DECLINED**

Without compensation, the hotel and flight remain booked. The customer sees a "payment failed" error but the hotel holds a room and the airline holds a seat. The hotel charges a no-show fee, the flight seat is wasted, and the customer gets billed for a trip they never took.

The saga pattern solves this by defining a compensating action for every forward step. When payment fails, Conductor runs `cancel_flight` then `cancel_hotel` in reverse order. Undoing exactly the steps that completed.

## The Solution

**You just write the booking and compensation logic for each service. Conductor handles forward sequencing, SWITCH-based failure detection, reverse-order compensation execution, retries on each booking and cancellation step, and a full audit trail of every saga with its forward and rollback paths.**

Each forward step (book flight, reserve hotel, charge payment) and its compensation (cancel flight, cancel hotel, refund payment) are independent workers. Conductor runs the forward steps in sequence and, on failure, triggers the compensation workflow that runs undo steps in reverse order. Every step in both directions is tracked with full context.

### What You Write: Workers

Six workers form the saga: ReserveHotelWorker, BookFlightWorker, and ChargePaymentWorker handle forward booking, while CancelHotelWorker, CancelFlightWorker, and RefundPaymentWorker execute compensating rollbacks in reverse order when any step fails.

| Worker | Task | What It Does |
|---|---|---|
| **BookFlightWorker** | `saga_book_flight` | Books a flight for the given tripId, returns a booking ID like `FLT-TRIP-001`. |
| **CancelFlightWorker** | `saga_cancel_flight` | Compensation: cancels a previously booked flight using the tripId. Returns `{cancelled: true}`. |
| **CancelHotelWorker** | `saga_cancel_hotel` | Compensation: cancels a previously reserved hotel using the tripId. Returns `{cancelled: true}`. |
| **ChargePaymentWorker** | `saga_charge_payment` | Charges payment for the trip. When `shouldFail=true`, returns `{status: "failed"}` to trigger saga rollback. Otherwise returns `{status: "success", transactionId: "TXN-TRIP-001"}`. |
| **RefundPaymentWorker** | `saga_refund_payment` | Compensation: refunds a previously charged payment. Returns `{refunded: true}`. Registered but not used in the current workflow (payment failure prevents a charge from existing). |
| **ReserveHotelWorker** | `saga_reserve_hotel` | Reserves a hotel room for the given tripId, returns a reservation ID like `HTL-TRIP-001`. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
saga_reserve_hotel
 |
 v
saga_book_flight
 |
 v
saga_charge_payment
 |
 v
SWITCH (check_payment_ref)
 |-- "failed": saga_cancel_flight -> saga_cancel_hotel -> TERMINATE(ROLLED_BACK)
 |-- default: workflow completes with booking details

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
