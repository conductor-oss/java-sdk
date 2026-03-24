# Request-Reply in Java Using Conductor : Send Request, Wait for Response, Correlate, Deliver

## Asynchronous Request-Reply Needs Correlation and Timeout Handling

You send a credit check request to an external bureau, but the response comes back asynchronously. minutes later, on a different channel (a callback webhook or a reply queue). Your system needs to hold the request context, wait for the response within a timeout window, match the response to the original request using a correlation ID, and deliver the result. If the response never arrives, you need to handle the timeout gracefully rather than waiting forever.

Building request-reply manually means storing pending requests in a database, implementing a timeout mechanism (scheduled tasks, TTLs), matching responses to requests by correlation ID, and handling edge cases like duplicate responses or responses that arrive after the timeout.

## The Solution

**You write the send and correlate logic. Conductor handles timeout management, retries, and response matching.**

`RqrSendRequestWorker` dispatches the request to the target service with a correlation ID and the configured timeout. `RqrWaitResponseWorker` waits for the response, respecting the timeout window. `RqrCorrelateWorker` matches the incoming response to the original request using the correlation ID, verifying they belong together. `RqrDeliverWorker` returns the correlated result to the caller. Conductor manages the timeout, retries the send if it fails, and records the full request-response lifecycle. send time, wait duration, correlation match, and delivery status.

### What You Write: Workers

Four workers manage the request-response lifecycle: sending with a correlation ID, waiting with a timeout, response matching, and result delivery, each handling one phase of asynchronous communication.

### The Workflow

```
rqr_send_request
 │
 ▼
rqr_wait_response
 │
 ▼
rqr_correlate
 │
 ▼
rqr_deliver

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
