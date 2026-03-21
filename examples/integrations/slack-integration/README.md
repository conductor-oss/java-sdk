# Slack Integration in Java Using Conductor

## Processing Slack Events into Channel Messages

When a Slack event arrives (a new message, a reaction, a channel join), you typically need to receive and parse the event, process it into a formatted response, post the response to the appropriate channel, and track whether the message was delivered. Each step depends on the previous one. you cannot format a message without the event data, and you cannot track delivery without a message ID from the post step.

Without orchestration, you would chain Slack API calls manually, manage event payloads and message IDs between steps, and build custom delivery tracking. Conductor sequences the pipeline and routes event data, formatted messages, and message IDs between workers automatically.

## The Solution

**You just write the Slack workers. Event reception, message formatting, channel posting, and delivery tracking. Conductor handles event-to-delivery sequencing, Slack API rate-limit retries, and message ID routing between posting and tracking stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers process Slack events: ReceiveEventWorker parses incoming payloads, ProcessEventWorker formats the response message, PostMessageWorker sends to the channel, and TrackDeliveryWorker confirms message delivery.

| Worker | Task | What It Does |
|---|---|---|
| **PostMessageWorker** | `slk_post_message` | Posts a message to a Slack channel. |
| **ProcessEventWorker** | `slk_process_event` | Processes a Slack event and produces a formatted message. |
| **ReceiveEventWorker** | `slk_receive_event` | Receives an incoming Slack event and extracts the event type and data. |
| **TrackDeliveryWorker** | `slk_track_delivery` | Tracks delivery status of a posted Slack message. |

The workers auto-detect Slack credentials at startup. When `SLACK_BOT_TOKEN` is set, PostMessageWorker uses the real Slack Web API (chat.postMessage, via `java.net.http`) to post messages to channels. Without the token, it falls back to demo mode with realistic output shapes so the workflow runs end-to-end without a Slack bot token.

### The Workflow

```
Input -> PostMessageWorker -> ProcessEventWorker -> ReceiveEventWorker -> TrackDeliveryWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
