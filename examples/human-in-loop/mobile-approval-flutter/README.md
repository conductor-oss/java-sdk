# Mobile Approval with Flutter in Java Using Conductor : Request Submission, FCM Push Notification, WAIT for Mobile Response, and Finalization

## Mobile Users Need Push Notifications for Approvals

When an approval is needed, the approver might not be at their desk. The workflow submits the request, sends a push notification to the approver's mobile device, and pauses at a WAIT task until the mobile app sends back the decision. If the push notification fails to send, you need to retry it without re-submitting the request. And you need to track whether the approval came via mobile or another channel.

## The Solution

**You just write the request-submission, push-notification, and finalization workers. Conductor handles the durable wait for the mobile response.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

MobSubmitWorker validates the request, MobSendPushWorker delivers the FCM notification, and MobFinalizeWorker records the mobile decision. None of them poll for the Flutter app's response.

| Worker | Task | What It Does |
|---|---|---|
| **MobSubmitWorker** | `mob_submit` | Submits the approval request. validates the request and identifies the approver's device token for push delivery |
| **MobSendPushWorker** | `mob_send_push` | Sends an FCM push notification to the approver's mobile device with the request details and approve/reject action buttons |
| *WAIT task* | `mob_approval` | Pauses until the Flutter app sends the approver's decision back via `POST /tasks/{taskId}` when they tap approve or reject in the push notification | Built-in Conductor WAIT. no worker needed |
| **MobFinalizeWorker** | `mob_finalize` | Finalizes the approval. records the decision, the channel (mobile), and triggers downstream actions |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
mob_submit
 │
 ▼
mob_send_push
 │
 ▼
mobile_response [WAIT]
 │
 ▼
mob_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
