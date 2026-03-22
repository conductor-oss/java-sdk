# Order Management -- Production Deployment Guide

## Required Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `CONDUCTOR_BASE_URL` | Yes | `http://localhost:8080/api` | Conductor server API endpoint |

## Security Considerations

- **In-memory order store**: The `OrderStore` is an in-memory ConcurrentHashMap. In production, replace with a persistent database (PostgreSQL, DynamoDB) to survive restarts and scale horizontally.
- **State machine enforcement**: All state transitions are validated via `OrderStore.VALID_TRANSITIONS`. Invalid transitions (e.g., shipping a cancelled order) are rejected with `FAILED` status. The valid transitions are: CREATED -> CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED, and any state -> CANCELLED.
- **Input validation**: All workers validate required inputs (orderId, items, warehouseId, etc.) and reject invalid data with `FAILED_WITH_TERMINAL_ERROR`. Items with zero/negative prices or quantities are rejected.
- **Order IDs**: Generated using timestamp + counter. In production, consider UUIDs or database sequences for guaranteed uniqueness across instances.
- **PII handling**: Customer IDs and shipping addresses flow through Conductor. Ensure encryption at rest and in transit.

## Deployment Notes

- **Worker ordering**: Workers are independent and stateless. They can be deployed on separate instances. The `OrderStore` must be shared (externalized to a database) in multi-instance deployments.
- **Idempotency**: Conductor handles task retries. Workers should be idempotent -- creating the same order twice should not cause issues. The current implementation uses atomic counters for uniqueness.
- **Workflow definition**: Register `workflow.json` before starting workflows. The workflow defines the sequence: create -> validate -> fulfill -> ship -> deliver.

## Monitoring Expectations

- **State transition failures**: Monitor `FAILED` status on ValidateOrderWorker, FulfillOrderWorker, ShipOrderWorker, and DeliverOrderWorker. These indicate out-of-order processing or concurrent modifications.
- **Cancellation rate**: Track how many orders reach CANCELLED state vs DELIVERED. High cancellation rates may indicate UX or inventory issues.
- **Fulfillment latency**: Time from CONFIRMED to PROCESSING should be minutes, not hours. Delays indicate warehouse bottlenecks.
- **Delivery success rate**: Track the ratio of SHIPPED orders that reach DELIVERED. Orders stuck in SHIPPED state indicate delivery failures.
- **Terminal errors**: `FAILED_WITH_TERMINAL_ERROR` indicates bad input data (missing customer ID, empty items). These should be near zero in production -- they indicate upstream integration bugs.
