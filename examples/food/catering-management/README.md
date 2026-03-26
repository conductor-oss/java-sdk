# Catering Management

Orchestrates catering management through a multi-stage Conductor workflow.

**Input:** `clientName`, `eventDate`, `guestCount` | **Timeout:** 60s

## Pipeline

```
cat_inquiry
    │
cat_quote
    │
cat_plan_menu
    │
cat_execute
    │
cat_invoice
```

## Workers

**ExecuteWorker** (`cat_execute`)

Reads `eventDate`. Outputs `executed`, `staff`, `satisfaction`.

**InquiryWorker** (`cat_inquiry`)

Reads `clientName`, `guestCount`. Outputs `requirements`.

**InvoiceWorker** (`cat_invoice`)

Reads `clientName`, `total`. Outputs `invoice`.

**PlanMenuWorker** (`cat_plan_menu`)

Reads `budget`, `guestCount`. Outputs `menu`.

**QuoteWorker** (`cat_quote`)

```java
int total = perGuest * guestCount;
```

Reads `guestCount`. Outputs `total`, `budget`, `perGuest`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
