# Slack Integration

Slack integration: receive event, process event, post message, track delivery

**Input:** `channel`, `eventType`, `payload` | **Timeout:** 1800s

## Pipeline

```
slk_receive_event
    │
slk_process_event
    │
slk_post_message
    │
slk_track_delivery
```

## Workers

**PostMessageWorker** (`slk_post_message`): Posts a message to a Slack channel.

```java
this.liveMode = botToken != null && !botToken.isBlank();
```

Reads `channel`, `message`. Outputs `messageId`, `postedAt`.
Returns `FAILED` on validation errors.

**ProcessEventWorker** (`slk_process_event`): Processes a Slack event and produces a formatted message.

Reads `data`, `eventType`. Outputs `message`, `processed`.

**ReceiveEventWorker** (`slk_receive_event`): Receives an incoming Slack event and extracts the event type and data.

Reads `channel`, `eventType`, `payload`. Outputs `eventType`, `data`, `receivedAt`.

**TrackDeliveryWorker** (`slk_track_delivery`): Tracks delivery status of a posted Slack message.

Reads `channel`, `messageId`. Outputs `delivered`, `trackedAt`.

## Tests

**32 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
