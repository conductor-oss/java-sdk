# Implementing Error Notification in Java with Conductor : Order Processing with Automated Failure Alerts

## The Problem

You have an order processing pipeline, and when it fails, the right people need to know immediately. the on-call engineer via PagerDuty, the ops team via Slack, and the account manager via email. The notification must happen automatically and in parallel, because a slow email send shouldn't delay the PagerDuty page.

Without orchestration, failure notifications are afterthoughts. a catch block that sends one email and hopes it works. Adding Slack means another catch block. Adding PagerDuty means another. If the Slack webhook times out, the PagerDuty page never fires. Nobody knows which notifications actually sent for a given failure.

## The Solution

**You just write the order processor and notification channel integrations. Conductor handles failure detection, parallel notification dispatch across all channels, retries on delivery failures, and a record of which alerts were sent for every failed order.**

The order processing worker handles business logic. When it fails, Conductor's failure workflow automatically triggers, running Slack, email, and PagerDuty notifications in parallel. Even if one notification channel is down, the others still fire. Every notification attempt is tracked. you can see which alerts were sent, which failed, and how long each took. ### What You Write: Workers

ProcessOrderWorker handles the business logic, and when it fails, Conductor's failure workflow automatically fires SendSlackWorker, SendEmailWorker, and SendPagerDutyWorker in parallel so all channels are notified simultaneously.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessOrderWorker** | `en_process_order` | Worker for en_process_order. Processes an order. If the input contains shouldFail=true, the task returns FAILED sta.. |
| **SendEmailWorker** | `en_send_email` | Worker for en_send_email. Sends an email notification. Returns sent=true and the recipient address from input (defa.. |
| **SendPagerDutyWorker** | `en_send_pagerduty` | Worker for en_send_pagerduty. Sends a PagerDuty alert. Returns sent=true. |
| **SendSlackWorker** | `en_send_slack` | Worker for en_send_slack. Sends a Slack notification. Returns sent=true and the channel from input (defaults to "#a.. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
en_process_order

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
