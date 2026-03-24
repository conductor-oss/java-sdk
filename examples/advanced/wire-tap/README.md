# Wire Tap

A production pipeline processes events normally, but the debugging team needs to inspect the traffic without affecting throughput. The wire-tap pattern copies each event to a side channel for inspection, logging, or analysis -- without modifying the main pipeline's behavior or adding latency.

## Pipeline

```
[wtp_receive]
     |
     v
     +───────────────────────────────────+
     | [wtp_main_flow] | [wtp_tap_audit] |
     +───────────────────────────────────+
     [join]
```

**Workflow inputs:** `message`, `auditLevel`

The FORK_JOIN runs the main processing path and the audit tap in parallel. The main flow completes independently of the tap -- a slow or failing audit logger does not block the primary message path.

## Workers

**WtpReceiveWorker** (task: `wtp_receive`)

Receives the incoming message and passes it through unchanged. Reads `message` from workflow input and outputs it as-is for both the main flow and the audit tap to consume.

- Writes `message`

**WtpMainFlowWorker** (task: `wtp_main_flow`)

Processes the message through the primary business logic path. Outputs a `result` map containing `processed` = true, `action` = `"message_handled"`, and a `timestamp` from `Instant.now()`.

- Writes `result`

**WtpTapAuditWorker** (task: `wtp_tap_audit`)

Copies the message to a side-channel audit log without affecting the main flow. Generates a unique `auditLogId` prefixed with `"AUD-"` using `System.currentTimeMillis()` encoded in base-36. Builds an `auditEntry` map with the current `Instant.now()` timestamp, the `auditLevel` from workflow input (defaults to `"info"`), and `source` = `"wire_tap"`.

- Writes `tapped`, `auditLogId`, `auditEntry`

---

**12 tests** | Workflow: `wtp_wire_tap` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
