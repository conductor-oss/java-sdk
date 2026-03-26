# Twilio Integration

Orchestrates twilio integration through a multi-stage Conductor workflow.

**Input:** `toNumber`, `fromNumber`, `messageBody` | **Timeout:** 60s

## Pipeline

```
twl_send_sms
    │
twl_wait_response
    │
twl_process_response
    │
twl_send_reply
```

## Workers

**ProcessResponseWorker** (`twl_process_response`): Processes an SMS response.

```java
? "Thank you for confirming! Your appointment is set."
String intent = "YES".equals(responseBody) ? "confirm" : "decline";
```

Reads `responseBody`. Outputs `replyMessage`, `intent`.

**SendReplyWorker** (`twl_send_reply`): Sends a reply SMS.

```java
this.liveMode = sid != null && !sid.isBlank() && token != null && !token.isBlank();
```

Reads `body`, `from`, `to`. Outputs `messageSid`, `status`.

**SendSmsWorker** (`twl_send_sms`): Sends an SMS via Twilio.

```java
this.liveMode = sid != null && !sid.isBlank() && token != null && !token.isBlank();
```

Reads `body`, `from`, `to`. Outputs `messageSid`, `status`.

**WaitResponseWorker** (`twl_wait_response`): Waits for an SMS response.

Reads `toNumber`. Outputs `responseBody`, `receivedAt`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
