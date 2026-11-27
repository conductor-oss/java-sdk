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

import java.time.Duration;

/**
 * Settings for {@link AdaptiveConcurrencyController}. Defaults are starting points for load testing; see
 * {@code design/adaptive-worker-concurrency.md}.
 *
 * <p>Create instances with {@link AdaptiveConcurrencyConfig.Builder}.
 */
public class AdaptiveConcurrencyConfig {

    private final int maxProcessConcurrency;
    private final int initialProcessConcurrency;
    private final int minProcessConcurrency;
    private final int defaultMaxWorkerConcurrency;
    private final int maxPollBatch;
    private final Duration resourceSampleInterval;
    private final Duration metricFreshness;
    private final Duration depthSampleInterval;
    private final Duration depthFreshness;
    private final Duration heartbeatInterval;
    private final Duration standbyPollTimeout;
    private final Duration controlInterval;
    private final double cpuSmoothingAlpha;
    private final double growthThreshold;
    private final double cpuPressureThreshold;
    private final double heapPauseThreshold;
    private final double heapResumeThreshold;
    private final boolean demandOnlyEnabled;

    private AdaptiveConcurrencyConfig(Builder builder) {
        this.maxProcessConcurrency = builder.maxProcessConcurrency;
        this.initialProcessConcurrency = builder.initialProcessConcurrency;
        this.minProcessConcurrency = builder.minProcessConcurrency;
        this.defaultMaxWorkerConcurrency = builder.defaultMaxWorkerConcurrency;
        this.maxPollBatch = builder.maxPollBatch;
        this.resourceSampleInterval = builder.resourceSampleInterval;
        this.metricFreshness = builder.metricFreshness;
        this.depthSampleInterval = builder.depthSampleInterval;
        this.depthFreshness = builder.depthFreshness;
        this.heartbeatInterval = builder.heartbeatInterval;
        this.standbyPollTimeout = builder.standbyPollTimeout;
        this.controlInterval = builder.controlInterval;
        this.cpuSmoothingAlpha = builder.cpuSmoothingAlpha;
        this.growthThreshold = builder.growthThreshold;
        this.cpuPressureThreshold = builder.cpuPressureThreshold;
        this.heapPauseThreshold = builder.heapPauseThreshold;
        this.heapResumeThreshold = builder.heapResumeThreshold;
        this.demandOnlyEnabled = builder.demandOnlyEnabled;
    }

    /**
     * @return aggregate admission ceiling for all workers sharing the controller
     */
    public int getMaxProcessConcurrency() {
        return maxProcessConcurrency;
    }

    /**
     * @return configured initial process target, or 0 to use min(logical queues, available processors); never below
     *         {@link #getMinProcessConcurrency()}
     */
    public int getInitialProcessConcurrency() {
        return initialProcessConcurrency;
    }

    /**
     * @return lowest process target that demand decay settles at; CPU and heap pressure may still go below it
     */
    public int getMinProcessConcurrency() {
        return minProcessConcurrency;
    }

    /**
     * @return per-queue ceiling used when a worker does not configure its own
     */
    public int getDefaultMaxWorkerConcurrency() {
        return defaultMaxWorkerConcurrency;
    }

    /**
     * @return ceiling for the number of tasks requested by a single poll
     */
    public int getMaxPollBatch() {
        return maxPollBatch;
    }

    public Duration getResourceSampleInterval() {
        return resourceSampleInterval;
    }

    public Duration getMetricFreshness() {
        return metricFreshness;
    }

    public Duration getDepthSampleInterval() {
        return depthSampleInterval;
    }

    public Duration getDepthFreshness() {
        return depthFreshness;
    }

    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    /**
     * @return server wait of a standby poll; zero when standby polls are disabled
     */
    public Duration getStandbyPollTimeout() {
        return standbyPollTimeout;
    }

    public Duration getControlInterval() {
        return controlInterval;
    }

    public double getCpuSmoothingAlpha() {
        return cpuSmoothingAlpha;
    }

    public double getGrowthThreshold() {
        return growthThreshold;
    }

    public double getCpuPressureThreshold() {
        return cpuPressureThreshold;
    }

    public double getHeapPauseThreshold() {
        return heapPauseThreshold;
    }

    public double getHeapResumeThreshold() {
        return heapResumeThreshold;
    }

    /**
     * @return true if the controller may run without any supported resource metric
     */
    public boolean isDemandOnlyEnabled() {
        return demandOnlyEnabled;
    }

    /**
     * Builder used to create instances of {@link AdaptiveConcurrencyConfig}.
     */
    public static class Builder {

        private int maxProcessConcurrency = 256;
        private int initialProcessConcurrency = 0;
        private int minProcessConcurrency = 1;
        private int defaultMaxWorkerConcurrency = 64;
        private int maxPollBatch = 16;
        private Duration resourceSampleInterval = Duration.ofSeconds(1);
        private Duration metricFreshness = Duration.ofSeconds(5);
        private Duration depthSampleInterval = Duration.ofSeconds(1);
        private Duration depthFreshness = Duration.ofSeconds(5);
        private Duration heartbeatInterval = Duration.ofSeconds(5);
        private Duration standbyPollTimeout = Duration.ofSeconds(1);
        private Duration controlInterval = Duration.ofSeconds(5);
        private double cpuSmoothingAlpha = 0.2;
        private double growthThreshold = 0.75;
        private double cpuPressureThreshold = 0.90;
        private double heapPauseThreshold = 0.90;
        private double heapResumeThreshold = 0.80;
        private boolean demandOnlyEnabled;

        /**
         * @param maxProcessConcurrency aggregate admission ceiling, defaults to 256
         * @return Builder instance
         */
        public Builder withMaxProcessConcurrency(int maxProcessConcurrency) {
            this.maxProcessConcurrency = maxProcessConcurrency;
            return this;
        }

        /**
         * @param initialProcessConcurrency initial process target; 0 uses min(logical queues, available processors).
         *                                  Raised to the minimum process concurrency when below it
         * @return Builder instance
         */
        public Builder withInitialProcessConcurrency(int initialProcessConcurrency) {
            this.initialProcessConcurrency = initialProcessConcurrency;
            return this;
        }

        /**
         * Keeps the process ready for bursts: an idle process decays to this target instead of one, so the next burst
         * does not ramp up from a single slot. Idle queues still hold no threads. CPU and heap pressure may reduce
         * the target below it.
         *
         * @param minProcessConcurrency lowest target demand decay settles at, defaults to 1
         * @return Builder instance
         */
        public Builder withMinProcessConcurrency(int minProcessConcurrency) {
            this.minProcessConcurrency = minProcessConcurrency;
            return this;
        }

        /**
         * @param defaultMaxWorkerConcurrency per-queue ceiling for workers without their own, defaults to 64
         * @return Builder instance
         */
        public Builder withDefaultMaxWorkerConcurrency(int defaultMaxWorkerConcurrency) {
            this.defaultMaxWorkerConcurrency = defaultMaxWorkerConcurrency;
            return this;
        }

        /**
         * @param maxPollBatch ceiling for the number of tasks requested by one poll, defaults to 16
         * @return Builder instance
         */
        public Builder withMaxPollBatch(int maxPollBatch) {
            this.maxPollBatch = maxPollBatch;
            return this;
        }

        public Builder withResourceSampleInterval(Duration resourceSampleInterval) {
            this.resourceSampleInterval = resourceSampleInterval;
            return this;
        }

        public Builder withMetricFreshness(Duration metricFreshness) {
            this.metricFreshness = metricFreshness;
            return this;
        }

        public Builder withDepthSampleInterval(Duration depthSampleInterval) {
            this.depthSampleInterval = depthSampleInterval;
            return this;
        }

        public Builder withDepthFreshness(Duration depthFreshness) {
            this.depthFreshness = depthFreshness;
            return this;
        }

        public Builder withHeartbeatInterval(Duration heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
            return this;
        }

        /**
         * A queue whose share is fully held keeps one single-task long poll open on a slot no share claims, so a task
         * that arrives is picked up as it lands instead of at the next depth sample. An idle queue sends about one
         * request per timeout.
         *
         * @param standbyPollTimeout server wait of a standby poll, at most 5 seconds; zero disables standby polls,
         *                           defaults to 1 second
         * @return Builder instance
         */
        public Builder withStandbyPollTimeout(Duration standbyPollTimeout) {
            this.standbyPollTimeout = standbyPollTimeout;
            return this;
        }

        public Builder withControlInterval(Duration controlInterval) {
            this.controlInterval = controlInterval;
            return this;
        }

        public Builder withCpuSmoothingAlpha(double cpuSmoothingAlpha) {
            this.cpuSmoothingAlpha = cpuSmoothingAlpha;
            return this;
        }

        public Builder withGrowthThreshold(double growthThreshold) {
            this.growthThreshold = growthThreshold;
            return this;
        }

        public Builder withCpuPressureThreshold(double cpuPressureThreshold) {
            this.cpuPressureThreshold = cpuPressureThreshold;
            return this;
        }

        public Builder withHeapPauseThreshold(double heapPauseThreshold) {
            this.heapPauseThreshold = heapPauseThreshold;
            return this;
        }

        public Builder withHeapResumeThreshold(double heapResumeThreshold) {
            this.heapResumeThreshold = heapResumeThreshold;
            return this;
        }

        /**
         * @param demandOnlyEnabled allow running when neither CPU nor heap can be measured
         * @return Builder instance
         */
        public Builder withDemandOnlyEnabled(boolean demandOnlyEnabled) {
            this.demandOnlyEnabled = demandOnlyEnabled;
            return this;
        }

        /**
         * @return a validated configuration
         * @throws IllegalArgumentException if a value is out of range
         */
        public AdaptiveConcurrencyConfig build() {
            requirePositive(maxProcessConcurrency, "maxProcessConcurrency");
            requirePositive(defaultMaxWorkerConcurrency, "defaultMaxWorkerConcurrency");
            requirePositive(maxPollBatch, "maxPollBatch");
            if (minProcessConcurrency < 1 || minProcessConcurrency > maxProcessConcurrency) {
                throw new IllegalArgumentException("minProcessConcurrency must be between 1 and maxProcessConcurrency");
            }
            if (initialProcessConcurrency < 0 || initialProcessConcurrency > maxProcessConcurrency) {
                throw new IllegalArgumentException(
                        "initialProcessConcurrency must be between 0 and maxProcessConcurrency");
            }
            requirePositive(resourceSampleInterval, "resourceSampleInterval");
            requirePositive(metricFreshness, "metricFreshness");
            requirePositive(depthSampleInterval, "depthSampleInterval");
            requirePositive(depthFreshness, "depthFreshness");
            requirePositive(heartbeatInterval, "heartbeatInterval");
            requirePositive(controlInterval, "controlInterval");
            if (standbyPollTimeout == null || standbyPollTimeout.isNegative()
                    || standbyPollTimeout.toMillis() > AdaptiveConcurrencyController.MAX_POLL_TIMEOUT_MILLIS) {
                throw new IllegalArgumentException("standbyPollTimeout must be between 0 and 5 seconds");
            }
            if (metricFreshness.compareTo(resourceSampleInterval) <= 0) {
                throw new IllegalArgumentException("metricFreshness must exceed resourceSampleInterval");
            }
            if (depthFreshness.compareTo(depthSampleInterval) <= 0) {
                throw new IllegalArgumentException("depthFreshness must exceed depthSampleInterval");
            }
            if (!(cpuSmoothingAlpha > 0 && cpuSmoothingAlpha <= 1)) {
                throw new IllegalArgumentException("cpuSmoothingAlpha must be in (0, 1]");
            }
            requireRatio(growthThreshold, "growthThreshold");
            requireRatio(cpuPressureThreshold, "cpuPressureThreshold");
            requireRatio(heapPauseThreshold, "heapPauseThreshold");
            requireRatio(heapResumeThreshold, "heapResumeThreshold");
            if (!(growthThreshold < heapResumeThreshold && heapResumeThreshold < heapPauseThreshold)) {
                throw new IllegalArgumentException(
                        "Thresholds must satisfy growthThreshold < heapResumeThreshold < heapPauseThreshold");
            }
            if (!(growthThreshold < cpuPressureThreshold)) {
                throw new IllegalArgumentException("growthThreshold must be below cpuPressureThreshold");
            }
            return new AdaptiveConcurrencyConfig(this);
        }

        private static void requirePositive(int value, String name) {
            if (value < 1) {
                throw new IllegalArgumentException(name + " must be positive");
            }
        }

        private static void requirePositive(Duration value, String name) {
            if (value == null || value.isZero() || value.isNegative()) {
                throw new IllegalArgumentException(name + " must be positive");
            }
        }

        private static void requireRatio(double value, String name) {
            if (!(value > 0 && value <= 1)) {
                throw new IllegalArgumentException(name + " must be in (0, 1]");
            }
        }
    }
}
