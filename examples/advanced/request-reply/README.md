# Request Reply

A synchronous API needs to call an asynchronous workflow and wait for the result. The request-reply pattern sends the request, assigns a correlation ID, waits for the reply on a callback channel, and returns the result to the original caller -- with a timeout if the reply never arrives.

## Pipeline

```
[rqr_send_request]
     |
     v
[rqr_wait_response]
     |
     v
[rqr_correlate]
     |
     v
[rqr_deliver]
```

**Workflow inputs:** `requestPayload`, `targetService`, `timeoutMs`

## Workers

**RqrCorrelateWorker** (task: `rqr_correlate`)

- Reads `response`. Writes `matched`, `correlatedResponse`

**RqrDeliverWorker** (task: `rqr_deliver`)

- Reads `matched`. Writes `delivered`

**RqrSendRequestWorker** (task: `rqr_send_request`)

- Captures `instant.now()` timestamps, records wall-clock milliseconds
- Writes `correlationId`, `replyQueue`, `sentAt`

**RqrWaitResponseWorker** (task: `rqr_wait_response`)

- Writes `correlationId`, `response`, `latencyMs`

---

**16 tests** | Workflow: `rqr_request_reply` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
