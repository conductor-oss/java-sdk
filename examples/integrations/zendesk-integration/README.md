# Zendesk Integration

Orchestrates zendesk integration through a multi-stage Conductor workflow.

**Input:** `requesterEmail`, `subject`, `description`, `category` | **Timeout:** 60s

## Pipeline

```
zd_create_ticket
    │
zd_classify
    │
zd_route
    │
zd_resolve
```

## Workers

**ClassifyTicketWorker** (`zd_classify`): Classifies ticket priority.

```java
String priority = subject != null && subject.toLowerCase().contains("urgent") ? "high" : "normal";
```

Reads `subject`, `ticketId`. Outputs `priority`, `sentiment`, `language`.

**CreateTicketWorker** (`zd_create_ticket`): Creates a Zendesk support ticket.

```java
.header("Authorization", "Basic " + auth)
```

Reads `description`, `requesterEmail`, `subject`. Outputs `ticketId`, `createdAt`.

**ResolveTicketWorker** (`zd_resolve`): Resolves a ticket.

Reads `agentId`, `ticketId`. Outputs `resolved`, `resolvedAt`, `satisfaction`.

**RouteTicketWorker** (`zd_route`): Routes ticket to an agent.

Reads `priority`, `ticketId`. Outputs `agentId`, `agentName`, `group`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
