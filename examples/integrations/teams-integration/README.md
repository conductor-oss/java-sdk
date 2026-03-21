# Teams Integration in Java Using Conductor

## Turning Webhook Events into Teams Adaptive Cards

When an external system fires a webhook (a monitoring alert, a CI/CD status change, a support ticket update), you typically need to receive the raw payload, transform it into a rich Adaptive Card with severity colors and action buttons, post the card to the right Teams channel, and send an acknowledgment back to the source. Each step depends on the previous one. you cannot format a card without the event data, and you cannot acknowledge delivery without a message ID from the post step.

Without orchestration, you would chain Microsoft Graph API calls manually, manage webhook payloads and message IDs between steps, and build custom acknowledgment logic. Conductor sequences the pipeline and routes event data, card payloads, and message IDs between workers automatically.

## The Solution

**You just write the Teams workers. Webhook reception, Adaptive Card formatting, channel posting, and delivery acknowledgment. Conductor handles webhook-to-acknowledgment sequencing, Graph API retries, and message ID routing between formatting, posting, and acknowledgment stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers handle Teams notifications: ReceiveWebhookWorker parses incoming events, FormatCardWorker builds Adaptive Cards, PostCardWorker sends to the channel, and AcknowledgeWorker confirms delivery to the source system.

| Worker | Task | What It Does |
|---|---|---|
| **AcknowledgeWorker** | `tms_acknowledge` | Acknowledges a posted Teams message. |
| **FormatCardWorker** | `tms_format_card` | Formats an adaptive card for Teams from event data. |
| **PostCardWorker** | `tms_post_card` | Posts an adaptive card to a Teams channel. |
| **ReceiveWebhookWorker** | `tms_receive_webhook` | Receives a Teams webhook and extracts event data. |

the workflow orchestration and error handling stay the same.

### The Workflow

```
Input -> AcknowledgeWorker -> FormatCardWorker -> PostCardWorker -> ReceiveWebhookWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
