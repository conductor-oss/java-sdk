# Sendgrid Integration

Orchestrates sendgrid integration through a multi-stage Conductor workflow.

**Input:** `recipientEmail`, `recipientName`, `templateId`, `campaignId` | **Timeout:** 60s

## Pipeline

```
sgd_compose_email
    │
sgd_personalize
    │
sgd_send_email
    │
sgd_track_opens
```

## Workers

**ComposeEmailWorker** (`sgd_compose_email`): Composes an email from a template.

Reads `campaignId`, `templateId`. Outputs `template`.

**PersonalizeWorker** (`sgd_personalize`): Personalizes an email for a recipient.

Reads `recipientName`. Outputs `subject`, `htmlBody`.

**SendEmailWorker** (`sgd_send_email`): Sends an email via SendGrid.

```java
this.liveMode = apiKey != null && !apiKey.isBlank();
```

Reads `from`, `htmlBody`, `subject`, `to`. Outputs `messageId`, `delivered`, `sentAt`.
Returns `FAILED` on validation errors.

**TrackOpensWorker** (`sgd_track_opens`): Tracks email opens.

Reads `campaignId`, `messageId`. Outputs `trackingEnabled`, `pixelInserted`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
