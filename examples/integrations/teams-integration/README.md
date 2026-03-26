# Teams Integration

Teams integration: receive webhook, format card, post card, acknowledge

**Input:** `teamId`, `channelId`, `webhookPayload` | **Timeout:** 1800s

## Pipeline

```
tms_receive_webhook
    │
tms_format_card
    │
tms_post_card
    │
tms_acknowledge
```

## Workers

**AcknowledgeWorker** (`tms_acknowledge`): Acknowledges a posted Teams message.

Reads `messageId`. Outputs `acknowledged`, `status`.

**FormatCardWorker** (`tms_format_card`): Formats an adaptive card for Teams from event data.

Reads `data`, `eventType`. Outputs `card`.

**PostCardWorker** (`tms_post_card`): Posts an adaptive card to a Teams channel.

```java
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
```

Reads `card`, `channelId`. Outputs `messageId`, `postedAt`.

**ReceiveWebhookWorker** (`tms_receive_webhook`): Receives a Teams webhook and extracts event data.

Reads `payload`, `teamId`. Outputs `eventType`, `data`, `receivedAt`.

## Tests

**31 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
