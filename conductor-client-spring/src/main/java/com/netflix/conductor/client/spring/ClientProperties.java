/*
 * Copyright 2020 Conductor Authors.
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
package com.netflix.conductor.client.spring;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.netflix.conductor.client.automator.AdaptiveConcurrencyConfig;

@ConfigurationProperties("conductor.client")
public class ClientProperties {

    public static class Timeout {
        private int connect = -1;
        private int read = -1;
        private int write = -1;

        public int getConnect() {
            return connect;
        }

        public void setConnect(int connect) {
            this.connect = connect;
        }

        public int getRead() {
            return read;
        }

        public void setRead(int read) {
            this.read = read;
        }

        public int getWrite() {
            return write;
        }

        public void setWrite(int write) {
            this.write = write;
        }
    }

    public static class AdaptiveConcurrency {

        // Enables adaptive concurrency for workers started by this client
        private boolean enabled = false;

        // Aggregate admission ceiling for all adaptive workers
        private int maxProcessConcurrency = 256;

        // Initial process target; 0 uses min(logical queues, available processors)
        private int initialProcessConcurrency = 0;

        // Lowest process target an idle process decays to; CPU and heap pressure may go below it
        private int minProcessConcurrency = 1;

        // Per-worker ceiling for workers without their own
        private int defaultMaxWorkerConcurrency = 64;

        // Ceiling for the number of tasks requested by one poll
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

        // Allow running without CPU and heap metrics
        private boolean demandOnlyEnabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxProcessConcurrency() {
            return maxProcessConcurrency;
        }

        public void setMaxProcessConcurrency(int maxProcessConcurrency) {
            this.maxProcessConcurrency = maxProcessConcurrency;
        }

        public int getInitialProcessConcurrency() {
            return initialProcessConcurrency;
        }

        public void setInitialProcessConcurrency(int initialProcessConcurrency) {
            this.initialProcessConcurrency = initialProcessConcurrency;
        }

        public int getMinProcessConcurrency() {
            return minProcessConcurrency;
        }

        public void setMinProcessConcurrency(int minProcessConcurrency) {
            this.minProcessConcurrency = minProcessConcurrency;
        }

        public int getDefaultMaxWorkerConcurrency() {
            return defaultMaxWorkerConcurrency;
        }

        public void setDefaultMaxWorkerConcurrency(int defaultMaxWorkerConcurrency) {
            this.defaultMaxWorkerConcurrency = defaultMaxWorkerConcurrency;
        }

        public int getMaxPollBatch() {
            return maxPollBatch;
        }

        public void setMaxPollBatch(int maxPollBatch) {
            this.maxPollBatch = maxPollBatch;
        }

        public Duration getResourceSampleInterval() {
            return resourceSampleInterval;
        }

        public void setResourceSampleInterval(Duration resourceSampleInterval) {
            this.resourceSampleInterval = resourceSampleInterval;
        }

        public Duration getMetricFreshness() {
            return metricFreshness;
        }

        public void setMetricFreshness(Duration metricFreshness) {
            this.metricFreshness = metricFreshness;
        }

        public Duration getDepthSampleInterval() {
            return depthSampleInterval;
        }

        public void setDepthSampleInterval(Duration depthSampleInterval) {
            this.depthSampleInterval = depthSampleInterval;
        }

        public Duration getDepthFreshness() {
            return depthFreshness;
        }

        public void setDepthFreshness(Duration depthFreshness) {
            this.depthFreshness = depthFreshness;
        }

        public Duration getHeartbeatInterval() {
            return heartbeatInterval;
        }

        public void setHeartbeatInterval(Duration heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
        }

        public Duration getStandbyPollTimeout() {
            return standbyPollTimeout;
        }

        public void setStandbyPollTimeout(Duration standbyPollTimeout) {
            this.standbyPollTimeout = standbyPollTimeout;
        }

        public Duration getControlInterval() {
            return controlInterval;
        }

        public void setControlInterval(Duration controlInterval) {
            this.controlInterval = controlInterval;
        }

        public double getCpuSmoothingAlpha() {
            return cpuSmoothingAlpha;
        }

        public void setCpuSmoothingAlpha(double cpuSmoothingAlpha) {
            this.cpuSmoothingAlpha = cpuSmoothingAlpha;
        }

        public double getGrowthThreshold() {
            return growthThreshold;
        }

        public void setGrowthThreshold(double growthThreshold) {
            this.growthThreshold = growthThreshold;
        }

        public double getCpuPressureThreshold() {
            return cpuPressureThreshold;
        }

        public void setCpuPressureThreshold(double cpuPressureThreshold) {
            this.cpuPressureThreshold = cpuPressureThreshold;
        }

        public double getHeapPauseThreshold() {
            return heapPauseThreshold;
        }

        public void setHeapPauseThreshold(double heapPauseThreshold) {
            this.heapPauseThreshold = heapPauseThreshold;
        }

        public double getHeapResumeThreshold() {
            return heapResumeThreshold;
        }

        public void setHeapResumeThreshold(double heapResumeThreshold) {
            this.heapResumeThreshold = heapResumeThreshold;
        }

        public boolean isDemandOnlyEnabled() {
            return demandOnlyEnabled;
        }

        public void setDemandOnlyEnabled(boolean demandOnlyEnabled) {
            this.demandOnlyEnabled = demandOnlyEnabled;
        }

        public AdaptiveConcurrencyConfig toConfig() {
            return new AdaptiveConcurrencyConfig.Builder()
                    .withMaxProcessConcurrency(maxProcessConcurrency)
                    .withInitialProcessConcurrency(initialProcessConcurrency)
                    .withMinProcessConcurrency(minProcessConcurrency)
                    .withDefaultMaxWorkerConcurrency(defaultMaxWorkerConcurrency)
                    .withMaxPollBatch(maxPollBatch)
                    .withResourceSampleInterval(resourceSampleInterval)
                    .withMetricFreshness(metricFreshness)
                    .withDepthSampleInterval(depthSampleInterval)
                    .withDepthFreshness(depthFreshness)
                    .withHeartbeatInterval(heartbeatInterval)
                    .withStandbyPollTimeout(standbyPollTimeout)
                    .withControlInterval(controlInterval)
                    .withCpuSmoothingAlpha(cpuSmoothingAlpha)
                    .withGrowthThreshold(growthThreshold)
                    .withCpuPressureThreshold(cpuPressureThreshold)
                    .withHeapPauseThreshold(heapPauseThreshold)
                    .withHeapResumeThreshold(heapResumeThreshold)
                    .withDemandOnlyEnabled(demandOnlyEnabled)
                    .build();
        }
    }

    private String rootUri;

    private String basePath;

    private String workerNamePrefix = "workflow-worker-%d";

    private int threadCount = 1;

    private Duration sleepWhenRetryDuration = Duration.ofMillis(500);

    private int updateRetryCount = 3;

    private Map<String, String> taskToDomain = new HashMap<>();

    private Map<String, Integer> taskThreadCount = new HashMap<>();

    private int shutdownGracePeriodSeconds = 10;

    private int taskPollTimeout = 100;

    private Timeout timeout = new Timeout();

    private boolean verifyingSsl = true;

    private boolean useVirtualThreads = false;

    private boolean updateV2 = false;

    private AdaptiveConcurrency adaptiveConcurrency = new AdaptiveConcurrency();

    public boolean isUseVirtualThreads() {
        return useVirtualThreads;
    }

    public void setUseVirtualThreads(boolean useVirtualThreads) {
        this.useVirtualThreads = useVirtualThreads;
    }

    public boolean isUpdateV2() {
        return updateV2;
    }

    public void setUpdateV2(boolean updateV2) {
        this.updateV2 = updateV2;
    }

    public AdaptiveConcurrency getAdaptiveConcurrency() {
        return adaptiveConcurrency;
    }

    public void setAdaptiveConcurrency(AdaptiveConcurrency adaptiveConcurrency) {
        this.adaptiveConcurrency = adaptiveConcurrency;
    }

    public void setVerifyingSsl(boolean verifyingSsl) {
        this.verifyingSsl = verifyingSsl;
    }

    public boolean isVerifyingSsl() {
        return verifyingSsl;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public void setTimeout(Timeout timeout) {
        this.timeout = timeout;
    }

    public String getRootUri() {
        return rootUri;
    }

    public void setRootUri(String rootUri) {
        this.rootUri = rootUri;
    }

    public String getWorkerNamePrefix() {
        return workerNamePrefix;
    }

    public void setWorkerNamePrefix(String workerNamePrefix) {
        this.workerNamePrefix = workerNamePrefix;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public Duration getSleepWhenRetryDuration() {
        return sleepWhenRetryDuration;
    }

    public void setSleepWhenRetryDuration(Duration sleepWhenRetryDuration) {
        this.sleepWhenRetryDuration = sleepWhenRetryDuration;
    }

    public int getUpdateRetryCount() {
        return updateRetryCount;
    }

    public void setUpdateRetryCount(int updateRetryCount) {
        this.updateRetryCount = updateRetryCount;
    }

    public Map<String, String> getTaskToDomain() {
        return taskToDomain;
    }

    public void setTaskToDomain(Map<String, String> taskToDomain) {
        this.taskToDomain = taskToDomain;
    }

    public int getShutdownGracePeriodSeconds() {
        return shutdownGracePeriodSeconds;
    }

    public void setShutdownGracePeriodSeconds(int shutdownGracePeriodSeconds) {
        this.shutdownGracePeriodSeconds = shutdownGracePeriodSeconds;
    }

    public Map<String, Integer> getTaskThreadCount() {
        return taskThreadCount;
    }

    public void setTaskThreadCount(Map<String, Integer> taskThreadCount) {
        this.taskThreadCount = taskThreadCount;
    }

    public int getTaskPollTimeout() {
        return taskPollTimeout;
    }

    public void setTaskPollTimeout(int taskPollTimeout) {
        this.taskPollTimeout = taskPollTimeout;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}
