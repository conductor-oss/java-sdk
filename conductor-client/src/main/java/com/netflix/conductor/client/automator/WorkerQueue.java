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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.function.BooleanSupplier;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.events.dispatcher.EventDispatcher;
import com.netflix.conductor.client.events.taskrunner.TaskRunnerEvent;
import com.netflix.conductor.client.http.TaskClient;

/**
 * Controller state for one logical worker queue: server/client scope, task type and resolved domain. All pollers
 * registered for the same logical queue share this state and its execution pool.
 *
 * <p>Mutable fields are guarded by the controller lock.
 */
final class WorkerQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerQueue.class);
    private static final long EXECUTION_THREAD_KEEP_ALIVE_SECONDS = 60;

    /**
     * Identity of a logical queue. {@link TaskClient} compares by identity, so distinct client scopes are never
     * merged.
     */
    record Key(TaskClient taskClient, String taskType, String domain) {}

    final Key key;
    final String queueName;
    final int maximumConcurrency;
    final int pollingIntervalMillis;
    final Condition slotAvailable;
    // Replaced by later registrations; read outside the lock by the depth sampler.
    private volatile EventDispatcher<TaskRunnerEvent> eventDispatcher;
    private volatile BooleanSupplier pollable;
    private final ExecutorService executionPool;
    private final ThreadPoolExecutor platformPool;
    private int configuredPoolSize = 1;

    int registrations;
    int targetConcurrency;
    int publishedTarget = -1;
    int heldSlots;
    int depth;
    long depthObservedAt = AdaptiveConcurrencyController.NONE;
    int outstandingPolls;
    int consecutiveEmptyPolls;
    /** Sampled depth when the latest empty response settled; arrivals since then count in full. */
    long depthAtEmptyPoll;
    long nextPollAllowedAt;
    long nextHeartbeatAt;
    long nextStandbyAt;
    /** Outstanding standby polls; their slots are not part of the backlog. */
    int standbyPolls;
    /** Slots held by outstanding poll requests; the rest of {@link #heldSlots} back accepted tasks. */
    int reservedPollSlots;
    /** When a task slot was last released, or tasks last started arriving; a wedged worker's age grows from here. */
    long lastTaskSlotReleaseAt;
    /** Pause and poll filter result at the latest depth sample. */
    boolean pollableAtSample = true;
    /** Pollers waiting for an open slot for a due heartbeat. */
    int heartbeatWaiters;
    int waiters;
    int processWaiters;
    long processWaitSequence;

    WorkerQueue(Key key,
                int maximumConcurrency,
                int pollingIntervalMillis,
                BooleanSupplier pollable,
                ThreadFactory threadFactory,
                boolean useVirtualThreads,
                EventDispatcher<TaskRunnerEvent> eventDispatcher,
                Condition slotAvailable,
                long now) {
        this.key = key;
        this.queueName = queueName(key.taskType(), key.domain());
        this.maximumConcurrency = maximumConcurrency;
        this.pollingIntervalMillis = pollingIntervalMillis;
        this.pollable = pollable;
        this.eventDispatcher = eventDispatcher;
        this.slotAvailable = slotAvailable;
        this.nextPollAllowedAt = now;
        this.nextHeartbeatAt = now;
        this.nextStandbyAt = now;
        this.lastTaskSlotReleaseAt = now;
        if (useVirtualThreads) {
            this.platformPool = null;
            this.executionPool = Executors.newThreadPerTaskExecutor(threadFactory);
        } else {
            this.platformPool = new ThreadPoolExecutor(
                    1,
                    1,
                    EXECUTION_THREAD_KEEP_ALIVE_SECONDS,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(maximumConcurrency),
                    threadFactory);
            this.platformPool.allowCoreThreadTimeOut(true);
            this.executionPool = platformPool;
        }
    }

    /**
     * @return the server's name for this queue: the task type, or {@code domain:taskType} for a domain queue
     */
    static String queueName(String taskType, String domain) {
        return StringUtils.isBlank(domain) ? taskType : domain + ":" + taskType;
    }

    String taskType() {
        return key.taskType();
    }

    EventDispatcher<TaskRunnerEvent> eventDispatcher() {
        return eventDispatcher;
    }

    void updateRegistration(BooleanSupplier pollable, EventDispatcher<TaskRunnerEvent> eventDispatcher) {
        this.pollable = pollable;
        this.eventDispatcher = eventDispatcher;
    }

    String domain() {
        return key.domain();
    }

    ExecutorService executionPool() {
        return executionPool;
    }

    int deficit() {
        return targetConcurrency - heldSlots;
    }

    /**
     * @return arrival order among queues waiting for a process slot; later than any waiter when not waiting
     */
    long waitSequence() {
        return processWaiters > 0 ? processWaitSequence : Long.MAX_VALUE;
    }

    /**
     * Evaluates worker pause and poll filters. Runs user code, so it is never called under the controller lock.
     */
    boolean isPollable() {
        try {
            return pollable.getAsBoolean();
        } catch (RuntimeException e) {
            LOGGER.warn("Pause or poll filter check failed for {}; treating the worker as pollable", queueName, e);
            return true;
        }
    }

    /**
     * Sizes the platform pool to cover the target and all promised capacity. Threads are created lazily on
     * submission and idle threads above the size retire after the keep-alive.
     */
    void resizePool() {
        if (platformPool == null) {
            return;
        }
        int required = Math.max(1, Math.max(targetConcurrency, heldSlots));
        if (required > configuredPoolSize) {
            platformPool.setMaximumPoolSize(required);
            platformPool.setCorePoolSize(required);
        } else if (required < configuredPoolSize) {
            platformPool.setCorePoolSize(required);
            platformPool.setMaximumPoolSize(required);
        }
        configuredPoolSize = required;
    }
}
