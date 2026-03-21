# Twilio Integration in Java Using Conductor

## Running Two-Way SMS Conversations Through Twilio

Two-way SMS involves more than sending a single message. You send an outbound SMS, wait for the recipient to reply (polling or webhook), process the reply to determine the appropriate response (parsing keywords, looking up context, generating a follow-up), and send the reply back. Each step depends on the previous one. you cannot wait for a reply without a message SID from the send step, and you cannot generate a reply without the response body.

Without orchestration, you would chain Twilio REST API calls manually, manage message SIDs and response bodies between steps, and build custom polling or webhook handling for inbound messages. Conductor sequences the pipeline and routes message SIDs, response content, and reply text between workers automatically.

## The Solution

**You just write the SMS workers. Outbound sending, reply waiting, response processing, and follow-up replies. Conductor handles send-wait-reply sequencing, Twilio API retries, and message SID routing between outbound and inbound stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers run two-way SMS conversations: SendSmsWorker delivers the outbound message, WaitResponseWorker captures the reply, ProcessResponseWorker generates contextual follow-up content, and SendReplyWorker transmits the response.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessResponseWorker** | `twl_process_response` | Processes an SMS response. |
| **SendReplyWorker** | `twl_send_reply` | Sends a reply SMS. |
| **SendSmsWorker** | `twl_send_sms` | Sends an SMS via Twilio. |
| **WaitResponseWorker** | `twl_wait_response` | Waits for an SMS response. |

The workers auto-detect Twilio credentials at startup. When `TWILIO_ACCOUNT_SID` and `TWILIO_AUTH_TOKEN` are set, SendSmsWorker and SendReplyWorker use the real Twilio API to deliver messages. Without credentials, they fall back to demo mode with realistic output shapes so the workflow runs end-to-end without a Twilio account.

### The Workflow

```
twl_send_sms
 │
 ▼
twl_wait_response
 │
 ▼
twl_process_response
 │
 ▼
twl_send_reply

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
