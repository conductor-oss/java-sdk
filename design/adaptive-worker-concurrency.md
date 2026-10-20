# Adaptive worker polling and concurrency

Status: implemented; see `AdaptiveConcurrencyController` and the automator package in `conductor-client`. This document is the design of record for that implementation. Numeric defaults are starting points for validation through load testing.

## Purpose and scope

Introduce an opt-in mode that adjusts worker execution concurrency and polling batch sizes according to queue depth, actual task delivery, JVM process CPU utilization, and post-collection JVM heap utilization. One controller coordinates participating workers in a process and divides its capacity among them in proportion to their backlogs.

Adaptive mode uses separate concurrency limits. Existing thread-count configuration continues to govern fixed mode. Poller count stays configured and fixed; the controller adjusts execution capacity and the number of tasks requested per poll.

The feature uses existing Conductor APIs and task execution semantics. Existing SDK cleanup, unrelated reliability changes, native process-memory measurement, and container instrumentation are outside this proposal.

JVM CPU and heap thresholds guide acceptance of new work. They are not hard resource guarantees: tasks already running can increase resource use. Heap utilization does not measure total process or container memory.

## Relationship to the existing runner

The current runner already has most of the shape this design needs. Adaptive mode changes the source of permits and the sizing of requests; it keeps the loop, the execution path, the update path, the event model, and the configuration mechanisms. Implementers should modify these existing pieces, not build parallel ones.

| Existing mechanism | Where | Adaptive mode |
|---|---|---|
| One polling thread per worker registration | `TaskRunnerConfigurer.init()` and `startWorker()` | Retained. Registrations for the same logical queue are grouped and share one controller-managed worker state. |
| Acquire all free permits, poll with `count` equal to permits, release unused permits | `TaskRunner.pollTasksForWorker()` using `Semaphore permits` | Same shape. Permits come from controller targets instead of a fixed semaphore, a queue with no demand polls only as a heartbeat, and a queue whose last response was empty requests one task. |
| Submit each task to the executor, then schedule lease extension when `responseTimeoutSeconds > 0` and `leaseExtendEnabled()` | `TaskRunner.pollAndExecute()` | Factored into one `dispatch(task)` method used for polled tasks and for update-and-fetch replacements, so both paths get lease extension. |
| `processTask()` runs the worker, `executeTask()` builds the result, `updateTaskResult()` uploads large payloads and retries the update, `finally` cancels lease extension and releases the permit | `TaskRunner` | Retained. `updateTaskResult()` returns the slot disposition, the `finally` releases the controller slot unless it was transferred, and execution and reporting durations are recorded. |
| Replacement task from update-v2 is placed on `tasksTobeExecuted` and drained by the next poll | `TaskRunner.pollTask()` and `updateTaskResult()` | The in-memory queue is not used in adaptive mode. A replacement task is dispatched directly with the finishing task's slot. |
| Fixed pool `Executors.newFixedThreadPool(threadCount, threadFactory)`, platform or virtual thread factory | `TaskRunner` constructor | Platform threads: resizable `ThreadPoolExecutor` with the same factory and uncaught-exception handler. Virtual threads: thread-per-task executor bounded by slots; no pool. |
| `useVirtualThreads` opt-in on the builder only; the virtual factory uses `Thread.ofVirtual().name(prefix)`, so every thread carries the literal prefix pattern | `TaskRunnerConfigurer.Builder`, `TaskRunner` constructor | Flag retained and also exposed through Spring `conductor.client.*` and the annotated-worker configuration. Virtual threads are named with the numbered `name(prefix, start)` form. |
| `worker.paused()`, `PollFilter.filter(taskType, domain)`, `worker.getPollingInterval()`, per-task poll timeout | `TaskRunner.pollTasksForWorker()` and constructor | Retained as inputs. The polling interval becomes the base of the empty-response backoff. |
| Domain resolved from `conductor.worker.<task>.domain`, then `conductor.worker.all.domain`, then the builder map | `TaskRunner` constructor | Retained. The logical queue identity uses this resolved domain. |
| Per-worker properties via `PropertyFactory` (`conductor.worker.<task>.<prop>` with `conductor.worker.<prop>` fallback) | `PropertyFactory` | Per-worker adaptive settings use the same keys. Process-level settings use the builder and, in Spring, `conductor.client.*`. |
| Events published through `EventDispatcher` to `TaskRunnerEventsListener` and `MetricsCollector` | `events` and `metrics` packages, `PrometheusMetricsCollector` | New event types are added as default methods on the listener so existing collectors remain source-compatible. |
| Fatal authentication exit, `shutdown(timeout)` draining the executor and lease executor | `TaskRunner` | Unchanged. |
| Update-v2 enabled by the JVM-wide `taskUpdateV2` system property or environment variable | `TaskRunner` constructor | Retained as a fallback; a builder option is added so adaptive mode can authorize replacements per runner. |
| `withTaskPollCount(...)` accepted but never passed to the runner | `TaskRunnerConfigurer.Builder` | Superseded by the adaptive batch maximum and deprecated. |
| `getQueueSizeForTask(taskType)` calls `/tasks/queue/sizes` with a single name and reads one entry of the returned map | `TaskClient` | Generalized to take a list of queue names, `taskType` or `domain:taskType`, and return the whole map, so one request sizes every logical queue of the process. The domain overload, which drops its domain, is not used by this feature; fixing it is a separate bug. |

Behavior change to document for users: today a poller count of N registers the same worker N times, and each registration gets its own runner, semaphore, and fixed pool, so fixed-mode capacity is N multiplied by the thread count. In adaptive mode the N pollers share one worker target and one execution pool. Poller count affects only how many concurrent poll requests a logical queue can have outstanding.

## Server and SDK responsibilities

| Conductor server | Java SDK |
|---|---|
| Owns queues and determines which tasks are eligible for delivery | Determines how much work this process can accept |
| Applies task-definition concurrency and rate limits | Adjusts local execution concurrency within configured ceilings |
| Tracks execution state, retries, and timeouts | Executes accepted tasks and reports their results |
| Distributes queued tasks among polling consumers | Coordinates local workers sharing CPU and heap |
| Reports queue depth per task type and domain | Samples depth and divides its process budget among workers in proportion to it, each capped at what the worker can use |
| Routes domain-fallback tasks to domains with a recent poll | Keeps a heartbeat poll on idle queues so routing and poll-data views keep working |

The server marks delivered tasks `IN_PROGRESS`. A poll therefore accepts work; it is not a read-only preview of the queue. The SDK reserves execution capacity before requesting tasks and keeps accepted work accounted for until local execution and result-reporting handling finish.

Batch poll semantics matter for interpreting responses. The inspected Postgres queue implementation returns as soon as at least one task is available, up to `count`, and waits for the timeout only while the queue is empty; its comments state the Redis implementation behaves the same. A response smaller than `count` therefore means fewer than `count` tasks were available at that instant. It does not mean the queue is draining, and it does not mean the arrival rate is below what one slot can process. An empty response means the queue stayed empty for the whole timeout, or that server-side limits or other consumers took the work.

Queue depth is the server's size for the logical queue. It is a shared snapshot, not work reserved for this process: other consumers, task-definition concurrency and rate limits, and delayed tasks all mean a positive depth does not guarantee delivery. Backends also differ in what they count. In the inspected source the Postgres queue's size query counts every message row for the queue, including messages delivered to a consumer and not yet acknowledged, while its all-queues query and the Redis queue count only waiting messages. Depth is therefore an upper bound on waiting work. The controller uses it to divide capacity and to bound growth; it never admits work on the strength of depth alone, because every slot is still backed by a delivered task. An overstated depth inflates a queue's share, and the empty responses that follow shrink that queue's requests to one task until a delivery, so the cost is bounded. Backends should report waiting messages; aligning the Postgres size query with its all-queues query is a server-side fix to file alongside this feature.

The server also uses polls as a liveness signal. `WorkflowExecutorOps.getActiveDomain` routes a task with an ordered domain list to the first domain some worker polled within `conductor.app.activeWorkerLastPollTimeout`, ten seconds by default, and `ExecutionService.poll` refreshes that poll data on every poll, empty or not. A worker that stops polling a domain therefore stops being offered that domain's tasks. Adaptive mode keeps a heartbeat poll on every queue so this routing, and the poll-data views built on the same record, keep working.

### Existing APIs

Paths are relative to the configured API base URL, usually ending in `/api`.

| Operation | API | Use |
|---|---|---|
| Batch poll | `GET /tasks/poll/batch/T?workerid=W&domain=D&count=N&timeout=M` | Accept up to the reserved capacity |
| Result update | `POST /tasks` | Report task results |
| Update and fetch | `POST /tasks/update-v2` | Existing optional replacement-task path |
| Queue depth | `GET /tasks/queue/sizes?taskType=N1&taskType=N2` | One request per second naming every logical queue; a domain queue is named `domain:taskType` |

Omit `domain` for the default queue. The server rejects a batch poll whose `timeout` exceeds 5000 milliseconds with an error rather than clamping it. Validate configured poll timeouts against that bound at configuration time. The batch sizes endpoint takes repeated `taskType` parameters and returns a map keyed by the names passed. In the inspected source each name goes straight to the queue store's size lookup, so it sizes whatever queue it names, and the server names a domain queue `domain:taskType` (`QueueUtils.getQueueName`, separator `:`). Passing that form sizes the domain queue in the same request. The server marks the endpoint deprecated in favor of the single-queue `GET /tasks/queue/size`; this design depends on it as the only call that sizes many queues at once and asks that it be kept. Isolation groups and execution namespaces are not part of the polling path and are not passed.

No new server protocol or task-definition modification is required.

```text
Queue depth per worker ----+
Actual poll responses -----+--> Adaptive controller <-- JVM CPU and post-GC heap
Task completion -----------+             |
                                        v
                 Process target, divided among workers by demand
                                        |
                                        v
                             Reserve capacity before polling
                                        |
                                        v
                      Poll with count = min(free worker slots, free process slots, batch maximum)
                                        |
                                        v
                            Execute tasks and report results
                                        |
                                        v
                                Release capacity
```

## Configuration and terminology

An execution slot represents capacity promised to one pending task fetch or owned by one accepted task. It is not a guarantee that an execution thread is currently busy. In the existing runner a slot is a semaphore permit; adaptive mode issues slots from the controller instead.

`heldSlots` includes pending fetch reservations, accepted tasks awaiting dispatch, executing tasks, and tasks undergoing result-reporting handling. A target can temporarily be below `heldSlots` after a reduction; no new slots are admitted until enough work drains.

A logical worker queue is identified by server/client scope, task type, and resolved domain. The resolved domain is the one the runner computes today from worker properties and the builder map. Multiple configured pollers for that logical queue share one worker concurrency target and execution pool. Distinct server/client scopes are not merged merely because their task names match.

Applications supply one shared adaptive controller to participating runners in the process. Framework integration shares one controller across participating worker registrations. The controller observes process resources but controls only the workers registered with it; unrelated application work is reflected in resource pressure.

### Proposed defaults

| Setting | Default | Purpose |
|---|---:|---|
| Adaptive mode | Disabled | Explicit opt-in |
| Initial process concurrency | min(logical queues, available processors), clamped to [minimum, process maximum] | Aggregate startup target. Set it to the expected working level to skip the ramp from a small start; the auto value is 1 in a pod with one CPU or less |
| Minimum process concurrency | 1 | Lowest target demand decay settles at, so an idle process keeps a ready budget and the next burst does not ramp from one slot. Idle queues still hold no threads, because their shares are zero. CPU and heap pressure may reduce the target below it, and decay never raises a target that pressure lowered |
| Maximum process concurrency | 256 | Aggregate admission ceiling |
| Default maximum worker concurrency | 64 | Per-queue ceiling, explicitly overridable |
| Maximum polling batch | 16 | Ceiling for a single request |
| Resource sampling interval | 1 second | Observe JVM CPU and post-GC heap |
| CPU smoothing | EWMA, alpha 0.2 | Filter one-second sample noise out of the growth decision |
| Control interval | 5 seconds, ±20% jitter | Evaluate target adjustments |
| Growth thresholds | Smoothed CPU below 75% and latest CPU sample below 90%; post-GC heap below 75% | Headroom required for growth |
| CPU pressure threshold | CPU samples at or above 90% for 3 seconds | Reduce targets |
| Heap pause threshold | Post-GC heap at or above 90%, from a collection newer than the last resume | Stop new acquisition immediately; also reduction evidence |
| Heap resume threshold | Post-GC heap below 80% | Resume at current targets; a reading below 80% can only come from a collection after the pause |
| Drained resume | Paused, `heldSlots == 0`, and 30 seconds since the high collection that caused or renewed the pause | Resume at concurrency one so ordinary allocation can trigger the collection that refreshes the estimate |
| Pre-collection heap reading | Current used heap until the first collection | Upper bound on live data; gates growth before any GC, never enters a pause |
| Reduction factor | 0.75 | Withdraw 25% of the process target under pressure; shares shrink with it |
| Reduction interval | 15 seconds | Minimum spacing between reductions, and growth cooldown after one |
| Growth step | 20% of the process target, rounded up, clamped to 1–16 slots, never past total demand | Bounded expansion per control tick |
| Demand decay | After 30 seconds with total demand below the process target, shrink the target to the larger of demand and half of itself, floor 1 | Return surplus capacity gradually so the next burst ramps under the growth gate |
| Depth sampling interval | 1 second | One `GET /tasks/queue/sizes` request per interval naming every logical queue, on a dedicated thread |
| Depth freshness | 5 seconds | A depth older than this is unknown and counts as one; must cover a full sweep of the process's queues |
| Heartbeat interval | 5 seconds | A queue with no share and no standby poll polls one task this often on an open slot, without a server wait, keeping the server's poll data inside its 10-second active-poller window and verifying a zero depth |
| Standby poll timeout | 1 second | Server wait of the single-task long poll a queue keeps open on a slot no share claims, so a task is picked up as it arrives; at most 5 seconds, zero disables standby polls. An idle queue sends about one request per timeout |
| Metric freshness | 5 seconds | A metric older than this freezes growth; a gap longer than this resets smoothing and pressure timing |
| Demand-only operation | Disabled | Startup fails if neither resource metric is supported unless explicitly enabled |
| Execution threads | Platform; `useVirtualThreads` selects virtual | Existing opt-in; chooses the executor strategy below |
| Execution-thread idle timeout | 60 seconds | Retire idle platform execution threads; not applicable to virtual threads |

All concurrency and batch ceilings must be positive. Initial process concurrency must not exceed the process maximum. Per-worker maxima remain subject to the process ceiling. Validate finite thresholds in `(0, 1]`, with growth < heap resume < heap pause and growth < CPU pressure. Durations must be positive; each freshness must exceed its sampling interval; the heartbeat interval should stay below the server's active-poller window. The standby poll timeout is between zero and 5000 milliseconds. Configurable smoothing uses `0 < alpha <= 1`. Configured long-poll timeouts must not exceed 5000 milliseconds and must fit inside the HTTP read timeout with room for overhead.

Expose adaptive options through the task-runner builder and worker configuration path, including framework bindings. Required concepts are enablement, the shared controller, process maximum, default/per-worker maximum, batch maximum, resource and depth sampling intervals and freshness, heartbeat interval, control interval, resource thresholds, demand-only operation, update-and-fetch enablement, and virtual-thread selection. Virtual threads stay opt-in with the existing `useVirtualThreads` flag; that flag is additionally exposed as a Spring property and through the annotated-worker configuration, since today only the builder sets it. Per-worker values use the existing `conductor.worker.<task>.<prop>` property path with the `conductor.worker.<prop>` fallback. Do not reinterpret existing `threadCount` values as adaptive ceilings.

Existing worker polling intervals, long-poll timeouts, pause/filter behavior, and configured poller counts remain inputs to this algorithm. Adaptive configuration must use distinct names for execution concurrency, requested batch count, and poller count. The existing `withTaskPollCount` builder option, which is stored but never applied today, is deprecated in favor of the adaptive batch maximum.

## Controller state and invariants

```text
ProcessState:
    maximumConcurrency
    targetConcurrency
    heldSlots                        // reservations plus accepted tasks, all queues
    admissionPaused                  // heap-pause latch
    lastResumeAt                     // -infinity at startup
    lastReductionAt                  // -infinity at startup
    surplusSince                     // when total demand last fell below the target; NONE while demand covers it
    heapGrowthCeiling                // growth limit left by heap pressure; unlimited until a heap reduction or pause sets it
    heapCeilingSince                 // when the ceiling was set; it holds 120 seconds, then rises one slot per control tick

    cpu:  supported, raw, smoothed, observedAt, pressureSince              // observedAt is NONE after a failed sample
    heap: supported, postGcUtilization, collectionAt, observedAt,
          lastHighCollectionAt,      // collectionAt of the latest collection at or above the pause threshold
          baseline,                  // lowest post-collection utilization seen: the heap the process needs without tasks, or close to it
          recentReadings             // post-collection readings of the last 30 seconds; their peak gates growth

WorkerQueueState:
    queueIdentity                    // scope, task type, resolved domain
    maximumConcurrency
    targetConcurrency                // this queue's share of the process target; derived by allocate()
    heldSlots                        // reservations plus accepted tasks
    depth                            // last sampled queue size
    depthObservedAt                  // NONE until the first successful sample and after a failed one
    outstandingPolls
    nextHeartbeatAt                  // now at startup; pushed forward by every granted poll; a refused heartbeat stays due
    nextStandbyAt                    // now at startup; a delivery resets it, an expired standby wait pushes it one standby timeout
    standbyPolls                     // outstanding standby polls; their slots are not part of the backlog
    consecutiveEmptyPolls            // poll errors count as empty responses
    depthAtEmptyPoll                 // sampled depth when the latest empty response settled; zero if the depth was stale
    pollableAtSample                 // worker pause and poll filter result at the latest depth sweep
    nextPollAllowedAt
    configuredPollInterval
    configuredPollTimeout
    executionPool
```

The following invariants apply:

1. Rule one: a queue is polled only with an open slot for every task the poll may return. A reservation never exceeds the free worker target or the free process target at the time it is made, except that a heartbeat may take one open process slot beyond the queue's share, never beyond the worker maximum. Held slots exceed a target only after that target was lowered by a reduction, a decay, or a reallocation, and then only until work drains. Nothing polls while admission is paused or every slot is held.
2. Each slot has exactly one owner: an outstanding request or an accepted task.
3. Reservations are transferred or released exactly once. Network calls do not run under the controller lock.
4. Reducing a target does not revoke work already accepted or a reservation backing an outstanding fetch.
5. Tasks already owned by the process continue execution and reporting while acquisition is paused.
6. All pollers for the same logical queue share its counters and target.
7. Worker targets are derived. `allocate()` recomputes every one of them from the process target and the queues' demands; nothing adjusts a worker target individually.

`applyTargets()` is the single exit for every target or admission change: it runs `allocate()`, publishes the targets, resizes platform execution pools to their required size, emits the adjustment observation, and wakes worker waiters of each queue up to its new deficit and process waiters in deficit order until the free process slots are covered, or every waiter when the pause lifts. Unchanged targets cause no wakeups. `NONE` timestamps compare as negative infinity, so a comparison such as `lastHighCollectionAt > lastReductionAt` is false while both are unset.

Use monotonic time for controller intervals, freshness, backoff, and duration comparisons. Resource sampling and control scheduling use a controller executor separate from task execution and task-result updates. The depth sampler runs on its own thread because it makes network calls; the controller executor's tasks never do, so a slow server cannot delay a CPU or heap sample.

## Startup

```text
function start(workerQueues):
    cpu.supported  = platform OperatingSystemMXBean reports getProcessCpuTime()
    heap.supported = heap max is positive AND (GC notifications are available
                     OR at least one heap pool reports collection usage)
    log each unsupported metric once; expose in observability
    if NOT cpu.supported AND NOT heap.supported AND NOT demandOnlyEnabled:
        fail startup with a configuration error

    process.targetConcurrency = configured initial value, or
        clamp(configured initial OR min(count(workerQueues), availableProcessors), minimumConcurrency, maximumConcurrency)
    process.heldSlots = 0
    process.admissionPaused = false
    process.lastResumeAt = -infinity
    process.lastReductionAt = -infinity
    process.surplusSince = NONE
    process.heapGrowthCeiling = unlimited; process.heapCeilingSince = NONE
    cpu.observedAt = NONE;  cpu.pressureSince = NONE
    heap.observedAt = NONE; heap.lastHighCollectionAt = NONE
    heap.baseline = undefined; heap.recentReadings = empty

    for each queue:
        queue.heldSlots = 0
        queue.outstandingPolls = 0
        queue.depth = 0; queue.depthObservedAt = NONE          // unknown until the first sample
        queue.consecutiveEmptyPolls = 0;  queue.depthAtEmptyPoll = 0
        queue.standbyPolls = 0;  queue.pollableAtSample = true
        queue.nextPollAllowedAt = now
        queue.nextHeartbeatAt = now;  queue.nextStandbyAt = now
        initialize adaptive execution pool with size 1
    allocate(now)                      // unknown depth counts as one, so every queue starts with a one-task share

    start resource sampler
    start depth sampler
    start control timer
    start configured pollers           // existing one-thread-per-registration model
```

If there are more queues than process slots, grants rotate among queues with equal deficits. Startup does not reserve one process slot permanently for every worker. Queues registered after the controller starts enter the next allocation like any other and do not raise the process target.

## Monitoring JVM capacity

### CPU

Read JVM process CPU as the growth of `OperatingSystemMXBean.getProcessCpuTime()` between samples, divided by the elapsed time and by the CPUs available to the process: the container quota when one is set (cgroup v2 `cpu.max`, or v1 `cpu.cfs_quota_us` over `cpu.cfs_period_us`), which may be fractional, capped by `Runtime.availableProcessors()`. The quota is re-read on every sample so a resized container takes effect. Clamp to `[0, 1]`; the first sample has no interval and is an invalid observation, not evidence that the metric is unsupported. Do not use `getProcessCpuLoad()`: it measures the interval since the previous call by any caller in the JVM, so a metrics library reading it, as Micrometer's `ProcessorMetrics` does on every scrape and Spring Boot Actuator registers by default, leaves this sampler a window of milliseconds. End-to-end testing measured readings of zero on a saturated core with another reader 20 milliseconds earlier, and the controller grew while CPU was saturated. `availableProcessors()` alone is not enough either, because it rounds a fractional quota up: at 0.5 CPU a saturated process would read 0.5 and never reach the pressure threshold. The metric is unsupported only when the platform bean does not implement the `com.sun.management` interface or reports no process CPU time.

CPU samples are one-second averages and are noisy. Growth therefore uses an exponentially weighted moving average, and the latest sample vetoes growth only at the pressure threshold. Pressure uses the raw samples directly: a run of samples at or above 90% lasting three seconds is pressure, and the start of that run is the evidence time.

### Heap

Current used heap from `MemoryMXBean.getHeapMemoryUsage()` includes garbage that has not yet been collected. On a healthy JVM it routinely rises above 90% just before a young collection: under G1 defaults the eden alone can be most of the heap, and under ParallelGC it is a third. Used heap is therefore not a pressure signal. The controlling signal is heap usage measured immediately after a collection, which is the JVM's best available estimate of live data.

```text
heapMax            = MemoryMXBean.getHeapMemoryUsage().getMax()      // must be positive
postGcHeapUsed     = sum over heap pools of usage after the most recent collection
longLived[pool]    = used / min(pool max, heapMax) after the most recent collection OF that pool
                     for each heap pool that supports usage thresholds   // old generation; never eden or survivor
postGcUtilization  = max(postGcHeapUsed / heapMax, max(longLived))      // controlling value
```

The whole-heap ratio alone misses pressure under collectors with a fixed-size old generation. SerialGC, the JVM's default in containers with less than two CPUs or 1792 MB, and ParallelGC give the old generation about two thirds of the heap; data that outlives a young collection can only live there. End-to-end testing with tasks holding multi-megabyte payloads in a 123 MB heap found the old generation 99% full after collection while the whole-heap ratio read 69%, because the empty eden counted as headroom; growth continued and tasks ran out of memory. The fullest long-lived pool measured against its own capacity catches this. Long-lived pools are identified by `MemoryPoolMXBean.isUsageThresholdSupported()`, which is true for Tenured Gen, PS Old Gen, G1 Old Gen, ZGC Old Generation, ZHeap and Shenandoah and false for every eden, survivor and ZGC young pool. A pool's reading is taken only from collections by a collector whose `getMemoryPoolNames()` include it, because after a young collection the old generation still holds garbage that only a full collection frees; the reading is kept until the next such collection. For G1, ZGC and Shenandoah the long-lived pool's capacity is the whole heap, so the reading equals the whole-heap ratio or less and nothing changes.

Obtain `postGcHeapUsed` in one of two ways, chosen at startup:

1. Preferred: register a listener on each `GarbageCollectorMXBean` that implements `NotificationEmitter`. On each collection notification, sum `getMemoryUsageAfterGc()` across heap pools, update the long-lived pools that collector collects, and store the resulting utilization with the collection end time converted from JVM uptime to the controller's monotonic clock. The listener only stores the latest value; the sampler reads it on its next tick, so a collector running many short collections per second produces at most one observation per sampling interval and the listener never does controller work.
2. Fallback: on each sampling tick, sum `MemoryPoolMXBean.getCollectionUsage().getUsed()` across heap pools that support it, and take each long-lived pool's collection usage against its capacity; collection usage is only updated by collections of that pool, which is the rule above. Collection usage carries no timestamp, so a collection is detected by a change in the sum of `GarbageCollectorMXBean.getCollectionCount()` across collectors between ticks, and `collectionAt` is the tick that observed the change. This path is accurate to within one sampling interval; the notification path is exact.

Either way the sampler reads the current post-collection value every tick, so the heap metric produces a fresh observation each second even when no collection has occurred. A quiet JVM is not an unavailable one. Each heap observation carries two times. `observedAt` is the sampler tick and drives freshness. `collectionAt` is the time of the collection that produced the value and drives pause entry and reduction evidence, so re-reading the same value never counts as new evidence.

Before the first collection there is no post-collection value; on both paths this is the state in which every collector's collection count is zero. Until the first collection, the heap reading is current used heap with `collectionAt` undefined. Used heap is an upper bound on live data and is small on a fresh JVM, so the metric is valid from the first tick and growth is gated normally. A reading without a `collectionAt` never enters a pause and never counts as pressure evidence, because without a collection it may be mostly garbage.

Post-collection values are not smoothed. They are already an aggregate over a collection cycle and change only when a collection happens; smoothing them would delay every decision without removing any noise. Reject a nonpositive or undefined heap maximum. If neither mechanism is available, the heap metric is unsupported. Unknown values are never interpreted as zero.

Known limitation: a live-set increase is visible only after the next collection. Between collections the post-collection estimate can understate live data by at most the allocation since that collection, which is bounded by free heap. The JVM collects before exhausting the heap, so the lag is one collection cycle.

### Sampling and freshness

Read CPU and heap independently: an exception or unavailable value from one must not prevent observing or acting on the other. Use fixed-delay sampling, with the next attempt scheduled one interval after the previous attempt finishes. Never replay missed timer ticks after a JVM pause. Timestamp observations when obtained.

A metric is fresh when its latest observation is at most five seconds old; a failed sample clears the observation time, so a metric whose last attempt failed is not fresh. Check age at the time of each decision, not only when the sampler publishes, so a sampler that stops is noticed. Preserve the heap-pause latch across missing measurements. All metric state is read and written under the controller lock; only the MXBean calls themselves run outside it.

```text
function sampleResources(now):
    // MXBean calls run outside the controller lock; each is attempted independently
    cpuReading  = cpu.supported  ? processCpuTimeDelta / (elapsed * availableCpus) : FAILED
    heapReading = heap.supported ? (latest post-collection usage and its collection time,
                                    or (current used heap, UNDEFINED) before the first collection) : FAILED

    under controller lock:
        if cpu.supported:
            if cpuReading is finite AND 0 <= cpuReading <= 1:
                if now - cpu.observedAt > freshness:          // also true after a failed sample
                    cpu.smoothed = cpuReading
                    cpu.pressureSince = NONE
                else:
                    cpu.smoothed = alpha * cpuReading + (1 - alpha) * cpu.smoothed
                cpu.raw = cpuReading
                cpu.observedAt = now
                if cpuReading >= cpuPressureThreshold:
                    if cpu.pressureSince == NONE: cpu.pressureSince = now
                else:
                    cpu.pressureSince = NONE
            else:
                cpu.observedAt = NONE
                cpu.pressureSince = NONE

        if heap.supported:
            if heapReading != FAILED:
                (used, collectionAt) = heapReading
                heap.postGcUtilization = double(used) / double(heapMax)
                heap.collectionAt = collectionAt
                heap.observedAt = now
                if collectionAt is defined AND heap.postGcUtilization >= heapPauseThreshold:
                    heap.lastHighCollectionAt = collectionAt
                if collectionAt is defined:
                    heap.baseline = min(heap.baseline, heap.postGcUtilization)   // the first collection sets it
                    append (collectionAt, now, heap.postGcUtilization) to heap.recentReadings
                        if collectionAt is newer than the last entry's
            else:
                heap.observedAt = NONE

        reconcileHeapPause(now)
```

### Heap pause and resume

A fresh post-collection observation at or above 90%, produced by a collection newer than the last resume, closes the admission gate immediately. Outstanding fetch reservations remain valid. Ordinary result reporting and lease handling continue. The high collection is also reduction evidence; the control tick applies the reduction when the reduction interval permits, whether or not the pause has ended by then, so a heap that flaps across the thresholds shrinks targets at most once per interval, the same bound CPU pressure has.

```text
function reconcileHeapPause(now):
    heapFresh = heap.supported AND now - heap.observedAt <= freshness
    if NOT process.admissionPaused:
        if heapFresh AND heap.collectionAt is defined
           AND heap.postGcUtilization >= heapPauseThreshold
           AND heap.collectionAt > process.lastResumeAt:
            process.admissionPaused = true
            limitHeapGrowth(floor(0.75 * max(process.targetConcurrency, process.heldSlots)), now)
            applyTargets()                                   // publishes the pause
        return

    if heapFresh AND heap.postGcUtilization < heapResumeThreshold:
        resumedByDrain = false                               // a collection after the pause shows headroom
    else if process.heldSlots == 0 AND now - heap.lastHighCollectionAt >= 30 seconds:
        resumedByDrain = true
        process.targetConcurrency = 1                            // allocate() derives the shares
    else:
        return

    process.admissionPaused = false
    process.lastResumeAt = resumedByDrain ? now : heap.collectionAt   // pause re-entry needs a collection newer than this
    applyTargets()                                           // wakes capacity waiters
```

The estimate path is the normal one. The pause was entered on a reading at or above 90%, so a reading below 80% can only come from a later collection; admission resumes at the current targets, and the pause's reduction lands at the next permitted control tick if it has not already. A brief spike costs one 25% reduction and one cooldown, not a restart from concurrency one.

The drain path handles a pause whose estimate is stuck between the resume and pause thresholds because nothing allocates enough to trigger a collection. With no held slots the process is idle, so resuming at concurrency one lets ordinary allocation trigger the collection that refreshes the estimate. Pause entry requires a collection newer than the last resume. On the estimate path the resume time is the time of the collection that justified it, so any later high collection re-enters, even one that completed just before the resume was observed. On the drain path there is no justifying collection, so the resume time is the current time; without that rule the stale reading would re-enter the pause on the next tick. After a drained resume the process runs at concurrency one until a new collection reports. Growth needs no separate rule for this case: the stale reading is at or above the pause threshold, which is above the growth threshold, so growth is blocked until a newer collection shows headroom. If that collection is at or above the pause threshold the pause re-enters and the drain timer restarts from that collection; if the process is still idle, it resumes again 30 seconds later, so a process whose live set genuinely sits above the threshold alternates between paused and concurrency one rather than staying paused forever. The drain timer is anchored on `lastHighCollectionAt`, the same field that carries reduction evidence, so a further high collection during a pause renews the pause and a collection between the thresholds does not.

The controller does not force garbage collection. Sustained genuine heap pressure keeps acquisition at or near zero; this design cannot guarantee progress if the process lacks memory headroom.

### Monitoring state

The monitoring state is derived for observability and is not consulted by the algorithm:

| State | Condition |
|---|---|
| `PAUSED` | `admissionPaused` |
| `PRESSURED` | CPU `pressureSince` is set, or a high collection is newer than the last reduction |
| `STALE` | A supported metric is not fresh |
| `HEALTHY` | None of the above |

Precedence is the order listed. An unsupported metric is absent from every condition. With only one supported metric, growth, pressure, and pause decisions use that metric alone. With neither supported and demand-only operation enabled, the controller skips resource gating entirely and growth is bounded by demand and configured ceilings; this is logged at startup as a warning.

## Adjusting concurrency targets

The process target bounds aggregate admission. Worker targets are its division among queues in proportion to backlog, each capped at the queue's demand. The process target changes on the control timer and on a drained resume. Worker targets are recomputed whenever the process target changes, after every depth sweep, and after an empty response; never after every task.

### Demand and allocation

A queue's backlog is the slots it holds plus the tasks waiting for it. Its demand is the work it could run now: the backlog capped by its maximum. Shares are weighted by backlog, not demand, so a queue with 600 waiting tasks outweighs one with 100 even when both exceed a maximum of 64; weighting by the capped demand would give them equal shares exactly when backlogs are largest.

```text
function backlog(queue, now):
    depth = (now - queue.depthObservedAt <= depthFreshness) ? queue.depth : 1   // unknown depth counts as one
    if queue.consecutiveEmptyPolls > 0:          // the server delivered nothing from this depth
        depth = min(depth, 1 + max(0, depth - queue.depthAtEmptyPoll))   // one, plus tasks that arrived since
    return (queue.heldSlots - queue.standbyPolls) + depth     // a standby slot counts once it delivers a task

function demand(queue, now):
    return min(queue.maximumConcurrency, backlog(queue, now))

function standbyDemand(queue, now):                          // the slot of the queue's standby poll
    held = queue.heldSlots - queue.standbyPolls
    return 1 if standby polls are enabled AND the queue was pollable at the last sweep
              AND backlog(queue, now) == held AND held < queue.maximumConcurrency
           else 0

function allocate(now):
    b[q] = backlog(q, now);  d[q] = demand(q, now);  total = sum(d)
    if total <= process.targetConcurrency:
        share[q] = d[q]                                            // every queue gets all it can use
    else:
        remaining = process.targetConcurrency;  open = queues with d[q] > 0
        repeat:                                                    // water-filling
            quota[q] = remaining * b[q] / sum(b over open) for q in open
            for each q in open with quota[q] >= d[q]:
                share[q] = d[q];  remaining -= d[q];  remove q from open     // capped at its demand
        until no queue was capped
        share[q] = floor(quota[q]) for q in open
        share[q] = 1 for q in open with share[q] == 0, while slots remain  // floors before rounding
        give one more slot to the remaining queues in open
            with the largest fractional parts of quota             // largest-remainder rounding: shares sum to the target
        share[q] = max(1, share[q]) for each queue with d[q] > 0    // floor; can push the sum above the target
    queue.targetConcurrency = share[q] for each queue               // 0 where d[q] == 0
```

Shares are proportional to backlog. A queue whose proportional share would exceed its demand is held at its demand and the excess goes to the other queues, so no slot is assigned that a queue cannot use. Floors are taken from the leftover before rounding, so they push the sum above the target only when there are more queues with work than slots. Largest-remainder rounding keeps the shares summing exactly to the process target, so rounding never leaves a slot that no queue may use; the floor of one keeps every queue with waiting work polling; and when total demand is at or below the process target every queue gets its whole demand. A share never exceeds demand, so a request never asks for more than the sampled depth. Floors can push the sum of the shares above the process target; the process target still bounds admission, and the grant rule in "Reserving poll capacity" decides who gets the difference. A queue holding long-running tasks keeps its share while they run, because held slots are demand, and releases it as they finish; nothing is preempted.

Two rules make depth safe to rely on. Unknown counts as one: while the depth endpoint is unavailable, every queue asks for one slot beyond what it holds, so a busy queue grows one slot per tick as it fills and an idle queue costs one single-task poll per backoff interval. An empty response caps the queue at one waiting task until a delivery, plus whatever arrived after it: server limits, other consumers, or a backend that counts in-flight messages can all leave a positive depth undeliverable, and without this rule such a queue would hold a full-size request for every poll timeout. Tasks that arrive after the empty response are new evidence, so they count in full, and a sweep that sees them also cancels the queue's empty-response backoff. Without that exception an idle queue, whose heartbeats all come back empty, met a burst of 200 tasks with a share of one and waited out a five-second backoff before polling, despite a warm process target of 16.

Depth is sampled once per interval on a dedicated thread. A sweep is one batch request that names every logical queue the process polls, a domain queue as `domain:taskType`, and returns every size at once. The sampler also evaluates `worker.paused()` and the poll filters there, outside the lock, and records a depth of zero for a worker that would not poll, so a paused or filtered worker keeps the share its running tasks need and nothing more, and user code never runs under the controller lock.

```text
function queueName(queue):
    return queue.domain == null ? queue.taskType : queue.domain + ":" + queue.taskType   // the server's own naming

function sampleDepths(now):
    // user code and the network call run outside the lock
    pollable[q] = NOT worker.paused() AND poll filters accept the queue, for each queue
    names = queueName(q) for each queue with pollable[q]
    sizes = names is empty ? {} : taskClient.getQueueSizes(names)       // GET /tasks/queue/sizes, one request

    under controller lock:
        for each queue:
            if NOT pollable[q]:               queue.depth = 0;                             queue.depthObservedAt = now
            else if the request succeeded:    queue.depth = max(0, sizes[queueName(q)]);  queue.depthObservedAt = now
            else:                             queue.depthObservedAt = NONE                 // unknown counts as one
        applyTargets()                                        // re-divides the process target; wakes queues whose share rose
```

A name missing from a successful response is read as zero, which is what the server returns for a queue it has never seen; a failed request leaves every queue unknown until the next sweep.

Use fixed-delay scheduling, a finite request timeout, and the existing sampled error logging. The one request must finish within the depth freshness window; a server that cannot answer in that time makes every depth unknown, which degrades to delivery-driven growth rather than stalling. Depth reads never reserve slots and never run in the poll loop. Discovery latency for an idle queue is one sampling interval plus one poll. Server load: one request per process per second replaces the ten idle polls per second per queue that the default 100-millisecond polling interval produces today, and idle queues stop polling except for heartbeats. The server still runs one count per name behind the request, and on Postgres each is a count over the queue's rows, so very large fleets may want a longer interval.

### Control tick

```text
function controlTick(now):
    under controller lock:
        reconcileHeapPause(now)

        cpuFresh  = cpu.supported AND now - cpu.observedAt <= freshness
        heapFresh = heap.supported AND now - heap.observedAt <= freshness

        cpuPressure  = cpuFresh AND cpu.pressureSince != NONE AND now - cpu.pressureSince >= 3 seconds
        heapPressure = heap.supported AND heap.lastHighCollectionAt > process.lastReductionAt

        if (cpuPressure OR heapPressure) AND now - process.lastReductionAt >= reductionInterval:
            process.targetConcurrency = max(1, floor(0.75 * process.targetConcurrency))
            process.lastReductionAt = now
            if cpu.pressureSince != NONE: cpu.pressureSince = now     // the next reduction needs 3 more seconds of pressure
            if heapPressure: limitHeapGrowth(process.targetConcurrency, now)
            applyTargets()                                           // allocate() cuts every share with it
            return

        total = sum(demand(queue, now) + standbyDemand(queue, now) for each queue)

        if total >= process.targetConcurrency:
            process.surplusSince = NONE
        else if process.surplusSince == NONE:
            process.surplusSince = now
        else if now - process.surplusSince >= decayInterval:        // 30 seconds of surplus
            process.targetConcurrency = min(process.targetConcurrency,               // decay only lowers
                                            max(minimumConcurrency, total, ceil(process.targetConcurrency / 2)))
            process.surplusSince = now

        heapPeak = highest post-collection utilization of the last 30 seconds, and the latest
        growthAllowed =
            total > process.targetConcurrency
            AND NOT process.admissionPaused
            AND now - process.lastReductionAt >= reductionInterval
            AND (NOT cpu.supported  OR (cpuFresh AND cpu.smoothed < growthThreshold
                                        AND cpu.raw < cpuPressureThreshold))
            AND (NOT heap.supported OR (heapFresh AND heapPeak < growthThreshold))
        relaxHeapGrowthCeiling(now)                                  // after 120 s without heap pressure: +1 per tick
        if growthAllowed:
            increment = clamp(ceil(0.20 * process.targetConcurrency), 1, 16)
            increment = min(increment, heapGrowthCeiling - process.targetConcurrency)
            taskSlots = process.heldSlots - slots held by standby polls
            if heapFresh AND taskSlots > 0 AND heapPeak > heap.baseline:
                perSlot = (heapPeak - heap.baseline) / taskSlots
                increment = min(increment, floor((growthThreshold - heapPeak) / perSlot))
            if increment > 0:
                process.targetConcurrency = min(process.maximumConcurrency, total,
                                                process.targetConcurrency + increment)

        applyTargets()

function limitHeapGrowth(ceiling, now):                 // on a heap reduction, and on a pause with 0.75 * max(target, held)
    heapGrowthCeiling = max(1, min(heapGrowthCeiling, ceiling));  heapCeilingSince = now
```

Reduction needs evidence newer than the previous reduction. For CPU the evidence is a run of high samples; resetting `pressureSince` at the reduction means the next reduction needs a run that starts after this one. For heap the evidence is the collection time of the latest high collection; re-reading a stale post-collection value is never new evidence, so one high collection yields at most one reduction. The first reduction has no interval to wait for because `lastReductionAt` starts at negative infinity. Growth waits for the same interval after a reduction, which is the growth cooldown. A reduction cuts only the process target; every share shrinks with it through `allocate()`.

The process target rises while total demand exceeds it, never past total demand or its ceiling. A single deep queue therefore carries it, a queue holding long-running tasks keeps counting through its held slots, and idle queues contribute nothing. Growth is 20% per tick with a floor of one slot, so a process at one slot still ramps and aggregate growth stays bounded and resource-gated. Depth is shared by every process polling the queue, so total demand overstates what this process alone will receive. That only means the target can run ahead of delivered work, which costs idle threads and nothing else, and decay returns it.

Heap needs two more limits, because running out of it fails tasks rather than slowing them. End-to-end testing with tasks that hold multi-megabyte payloads in a 123 MB heap showed both failure modes. First, the growth gate reads the last post-collection value, which does not yet include what newly granted slots will allocate, so a 20% step taken at 70% can land above 100%. Growth is therefore also limited to the slots whose observed cost fits under the growth threshold: the heap held by running tasks is the post-collection utilization minus the baseline, the lowest post-collection utilization seen, and dividing it by the slots held for tasks gives the cost of one more. Growth decisions use the highest post-collection reading of the last 30 seconds, not the latest: tasks that hold payloads for seconds make each reading depend on which of them a collection happened to find live or promote, and on a host with a 128 MB heap a low reading taken just after a high one let the target grow two slots into an `OutOfMemoryError`. Pause and resume still use the latest reading, since they must react to it. Second, after a pause and a reduction the controller regrew past the level that had caused the pressure and ran tasks out of memory, twelve times in one run. A heap reduction therefore leaves a growth ceiling at the reduced target, and a pause leaves one at three quarters of what was running; the ceiling holds for two minutes, then rises one slot per control tick, so the controller probes back toward the pressure point slowly enough for a collection to report before it gets there. CPU has no such ceiling because CPU saturation slows tasks without failing them.

The control tick runs from a fixed-delay timer that checks a jittered due time, not from a task that reschedules itself, and every timer catches and drops anything thrown, including failures to log it. When the heap ran out end to end, a self-rescheduling tick stopped for good: the target never changed again, through three more heap pauses.

When total demand has been below the process target for thirty seconds, the target shrinks to the largest of the configured minimum, total demand and half of itself, and again every thirty seconds while the surplus lasts. It never drops below demand, and demand is never below held slots, so decay never creates an overshoot. A process that goes idle reaches the minimum, one by default, within a few minutes, and the next burst ramps from there under the growth gate instead of jumping to a stale ceiling. A higher minimum trades nothing while idle, since idle queues have no shares and their pools release their threads, for a burst that starts with that many slots ready. Decay is not a reduction: it does not set `lastReductionAt` and starts no growth cooldown.

If the process is at its ceiling, capacity released by one queue is used by others through ordinary slot grants without raising the ceiling. The controller never terminates running tasks to rebalance capacity.

## Reserving poll capacity

### Shares and the grant rule

Shares are targets, and the grant rule makes holdings follow them. A queue's deficit is `targetConcurrency - heldSlots`, how far it is below its share. A freed process slot, or a slot opened by a higher process target, goes to the waiting queue with the largest positive deficit; equal deficits are served in arrival order, and a queue without a positive deficit is never granted a process slot. A queue at or above its share waits on its own worker target, not on the process, so it cannot take process slots from queues below theirs. When floors make the shares sum to more than the process target, deep queues reach their shares first and the remainder rotates among the queues with floor shares, each of which gets a slot in turn as tasks finish.

Idle queues have no share and poll only with standby polls on slots no share claims, or as heartbeats, one short single-task request per interval on an open slot, so they cannot starve busy ones. The only queues that poll without receiving are those whose depth cannot be delivered, and the empty-response rule holds each of them to one slot. An empty long poll still holds that slot for the whole timeout, so deployments with many such queues should keep poll timeouts short.

```text
function deficit(queue):
    return queue.targetConcurrency - queue.heldSlots

function reserveBatch(queue, now):
    under controller lock:
        if process.admissionPaused:
            return WAIT_FOR_PROCESS_SLOT                     // no poll without an open slot; the resume wakes every waiter

        processAvailable = max(0, process.targetConcurrency - process.heldSlots)
        workerAvailable  = max(0, deficit(queue))

        if workerAvailable == 0:                             // no share, or the share is already held
            if standby polls are enabled AND queue.outstandingPolls == 0 AND now >= queue.nextStandbyAt
               AND queue.heldSlots < queue.maximumConcurrency
               AND unclaimedSlots() > dueHeartbeats:
                count = 1                                    // standby: one task, server waits the standby timeout
                queue.standbyPolls += 1
                queue.nextStandbyAt = now + standbyPollTimeout
            else if queue.outstandingPolls > 0 OR now < queue.nextHeartbeatAt:
                return WAIT_FOR_WORKER_SLOT                  // the wait ends at nextStandbyAt or nextHeartbeatAt
            else if processAvailable == 0 OR queue.heldSlots >= queue.maximumConcurrency
               OR (processAvailable == 1 AND a more overdue heartbeat is waiting):
                return WAIT_FOR_HEARTBEAT_SLOT               // stays due; woken first when a slot opens
            else:
                count = 1                                    // heartbeat: one task on the open slot, no server wait
        else:
            dueHeartbeats = queues waiting with a heartbeat due
            if processAvailable <= dueHeartbeats:
                return WAIT_FOR_PROCESS_SLOT                 // due heartbeats take open slots first
            if some queue waiting for a process slot has deficit(w) > deficit(queue):
                return WAIT_FOR_PROCESS_SLOT                 // the slot belongs to a queue further below its share
            count = min(workerAvailable, processAvailable - dueHeartbeats, maximumBatch)

        ensure execution pool can accommodate queue.heldSlots + count
        queue.heldSlots += count
        queue.outstandingPolls += 1
        queue.nextHeartbeatAt = now + heartbeatInterval      // any poll is a heartbeat for the server
        process.heldSlots += count
        return Reservation(count)

function unclaimedSlots():                                   // neither held nor promised to a share
    return max(0, process.targetConcurrency - sum(max(q.targetConcurrency, q.heldSlots) for each queue q))
```

A poller that cannot reserve waits on the resource it lacks. Each logical queue keeps a FIFO of pollers waiting for its worker target, since pollers of one queue are interchangeable. Every wait is bounded: a poller re-attempts at least once per second, so it re-checks worker pause and shutdown on that cadence and a missed wakeup costs at most that bound rather than a hang. The process keeps the set of queues waiting for a process slot and grants by deficit as described, so every wakeup goes to a poller that can use the freed slot and hundreds of waiting queues never stampede. A finished task frees a worker slot and a process slot, so it wakes one worker waiter of its queue and the process waiter with the largest positive deficit. `applyTargets()` wakes as many waiters per resource as the new targets admit, in deficit order for the process, and every waiter when the pause lifts; a sweep that raises an idle queue's share from zero wakes it this way. It also wakes any process waiter whose deficit is no longer positive, so that poller re-enters `reserveBatch()` and parks on its worker target instead; the process waiting set therefore holds only queues that a freed slot could serve. The deficit check in `reserveBatch()` means a poller that arrives while a needier queue waits joins the waiters instead of taking the slot, so one queue cannot monopolize freed capacity by re-polling immediately. A woken poller re-checks worker pause and shutdown before reserving.

An outstanding empty long poll holds only the slots it was granted, and all requests have a finite timeout. The design provides fair opportunities rather than preemption: an outstanding request cannot be revoked safely because a different queue became busy.

### Heartbeat polls

A queue with no share and no standby poll still polls once per heartbeat interval, and like every poll the heartbeat needs an open slot: it reserves one process slot and requests one task for it. It asks the server not to wait, so the slot is held only for the request. A heartbeat that cannot get a slot stays due and waits; when a slot opens, due heartbeats are woken first, oldest first, ahead of queues below their share. That priority costs each busy queue at most one short request per idle queue per interval, and it is what keeps idle domains alive under load: end-to-end testing showed that when freed slots always went to queues below their share, an idle domain never polled, went stale, and its next task fell back to the default queue, which no worker polled. Staying due rather than retrying an interval later matters for the idle case: an idle process decays to one slot, and when refused heartbeats were pushed a full interval the same queues kept losing to each other, two of four going 26 seconds unpolled. With every slot held for longer than the server's active-poller window, nothing polls and domain routing may lapse until a slot opens; fixed mode behaves the same way when a task type's threads are all busy, and the minimum process concurrency keeps slots available in an idle process. Nothing polls while admission is paused. The heartbeat exists for the server: domain fallback routes a task to the first domain polled within the active-poller window, every poll refreshes that record, and a worker that stopped polling an idle domain would stop being offered its tasks within ten seconds. The heartbeat also checks a zero depth against the queue itself, which protects against a backend whose size query undercounts, and keeps the poll-data views operators rely on. A heartbeat that returns a task resets the empty streak, the task holds the slot like any other, and the next sweep counts it in the queue's demand; one that returns nothing takes the empty-response path. A paused worker sends no heartbeat, because the poll loop honors the pause before reserving, so the server stops routing to a domain whose worker is paused, exactly as today. The poller of a share-less queue waits in its worker FIFO with a timed wait that ends at `nextStandbyAt` or `nextHeartbeatAt`, and every poll pushes the heartbeat one interval forward, because any poll refreshes the server's record.

### Standby polls

Shares follow depth samples, so a queue that has caught up, holding exactly its share, learns of its next task only at the next sweep, and an idle queue only at its next heartbeat. End-to-end, that was the largest latency gap against fixed threads, which keep polling every queue: tasks waited about a second to be picked up after idle, against 30 to 100 milliseconds with fixed threads, and steady-state medians were 1.3 to 2.6 times higher.

A queue whose share is fully held therefore keeps one standby poll open: a single-task long poll whose server wait is the standby timeout. The server answers it the moment a task arrives, so pickup costs one round trip. Like every poll it holds an open slot for the task it may return, but only a slot no queue's share claims: the process target minus, for each queue, the larger of its share and its held slots. A standby poll therefore never takes a slot that a queue below its share could use; when a share grows, the slots it needs are either unclaimed or held by standby polls that return within the standby timeout, which bounds the delay. Due heartbeats still go first. Under contention every slot is claimed, standby polls stop, and idle queues fall back to heartbeats.

The control tick counts one standby slot for each pollable queue that has nothing waiting beyond what it holds, so with resource headroom the process target keeps room for every queue's standby poll; an idle process settles at one slot per queue, or at the minimum process concurrency if that is higher. A standby slot holds no thread: execution threads are created when a task is submitted and retire when idle. Standby slots are not part of a queue's backlog until the poll delivers a task, which then holds the slot and counts like any other; allocation and the replacement check ignore standby slots, so standby polls never change how shares are divided.

A standby poll that runs out its wait on an empty queue is expected, not a sign of an undeliverable queue: it does not count toward the empty streak or the backoff, and the next one follows when the wait has passed, so an idle queue sends one request per standby timeout, never faster even if the server answers early. A standby poll that delivers a task is followed by the next one at once. A failed standby poll is a poll error and backs off like any other. Each standby poll also refreshes the server's active-poller window, so a queue with standby polls needs no heartbeat.

## Task polling algorithm

The existing `pollAndExecute()` and `pollTasksForWorker()` loop is retained. Its steps today are: check `worker.paused()`, apply poll filters, acquire every free permit, poll with `count` equal to the permits acquired, release the permits not matched by returned tasks, submit each task to the executor, schedule lease extension, and on an empty result sleep for the polling interval. Adaptive mode changes four things in that loop:

1. Permits come from `reserveBatch(queue)` instead of a fixed semaphore.
2. Submission and lease-extension scheduling move into a shared `dispatch(task)` so update-and-fetch replacements take the same path.
3. The empty-result sleep becomes the backoff described below; a poll error takes the same path.
4. The in-memory `tasksTobeExecuted` queue is not consulted.

```text
function pollLoop(queue):
    while running:
        honor existing worker pause and poll filters          // unchanged
        wait until queue.nextPollAllowedAt

        reservation = controller.reserveBatch(queue, now)
        if reservation is a WAIT_FOR result:
            wait in that resource's FIFO until woken or shut down,
                or, in the worker FIFO, until queue.nextHeartbeatAt or queue.nextStandbyAt,
                and at most one second before re-attempting
            continue

        pollStartedAt = now
        try:
            tasks = taskClient.batchPollTasksInDomain(          // existing client call
                taskType, domain, worker.getIdentity(),
                reservation.count,
                0 for a heartbeat, standbyPollTimeout for a standby poll, else configuredPollTimeout
            )
            set clientReceiveTime on each task                  // unchanged
            record requested count, returned count, and poll duration

            under controller lock:
                unused = reservation.count - size(tasks)
                queue.heldSlots -= unused
                process.heldSlots -= unused
                if reservation is a standby poll: queue.standbyPolls -= 1

                if tasks is empty AND reservation is a standby poll:
                    pass                                        // its wait ran out; nextStandbyAt paces the next one
                else if tasks is empty:
                    queue.consecutiveEmptyPolls += 1
                    queue.depthAtEmptyPoll = (now - queue.depthObservedAt <= depthFreshness) ? queue.depth : 0
                    queue.nextPollAllowedAt = max(now, pollStartedAt + emptyDelay(queue))
                    applyTargets()                              // the share now counts one waiting task; see "Demand and allocation"
                else:
                    queue.consecutiveEmptyPolls = 0
                    queue.nextPollAllowedAt = now
                    queue.nextStandbyAt = now

            for each returned task:
                dispatch(task)                                  // the task owns one worker slot and one process slot

        catch poll error:
            under controller lock:
                release the reservation                         // count worker and process slots
                same bookkeeping as an empty response           // streak, backoff, applyTargets()
            record poll error                                   // existing sampled logging and PollFailure event
            if fatal authentication cause: existing exit action

        finally:
            under controller lock:
                queue.outstandingPolls -= 1
            wake capacity waiters

function dispatch(task):
    if task is a replacement from update-and-fetch:             // it reuses the finishing task's slots
        under controller lock:
            queue.consecutiveEmptyPolls = 0
    try:
        future = queue.executionPool.submit(() -> executeWithSlot(task))
    catch rejected execution:
        release the task's worker and process slots             // existing behavior logs and drops the task
        return
    if task.responseTimeoutSeconds > 0 AND worker.leaseExtendEnabled():
        schedule lease extension for task against future         // existing extendLease logic
```

Request cleanup never releases slots already transferred to tasks. An error after receiving a batch must not treat already accepted tasks as an empty response or fetch a replacement batch outside the capacity budget. Use the existing task failure/reporting lifecycle for accepted work; do not introduce a new requeue protocol.

Never send a task poll with `count=0`. Waiting for capacity is local and does not count as an empty poll or server failure.

### Poll timing

```text
Empty response or poll error:
    delay = min(
        max(configuredPollInterval, 5 seconds),
        configuredPollInterval * 2^(emptyStreak - 1)
    )
    nextPollAllowedAt = max(now, pollStartedAt + delay)       // the long poll's own wait counts toward the delay
```

Apply ±20% jitter, bounded arithmetic, and a positive delay. Measuring from poll start means a long-poll timeout that already covered the delay adds no second wait, while short timeouts still back off exponentially. Protecting busy queues from idle ones is not this delay's job; allocation does that, since a queue without depth has no share and polls only as a heartbeat. Honor applicable server retry instructions. A nonempty response resets the streak. A poll error is an empty response for backoff and demand: the existing runner also treats a failed poll as an empty result, and the capped backoff keeps a process with many queues from hammering an unavailable server. Existing terminal authentication behavior remains authoritative.

After a nonempty response, poll again immediately if capacity is available. Task completion wakes capacity waiters but does not bypass an active empty-poll or server-error delay.

Keep configured long-poll timeout and poller count stable in this version. The server rejects timeouts above 5000 milliseconds; configuration validation enforces that bound. The timeout must also fit inside the HTTP read timeout with room for request/response overhead.

## Adjusting execution threads

Each logical queue has one execution pool shared by its configured pollers. Adaptive concurrency describes admitted task slots independently of thread implementation: the controller bounds concurrency with slots, and the executor exists to run the tasks those slots admit. Two executor strategies are supported, selected by the existing `useVirtualThreads` flag. Platform threads are the default; virtual threads are opt-in. The controller, resource sampler, and lease-extension executors stay small platform scheduled executors under either strategy and are never resized from task targets.

| | Platform threads (default) | Virtual threads (`useVirtualThreads`) |
|---|---|---|
| Executor | Resizable `ThreadPoolExecutor` built with the existing platform thread factory and uncaught-exception handler | `Executors.newThreadPerTaskExecutor(factory)` built with the existing virtual thread factory and uncaught-exception handler |
| Concurrency bound | Slots, and the pool size that follows them | Slots only; one virtual thread per admitted task |
| Reaction to a target change | Pool resized as described below | Nothing to resize |
| Idle threads | Retire after 60 seconds | None exist; a virtual thread ends with its task |
| Thread cost at the 256 process ceiling | 256 platform threads and stacks | Negligible; stacks live on the heap and are covered by the heap signal |
| CPU-bound tasks | Limited by the pool size the controller chooses | Still limited by carrier threads, whose count defaults to available processors; the CPU signal governs |

### Platform threads

The pool must accommodate capacity already promised, even when a target has just fallen:

```text
requiredPoolSize = max(1, queue.targetConcurrency, queue.heldSlots)

function resizePool(pool, requiredSize):
    if requiredSize > currentConfiguredSize:
        pool.setMaximumPoolSize(requiredSize)
        pool.setCorePoolSize(requiredSize)
    else if requiredSize < currentConfiguredSize:
        pool.setCorePoolSize(requiredSize)
        pool.setMaximumPoolSize(requiredSize)
```

Prepare capacity before granting a new reservation, and recompute the required size after target changes and slot releases. Core and maximum are kept equal, so the pool must be created with `allowCoreThreadTimeOut(true)` and a 60-second keep-alive for idle threads to retire. Threads are created lazily as tasks are submitted, so raising the size does not start threads immediately. On reduction, running tasks continue; threads above the new requirement retire as they become idle.

Bound the executor handoff queue by the worker adaptive maximum. Slots bound accepted work, so a task waits in the handoff queue only for the moment between one thread finishing and picking up the next; the queue is not a prefetch buffer. Thread creation or submission failure must not leak a slot, and accepted tasks must follow the existing failure/lifecycle policy.

### Virtual threads

Today the runner pools virtual threads inside `Executors.newFixedThreadPool`, which works but contradicts the JDK guidance that virtual threads are not pooled. In adaptive mode the slot controller is the concurrency bound, so the virtual-thread strategy uses a thread-per-task executor and skips pool sizing entirely. `requiredPoolSize` and `resizePool` are not used. Submission failure is handled exactly as for platform threads: the slots are released and the task follows the existing failure policy.

The virtual thread factory keeps the existing uncaught-exception handler. Thread naming uses the numbered form `Thread.ofVirtual().name(prefix, 0)` so threads are distinguishable; the current fixed-name form leaves every virtual thread named with the literal prefix pattern.

Operational notes for the opt-in:

- Carrier parallelism defaults to available processors and is tunable with `jdk.virtualThreadScheduler.parallelism`. CPU-bound workers gain nothing from virtual threads; the CPU signal still limits their concurrency.
- Before JDK 24, a virtual thread that blocks inside a `synchronized` block or method pins its carrier. Worker code and libraries that do blocking I/O under `synchronized` can exhaust carriers at high slot counts. Recommend JDK 24 or later for the opt-in and `-Djdk.tracePinnedThreads=full` for diagnosis on earlier releases.
- Thread-locals and large per-task stacks are heap allocations per virtual thread. The heap signal covers them; no separate accounting is added.
- Thread-count metrics from `ThreadMXBean` do not include virtual threads. Observability reports active slots and the executor strategy rather than thread counts.

## Execution, reporting, and replacement tasks

### Ordinary execution

The existing `processTask()`, `executeTask()`, `updateTaskResult()`, `handleException()`, and `retryOperation()` chain is retained with these changes. `updateTaskResult()` returns the slot disposition, `SLOT_TO_RELEASE` or `SLOT_TRANSFERRED`, instead of void. The `finally` block of `processTask()`, which today cancels lease extension, releases a semaphore permit, and publishes `ActiveWorkersChanged`, releases the slots only if no call in the invocation transferred them and records execution and reporting durations. `updateTaskResult()` can run twice for one task when the worker fails after a successful update, because the outer catch in `processTask()` reports again through `handleException()`. A replacement is therefore attempted at most once per invocation, and once the slots are transferred every later update in that invocation uses the plain update call.

```text
function executeWithSlot(task):
    try:
        existing processTask body:
            execute task using existing worker behavior
            report result using existing update and retry behavior
        record execution and reporting observations
    finally:
        cancel lease extension for this task                   // unchanged
        release the task's worker and process slots exactly once, unless transferred
        publish ActiveWorkersChanged                            // unchanged
        recompute required execution pool size
        wake capacity waiters
```

The slots stay held through result-reporting retries. A successfully reported `IN_PROGRESS` result releases them when the invocation finishes. Adaptive mode does not change task-result semantics, retry policy, callback behavior, or server timeout handling.

### Update and fetch

Today update-v2 is enabled process-wide by the `taskUpdateV2` system property or environment variable, the returned task is appended to the in-memory `tasksTobeExecuted` queue, the next poll drains that queue before calling the server, and lease extension for the drained task is scheduled by the poll loop. In adaptive mode the in-memory queue is not used, a builder option enables update-v2 per runner with the system property as a fallback, and the replacement is dispatched directly through `dispatch(task)` so it is scheduled for lease extension exactly like a polled task.

When update-and-fetch is enabled, the result-update request can itself accept another task. Authorize a replacement before sending that request. The replacement reuses the finishing task's slots instead of releasing them and later acquiring new ones.

```text
function reportAndMaybeContinue(task, result):
    replacementAllowed =
        NOT process.admissionPaused
        AND queue.heldSlots <= queue.targetConcurrency           // within its share, counting the finishing task
        AND process.heldSlots <= process.targetConcurrency
        AND deficit(queue) + 1 >= largest positive deficit among queues waiting for a process slot
                                                                 // the freed slot would be granted back to this queue

    if updateAndFetchEnabled AND replacementAllowed:
        nextTask = taskClient.updateTaskV2(result, taskType)   // existing call, existing retry wrapper
        record reporting outcome for current task
        if nextTask exists:
            finish current task's invocation and lease bookkeeping
            dispatch(nextTask)                                  // takes over the finishing task's slots
            return SLOT_TRANSFERRED
    else:
        taskClient.updateTask(result, taskType)                 // existing call, existing retry wrapper
        record reporting outcome for current task

    return SLOT_TO_RELEASE
```

The caller releases only on `SLOT_TO_RELEASE`; its `finally` must not release transferred slots. Dispatch continuations through the executor handoff, not recursive calls that grow the stack.

If pressure rises while an authorized update-and-fetch request is outstanding, its possible returned task remains backed by the slots. Handle that accepted task and reconsider replacement permission afterward. Using ordinary updates when permission is denied lets the grant rule take effect without a server change.

## Shutdown and worker registration lifecycle

Existing `TaskRunnerConfigurer.shutdown()` and `TaskRunner.shutdown(timeout)` behavior is retained: stop polling, cancel lease futures, shut down the lease executor, then shut down the execution pool and await termination within the grace period. Adaptive mode adds:

1. Stop granting new polls and replacement requests to that worker.
2. Let outstanding polls resolve within their finite request timeouts.
3. Process and report accepted work under existing graceful-shutdown behavior.
4. Release reservations and remove the worker's pollers from the wait FIFOs and the process waiting set.
5. The shared controller reference-counts participating runners. Resource sampling, depth sampling, the control timer, and GC notification listeners start with the first registration and stop when the count returns to zero; a later registration restarts them and recomputes the initial process target from the queues registered at that moment.

Newly registered queues enter the next allocation and compete for grants like any other. Initial process concurrency is computed once at controller start; later registration does not raise process capacity or override a reduction or admission pause. The annotated-worker executor already rebuilds the configurer on restart, which recomputes the initial target.

## Observability

Expose bounded controller observations through the existing event dispatcher and `TaskRunnerEventsListener`. Add new event types as default methods so existing `MetricsCollector` implementations compile unchanged, and extend `PrometheusMetricsCollector` with gauges for them:

- Process and worker concurrency targets, ceilings, held slots, sampled depth and its age, demand, deficit, and the execution thread strategy per queue.
- Outstanding polls, requested and returned batch sizes, empty responses, heartbeat polls, poll errors, and depth sample failures.
- CPU raw and smoothed readings; post-GC heap reading; time since last collection; per-metric support, validity, and age; derived monitoring state; admission-pause latch.
- Last reduction time, pressure evidence times, and which resume path cleared the last pause.
- Execution duration and reporting duration per task, recorded separately so reporting delays are visible; successfully reported completions, failed results, and `IN_PROGRESS` results, recorded separately. The algorithm does not use completion throughput; see deferred work for the throughput probe.
- Adjustment reason: demand growth, CPU pressure, heap pressure, demand decay, reallocation, drained resume, or ceiling.

Emit a bounded adjustment observation when targets change. Avoid per-task controller logs and task identifiers as metric labels. These observations explain the controller; control accounting itself remains direct and synchronous with slot ownership changes.

## Validation and acceptance

Use an injected monotonic clock, resource sampler, and fake Conductor client for deterministic algorithm tests. Validate task dispatch behavior against a real Conductor server without altering task-definition policies for the feature. The existing `harness` module with its simulated workers and Prometheus collector is the starting point for load tests.

| Scenario | Expected behavior |
|---|---|
| Startup with queued work | Before the first depth sweep every queue's share is one task; after it, shares follow depth; process target is min(queues, processors) |
| Sustained backlog with resource headroom | The process target grows in bounded steps toward total demand; shares follow depth; requests are sized by free slots up to the batch maximum |
| Two queues with depths 10 000 and 100, process target 32 | Shares 32 and 1; the deep queue's deficit is served first; the shallow queue gets a slot as tasks finish |
| Twenty queues, one deep, process target 32 | The deep queue holds its share; the nineteen floor shares rotate through the remaining slots in deficit, then arrival, order |
| Underfilled nonempty responses | Share unchanged; the queue polls again immediately while it has a deficit |
| Server task concurrency or rate limit | SDK respects actual delivery and does not change server policy |
| CPU at or above 90% for 3 seconds | One 25% reduction; accepted tasks continue; the next reduction needs a new 3-second run and 15 seconds |
| CPU averaging 65% with one-second samples above 75% | Growth continues; smoothed stays below 75% and samples below 90% |
| CPU jumps from 20% to 100% | First sample vetoes growth despite a low average; reduction after 3 seconds |
| Healthy JVM whose used heap reaches 95% before each young collection | No pause and no reduction; post-GC utilization stays below thresholds |
| Post-GC heap at or above 90% | Admission closes immediately; execution and reporting continue; one reduction at the next permitted tick |
| Next collection below 80% | Admission resumes at current targets; the reduction still lands if the interval had not permitted it yet; growth resumes after cooldown once post-GC is below 75% |
| Collection during a pause shows 85% | Remains paused |
| Paused, tasks drained, 30 seconds since the high collection | Resume at concurrency one; the stale reading does not re-enter the pause; growth waits for a collection below 75%; a later collection at or above 90% re-pauses |
| One high collection, then no further collections | One pause entry and one reduction; stale re-reads produce neither |
| Heap flapping across 80% and 90% every few seconds | Admission closes on each high collection; at most one reduction per 15 seconds |
| Startup before the first collection | Used heap serves as the heap reading; valid from the first tick; growth proceeds when it is below 75%; no pause can occur |
| Collection-usage fallback path | Collections detected from collector count deltas; `collectionAt` accurate to one sampling interval |
| CPU metric unsupported by the runtime | Logged once; controller runs heap-only with full growth and pressure behavior |
| Both metrics unsupported | Startup fails with a configuration error; with demand-only enabled, growth follows demand within ceilings |
| Sampler stops after healthy readings | Stale metrics freeze growth; targets hold; the pause latch is preserved |
| JVM pause or delayed sampling | No catch-up samples; smoothing and pressure timing reset after a gap longer than the freshness window |
| Hundreds of queues registered at startup | One batch depth request per second; queues without depth poll only as heartbeats; admission bounded by the process target |
| Hundreds of idle queues and few process slots | Each idle queue takes an open slot for one non-waiting single-task request per interval; the busy queue keeps the rest of the process target |
| Idle queues in an idle process | The target keeps one slot per queue, and each queue holds a standby poll on it, so a task is picked up as it arrives and poll data stays fresh. With standby polls disabled or no slot unclaimed, heartbeats take turns on the open slots, oldest due first, so every queue polls every 5 seconds, poll data stays inside the server's 10-second active-poller window and domain fallback keeps routing to each domain |
| A queue's share grows while standby polls hold the unclaimed slots | The new share uses unclaimed and freed slots at once; slots held by standby polls return within the standby timeout, or sooner when the poll delivers a task |
| Idle domain queue while other queues wait for process slots | Its due heartbeat takes the next open slot ahead of them, once per interval, keeping domain routing on this worker |
| Every slot held for longer than the active-poller window | Nothing polls; domain routing may lapse until a slot opens, as in fixed mode when a task type's threads are all busy |
| Backend reports zero depth for a non-empty queue | The heartbeat returns a task, resets the empty streak, and the next sweep gives the queue a share |
| Positive depth the server does not deliver (task limits, other consumers, in-flight counting) | After the first empty response the queue asks for one task per backoff interval; its share counts one waiting task until a delivery |
| Queue with four pollers and a share of one | One outstanding poll of one slot; the other pollers wait on the worker target |
| Freed process slot while several queues wait | The queue with the largest deficit is woken; a poller arriving with a smaller deficit joins the waiters |
| Poller count of N for one busy queue | One shared share and pool; up to N outstanding polls while the queue has a deficit |
| Busy queue's depth drops to zero | Its share falls to its held slots; no further polls until a sweep shows depth; running tasks finish normally |
| Total demand below the process target for 30 seconds | The process target shrinks to the larger of demand and half of itself, repeatedly, never below demand |
| Depth endpoint unavailable or forbidden | Every depth counts as one; all queues poll with one-slot requests at backoff pace; busy queues grow one slot per tick as they fill; growth stays resource-gated |
| Depth request slower than the freshness window | Depths count as unknown; behavior as for an unavailable endpoint until the server recovers or the freshness is raised |
| Queue with a resolved domain | Named `domain:taskType` in the same request; its depth is the domain queue's, not the default queue's |
| Paused worker with a deep queue | Its share is its held slots; it takes no new process slots and sends no heartbeat while paused |
| Empty response with a long-poll timeout at or above the computed backoff | Delay is measured from poll start, so no second wait is added |
| Poll failure or timeout | Request-owned slots are released once; backoff and demand as for an empty response |
| Configured poll timeout above 5000 ms | Rejected at configuration time |
| Update-and-fetch | Slots transfer to the replacement or are released if none returns; the replacement is scheduled for lease extension and counts as a delivery |
| Update-and-fetch while paused, over share, or with a needier queue waiting for a process slot | Ordinary update used |
| Worker fails after a successful update-and-fetch transfer | The second report uses the plain update; no second replacement is requested; the failing invocation does not release the transferred slots |
| Virtual threads opted in | Concurrency equals admitted slots with no pool resizing; each task runs on its own virtual thread; a target reduction stops admission without cancelling running tasks |
| Virtual threads with blocking I/O under `synchronized` on a pre-24 JDK | Carriers pin and throughput plateaus; CPU and demand signals stop growth rather than oversubscribing; documented as an operational limit |
| Platform threads by default | Pool follows the target lazily; idle threads retire after 60 seconds; thread count never exceeds the worker target or held slots |
| Several processes consuming one queue | Depth is shared, so each process's demand overstates its own delivery; targets run ahead of delivered work and decay returns them; local decisions coexist with server dispatch |
| Adaptive mode disabled | Existing fixed-mode behavior remains available, including N × threadCount capacity for poller count N |
| Shutdown or worker removal | No new admission; existing lifecycle drains accepted work |

Load tests cover CPU-heavy, blocking-I/O, long-running, allocation-heavy, and mixed queues, each run with platform threads and with virtual threads. Compare completion throughput, queue drain time, CPU and post-GC heap utilization, poll request rate, result-update latency, and task timeouts against representative fixed-concurrency settings. The allocation-heavy case must show that used heap crossing 90% between collections does not trigger a pause.

Acceptance requires bounded slot ownership, growth under sustained demand, shares that follow depth under contention, backoff and recovery under resource pressure, and compatibility with existing server APIs. The numeric defaults remain subject to review and calibration; no claim of optimal throughput or protection against all memory exhaustion is made.

## Deferred work

- Sublinear weighting, such as the square root of demand, if load tests show that proportional shares leave shallow queues with too little when one queue is very deep.
- Removal of the deprecation marker on `GET /tasks/queue/sizes` and a documented guarantee that it sizes any queue name it is given, including `domain:taskType`, since this design depends on both.
- Server side: make the Postgres queue size query count only waiting messages, as its all-queues query already does, so depth means the same thing on every backend.
- Throughput comparison probe. A probe that withdraws growth which did not raise throughput can help when the bottleneck is a downstream dependency that CPU and heap do not reflect. If added, the pass threshold must be relative to the increment, the sample requirement must beat task-duration variance, and the probe must not hold a queue's target for the measurement window.
- Attribution of pressure to specific queues, so that a reduction can target the queue consuming resources rather than cutting every queue by the same factor.
- Container-aware memory, native process memory, and direct-buffer accounting.
- An opt-in explicit collection request when paused and idle, for operators who prefer it to the drained-resume path.
- A retreat to initial targets when metrics stay stale, if load testing shows that freezing growth alone is insufficient protection against a failed sampler.

## Code and API references

- [SDK runner configuration](https://github.com/conductor-oss/java-sdk/blob/9e335df4fcd214c0331910dde842a9cf3bad7c36/conductor-client/src/main/java/com/netflix/conductor/client/automator/TaskRunnerConfigurer.java)
- [SDK polling, execution, and task updates](https://github.com/conductor-oss/java-sdk/blob/9e335df4fcd214c0331910dde842a9cf3bad7c36/conductor-client/src/main/java/com/netflix/conductor/client/automator/TaskRunner.java)
- [SDK task HTTP client](https://github.com/conductor-oss/java-sdk/blob/9e335df4fcd214c0331910dde842a9cf3bad7c36/conductor-client/src/main/java/com/netflix/conductor/client/http/TaskClient.java)
- [SDK per-worker property resolution](https://github.com/conductor-oss/java-sdk/blob/9e335df4fcd214c0331910dde842a9cf3bad7c36/conductor-client/src/main/java/com/netflix/conductor/client/config/PropertyFactory.java)
- [SDK annotated worker registration and poller count](https://github.com/conductor-oss/java-sdk/blob/9e335df4fcd214c0331910dde842a9cf3bad7c36/conductor-client/src/main/java/com/netflix/conductor/sdk/workflow/executor/task/AnnotatedWorkerExecutor.java)
- [SDK Prometheus metrics collector](https://github.com/conductor-oss/java-sdk/blob/9e335df4fcd214c0331910dde842a9cf3bad7c36/conductor-client-metrics/src/main/java/com/netflix/conductor/client/metrics/prometheus/PrometheusMetricsCollector.java)
- [SDK load-test harness](https://github.com/conductor-oss/java-sdk/blob/9e335df4fcd214c0331910dde842a9cf3bad7c36/harness/src/main/java/io/orkes/conductor/harness/HarnessMain.java)
- [Conductor task resource endpoints](https://github.com/conductor-oss/conductor/blob/main/rest/src/main/java/com/netflix/conductor/rest/controllers/TaskResource.java)
- [Conductor polling and dispatch eligibility](https://github.com/conductor-oss/conductor/blob/main/core/src/main/java/com/netflix/conductor/service/ExecutionService.java)
- [Conductor Postgres queue long-poll semantics](https://github.com/conductor-oss/conductor/blob/main/postgres-persistence/src/main/java/com/netflix/conductor/postgres/dao/PostgresQueueDAO.java)
- [JVM process CPU measurements](https://docs.oracle.com/en/java/javase/21/docs/api/jdk.management/com/sun/management/OperatingSystemMXBean.html)
- [JVM heap measurements](https://docs.oracle.com/en/java/javase/21/docs/api/java.management/java/lang/management/MemoryMXBean.html)
- [JVM post-collection pool usage](https://docs.oracle.com/en/java/javase/21/docs/api/java.management/java/lang/management/MemoryPoolMXBean.html#getCollectionUsage())
- [JVM collection notifications](https://docs.oracle.com/en/java/javase/21/docs/api/jdk.management/com/sun/management/GarbageCollectionNotificationInfo.html)
- [Resizable execution pools](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html)
- [Thread-per-task executors and virtual threads](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Executors.html#newThreadPerTaskExecutor(java.util.concurrent.ThreadFactory))
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491)
