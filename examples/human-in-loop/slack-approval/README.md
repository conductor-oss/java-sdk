# Slack Interactive Approval in Java Using Conductor : Request Submission, Block Kit Message with Approve/Reject Buttons, WAIT for Slack Interaction Webhook, and Decision Finalization

## Approvals Can Be Triggered via Slack Interactive Buttons

Many teams live in Slack, so approval requests should show up there as interactive messages with Approve/Reject buttons. The workflow submits the request, posts a Slack message with Block Kit interactive buttons, and pauses at a WAIT task until someone clicks a button. The button click (via Slack's interaction webhook) completes the WAIT task with the decision.

## The Solution

**You just write the request-submission, Slack-posting, and decision-finalization workers. Conductor handles the durable wait for the Slack button click.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

SubmitWorker captures the request, PostSlackWorker builds the Block Kit message with interactive buttons, and FinalizeWorker processes the decision, the durable wait for the Slack webhook callback is managed by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `sa_submit` | Receives the approval request with requestor and reason, validates and marks the submission as received |
| **PostSlackWorker** | `sa_post_slack` | Builds a Slack Block Kit payload with a header block ("Approval Request"), a mrkdwn section showing the requestor and reason, and an actions block with styled Approve (primary) and Reject (danger) buttons. in production, POSTs this to Slack's chat.postMessage API |
| *WAIT task* | `slack_response` | Pauses until the Slack interaction webhook fires. when an approver clicks Approve or Reject in the Slack message, your webhook handler completes this WAIT task via `POST /tasks/{taskId}` with the decision | Built-in Conductor WAIT, no worker needed |
| **FinalizeWorker** | `sa_finalize` | Reads the decision from the completed WAIT task output and finalizes the approval. records the outcome and triggers downstream actions based on approved or rejected |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
sa_submit
 │
 ▼
sa_post_slack
 │
 ▼
slack_response [WAIT]
 │
 ▼
sa_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
