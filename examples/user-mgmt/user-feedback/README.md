# User Feedback

Orchestrates user feedback through a multi-stage Conductor workflow.

**Input:** `userId`, `feedbackText`, `source` | **Timeout:** 60s

## Pipeline

```
ufb_collect
    │
ufb_classify
    │
ufb_route
    │
ufb_respond
```

## Workers

**ClassifyFeedbackWorker** (`ufb_classify`)

```java
String category = text.contains("bug") ? "bug" : text.contains("feature") ? "feature_request" : "general";
String priority = text.contains("urgent") || text.contains("crash") ? "high" : "medium";
```

Reads `feedbackText`. Outputs `category`, `priority`, `sentiment`.

**CollectFeedbackWorker** (`ufb_collect`)

Reads `source`, `userId`. Outputs `feedbackId`, `receivedAt`.

**RespondFeedbackWorker** (`ufb_respond`)

Reads `category`, `team`, `userId`. Outputs `responseSent`, `message`.

**RouteFeedbackWorker** (`ufb_route`)

```java
Map<String, String> teams = Map.of("bug", "engineering", "feature_request", "product", "general", "support");
```

Reads `category`, `priority`. Outputs `assignedTeam`, `ticketCreated`.

## Tests

**14 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
