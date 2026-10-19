/*
 * Copyright 2022 Conductor Authors.
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.automator.filters.PollFilter;
import com.netflix.conductor.client.config.ConductorClientConfiguration;
import com.netflix.conductor.client.config.PropertyFactory;
import com.netflix.conductor.client.events.dispatcher.EventDispatcher;
import com.netflix.conductor.client.events.listeners.ListenerRegister;
import com.netflix.conductor.client.events.taskrunner.TaskRunnerEvent;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.metrics.MetricsCollector;
import com.netflix.conductor.client.worker.Worker;

import com.google.common.base.Preconditions;

public class TaskRunnerConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskRunnerConfigurer.class);
    private final TaskClient taskClient;
    private final List<Worker> workers;
    private final int sleepWhenRetry;
    private final int updateRetryCount;
    private final int shutdownGracePeriodSeconds;
    private final String workerNamePrefix;
    private final Map<String /* taskType */, String /* domain */> taskToDomain;
    private final Map<String /* taskType */, Integer /* threadCount */> taskToThreadCount;
    private final Map<String /* taskType */, Integer /* timeoutInMillisecond */> taskPollTimeout;
    private final Map<String /* taskType */, Integer /* timeoutInMillisecond */> taskPollCount;
    private final Integer defaultPollTimeout;
    private final int threadCount;
    private final List<TaskRunner> taskRunners;
    private ExecutorService scheduledExecutorService;
    private final List<PollFilter> pollFilters;
    private final EventDispatcher<TaskRunnerEvent> eventDispatcher;
    private final MetricsCollector metricsCollector;
    private final boolean useVirtualThreads;
    private final boolean updateV2;
    private final AdaptiveConcurrencyController adaptiveConcurrencyController;
    private final Map<String /* taskType */, Integer /* maxConcurrency */> taskToAdaptiveMaxConcurrency;

    /**
     * @see TaskRunnerConfigurer.Builder
     * @see TaskRunnerConfigurer#init()
     */
    private TaskRunnerConfigurer(TaskRunnerConfigurer.Builder builder) {
        this.taskClient = builder.taskClient;
        this.sleepWhenRetry = builder.sleepWhenRetry;
        this.updateRetryCount = builder.updateRetryCount;
        this.workerNamePrefix = builder.workerNamePrefix;
        this.taskToDomain = builder.taskToDomain;
        this.taskToThreadCount = builder.taskToThreadCount;
        this.taskPollTimeout = builder.taskPollTimeout;
        this.taskPollCount = builder.taskPollCount;
        this.defaultPollTimeout = builder.defaultPollTimeout;
        this.shutdownGracePeriodSeconds = builder.shutdownGracePeriodSeconds;
        this.workers = new LinkedList<>();
        this.threadCount = builder.threadCount;
        this.pollFilters = builder.pollFilters;
        this.eventDispatcher = builder.eventDispatcher;
        this.metricsCollector = builder.metricsCollector;
        this.useVirtualThreads = builder.useVirtualThreads;
        this.updateV2 = builder.updateV2;
        this.adaptiveConcurrencyController = builder.adaptiveConcurrencyController;
        this.taskToAdaptiveMaxConcurrency = builder.taskToAdaptiveMaxConcurrency;
        builder.workers.forEach(this.workers::add);
        taskRunners = new LinkedList<>();
    }

    /**
     * @return Thread Count for the shared executor pool
     */
    @Deprecated
    public int getThreadCount() {
        return this.threadCount;
    }

    public int getWorkerCount() {
        return workers.size();
    }

    /**
     * @return Thread Count for individual task type
     */
    public Map<String, Integer> getTaskThreadCount() {
        return taskToThreadCount;
    }

    /**
     * @return seconds before forcing shutdown of worker
     */
    public int getShutdownGracePeriodSeconds() {
        return shutdownGracePeriodSeconds;
    }

    /**
     * @return sleep time in millisecond before task update retry is done when receiving error from
     * the Conductor server
     */
    public int getSleepWhenRetry() {
        return sleepWhenRetry;
    }

    /**
     * @return Number of times updateTask should be retried when receiving error from Conductor
     * server
     */
    public int getUpdateRetryCount() {
        return updateRetryCount;
    }

    /**
     * @return prefix used for worker names
     */
    public String getWorkerNamePrefix() {
        return workerNamePrefix;
    }

    /**
     *
     * @return true if virtual threads are being used to start worker threads
     */
    public boolean isUseVirtualThreads() {
        return useVirtualThreads;
    }

    /**
     * @return true if workers use adaptive concurrency instead of fixed thread counts
     */
    public boolean isAdaptiveConcurrencyEnabled() {
        return adaptiveConcurrencyController != null;
    }

    /**
     * @return true if task results are reported with update-and-fetch
     */
    public boolean isUpdateV2() {
        return updateV2;
    }

    /**
     * Starts the polling. Must be called after {@link TaskRunnerConfigurer.Builder#build()} method.
     */
    public synchronized void init() {
        this.scheduledExecutorService = Executors.newScheduledThreadPool(workers.size(),
            new BasicThreadFactory.Builder().namingPattern("TaskRunner %d")
            .build());
        if (adaptiveConcurrencyController != null) {
            initAdaptive();
            return;
        }
        workers.forEach(worker -> scheduledExecutorService.submit(() -> this.startWorker(worker)));
    }

    /**
     * Registers every worker with the controller before any poller starts, so the initial process target sees all
     * logical queues.
     */
    private void initAdaptive() {
        List<TaskRunner> runners = new LinkedList<>();
        for (Worker worker : workers) {
            runners.add(createTaskRunner(worker));
        }
        synchronized (taskRunners) {
            taskRunners.addAll(runners);
        }
        adaptiveConcurrencyController.activate();
        for (TaskRunner taskRunner : runners) {
            scheduledExecutorService.submit(taskRunner::pollAndExecute);
        }
    }

    /**
     * Invoke this method within a PreDestroy block within your application to facilitate a graceful
     * shutdown of your worker, during process termination.
     */
    public void shutdown() {
        if (taskRunners != null) {
            synchronized (taskRunners) {
                taskRunners.forEach(taskRunner -> taskRunner.shutdown(shutdownGracePeriodSeconds));
            }
        }
        scheduledExecutorService.shutdown();
        if (metricsCollector != null) {
            ListenerRegister.unregister(metricsCollector, eventDispatcher);
            taskClient.unregisterListeners();
        }
    }

    private TaskRunner createTaskRunner(Worker worker) {
        final Integer threadCountForTask = this.taskToThreadCount.getOrDefault(worker.getTaskDefName(), threadCount);
        final Integer taskPollTimeout = this.taskPollTimeout.getOrDefault(worker.getTaskDefName(), defaultPollTimeout);
        LOGGER.info("Domain map for tasks = {}", taskToDomain);
        return new TaskRunner(
                worker,
                taskClient,
                updateRetryCount,
                taskToDomain,
                workerNamePrefix,
                threadCountForTask,
                taskPollTimeout,
                pollFilters,
                eventDispatcher,
                useVirtualThreads,
                updateV2,
                adaptiveConcurrencyController,
                adaptiveMaxConcurrency(worker.getTaskDefName()));
    }

    /**
     * Resolves a worker's adaptive ceiling: the builder map, then {@code conductor.worker.<task>.adaptive_max_concurrency}
     * with the {@code conductor.worker.adaptive_max_concurrency} fallback, then the controller default.
     */
    private int adaptiveMaxConcurrency(String taskType) {
        if (adaptiveConcurrencyController == null) {
            return 0;
        }
        int defaultMax = adaptiveConcurrencyController.getConfig().getDefaultMaxWorkerConcurrency();
        Integer value = taskToAdaptiveMaxConcurrency.get(taskType);
        if (value == null) {
            value = PropertyFactory.getInteger(taskType, Worker.PROP_ADAPTIVE_MAX_CONCURRENCY, defaultMax);
        }
        if (value < 1) {
            throw new IllegalArgumentException("Adaptive maximum concurrency for " + taskType + " must be positive");
        }
        return Math.min(value, adaptiveConcurrencyController.getConfig().getMaxProcessConcurrency());
    }

    private void startWorker(Worker worker) {
        final TaskRunner taskRunner = createTaskRunner(worker);
        // startWorker(worker) is executed by several threads.
        // taskRunners.add(taskRunner) without synchronization could lead to a race condition and unpredictable behavior,
        // including potential null values being inserted or corrupted state.
        synchronized (taskRunners) {
            taskRunners.add(taskRunner);
        }
        LOGGER.info("Starting worker {}", worker.getTaskDefName());
        taskRunner.pollAndExecute();
    }

    /**
     * Builder used to create the instances of TaskRunnerConfigurer
     */
    public static class Builder {

        private String workerNamePrefix = "workflow-worker-%d";
        private int sleepWhenRetry = 500;
        private int updateRetryCount = 3;
        private int threadCount = -1;
        private int shutdownGracePeriodSeconds = 10;
        private int defaultPollTimeout = 100;
        private int defaultPollCount = 20;
        private final Iterable<Worker> workers;
        private final TaskClient taskClient;
        private Map<String /* taskType */, String /* domain */> taskToDomain = new HashMap<>();
        private Map<String /* taskType */, Integer /* threadCount */> taskToThreadCount =
                new HashMap<>();
        private Map<String /* taskType */, Integer /* timeoutInMillisecond */> taskPollTimeout = new HashMap<>();
        private Map<String /* taskType */, Integer /* timeoutInMillisecond */> taskPollCount = new HashMap<>();
        private final List<PollFilter> pollFilters = new LinkedList<>();
        private final EventDispatcher<TaskRunnerEvent> eventDispatcher = new EventDispatcher<>();
        private boolean useVirtualThreads;
        private boolean updateV2;
        private AdaptiveConcurrencyController adaptiveConcurrencyController;
        private Map<String /* taskType */, Integer /* maxConcurrency */> taskToAdaptiveMaxConcurrency = new HashMap<>();

        /**
         * Returns the event dispatcher used by this builder, allowing direct
         * listener registration via {@link ListenerRegister} for implementations
         * of {@link TaskRunnerEventsListener} or other custom listener interfaces.
         */
        public EventDispatcher<TaskRunnerEvent> getEventDispatcher() {
            return eventDispatcher;
        }

        private MetricsCollector metricsCollector;
        private boolean metricsCollectorExplicitlySet;

        public Builder(TaskClient taskClient, Iterable<Worker> workers) {
            Preconditions.checkNotNull(taskClient, "TaskClient cannot be null");
            Preconditions.checkNotNull(workers, "Workers cannot be null");
            this.taskClient = taskClient;
            this.workers = workers;
        }

        /**
         * @param workerNamePrefix prefix to be used for worker names, defaults to workflow-worker-
         *                         if not supplied.
         * @return Returns the current instance.
         */
        public TaskRunnerConfigurer.Builder withWorkerNamePrefix(String workerNamePrefix) {
            this.workerNamePrefix = workerNamePrefix;
            return this;
        }

        /**
         * @param sleepWhenRetry time in milliseconds, for which the thread should sleep when task
         *                       update call fails, before retrying the operation.
         * @return Returns the current instance.
         */
        public TaskRunnerConfigurer.Builder withSleepWhenRetry(int sleepWhenRetry) {
            this.sleepWhenRetry = sleepWhenRetry;
            return this;
        }

        /**
         * @param updateRetryCount number of times to retry the failed updateTask operation
         * @return Builder instance
         * @see #withSleepWhenRetry(int)
         */
        public TaskRunnerConfigurer.Builder withUpdateRetryCount(int updateRetryCount) {
            this.updateRetryCount = updateRetryCount;
            return this;
        }

        /**
         * @param conductorClientConfiguration client configuration to handle external payloads
         * @return Builder instance
         */
        public TaskRunnerConfigurer.Builder withConductorClientConfiguration(
                ConductorClientConfiguration conductorClientConfiguration) {
            return this;
        }

        /**
         * @param shutdownGracePeriodSeconds waiting seconds before forcing shutdown of your worker
         * @return Builder instance
         */
        public TaskRunnerConfigurer.Builder withShutdownGracePeriodSeconds(
                int shutdownGracePeriodSeconds) {
            if (shutdownGracePeriodSeconds < 1) {
                throw new IllegalArgumentException(
                        "Seconds of shutdownGracePeriod cannot be less than 1");
            }
            this.shutdownGracePeriodSeconds = shutdownGracePeriodSeconds;
            return this;
        }

        public TaskRunnerConfigurer.Builder withTaskToDomain(Map<String, String> taskToDomain) {
            this.taskToDomain = taskToDomain;
            return this;
        }

        public TaskRunnerConfigurer.Builder withTaskThreadCount(
                Map<String, Integer> taskToThreadCount) {
            this.taskToThreadCount = taskToThreadCount;
            if (taskToThreadCount.values().stream().anyMatch(v -> v < 1)) {
                throw new IllegalArgumentException("No. of threads cannot be less than 1");
            }
            return this;
        }

        public TaskRunnerConfigurer.Builder withTaskToThreadCount(
                Map<String, Integer> taskToThreadCount) {
            this.taskToThreadCount = taskToThreadCount;
            return this;
        }

        public TaskRunnerConfigurer.Builder withTaskPollTimeout(
                Map<String, Integer> taskPollTimeout) {
            this.taskPollTimeout = taskPollTimeout;
            return this;
        }

        public TaskRunnerConfigurer.Builder withTaskPollTimeout(Integer taskPollTimeout) {
            this.defaultPollTimeout = taskPollTimeout;
            return this;
        }

        /**
         * @deprecated never applied to polling; use adaptive concurrency, whose poll batch follows free slots up to
         * {@link AdaptiveConcurrencyConfig#getMaxPollBatch()}
         */
        @Deprecated
        public TaskRunnerConfigurer.Builder withTaskPollCount(Map<String, Integer> taskPollCount) {
            this.taskPollCount = taskPollCount;
            return this;
        }

        /**
         * @deprecated never applied to polling; use adaptive concurrency, whose poll batch follows free slots up to
         * {@link AdaptiveConcurrencyConfig#getMaxPollBatch()}
         */
        @Deprecated
        public TaskRunnerConfigurer.Builder withTaskPollCount(int defaultPollCount) {
            this.defaultPollCount = defaultPollCount;
            return this;
        }

        /**
         * Builds an instance of the TaskRunnerConfigurer.
         *
         * <p>Please see {@link TaskRunnerConfigurer#init()} method. The method must be called after
         * this constructor for the polling to start.
         *
         * @return Builder instance
         */
        public TaskRunnerConfigurer build() {
            if (adaptiveConcurrencyController != null) {
                validateAdaptivePollTimeouts();
            }
            if (!metricsCollectorExplicitlySet) {
                var conductorClient = taskClient.getConductorClient();
                if (conductorClient != null) {
                    MetricsCollector mc = conductorClient.getMetricsCollector();
                    if (mc != null) {
                        this.metricsCollector = mc;
                        ListenerRegister.register(mc, eventDispatcher);
                    }
                }
            }
            return new TaskRunnerConfigurer(this);
        }

        /**
         * @param threadCount # of threads assigned to the workers. Should be at-least the size of
         *                    taskWorkers to avoid starvation in a busy system.
         * @return Builder instance
         */
        public Builder withThreadCount(int threadCount) {
            if (threadCount < 1) {
                throw new IllegalArgumentException("No. of threads cannot be less than 1");
            }
            this.threadCount = threadCount;
            return this;
        }

        public Builder withPollFilter(PollFilter filter) {
            pollFilters.add(filter);
            return this;
        }

        public <T extends TaskRunnerEvent> Builder withListener(Class<T> eventType, Consumer<T> listener) {
            eventDispatcher.register(eventType, listener);
            return this;
        }

        public Builder withMetricsCollector(MetricsCollector metricsCollector) {
            this.metricsCollector = metricsCollector;
            ListenerRegister.register(metricsCollector, eventDispatcher);
            this.metricsCollectorExplicitlySet = true;
            return this;
        }

        public Builder withUseVirtualThreads(boolean useVirtualThreads) {
            this.useVirtualThreads = useVirtualThreads;
            return this;
        }

        /**
         * @param updateV2 report results with update-and-fetch, which can return the next task in the same call.
         *                 The {@code taskUpdateV2} system property or environment variable also enables it.
         * @return Builder instance
         */
        public Builder withUpdateV2(boolean updateV2) {
            this.updateV2 = updateV2;
            return this;
        }

        /**
         * Enables adaptive concurrency. Thread counts no longer apply: the controller divides one process target
         * among workers in proportion to queue depth, grows it under demand while CPU and heap have headroom, and
         * shrinks it under pressure. Share one controller among all configurers of a process.
         *
         * @param adaptiveConcurrencyController the shared controller
         * @return Builder instance
         */
        public Builder withAdaptiveConcurrency(AdaptiveConcurrencyController adaptiveConcurrencyController) {
            this.adaptiveConcurrencyController = adaptiveConcurrencyController;
            return this;
        }

        /**
         * @param taskToAdaptiveMaxConcurrency per-task ceilings for adaptive concurrency; tasks not listed use
         *                                     {@link AdaptiveConcurrencyConfig#getDefaultMaxWorkerConcurrency()}
         * @return Builder instance
         */
        public Builder withTaskAdaptiveMaxConcurrency(Map<String, Integer> taskToAdaptiveMaxConcurrency) {
            if (taskToAdaptiveMaxConcurrency.values().stream().anyMatch(v -> v == null || v < 1)) {
                throw new IllegalArgumentException("Adaptive maximum concurrency cannot be less than 1");
            }
            this.taskToAdaptiveMaxConcurrency = taskToAdaptiveMaxConcurrency;
            return this;
        }

        private void validateAdaptivePollTimeouts() {
            int limit = AdaptiveConcurrencyController.MAX_POLL_TIMEOUT_MILLIS;
            if (defaultPollTimeout > limit || taskPollTimeout.values().stream().anyMatch(t -> t != null && t > limit)) {
                throw new IllegalArgumentException("Poll timeout cannot exceed " + limit
                        + " ms; the server rejects longer batch polls");
            }
        }

    }
}
