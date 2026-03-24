# Ticket Management

Orchestrates ticket management through a multi-stage Conductor workflow.

**Input:** `subject`, `description`, `reportedBy` | **Timeout:** 60s

## Pipeline

```
tkt_create
    │
tkt_classify
    │
tkt_assign
    │
tkt_resolve
    │
tkt_close
```

## Workers

**AssignTicketWorker** (`tkt_assign`)

```java
Map<String, String> teams = Map.of("authentication", "Auth Team - Kim", "performance", "Infra Team - Leo", "general", "Support - Maria");
```

Reads `category`, `priority`, `ticketId`. Outputs `assignee`, `slaHours`.

**ClassifyTicketWorker** (`tkt_classify`)

```java
String category = desc.contains("login") ? "authentication" : desc.contains("slow") ? "performance" : "general";
String priority = (desc.contains("cannot") || desc.contains("urgent")) ? "P1" : "P2";
```

Reads `description`, `ticketId`. Outputs `category`, `priority`.

**CloseTicketWorker** (`tkt_close`)

Reads `ticketId`. Outputs `closed`, `closedAt`.

**CreateTicketWorker** (`tkt_create`)

```java
String ticketId = "TKT-" + (new Random().nextInt(90000) + 10000);
```

Reads `subject`. Outputs `ticketId`, `createdAt`.

**ResolveTicketWorker** (`tkt_resolve`)

Reads `assignee`, `ticketId`. Outputs `resolution`, `resolvedAt`.

## Tests

**13 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
