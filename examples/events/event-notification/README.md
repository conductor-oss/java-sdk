# Event Notification

A platform needs to notify users when specific events occur: order shipped, payment failed, account locked. Each notification needs template rendering with event-specific variables, channel selection (email, SMS, push), delivery, and confirmation tracking.

## Pipeline

```
[en_parse_event]
     |
     v
     +──────────────────────────────────────────────────+
     | [en_send_email] | [en_send_sms] | [en_send_push] |
     +──────────────────────────────────────────────────+
     [join]
     |
     v
[en_record_delivery]
```

**Workflow inputs:** `event`, `recipientId`

## Workers

**ParseEventWorker** (task: `en_parse_event`)

Parses an incoming event and prepares notification content for all channels.

- Reads `event`, `recipientId`. Writes `recipientId`, `subject`, `body`, `smsMessage`

**RecordDeliveryWorker** (task: `en_record_delivery`)

Records the delivery status across all notification channels.

- Reads `emailStatus`, `smsStatus`, `pushStatus`. Writes `overallStatus`

**SendEmailWorker** (task: `en_send_email`)

Sends an email notification to a recipient.

- Reads `recipientId`, `subject`. Writes `deliveryStatus`, `channel`

**SendPushWorker** (task: `en_send_push`)

Sends a push notification to a recipient.

- Reads `recipientId`, `title`. Writes `deliveryStatus`, `channel`

**SendSmsWorker** (task: `en_send_sms`)

Sends an SMS notification to a recipient.

- Reads `recipientId`, `message`. Writes `deliveryStatus`, `channel`

---

**5 tests** | Workflow: `event_notification` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
