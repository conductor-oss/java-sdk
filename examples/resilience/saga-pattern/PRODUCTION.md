# Saga Pattern -- Production Deployment Guide

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `CONDUCTOR_BASE_URL` | Yes | `http://localhost:8080/api` | Conductor server API endpoint |
| `CONDUCTOR_AUTH_KEY` | If auth enabled | none | API key for Conductor authentication |
| `CONDUCTOR_AUTH_SECRET` | If auth enabled | none | API secret for Conductor authentication |

## Security Considerations

- **Idempotent compensation**: Cancel workers are safe to retry. Cancelling a non-existent booking returns `removedFromStore=false` without error.
- **Booking store**: In production, replace the in-memory `BookingStore` with a persistent store (database, Redis). The current `ConcurrentHashMap` is for testing only and does not survive process restarts.
- **Payment amounts**: `ChargePaymentWorker` validates that `amount` is positive when provided. Zero or negative amounts are terminal errors.
- **Trip ID**: Always required. Blank or null trip IDs cause terminal errors.

## Deployment

1. Build:
   ```bash
   mvn clean package -DskipTests
   ```

2. Run workers:
   ```bash
   export CONDUCTOR_BASE_URL=https://your-conductor:8080/api
   java -jar target/saga-pattern-1.0.0.jar --workers
   ```

## Monitoring

- **Compensation tracking**: Every book/cancel action is logged to the BookingStore action log. Monitor for orphaned bookings (book without matching cancel).
- **Payment failure rate**: Track `FAILED_WITH_TERMINAL_ERROR` from `saga_charge_payment`. High rates may indicate payment provider issues.
- **Compensation completeness**: After a failed saga, verify that all compensation workers ran. Alert if bookings remain after workflow completion.
- **Worker health**: Monitor Conductor task queue depth for `saga_*` tasks.

## Saga Workers and Compensation Pairs

| Forward Worker | Task Name | Compensation Worker | Task Name |
|---|---|---|---|
| BookFlightWorker | `saga_book_flight` | CancelFlightWorker | `saga_cancel_flight` |
| ReserveHotelWorker | `saga_reserve_hotel` | CancelHotelWorker | `saga_cancel_hotel` |
| ChargePaymentWorker | `saga_charge_payment` | RefundPaymentWorker | `saga_refund_payment` |

Compensation must run in **reverse order** of the forward path.

## Error Classification

| Error | Type | Action |
|---|---|---|
| Missing `tripId` | Terminal | Fix caller input |
| Negative/zero `amount` | Terminal | Fix caller input |
| Non-numeric `amount` | Terminal | Fix caller input |
| Hotel reservation failure | Terminal | Triggers compensation for previously booked resources |
| Payment failure | Terminal | Triggers compensation for flight + hotel |
| Conductor connection error | Retryable | Check network/server health |
