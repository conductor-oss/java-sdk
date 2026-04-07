/*
 * Copyright 2024 Conductor Authors.
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
package io.orkes.conductor.harness;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SimulatedTaskWorker implements Worker {

    private static final Logger log = LoggerFactory.getLogger(SimulatedTaskWorker.class);

    private static final String ALPHANUMERIC_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final String INSTANCE_ID;

    static {
        String hostname = System.getenv("HOSTNAME");
        INSTANCE_ID = (hostname != null && !hostname.isBlank()) ? hostname
                : UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private final String taskName;
    private final String codename;
    private final int defaultDelayMs;
    private final int batchSize;
    private final int pollIntervalMs;
    private final String workerId;
    private final Random random = new Random();

    public SimulatedTaskWorker(String taskName, String codename, int sleepSeconds, int batchSize, int pollIntervalMs) {
        this.taskName = taskName;
        this.codename = codename;
        this.defaultDelayMs = sleepSeconds * 1000;
        this.batchSize = batchSize;
        this.pollIntervalMs = pollIntervalMs;
        this.workerId = taskName + "-" + INSTANCE_ID;

        log.info("[{}] Initialized worker [workerId={}, codename={}, batchSize={}, pollInterval={}ms]", taskName,
                workerId, codename, batchSize, pollIntervalMs);
    }

    @Override
    public String getTaskDefName() {
        return taskName;
    }

    @Override
    public String getIdentity() {
        return workerId;
    }

    @Override
    public int getPollingInterval() {
        return pollIntervalMs;
    }

    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> input = task.getInputData() != null ? task.getInputData() : new HashMap<>();
        String taskId = task.getTaskId();
        int taskIndex = getOrDefault(input, "taskIndex", -1);

        log.info("[{}] Starting simulated task [id={}, index={}, codename={}]", taskName, taskId, taskIndex, codename);

        long startTime = System.currentTimeMillis();

        String delayType = getOrDefault(input, "delayType", "fixed");
        int minDelay = getOrDefault(input, "minDelay", defaultDelayMs);
        int maxDelay = getOrDefault(input, "maxDelay", minDelay + 100);
        int meanDelay = getOrDefault(input, "meanDelay", (minDelay + maxDelay) / 2);
        int stdDeviation = getOrDefault(input, "stdDeviation", 30);
        double successRate = getOrDefault(input, "successRate", 1.0);
        String failureMode = getOrDefault(input, "failureMode", "random");
        int outputSize = getOrDefault(input, "outputSize", 1024);

        long delayMs = 0L;
        if (!"wait".equalsIgnoreCase(delayType)) {
            delayMs = calculateDelay(delayType, minDelay, maxDelay, meanDelay, stdDeviation);

            log.info("[{}] Simulated task [id={}, index={}] sleeping for {} ms", taskName, taskId, taskIndex, delayMs);
            try {
                TimeUnit.MILLISECONDS.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Task interrupted", e);
            }
        }

        if (!shouldTaskSucceed(successRate, failureMode, input)) {
            log.info("[{}] Simulated task [id={}, index={}] failed as configured", taskName, taskId, taskIndex);
            throw new SimulatedTaskException("Simulated task failure based on configuration");
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        Map<String, Object> output = generateOutput(input, taskId, taskIndex, delayMs, elapsedTime, outputSize);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }

    private long calculateDelay(String delayType, int minDelay, int maxDelay, int meanDelay, int stdDeviation) {
        switch (delayType.toLowerCase()) {
            case "fixed":
                return minDelay;

            case "random":
                return minDelay + random.nextInt(Math.max(1, maxDelay - minDelay + 1));

            case "normal":
                double gaussian = random.nextGaussian();
                long delay = Math.round(meanDelay + gaussian * stdDeviation);
                return Math.max(1, delay);

            case "exponential":
                double exp = -meanDelay * Math.log(1 - random.nextDouble());
                return Math.max(minDelay, Math.min(maxDelay, (long) exp));

            default:
                return minDelay;
        }
    }

    private boolean shouldTaskSucceed(double successRate, String failureMode, Map<String, Object> input) {
        Boolean forceSuccess = getOrDefault(input, "forceSuccess", null);
        if (forceSuccess != null) {
            return forceSuccess;
        }

        Boolean forceFail = getOrDefault(input, "forceFail", null);
        if (forceFail != null) {
            return !forceFail;
        }

        switch (failureMode.toLowerCase()) {
            case "random":
                return random.nextDouble() < successRate;

            case "conditional":
                int taskIndex = getOrDefault(input, "taskIndex", -1);
                if (taskIndex >= 0) {
                    Object failIndexesObj = input.get("failIndexes");
                    if (failIndexesObj instanceof Iterable) {
                        for (Object index : (Iterable<?>) failIndexesObj) {
                            if (index != null && index.toString().equals(String.valueOf(taskIndex))) {
                                return false;
                            }
                        }
                    }

                    int failEvery = getOrDefault(input, "failEvery", 0);
                    if (failEvery > 0 && taskIndex % failEvery == 0) {
                        return false;
                    }
                }
                return random.nextDouble() < successRate;

            case "sequential":
                int attempt = getOrDefault(input, "attempt", 1);
                int failUntilAttempt = getOrDefault(input, "failUntilAttempt", 2);
                return attempt >= failUntilAttempt;

            default:
                return random.nextDouble() < successRate;
        }
    }

    private Map<String, Object> generateOutput(Map<String, Object> input, String taskId, int taskIndex, long delayMs,
            long elapsedTimeMs, int outputSize) {
        Map<String, Object> output = new HashMap<>();

        output.put("taskId", taskId);
        output.put("taskIndex", taskIndex);
        output.put("codename", codename);
        output.put("status", "completed");
        output.put("configuredDelayMs", delayMs);
        output.put("actualExecutionTimeMs", elapsedTimeMs);
        output.put("a_or_b", random.nextInt(100) > 20 ? "a" : "b");
        output.put("c_or_d", random.nextInt(100) > 33 ? "c" : "d");

        if (getOrDefault(input, "includeInput", false)) {
            output.put("input", input);
        }

        Object previousTaskOutput = input.get("previousTaskOutput");
        if (previousTaskOutput != null) {
            output.put("previousTaskData", previousTaskOutput);
        }

        if (outputSize > 0) {
            output.put("data", generateRandomData(outputSize));
        }

        Map<String, Object> outputTemplate = getMapOrDefault(input, "outputTemplate");
        if (outputTemplate != null) {
            output.putAll(outputTemplate);
        }

        return output;
    }

    private String generateRandomData(int size) {
        if (size <= 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append(ALPHANUMERIC_CHARS.charAt(random.nextInt(ALPHANUMERIC_CHARS.length())));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private <T> T getOrDefault(Map<String, Object> map, String key, T defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            if (defaultValue instanceof Integer && value instanceof Number) {
                return (T) Integer.valueOf(((Number) value).intValue());
            } else if (defaultValue instanceof Double && value instanceof Number) {
                return (T) Double.valueOf(((Number) value).doubleValue());
            } else if (defaultValue instanceof Boolean) {
                if (value instanceof Boolean) {
                    return (T) value;
                }
                if (value instanceof String) {
                    return (T) Boolean.valueOf(Boolean.parseBoolean((String) value));
                }
            } else if (defaultValue instanceof String) {
                return (T) value.toString();
            }
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapOrDefault(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    public static class SimulatedTaskException extends RuntimeException {

        public SimulatedTaskException(String message) {
            super(message);
        }
    }
}
