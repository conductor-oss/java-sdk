# Publish Subscribe

A domain event bus needs to deliver each event to all interested subscribers. When a "user.created" event fires, the welcome-email service, the CRM sync, and the analytics tracker all need a copy. Subscribers join and leave dynamically, and a slow subscriber should not block delivery to fast ones.

## Pipeline

```
[pbs_publish]
     |
     v
     +──────────────────────────────────────────────────────────────+
     | [pbs_subscriber_1] | [pbs_subscriber_2] | [pbs_subscriber_3] |
     +──────────────────────────────────────────────────────────────+
     [join]
     |
     v
[pbs_confirm]
```

**Workflow inputs:** `event`, `topic`

## Workers

**PbsConfirmWorker** (task: `pbs_confirm`)

- Writes `subscribersNotified`, `allDelivered`

**PbsPublishWorker** (task: `pbs_publish`)

- Records wall-clock milliseconds
- Writes `eventId`, `published`

**PbsSubscriber1Worker** (task: `pbs_subscriber_1`)

- Sets `action` = `"logged_analytics"`
- Writes `received`, `subscriber`, `action`

**PbsSubscriber2Worker** (task: `pbs_subscriber_2`)

- Sets `action` = `"sent_notification"`
- Writes `received`, `subscriber`, `action`

**PbsSubscriber3Worker** (task: `pbs_subscriber_3`)

- Sets `action` = `"wrote_audit_log"`
- Writes `received`, `subscriber`, `action`

---

**20 tests** | Workflow: `pbs_publish_subscribe` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
