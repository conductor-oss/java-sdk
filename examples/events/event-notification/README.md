# Event Notification in Java Using Conductor

A customer's payment fails. They need to know immediately: via email, SMS, and push. But your notification code sends them sequentially: email first, then SMS, then push. The SMS provider is having a bad day and hangs for 30 seconds before timing out. Now your push notification arrives a minute late, and the email, which actually succeeded, is sitting in a retry loop because the whole pipeline is blocked. The customer sees nothing, retries the payment, gets double-charged, and opens a support ticket. This workflow sends notifications across all channels in parallel, so a slow SMS never blocks the email, and every delivery attempt is tracked. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to send notifications about events across multiple channels simultaneously. When an event occurs, the recipient must be notified via email, SMS, and push notification, all in parallel to minimize delivery latency. After all channels complete, the delivery status for each channel must be recorded for tracking and compliance. A single slow channel (e.g., email SMTP delays) should not block the other channels.

Without orchestration, you'd spawn threads for each notification channel, manage per-channel retry logic with different backoff strategies, aggregate delivery receipts from multiple providers, and handle partial failures where email succeeds but SMS fails.

## The Solution

**You just write the event-parse, email, SMS, push, and delivery-recording workers. Conductor handles parallel multi-channel delivery, per-channel retry isolation, and unified delivery status tracking.**

Each notification channel is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of parsing the event, sending across all three channels in parallel via FORK_JOIN, recording delivery status for each, retrying any failed channel independently, and tracking the entire notification lifecycle.

### What You Write: Workers

Five workers deliver multi-channel notifications: ParseEventWorker extracts recipient and content, then SendEmailWorker, SendSmsWorker, and SendPushWorker deliver in parallel via FORK_JOIN, and RecordDeliveryWorker logs the outcome per channel.

| Worker | Task | What It Does |
|---|---|---|
| **ParseEventWorker** | `en_parse_event` | Parses an incoming event and prepares notification content for all channels. |
| **RecordDeliveryWorker** | `en_record_delivery` | Records the delivery status across all notification channels. |
| **SendEmailWorker** | `en_send_email` | Sends an email notification to a recipient. |
| **SendPushWorker** | `en_send_push` | Sends a push notification to a recipient. |
| **SendSmsWorker** | `en_send_sms` | Sends an SMS notification to a recipient. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources, the workflow and routing logic stay the same.

### The Workflow

```
en_parse_event
 │
 ▼
FORK_JOIN
 ├── en_send_email
 ├── en_send_sms
 └── en_send_push
 │
 ▼
JOIN (wait for all branches)
en_record_delivery

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
