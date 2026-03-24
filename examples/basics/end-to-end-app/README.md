# End To End App

Complete support ticket pipeline: classify, assign, and notify

**Input:** `ticketId`, `subject`, `description`, `category`, `customerEmail` | **Timeout:** 120s

## Pipeline

```
classify_ticket
    │
assign_ticket
    │
notify_customer
```

## Workers

**AssignTicketWorker** (`assign_ticket`): Assigns a support ticket to the appropriate team based on category and priority.

```java
String normalizedCategory = (category != null) ? category.toLowerCase() : "general";
```

Reads `category`, `priority`, `ticketId`. Outputs `team`, `estimatedResponse`.

**ClassifyTicketWorker** (`classify_ticket`): Classifies a support ticket's priority based on keyword matching.

Reads `description`, `subject`, `ticketId`. Outputs `priority`, `keywordsMatched`.

**NotifyCustomerWorker** (`notify_customer`): Sends a notification to the customer about their ticket status.

Reads `assignedTeam`, `customerEmail`, `estimatedResponse`, `priority`, `ticketId`. Outputs `sent`, `channel`.

## Tests

**11 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
