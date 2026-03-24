# Worker Auto-Scaling in Java Using Conductor : Monitor Queue, Calculate Capacity, Scale, Verify

## Fixed Worker Counts Waste Money or Miss SLAs

Your task queue processes 100 messages per second during business hours and 5 per second overnight. Running enough workers for peak load 24/7 wastes 95% of compute during off-hours. Running for average load means queue depth explodes during peaks and latency exceeds your SLA. You need to dynamically scale workers based on current queue depth and target latency.

Auto-scaling means monitoring the queue to measure current depth and processing rate, calculating how many workers are needed to drain the queue within the target latency, scaling the fleet to match, and verifying that the new worker count actually reduced queue depth and latency. Each step feeds the next. you can't calculate without monitoring data, and you can't verify without knowing what scaling action was taken.

## The Solution

**You write the monitoring and scaling logic. Conductor handles the scaling cycle, retries, and capacity verification.**

`WksMonitorQueueWorker` samples the queue depth and current worker count for the specified queue. `WksCalculateNeededWorker` computes the required worker count based on the current depth, processing rate, and target latency SLA. `WksScaleWorkersWorker` adjusts the worker fleet to the calculated count. scaling up or down. `WksVerifyScalingWorker` checks that the scaling action took effect and the queue is draining toward the target latency. Conductor records the monitoring snapshot, scaling decision, and verification result for every scaling cycle.

### What You Write: Workers

Four workers form the auto-scaling loop. Queue monitoring, capacity calculation based on target latency, fleet scaling, and verification that the scaling action reduced queue depth.

### The Workflow

```
wks_monitor_queue
 │
 ▼
wks_calculate_needed
 │
 ▼
wks_scale_workers
 │
 ▼
wks_verify_scaling

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
