# Queue Backpressure in Java Using Conductor : Monitor Depth, Throttle Producers, Shed Load

## When Queues Overflow

A downstream service slows down, consumers fall behind, and your queue depth starts climbing. At 10,000 messages, latency creeps up. At 50,000, memory pressure kicks in. At 100,000, the broker starts rejecting publishes and upstream services cascade-fail. The difference between a minor slowdown and a full outage is whether you react to queue pressure before it becomes critical.

Reacting means measuring queue depth, classifying the pressure level (ok, high, critical), and taking graduated action. letting traffic flow normally when depth is low, throttling producer rates when it climbs past a high-water mark, and actively shedding low-priority messages when depth crosses the critical threshold. Building that decision tree with the right fallbacks and observability is where manual approaches break down.

## The Solution

**You write the monitoring and throttling logic. Conductor handles the conditional routing, retries, and execution tracking.**

`BkpMonitorQueueWorker` samples the queue depth and classifies pressure as `ok`, `high`, or `critical` based on the configured thresholds. A `SWITCH` task routes to the appropriate response: `BkpHandleOkWorker` logs normal operation, `BkpThrottleWorker` reduces the producer rate by a calculated percentage, and `BkpShedLoadWorker` drops the lowest-priority messages to bring depth back under control. Conductor's conditional routing makes the graduated response declarative. no if/else chains, just a clean branch per pressure level.

### What You Write: Workers

The backpressure response splits across four workers: queue monitoring, normal-flow handling, rate throttling, and load shedding, each owning one pressure tier.### The Workflow

```
bkp_monitor_queue
 │
 ▼
SWITCH (bkp_switch_ref)
 ├── ok: bkp_handle_ok
 ├── high: bkp_throttle
 ├── critical: bkp_shed_load
 └── default: bkp_handle_ok

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
