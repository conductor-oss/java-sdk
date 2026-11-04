/*
 * Copyright 2026 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.client.automator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.events.dispatcher.EventDispatcher;
import com.netflix.conductor.client.events.taskrunner.AdaptiveConcurrencyChanged;
import com.netflix.conductor.client.events.taskrunner.TaskRunnerEvent;
import com.netflix.conductor.client.http.TaskClient;

import com.google.common.annotations.VisibleForTesting;

/**
 * Coordinates execution concurrency for the workers of a process. One process target bounds aggregate admission and
 * is divided among logical worker queues in proportion to their demand, the slots they hold plus their sampled queue
 * depth. The target grows under demand while JVM CPU and post-collection heap have headroom, shrinks under pressure,
 * and admission pauses when post-collection heap is near the maximum. Pollers reserve capacity before every poll.
 *
 * <p>Share one instance among all participating {@link TaskRunnerConfigurer}s of a process. The controller starts
 * its sampling threads when the first configurer starts and stops them when the last one shuts down.
 *
 * <p>See {@code design/adaptive-worker-concurrency.md}.
 */
public class AdaptiveConcurrencyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptiveConcurrencyController.class);

    /** Unset timestamp; compares as negative infinity. */
    static final long NONE = Long.MIN_VALUE;

    /** The server rejects batch polls with a longer timeout. */
    static final int MAX_POLL_TIMEOUT_MILLIS = 5000;

    private static final double GROWTH_FRACTION = 0.20;
    private static final int MAX_GROWTH_STEP = 16;
    private static final double REDUCTION_FACTOR = 0.75;
    private static final double JITTER_FRACTION = 0.20;
    private static final long CPU_PRESSURE_DURATION = TimeUnit.SECONDS.toNanos(3);
    private static final long REDUCTION_INTERVAL = TimeUnit.SECONDS.toNanos(15);
    private static final long DECAY_INTERVAL = TimeUnit.SECONDS.toNanos(30);
    private static final long DRAINED_RESUME_DELAY = TimeUnit.SECONDS.toNanos(30);
    private static final long HEAP_CEILING_HOLD = TimeUnit.SECONDS.toNanos(120);
    private static final long HEAP_GROWTH_WINDOW = TimeUnit.SECONDS.toNanos(30);
    private static final long MAX_EMPTY_POLL_DELAY = TimeUnit.SECONDS.toNanos(5);
    private static final long MAX_RESERVE_WAIT = TimeUnit.SECONDS.toNanos(1);

    enum AdjustmentReason {
        STARTUP,
        DEPTH_SAMPLE,
        EMPTY_POLL,
        DEMAND_GROWTH,
        DEMAND_DECAY,
        CPU_PRESSURE,
        HEAP_PRESSURE,
        HEAP_PAUSE,
        HEAP_RESUME,
        DRAINED_RESUME,
        REGISTRATION
    }

    private enum Wait {
        WORKER,
        PROCESS,
        HEARTBEAT_SLOT
    }

    private static final class CpuState {
        boolean supported;
        double raw;
        double smoothed;
        long observedAt = NONE;
        long pressureSince = NONE;
    }

    private static final class HeapState {
        boolean supported;
        double utilization;
        long collectionAt = NONE;
        long observedAt = NONE;
        long lastHighCollectionAt = NONE;
        /** Lowest post-collection utilization seen: the heap the process needs without tasks, or close to it. */
        double baseline = Double.NaN;
        /** Collections of the last {@link #HEAP_GROWTH_WINDOW}, oldest first. */
        final ArrayDeque<HeapReading> recent = new ArrayDeque<>();
    }

    private record HeapReading(long collectionAt, long observedAt, double utilization) {}

    private final AdaptiveConcurrencyConfig config;
    private final Supplier<ResourceSampler> resourceSamplerFactory;
    private final LongSupplier clock;
    private final boolean scheduleTimers;
    private final long metricFreshness;
    private final long depthFreshness;
    private final long heartbeatInterval;
    private final long standbyPollTimeout;

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<WorkerQueue.Key, WorkerQueue> queues = new LinkedHashMap<>();
    private final CpuState cpu = new CpuState();
    private final HeapState heap = new HeapState();

    private int registeredRunners;
    private boolean running;
    private ResourceSampler resourceSampler;
    private ScheduledExecutorService controlExecutor;
    private ScheduledExecutorService depthExecutor;
    private int depthSampleFailures;
    /** Due time of the next control tick; read and written only by the control timer. */
    private long nextControlTickAt = NONE;

    private int targetConcurrency = 1;
    private int heldSlots;
    private boolean admissionPaused;
    private long lastResumeAt = NONE;
    private long lastReductionAt = NONE;
    /** Growth limit after heap pressure: the reduced target, raised one slot per tick after the hold. */
    private int heapGrowthCeiling = Integer.MAX_VALUE;
    private long heapCeilingSince = NONE;
    private long surplusSince = NONE;
    private long waitSequence;
    private int publishedProcessTarget = -1;
    private boolean publishedAdmissionPaused;

    public AdaptiveConcurrencyController(AdaptiveConcurrencyConfig config) {
        this(config, JvmResourceSampler::new, System::nanoTime, true);
    }

    @VisibleForTesting
    AdaptiveConcurrencyController(AdaptiveConcurrencyConfig config,
                                  Supplier<ResourceSampler> resourceSamplerFactory,
                                  LongSupplier clock,
                                  boolean scheduleTimers) {
        this.config = config;
        this.resourceSamplerFactory = resourceSamplerFactory;
        this.clock = clock;
        this.scheduleTimers = scheduleTimers;
        this.metricFreshness = config.getMetricFreshness().toNanos();
        this.depthFreshness = config.getDepthFreshness().toNanos();
        this.heartbeatInterval = config.getHeartbeatInterval().toNanos();
        this.standbyPollTimeout = config.getStandbyPollTimeout().toNanos();
    }

    public AdaptiveConcurrencyConfig getConfig() {
        return config;
    }

    /**
     * @return the current process target
     */
    public int getTargetConcurrency() {
        lock.lock();
        try {
            return targetConcurrency;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return slots held by outstanding polls and accepted tasks across all workers
     */
    public int getHeldSlots() {
        lock.lock();
        try {
            return heldSlots;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return true while admission is paused by heap pressure
     */
    public boolean isAdmissionPaused() {
        lock.lock();
        try {
            return admissionPaused;
        } finally {
            lock.unlock();
        }
    }

    // ----- Registration and lifecycle -----

    /**
     * Joins or creates the logical queue identified by client scope, task type and resolved domain. Every poller
     * registers once; pollers of the same logical queue share its share and execution pool.
     */
    WorkerQueue register(TaskClient taskClient,
                         String taskType,
                         String domain,
                         int maximumConcurrency,
                         int pollingIntervalMillis,
                         BooleanSupplier pollable,
                         ThreadFactory threadFactory,
                         boolean useVirtualThreads,
                         EventDispatcher<TaskRunnerEvent> eventDispatcher) {
        lock.lock();
        try {
            var key = new WorkerQueue.Key(taskClient, taskType, StringUtils.trimToNull(domain));
            WorkerQueue queue = queues.get(key);
            if (queue == null) {
                queue = new WorkerQueue(key, maximumConcurrency, pollingIntervalMillis, pollable, threadFactory,
                        useVirtualThreads, eventDispatcher, lock.newCondition(), clock.getAsLong());
                queues.put(key, queue);
                if (running) {
                    applyTargets(clock.getAsLong(), AdjustmentReason.REGISTRATION, false);
                }
            } else {
                // The newest registration wins, so a restarted configurer replaces the one it supersedes.
                queue.updateRegistration(pollable, eventDispatcher);
            }
            queue.registrations++;
            registeredRunners++;
            return queue;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Starts sampling after a batch of registrations. The initial process target is computed from the queues
     * registered at this moment; later calls while running only re-divide the target.
     *
     * @throws IllegalStateException if neither CPU nor heap can be measured and demand-only operation is disabled
     */
    void activate() {
        lock.lock();
        try {
            if (running || registeredRunners == 0) {
                return;
            }
            ResourceSampler sampler = resourceSamplerFactory.get();
            cpu.supported = sampler.isCpuSupported();
            heap.supported = sampler.isHeapSupported();
            if (!cpu.supported) {
                LOGGER.warn("JVM process CPU is not measurable; adaptive concurrency runs without the CPU signal");
            }
            if (!heap.supported) {
                LOGGER.warn("Post-collection heap is not measurable; adaptive concurrency runs without the heap signal");
            }
            if (!cpu.supported && !heap.supported) {
                if (!config.isDemandOnlyEnabled()) {
                    sampler.close();
                    throw new IllegalStateException("Adaptive concurrency needs JVM CPU or heap metrics and neither is "
                            + "supported; enable demand-only operation to run without resource gating");
                }
                LOGGER.warn("Adaptive concurrency is running in demand-only mode without resource gating");
            }
            resourceSampler = sampler;

            int initial = config.getInitialProcessConcurrency();
            if (initial == 0) {
                initial = Math.min(queues.size(), Runtime.getRuntime().availableProcessors());
            }
            targetConcurrency = clamp(initial, config.getMinProcessConcurrency(), config.getMaxProcessConcurrency());
            admissionPaused = false;
            lastResumeAt = NONE;
            lastReductionAt = NONE;
            surplusSince = NONE;
            resetCpu();
            resetHeap();
            running = true;
            LOGGER.info("Adaptive concurrency started with process target {} for {} queues, ceiling {}",
                    targetConcurrency, queues.size(), config.getMaxProcessConcurrency());
            applyTargets(clock.getAsLong(), AdjustmentReason.STARTUP, true);
            if (scheduleTimers) {
                startTimers();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes one poller registration.
     *
     * @return true if this was the last registration of the queue, so its execution pool may be shut down
     */
    boolean deregister(WorkerQueue queue) {
        lock.lock();
        try {
            queue.registrations--;
            registeredRunners = Math.max(0, registeredRunners - 1);
            boolean last = queue.registrations <= 0;
            if (last) {
                queues.remove(queue.key, queue);
            }
            queue.slotAvailable.signalAll();
            if (registeredRunners == 0) {
                stop();
            } else if (last && running) {
                applyTargets(clock.getAsLong(), AdjustmentReason.REGISTRATION, false);
            }
            return last;
        } finally {
            lock.unlock();
        }
    }

    private void stop() {
        if (!running) {
            return;
        }
        running = false;
        if (controlExecutor != null) {
            controlExecutor.shutdownNow();
            controlExecutor = null;
        }
        if (depthExecutor != null) {
            depthExecutor.shutdownNow();
            depthExecutor = null;
        }
        if (resourceSampler != null) {
            resourceSampler.close();
            resourceSampler = null;
        }
        LOGGER.info("Adaptive concurrency stopped");
    }

    private void startTimers() {
        controlExecutor = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory("adaptive-concurrency-control-%d"));
        depthExecutor = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory("adaptive-concurrency-depth-%d"));
        controlExecutor.scheduleWithFixedDelay(guarded(this::sampleResources, "resource sampling"),
                0, config.getResourceSampleInterval().toNanos(), TimeUnit.NANOSECONDS);
        // A periodic task survives a failed run, where a tick that reschedules itself stops for good if the
        // rescheduling fails, as it did when the heap ran out. The jittered due time keeps processes apart.
        long controlInterval = config.getControlInterval().toNanos();
        nextControlTickAt = clock.getAsLong() + jitter(controlInterval);
        controlExecutor.scheduleWithFixedDelay(guarded(this::controlTickIfDue, "control tick"),
                controlInterval / 20, controlInterval / 20, TimeUnit.NANOSECONDS);
        depthExecutor.scheduleWithFixedDelay(guarded(this::sampleDepths, "queue depth sampling"),
                0, config.getDepthSampleInterval().toNanos(), TimeUnit.NANOSECONDS);
    }

    private void controlTickIfDue() {
        long now = clock.getAsLong();
        if (now < nextControlTickAt) {
            return;
        }
        nextControlTickAt = now + jitter(config.getControlInterval().toNanos());
        controlTick();
    }

    // ----- Poll path -----

    /**
     * Reserves capacity for one poll, waiting up to one second for a slot. A poller whose share is held is granted a
     * single-task standby poll on capacity no share claims, or else a heartbeat when one is due and a slot is open.
     *
     * @param active checked before every attempt; the wait ends when it returns false
     * @return the reservation, or null if none was granted within the wait
     */
    SlotReservation reserve(WorkerQueue queue, BooleanSupplier active) {
        lock.lock();
        boolean waitingForProcess = false;
        boolean waitingForHeartbeat = false;
        try {
            long remaining = MAX_RESERVE_WAIT;
            while (active.getAsBoolean() && queue.registrations > 0) {
                long now = clock.getAsLong();
                Object outcome = tryReserveLocked(queue, now);
                if (outcome instanceof SlotReservation reservation) {
                    return reservation;
                }
                boolean heartbeatWait = outcome == Wait.HEARTBEAT_SLOT;
                if (heartbeatWait != waitingForHeartbeat) {
                    queue.heartbeatWaiters += heartbeatWait ? 1 : -1;
                    waitingForHeartbeat = heartbeatWait;
                }
                boolean processWait = outcome == Wait.PROCESS;
                if (processWait != waitingForProcess) {
                    if (processWait) {
                        if (queue.processWaiters++ == 0) {
                            queue.processWaitSequence = ++waitSequence;
                        }
                    } else {
                        queue.processWaiters--;
                    }
                    waitingForProcess = processWait;
                }
                long waitNanos = remaining;
                if (outcome == Wait.WORKER && queue.nextHeartbeatAt > now) {
                    waitNanos = Math.min(waitNanos, queue.nextHeartbeatAt - now);
                }
                if (outcome == Wait.WORKER && standbyPollTimeout > 0 && queue.nextStandbyAt > now) {
                    waitNanos = Math.min(waitNanos, queue.nextStandbyAt - now);
                }
                if (waitNanos <= 0) {
                    return null;
                }
                long waitStarted = System.nanoTime();
                queue.waiters++;
                try {
                    queue.slotAvailable.awaitNanos(waitNanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                } finally {
                    queue.waiters--;
                }
                remaining -= System.nanoTime() - waitStarted;
            }
            return null;
        } finally {
            if (waitingForProcess) {
                queue.processWaiters--;
            }
            if (waitingForHeartbeat) {
                queue.heartbeatWaiters--;
            }
            lock.unlock();
        }
    }

    /**
     * One reservation attempt under the lock.
     *
     * @return a {@link SlotReservation}, or the resource the poller must wait for
     */
    @VisibleForTesting
    Object tryReserve(WorkerQueue queue, long now) {
        lock.lock();
        try {
            return tryReserveLocked(queue, now);
        } finally {
            lock.unlock();
        }
    }

    private Object tryReserveLocked(WorkerQueue queue, long now) {
        // Rule: a queue is polled only with an open slot for every task the poll may return.
        if (!running || admissionPaused) {
            return Wait.PROCESS;
        }
        int processAvailable = Math.max(0, targetConcurrency - heldSlots);
        int workerAvailable = Math.max(0, queue.deficit());
        if (workerAvailable == 0) {
            SlotReservation standby = tryReserveStandbyLocked(queue, now);
            return standby != null ? standby : tryReserveHeartbeatLocked(queue, now, processAvailable);
        }
        // Due heartbeats go first: one slot each, briefly, at most once per interval per queue.
        int forHeartbeats = dueHeartbeatWaiters(now, queue);
        if (processAvailable <= forHeartbeats) {
            WorkerQueue heartbeat = oldestDueHeartbeatWaiter(now, queue);
            if (heartbeat != null) {
                heartbeat.slotAvailable.signal();
            }
            return Wait.PROCESS;
        }
        WorkerQueue needier = bestProcessWaiter(queue);
        if (needier != null && outranks(needier, queue)) {
            // The slot belongs to a queue further below its share.
            needier.slotAvailable.signal();
            return Wait.PROCESS;
        }
        int count = Math.min(Math.min(workerAvailable, processAvailable - forHeartbeats), config.getMaxPollBatch());
        queue.heldSlots += count;
        heldSlots += count;
        queue.reservedPollSlots += count;
        queue.outstandingPolls++;
        queue.nextHeartbeatAt = now + heartbeatInterval;
        queue.resizePool();
        if (targetConcurrency > heldSlots) {
            // Capacity remains: pass the turn on so it is not left idle until a waiter's timeout.
            if (queue.deficit() > 0 && queue.waiters > 0) {
                queue.slotAvailable.signal();
            }
            WorkerQueue next = bestProcessWaiter(queue);
            if (next != null) {
                next.slotAvailable.signal();
            }
        }
        return new SlotReservation(queue, count, SlotReservation.Kind.SHARE);
    }

    /**
     * Share held: keep one single-task long poll open so a task is picked up as it arrives, instead of at the next
     * depth sample. It uses only a slot that no queue's share claims, after due heartbeats, so it never delays a
     * queue's share by more than the standby timeout. Control counts one standby slot per queue with nothing waiting
     * as demand, so the process target leaves room for them while resources allow.
     */
    private SlotReservation tryReserveStandbyLocked(WorkerQueue queue, long now) {
        // Like every poll it also needs an open process slot: unclaimed slots are computed over registered
        // queues, which can overstate free capacity while a deregistered worker's tasks drain.
        if (standbyPollTimeout == 0
                || queue.outstandingPolls > 0
                || now < queue.nextStandbyAt
                || queue.heldSlots >= queue.maximumConcurrency
                || heldSlots >= targetConcurrency
                || unclaimedSlots() <= dueHeartbeatWaiters(now, queue)) {
            return null;
        }
        queue.heldSlots++;
        heldSlots++;
        queue.reservedPollSlots++;
        queue.outstandingPolls++;
        queue.standbyPolls++;
        // It refreshes the server's active-poller window, as a heartbeat would.
        queue.nextHeartbeatAt = now + heartbeatInterval;
        queue.nextStandbyAt = now + standbyPollTimeout;
        queue.resizePool();
        return new SlotReservation(queue, 1, SlotReservation.Kind.STANDBY);
    }

    /**
     * @return process slots neither held nor promised to a queue's share
     */
    private int unclaimedSlots() {
        long claimed = 0;
        for (WorkerQueue candidate : queues.values()) {
            claimed += Math.max(candidate.targetConcurrency, candidate.heldSlots);
        }
        return (int) Math.max(0, targetConcurrency - claimed);
    }

    /**
     * No share, or the share is already held: only a heartbeat, at most once per interval. Like every poll it needs an
     * open slot, and it requests one task for it without waiting, so it holds the slot only for the request. A due
     * heartbeat stays due until it gets a slot and goes first when one opens, so idle queues keep the server's
     * active-poller window, which domain routing depends on, whenever slots turn over. With every slot held it waits.
     */
    private Object tryReserveHeartbeatLocked(WorkerQueue queue, long now, int processAvailable) {
        if (queue.outstandingPolls > 0 || now < queue.nextHeartbeatAt) {
            return Wait.WORKER;
        }
        if (processAvailable == 0 || queue.heldSlots >= queue.maximumConcurrency) {
            return Wait.HEARTBEAT_SLOT;
        }
        WorkerQueue older = oldestDueHeartbeatWaiter(now, queue);
        if (older != null && older.nextHeartbeatAt < queue.nextHeartbeatAt && processAvailable == 1) {
            older.slotAvailable.signal();
            return Wait.HEARTBEAT_SLOT;
        }
        queue.heldSlots++;
        heldSlots++;
        queue.reservedPollSlots++;
        queue.outstandingPolls++;
        queue.nextHeartbeatAt = now + heartbeatInterval;
        queue.resizePool();
        return new SlotReservation(queue, 1, SlotReservation.Kind.HEARTBEAT);
    }

    private boolean isDueHeartbeatWaiter(WorkerQueue queue, long now) {
        return queue.heartbeatWaiters > 0
                && queue.outstandingPolls == 0
                && queue.nextHeartbeatAt <= now
                && queue.heldSlots < queue.maximumConcurrency;
    }

    private int dueHeartbeatWaiters(long now, WorkerQueue exclude) {
        int count = 0;
        for (WorkerQueue candidate : queues.values()) {
            if (candidate != exclude && isDueHeartbeatWaiter(candidate, now)) {
                count++;
            }
        }
        return count;
    }

    private WorkerQueue oldestDueHeartbeatWaiter(long now, WorkerQueue exclude) {
        WorkerQueue oldest = null;
        for (WorkerQueue candidate : queues.values()) {
            if (candidate != exclude && isDueHeartbeatWaiter(candidate, now)
                    && (oldest == null || candidate.nextHeartbeatAt < oldest.nextHeartbeatAt)) {
                oldest = candidate;
            }
        }
        return oldest;
    }

    /**
     * Settles a poll: slots not matched by returned tasks are released, the returned tasks keep theirs.
     */
    void completePoll(SlotReservation reservation, int delivered, long pollStartedAt) {
        settlePoll(reservation, delivered, pollStartedAt, false);
    }

    /**
     * Settles a failed poll. A poll error is an empty response for backoff and demand.
     */
    void failPoll(SlotReservation reservation, long pollStartedAt) {
        settlePoll(reservation, 0, pollStartedAt, true);
    }

    private void settlePoll(SlotReservation reservation, int delivered, long pollStartedAt, boolean failed) {
        lock.lock();
        try {
            WorkerQueue queue = reservation.queue();
            long now = clock.getAsLong();
            boolean hadTaskSlots = queue.heldSlots - queue.reservedPollSlots > 0;
            int unused = reservation.count() - delivered;
            queue.heldSlots -= unused;
            heldSlots -= unused;
            queue.reservedPollSlots -= reservation.count();
            queue.outstandingPolls--;
            if (reservation.standby()) {
                queue.standbyPolls--;
            }
            if (!hadTaskSlots && delivered > 0) {
                // The slot-age observation restarts when an idle queue accepts work again.
                queue.lastTaskSlotReleaseAt = now;
            }
            guardCounters(queue);
            // A standby wait that ran out on an empty queue is expected: nextStandbyAt paces it, not the backoff.
            boolean standbyExpired = reservation.standby() && delivered == 0 && !failed;
            if (delivered == 0 && !standbyExpired) {
                queue.consecutiveEmptyPolls++;
                queue.depthAtEmptyPoll = elapsed(now, queue.depthObservedAt) <= depthFreshness ? queue.depth : 0;
                queue.nextPollAllowedAt = Math.max(now, pollStartedAt + emptyPollDelay(queue));
                if (running) {
                    // The share now counts at most one waiting task until a delivery.
                    applyTargets(now, AdjustmentReason.EMPTY_POLL, false);
                }
            } else if (delivered > 0) {
                queue.consecutiveEmptyPolls = 0;
                queue.nextPollAllowedAt = now;
                queue.nextStandbyAt = now;
            }
            if (unused > 0) {
                queue.resizePool();
                wakeAfterRelease(queue);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Releases the slot owned by a finished or rejected task.
     */
    void releaseTaskSlot(WorkerQueue queue) {
        lock.lock();
        try {
            queue.heldSlots--;
            heldSlots--;
            queue.lastTaskSlotReleaseAt = clock.getAsLong();
            guardCounters(queue);
            queue.resizePool();
            wakeAfterRelease(queue);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Invariant backstop: a counter driven negative by a release bug would inflate deficits and over-admit, the one
     * direction the design forbids, so a breach is clamped and reported instead of compounding silently.
     */
    private void guardCounters(WorkerQueue queue) {
        if (queue.heldSlots < 0 || queue.outstandingPolls < 0 || queue.standbyPolls < 0
                || queue.reservedPollSlots < 0 || heldSlots < 0) {
            LOGGER.error("Slot accounting breach for {}: heldSlots={}, outstandingPolls={}, standbyPolls={}, "
                            + "reservedPollSlots={}, processHeldSlots={}; clamping to zero",
                    queue.queueName, queue.heldSlots, queue.outstandingPolls, queue.standbyPolls,
                    queue.reservedPollSlots, heldSlots);
            queue.heldSlots = Math.max(0, queue.heldSlots);
            queue.outstandingPolls = Math.max(0, queue.outstandingPolls);
            queue.standbyPolls = Math.max(0, queue.standbyPolls);
            queue.reservedPollSlots = Math.max(0, queue.reservedPollSlots);
            heldSlots = Math.max(0, heldSlots);
        }
    }

    /**
     * @return true if a finishing task may keep its slot for a task returned by update-and-fetch
     */
    boolean isReplacementAllowed(WorkerQueue queue) {
        lock.lock();
        try {
            if (!running || admissionPaused) {
                return false;
            }
            if (queue.heldSlots - queue.standbyPolls > queue.targetConcurrency || heldSlots > targetConcurrency) {
                return false;
            }
            // The freed slot would be granted back to this queue anyway.
            WorkerQueue needier = bestProcessWaiter(queue);
            return needier == null || queue.deficit() + 1 >= needier.deficit();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Records a task delivered by update-and-fetch; it reuses the finishing task's slot.
     */
    void recordReplacement(WorkerQueue queue) {
        lock.lock();
        try {
            queue.consecutiveEmptyPolls = 0;
            // A replacement means a task just finished and handed over its slot: release activity for the slot age.
            queue.lastTaskSlotReleaseAt = clock.getAsLong();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return nanoseconds until the queue may poll again after an empty response or poll error
     */
    long pollDelayNanos(WorkerQueue queue) {
        lock.lock();
        try {
            return Math.max(0, queue.nextPollAllowedAt - clock.getAsLong());
        } finally {
            lock.unlock();
        }
    }

    long now() {
        return clock.getAsLong();
    }

    // ----- Monitoring -----

    @VisibleForTesting
    void sampleResources() {
        ResourceSampler sampler;
        boolean cpuSupported;
        boolean heapSupported;
        lock.lock();
        try {
            sampler = resourceSampler;
            cpuSupported = cpu.supported;
            heapSupported = heap.supported;
        } finally {
            lock.unlock();
        }
        if (sampler == null) {
            return;
        }
        // MXBean calls run outside the lock; each metric is attempted independently.
        double cpuReading = cpuSupported ? sampler.sampleCpu() : Double.NaN;
        ResourceSampler.HeapSample heapReading = heapSupported ? sampler.sampleHeap() : null;

        lock.lock();
        try {
            if (!running) {
                return;
            }
            long now = clock.getAsLong();
            if (cpu.supported) {
                if (Double.isFinite(cpuReading) && cpuReading >= 0 && cpuReading <= 1) {
                    if (elapsed(now, cpu.observedAt) > metricFreshness) {
                        cpu.smoothed = cpuReading;
                        cpu.pressureSince = NONE;
                    } else {
                        double alpha = config.getCpuSmoothingAlpha();
                        cpu.smoothed = alpha * cpuReading + (1 - alpha) * cpu.smoothed;
                    }
                    cpu.raw = cpuReading;
                    cpu.observedAt = now;
                    if (cpuReading >= config.getCpuPressureThreshold()) {
                        if (cpu.pressureSince == NONE) {
                            cpu.pressureSince = now;
                        }
                    } else {
                        cpu.pressureSince = NONE;
                    }
                } else {
                    cpu.observedAt = NONE;
                    cpu.pressureSince = NONE;
                }
            }
            if (heap.supported) {
                if (heapReading != null) {
                    heap.utilization = heapReading.utilization();
                    heap.collectionAt = heapReading.collectionAtNanos();
                    heap.observedAt = now;
                    if (heap.collectionAt != NONE && heap.utilization >= config.getHeapPauseThreshold()) {
                        heap.lastHighCollectionAt = heap.collectionAt;
                    }
                    if (heap.collectionAt != NONE) {
                        heap.baseline = Double.isNaN(heap.baseline)
                                ? heap.utilization
                                : Math.min(heap.baseline, heap.utilization);
                        HeapReading last = heap.recent.peekLast();
                        if (last == null || last.collectionAt() != heap.collectionAt) {
                            heap.recent.addLast(new HeapReading(heap.collectionAt, now, heap.utilization));
                        }
                    }
                } else {
                    heap.observedAt = NONE;
                }
            }
            reconcileHeapPause(now);
        } finally {
            lock.unlock();
        }
    }

    private void reconcileHeapPause(long now) {
        boolean heapFresh = isFresh(heap.supported, heap.observedAt, now, metricFreshness);
        if (!admissionPaused) {
            if (heapFresh
                    && heap.collectionAt != NONE
                    && heap.utilization >= config.getHeapPauseThreshold()
                    && heap.collectionAt > lastResumeAt) {
                admissionPaused = true;
                limitHeapGrowth((int) Math.floor(REDUCTION_FACTOR * Math.max(targetConcurrency, heldSlots)), now);
                LOGGER.warn("Post-collection heap at {}%; pausing task acquisition",
                        Math.round(heap.utilization * 100));
                applyTargets(now, AdjustmentReason.HEAP_PAUSE, false);
            }
            return;
        }

        boolean drained;
        if (heapFresh && heap.utilization < config.getHeapResumeThreshold()) {
            drained = false;
        } else if (heldSlots == 0 && elapsed(now, heap.lastHighCollectionAt) >= DRAINED_RESUME_DELAY) {
            // Idle with a stale estimate: resume at one so ordinary allocation triggers a fresh collection.
            drained = true;
            targetConcurrency = 1;
        } else {
            return;
        }
        admissionPaused = false;
        // Pause re-entry needs a collection newer than this.
        lastResumeAt = drained ? now : heap.collectionAt;
        LOGGER.info("Resuming task acquisition at process target {}", targetConcurrency);
        applyTargets(now, drained ? AdjustmentReason.DRAINED_RESUME : AdjustmentReason.HEAP_RESUME, false);
    }

    @VisibleForTesting
    void controlTick() {
        lock.lock();
        try {
            if (!running) {
                return;
            }
            long now = clock.getAsLong();
            reconcileHeapPause(now);

            boolean cpuFresh = isFresh(cpu.supported, cpu.observedAt, now, metricFreshness);
            boolean heapFresh = isFresh(heap.supported, heap.observedAt, now, metricFreshness);
            boolean cpuPressure = cpuFresh
                    && cpu.pressureSince != NONE
                    && now - cpu.pressureSince >= CPU_PRESSURE_DURATION;
            boolean heapPressure = heap.supported && heap.lastHighCollectionAt > lastReductionAt;
            boolean reductionIntervalElapsed = elapsed(now, lastReductionAt) >= REDUCTION_INTERVAL;

            if ((cpuPressure || heapPressure) && reductionIntervalElapsed) {
                targetConcurrency = Math.max(1, (int) Math.floor(REDUCTION_FACTOR * targetConcurrency));
                lastReductionAt = now;
                if (heapPressure) {
                    limitHeapGrowth(targetConcurrency, now);
                }
                if (cpu.pressureSince != NONE) {
                    // The next reduction needs a new run of pressure.
                    cpu.pressureSince = now;
                }
                applyTargets(now, cpuPressure ? AdjustmentReason.CPU_PRESSURE : AdjustmentReason.HEAP_PRESSURE, false);
                return;
            }

            long totalDemand = totalDemand(now);
            AdjustmentReason reason = null;
            if (totalDemand >= targetConcurrency) {
                surplusSince = NONE;
            } else if (surplusSince == NONE) {
                surplusSince = now;
            } else if (now - surplusSince >= DECAY_INTERVAL) {
                long halved = (targetConcurrency + 1) / 2;
                long decayed = Math.max(config.getMinProcessConcurrency(), Math.max(totalDemand, halved));
                // Decay only lowers: a target that pressure took below the minimum stays there until growth.
                targetConcurrency = (int) Math.min(targetConcurrency, decayed);
                surplusSince = now;
                reason = AdjustmentReason.DEMAND_DECAY;
            }

            double heapForGrowth = heapGrowthUtilization(now);
            boolean growthAllowed = totalDemand > targetConcurrency
                    && !admissionPaused
                    && reductionIntervalElapsed
                    && (!cpu.supported || (cpuFresh
                            && cpu.smoothed < config.getGrowthThreshold()
                            && cpu.raw < config.getCpuPressureThreshold()))
                    && (!heap.supported || (heapFresh && heapForGrowth < config.getGrowthThreshold()));
            relaxHeapGrowthCeiling(now);
            int increment = growthAllowed ? growthIncrement(heapFresh, heapForGrowth) : 0;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Control tick: target {}, demand {}, held {} ({} standby), cpu {}/{}, heap {} (peak {}, "
                                + "baseline {}, fresh {}), ceiling {}, increment {}",
                        targetConcurrency, totalDemand, heldSlots, standbyPolls(), cpu.raw, cpu.smoothed,
                        heap.utilization, heapForGrowth, heap.baseline, heapFresh, heapGrowthCeiling, increment);
            }
            if (increment > 0) {
                long grown = Math.min(totalDemand, (long) targetConcurrency + increment);
                targetConcurrency = (int) Math.min(config.getMaxProcessConcurrency(), grown);
                reason = AdjustmentReason.DEMAND_GROWTH;
            }
            if (reason != null) {
                applyTargets(now, reason, false);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Post-collection utilization for growth decisions: the highest of the last 30 seconds. A single reading depends
     * on which task payloads a collection happened to find live or promote, and end to end a low one let the target
     * grow into an OutOfMemoryError seconds later.
     */
    private double heapGrowthUtilization(long now) {
        while (!heap.recent.isEmpty() && now - heap.recent.peekFirst().observedAt() > HEAP_GROWTH_WINDOW) {
            heap.recent.removeFirst();
        }
        double peak = heap.utilization;
        for (HeapReading reading : heap.recent) {
            peak = Math.max(peak, reading.utilization());
        }
        return peak;
    }

    /**
     * The growth step: a fraction of the target, limited by the ceiling left by heap pressure and by the slots whose
     * observed heap cost still fits under the growth threshold.
     */
    private int growthIncrement(boolean heapFresh, double heapUtilization) {
        int increment = clamp((int) Math.ceil(GROWTH_FRACTION * targetConcurrency), 1, MAX_GROWTH_STEP);
        increment = (int) Math.min(increment, (long) heapGrowthCeiling - targetConcurrency);
        // Standby slots hold no task, so they carry none of the heap.
        int taskSlots = heldSlots - standbyPolls();
        if (heap.supported && heapFresh && taskSlots > 0 && heapUtilization > heap.baseline) {
            double perSlot = (heapUtilization - heap.baseline) / taskSlots;
            int room = (int) Math.floor((config.getGrowthThreshold() - heapUtilization) / perSlot);
            increment = Math.min(increment, room);
        }
        return Math.max(0, increment);
    }

    private int standbyPolls() {
        int total = 0;
        for (WorkerQueue queue : queues.values()) {
            total += queue.standbyPolls;
        }
        return total;
    }

    private void limitHeapGrowth(int ceiling, long now) {
        heapGrowthCeiling = Math.max(1, Math.min(heapGrowthCeiling, ceiling));
        heapCeilingSince = now;
    }

    private void relaxHeapGrowthCeiling(long now) {
        if (heapGrowthCeiling == Integer.MAX_VALUE || elapsed(now, heapCeilingSince) < HEAP_CEILING_HOLD) {
            return;
        }
        heapGrowthCeiling = heapGrowthCeiling >= config.getMaxProcessConcurrency()
                ? Integer.MAX_VALUE
                : heapGrowthCeiling + 1;
    }

    /**
     * Samples the depth of every logical queue with one {@code GET /tasks/queue/sizes} request per client scope.
     * Worker pause and poll filters are evaluated here, outside the lock; a worker that would not poll has a depth
     * of zero.
     */
    @VisibleForTesting
    void sampleDepths() {
        List<WorkerQueue> snapshot;
        lock.lock();
        try {
            if (!running) {
                return;
            }
            snapshot = new ArrayList<>(queues.values());
        } finally {
            lock.unlock();
        }

        Map<WorkerQueue, Boolean> pollable = new HashMap<>();
        Map<TaskClient, List<WorkerQueue>> byClient = new LinkedHashMap<>();
        for (WorkerQueue queue : snapshot) {
            boolean canPoll = queue.isPollable();
            pollable.put(queue, canPoll);
            if (canPoll) {
                byClient.computeIfAbsent(queue.key.taskClient(), c -> new ArrayList<>()).add(queue);
            }
        }
        Map<WorkerQueue, Integer> sizes = new HashMap<>();
        for (Map.Entry<TaskClient, List<WorkerQueue>> entry : byClient.entrySet()) {
            var names = new LinkedHashSet<String>();
            entry.getValue().forEach(queue -> names.add(queue.queueName));
            try {
                Map<String, Integer> result = entry.getKey().getQueueSizesForTasks(new ArrayList<>(names));
                if (result == null) {
                    throw new IllegalStateException("Queue sizes response was empty");
                }
                for (WorkerQueue queue : entry.getValue()) {
                    Integer size = result.get(queue.queueName);
                    sizes.put(queue, size == null ? 0 : size);
                }
                depthSampleFailures = 0;
            } catch (Exception e) {
                if (depthSampleFailures < 3 || depthSampleFailures % 60 == 0) {
                    LOGGER.warn("Unable to sample queue depth for {}; depths count as unknown", names, e);
                }
                depthSampleFailures++;
            }
        }

        lock.lock();
        try {
            long now = clock.getAsLong();
            for (WorkerQueue queue : snapshot) {
                queue.pollableAtSample = pollable.get(queue);
                if (!pollable.get(queue)) {
                    queue.depth = 0;
                    queue.depthObservedAt = now;
                } else if (sizes.containsKey(queue)) {
                    queue.depth = Math.max(0, sizes.get(queue));
                    queue.depthObservedAt = now;
                    if (queue.consecutiveEmptyPolls > 0 && queue.depth > queue.depthAtEmptyPoll) {
                        // Tasks arrived since the last empty response: poll now rather than after the backoff.
                        queue.nextPollAllowedAt = Math.min(queue.nextPollAllowedAt, now);
                    }
                } else {
                    queue.depthObservedAt = NONE;
                }
            }
            if (running) {
                applyTargets(now, AdjustmentReason.DEPTH_SAMPLE, true);
            }
        } finally {
            lock.unlock();
        }
    }

    // ----- Allocation -----

    /**
     * @return the queue's backlog: the slots it holds for tasks plus its sampled depth, uncapped. Standby slots do
     *     not count until they deliver a task.
     */
    private long backlog(WorkerQueue queue, long now) {
        // Unknown depth counts as one.
        long depth = elapsed(now, queue.depthObservedAt) <= depthFreshness ? queue.depth : 1;
        if (queue.consecutiveEmptyPolls > 0) {
            // The server just delivered nothing from this depth: count one task, plus any that arrived since.
            depth = Math.min(depth, 1 + Math.max(0, depth - queue.depthAtEmptyPoll));
        }
        return Math.max(0, queue.heldSlots - queue.standbyPolls) + depth;
    }

    /**
     * @return the slots the queue could use: its backlog, capped at its maximum concurrency
     */
    private long demand(WorkerQueue queue, long now) {
        return Math.min(queue.maximumConcurrency, backlog(queue, now));
    }

    /**
     * @return one for a pollable queue with nothing waiting beyond the slots it holds and room for one more task: the
     *     slot of its standby poll
     */
    private int standbyDemand(WorkerQueue queue, long now) {
        if (standbyPollTimeout == 0 || !queue.pollableAtSample) {
            return 0;
        }
        long held = Math.max(0, queue.heldSlots - queue.standbyPolls);
        return backlog(queue, now) == held && held < queue.maximumConcurrency ? 1 : 0;
    }

    /**
     * @return the slots all queues could use, including a standby slot for each queue that has nothing waiting
     */
    private long totalDemand(long now) {
        long total = 0;
        for (WorkerQueue queue : queues.values()) {
            total += demand(queue, now) + standbyDemand(queue, now);
        }
        return total;
    }

    /**
     * Divides the process target among queues in proportion to backlog, so a deeper queue gets more slots even
     * beyond its maximum concurrency. A share never exceeds the queue's demand: a queue that reaches it keeps
     * exactly its demand and the rest is re-divided among the others. Every queue with demand gets at least one
     * slot, then largest-remainder rounding keeps the shares summing to the target.
     */
    private void allocate(long now) {
        WorkerQueue[] all = queues.values().toArray(new WorkerQueue[0]);
        int n = all.length;
        if (n == 0) {
            return;
        }
        long[] backlogs = new long[n];
        long[] demands = new long[n];
        long totalDemand = 0;
        for (int i = 0; i < n; i++) {
            backlogs[i] = backlog(all[i], now);
            demands[i] = Math.min(all[i].maximumConcurrency, backlogs[i]);
            totalDemand += demands[i];
        }
        if (totalDemand <= targetConcurrency) {
            for (int i = 0; i < n; i++) {
                all[i].targetConcurrency = (int) demands[i];
            }
            return;
        }
        int[] shares = new int[n];
        double[] quotas = new double[n];
        boolean[] capped = new boolean[n];
        long remaining = targetConcurrency;
        boolean cappedAny = true;
        while (cappedAny) {
            // Water-filling: queues whose proportional quota reaches their demand are fixed at it.
            cappedAny = false;
            long weight = 0;
            for (int i = 0; i < n; i++) {
                if (!capped[i]) {
                    weight += backlogs[i];
                }
            }
            for (int i = 0; i < n; i++) {
                if (capped[i] || demands[i] == 0) {
                    continue;
                }
                quotas[i] = (double) remaining * backlogs[i] / weight;
                if (quotas[i] >= demands[i]) {
                    capped[i] = true;
                    shares[i] = (int) demands[i];
                    remaining -= demands[i];
                    cappedAny = true;
                }
            }
        }
        long leftover = remaining;
        for (int i = 0; i < n; i++) {
            if (!capped[i] && demands[i] > 0) {
                shares[i] = (int) Math.floor(quotas[i]);
                leftover -= shares[i];
            }
        }
        // Floors first: every queue with demand gets a slot before remainders are rounded up.
        for (int i = 0; i < n && leftover > 0; i++) {
            if (!capped[i] && demands[i] > 0 && shares[i] == 0) {
                shares[i] = 1;
                leftover--;
            }
        }
        Integer[] order = new Integer[n];
        Arrays.setAll(order, i -> i);
        Arrays.sort(order, Comparator.comparingDouble((Integer i) -> quotas[i] - Math.floor(quotas[i])).reversed());
        for (int k = 0; k < n && leftover > 0; k++) {
            int i = order[k];
            if (!capped[i] && demands[i] > 0 && shares[i] < demands[i] && shares[i] == Math.floor(quotas[i])) {
                shares[i]++;
                leftover--;
            }
        }
        for (int i = 0; i < n; i++) {
            all[i].targetConcurrency = demands[i] > 0 ? Math.max(1, shares[i]) : 0;
        }
    }

    /**
     * The single exit for target and admission changes: re-divides the process target, resizes execution pools,
     * publishes observations and wakes pollers that the new targets admit.
     */
    private void applyTargets(long now, AdjustmentReason reason, boolean publishAll) {
        allocate(now);
        boolean processChanged = targetConcurrency != publishedProcessTarget
                || admissionPaused != publishedAdmissionPaused;
        boolean resumed = publishedAdmissionPaused && !admissionPaused;
        for (WorkerQueue queue : queues.values()) {
            boolean changed = queue.targetConcurrency != queue.publishedTarget;
            queue.resizePool();
            if (changed || processChanged || publishAll) {
                int taskSlots = queue.heldSlots - queue.reservedPollSlots;
                long taskSlotAgeSeconds = taskSlots > 0
                        ? TimeUnit.NANOSECONDS.toSeconds(Math.max(0, now - queue.lastTaskSlotReleaseAt))
                        : 0;
                queue.eventDispatcher().publish(new AdaptiveConcurrencyChanged(
                        queue.taskType(),
                        queue.domain(),
                        queue.targetConcurrency,
                        queue.heldSlots,
                        taskSlotAgeSeconds,
                        queue.depth,
                        targetConcurrency,
                        heldSlots,
                        admissionPaused,
                        reason.name()));
                queue.publishedTarget = queue.targetConcurrency;
            }
            if (resumed) {
                queue.slotAvailable.signalAll();
            } else if ((changed || processChanged) && queue.waiters > 0) {
                int wakeups = Math.min(queue.waiters, Math.max(0, queue.deficit()));
                for (int i = 0; i < wakeups; i++) {
                    queue.slotAvailable.signal();
                }
            }
        }
        if (processChanged) {
            LOGGER.debug("Adaptive concurrency process target {} -> {} ({}), paused={}",
                    publishedProcessTarget, targetConcurrency, reason, admissionPaused);
        }
        publishedProcessTarget = targetConcurrency;
        publishedAdmissionPaused = admissionPaused;
    }

    /**
     * Wakes one poller of the queue that released a slot and the waiting queue furthest below its share.
     */
    private void wakeAfterRelease(WorkerQueue queue) {
        WorkerQueue heartbeat = oldestDueHeartbeatWaiter(clock.getAsLong(), null);
        if (heartbeat != null) {
            heartbeat.slotAvailable.signal();
        }
        if (queue.waiters > 0) {
            queue.slotAvailable.signal();
        }
        WorkerQueue needier = bestProcessWaiter(queue);
        if (needier != null) {
            needier.slotAvailable.signal();
        }
    }

    /**
     * @return the queue waiting for a process slot with the largest positive deficit, earliest arrival first, other
     *     than {@code exclude}; null if none
     */
    private WorkerQueue bestProcessWaiter(WorkerQueue exclude) {
        WorkerQueue best = null;
        for (WorkerQueue candidate : queues.values()) {
            if (candidate == exclude || candidate.processWaiters == 0 || candidate.deficit() <= 0) {
                continue;
            }
            if (best == null || outranks(candidate, best)) {
                best = candidate;
            }
        }
        return best;
    }

    private static boolean outranks(WorkerQueue a, WorkerQueue b) {
        if (a.deficit() != b.deficit()) {
            return a.deficit() > b.deficit();
        }
        return a.waitSequence() < b.waitSequence();
    }

    // ----- Helpers -----

    private long emptyPollDelay(WorkerQueue queue) {
        long base = TimeUnit.MILLISECONDS.toNanos(Math.max(1, queue.pollingIntervalMillis));
        long cap = Math.max(base, MAX_EMPTY_POLL_DELAY);
        long delay = base;
        for (int i = 1; i < queue.consecutiveEmptyPolls && delay < cap; i++) {
            delay = Math.min(cap, delay * 2);
        }
        return jitter(delay);
    }

    private static long jitter(long nanos) {
        double factor = 1 + ThreadLocalRandom.current().nextDouble(-JITTER_FRACTION, JITTER_FRACTION);
        return Math.max(1, (long) (nanos * factor));
    }

    private static long elapsed(long now, long since) {
        return since == NONE ? Long.MAX_VALUE : now - since;
    }

    private static boolean isFresh(boolean supported, long observedAt, long now, long freshness) {
        return supported && observedAt != NONE && now - observedAt <= freshness;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void resetCpu() {
        cpu.raw = 0;
        cpu.smoothed = 0;
        cpu.observedAt = NONE;
        cpu.pressureSince = NONE;
    }

    private void resetHeap() {
        heap.utilization = 0;
        heap.collectionAt = NONE;
        heap.observedAt = NONE;
        heap.lastHighCollectionAt = NONE;
        heap.baseline = Double.NaN;
        heap.recent.clear();
        heapGrowthCeiling = Integer.MAX_VALUE;
        heapCeilingSince = NONE;
    }

    private static ThreadFactory daemonThreadFactory(String namingPattern) {
        return new BasicThreadFactory.Builder()
                .namingPattern(namingPattern)
                .daemon(true)
                .build();
    }

    private static Runnable guarded(Runnable runnable, String name) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                // A periodic task that throws never runs again, so a failure to log is dropped too.
                try {
                    LOGGER.error("Adaptive concurrency {} failed", name, t);
                } catch (Throwable ignored) {
                    // Nothing left to report it with.
                }
            }
        };
    }
}
