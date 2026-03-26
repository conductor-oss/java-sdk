# Event Ttl

A caching system receives events that have a limited useful lifetime. A price quote is only valid for 5 minutes; a session token expires after 30 minutes. The TTL pipeline needs to assign an expiration timestamp to each event, detect expired events before processing, and route expired events to a cleanup handler.

## Pipeline

```
[xl_check_expiry]
     |
     v
     <SWITCH>
       |-- valid -> [xl_process_event]
       |-- valid -> [xl_acknowledge]
       |-- expired -> [xl_log_expired]
```

**Workflow inputs:** `eventId`, `payload`, `ttlSeconds`, `createdAt`

## Workers

**AcknowledgeWorker** (task: `xl_acknowledge`)

Acknowledges a successfully processed event.

- Reads `eventId`. Writes `acknowledged`

**CheckExpiryWorker** (task: `xl_check_expiry`)

Checks whether an event has exceeded its TTL.

- Parses strings to `int`
- Reads `eventId`, `ttlSeconds`. Writes `status`, `ageSeconds`, `ttlSeconds`

**LogExpiredWorker** (task: `xl_log_expired`)

Logs an expired event that exceeded its TTL.

- Reads `eventId`, `age`, `ttl`. Writes `logged`, `reason`

**ProcessEventWorker** (task: `xl_process_event`)

Processes a valid (non-expired) event.

- Reads `eventId`. Writes `processed`

---

**34 tests** | Workflow: `event_ttl` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
