# Publish-Subscribe in Java Using Conductor : Publish Event, Fan Out to Subscribers in Parallel, Confirm

## One Event, Multiple Subscribers, All Must Succeed

A user signs up and three things need to happen: the welcome email service sends an onboarding email, the analytics service records the signup event, and the provisioning service creates the user's workspace. These are independent subscribers. none depends on the others; but all three must eventually succeed. If the email service is down, the signup shouldn't block provisioning, and you need to know which subscribers processed the event and which didn't.

Building pub-sub fan-out manually means spawning threads for each subscriber, implementing independent retry logic for each, tracking which subscribers acknowledged, and deciding what to do when one fails permanently while the others succeeded.

## The Solution

**You write each subscriber's handler. Conductor handles parallel fan-out, per-subscriber retries, and delivery confirmation.**

`PbsPublishWorker` accepts the event and topic, preparing it for distribution. A `FORK_JOIN` fans the event out to all three subscribers in parallel. `PbsSubscriber1Worker`, `PbsSubscriber2Worker`, and `PbsSubscriber3Worker` each process the event independently. The `JOIN` waits for all subscribers to complete. `PbsConfirmWorker` verifies that every subscriber processed the event and produces a confirmation summary. Conductor retries any subscriber that fails without affecting the others, and records which subscribers succeeded and which needed retries.

### What You Write: Workers

Five workers implement the pub-sub fan-out: event publishing, three parallel subscriber handlers (analytics, notification, audit), and delivery confirmation, each subscriber processing the event independently.

### The Workflow

```
pbs_publish
 │
 ▼
FORK_JOIN
 ├── pbs_subscriber_1
 ├── pbs_subscriber_2
 └── pbs_subscriber_3
 │
 ▼
JOIN (wait for all branches)
pbs_confirm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
