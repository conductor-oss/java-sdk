# Mobile Approval Flutter

Mobile Approval with Push Notifications -- submit, send push, WAIT for mobile response, finalize.

**Input:** `requestId`, `userId` | **Timeout:** 300s

## Pipeline

```
mob_submit
    │
mob_send_push
    │
mobile_response [WAIT]
    │
mob_finalize
```

## Workers

**MobFinalizeWorker** (`mob_finalize`): Worker for mob_finalize -- finalizes the mobile approval after.

Outputs `done`.

**MobSendPushWorker** (`mob_send_push`): Worker for mob_send_push -- performs sending an FCM push notification.

Outputs `pushSent`.

**MobSubmitWorker** (`mob_submit`): Worker for mob_submit -- submits a mobile approval request.

Outputs `submitted`.

## Tests

**16 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
