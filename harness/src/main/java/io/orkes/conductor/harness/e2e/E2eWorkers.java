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
package io.orkes.conductor.harness.e2e;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** The four end-to-end workers. Each does real work: HTTP calls, JSON processing or a bounded connection pool. */
final class E2eWorkers {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private E2eWorkers() {}

    /** Records each execution in the {@link ExecutionLog} around the worker's own logic. */
    abstract static class InstrumentedWorker implements Worker {

        private final String taskDefName;
        private final String identity;
        private final int pollingInterval;
        private final ExecutionLog executionLog;

        InstrumentedWorker(String taskDefName, String identity, int pollingInterval, ExecutionLog executionLog) {
            this.taskDefName = taskDefName;
            this.identity = identity;
            this.pollingInterval = pollingInterval;
            this.executionLog = executionLog;
        }

        @Override
        public String getTaskDefName() {
            return taskDefName;
        }

        @Override
        public String getIdentity() {
            return identity;
        }

        @Override
        public int getPollingInterval() {
            return pollingInterval;
        }

        @Override
        public final TaskResult execute(Task task) {
            long startMillis = executionLog.start(task);
            String status = "EXCEPTION";
            try {
                TaskResult result = doExecute(task);
                status = result.getStatus().name();
                return result;
            } finally {
                executionLog.finish(task, startMillis, status);
            }
        }

        abstract TaskResult doExecute(Task task);
    }

    /** Calls a shared downstream API; a small fraction of calls fail transiently and are retried by the server. */
    static class FetchProfileWorker extends InstrumentedWorker {

        private final Downstream downstream;
        private final double failureRate;

        FetchProfileWorker(String identity, int pollingInterval, ExecutionLog executionLog, Downstream downstream,
                double failureRate) {
            super(E2eConfig.FETCH_PROFILE, identity, pollingInterval, executionLog);
            this.downstream = downstream;
            this.failureRate = failureRate;
        }

        @Override
        TaskResult doExecute(Task task) {
            if (ThreadLocalRandom.current().nextDouble() < failureRate) {
                throw new IllegalStateException("Injected transient failure");
            }
            long latencyMicros = downstream.call("profile");
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("customerId", task.getInputData().get("id"));
            result.addOutputData("tier", ThreadLocalRandom.current().nextInt(1, 4));
            result.addOutputData("latencyMicros", latencyMicros);
            return result;
        }
    }

    /** Builds, serializes and parses a JSON document whose size comes from the workflow input. */
    static class ScoreWorker extends InstrumentedWorker {

        private final int passes;

        ScoreWorker(String identity, int pollingInterval, ExecutionLog executionLog, int passes) {
            super(E2eConfig.SCORE, identity, pollingInterval, executionLog);
            this.passes = passes;
        }

        @Override
        TaskResult doExecute(Task task) {
            int docKb = ((Number) task.getInputData().getOrDefault("docKb", 64)).intValue();
            long seed = task.getTaskId().hashCode();
            double score = 0;
            List<JsonNode> sample = new ArrayList<>();
            try {
                for (int pass = 0; pass < passes; pass++) {
                    byte[] document = MAPPER.writeValueAsBytes(document(docKb, seed + pass));
                    JsonNode parsed = MAPPER.readTree(document);
                    for (JsonNode record : parsed.get("records")) {
                        score += record.get("amount").asDouble() * (record.get("name").asText().hashCode() & 7);
                        if (sample.size() < 10) {
                            sample.add(record);
                        }
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("score", score);
            result.addOutputData("sample", MAPPER.convertValue(sample, List.class));
            return result;
        }

        private static ObjectNode document(int docKb, long seed) {
            Random random = new Random(seed);
            ObjectNode root = MAPPER.createObjectNode();
            ArrayNode records = root.putArray("records");
            int count = Math.max(1, docKb * 1024 / 160);
            for (int i = 0; i < count; i++) {
                ObjectNode record = records.addObject();
                record.put("id", i);
                record.put("name", "item-" + Long.toHexString(random.nextLong()));
                record.put("amount", random.nextDouble() * 1000);
                record.putArray("tags").add("t" + random.nextInt(50)).add("t" + random.nextInt(50));
            }
            return root;
        }
    }

    /** Writes through a fixed-size connection pool, like a JDBC pool in front of a database. */
    static class PersistWorker extends InstrumentedWorker {

        private final Downstream downstream;
        private final Semaphore connections;

        PersistWorker(String identity, int pollingInterval, ExecutionLog executionLog, Downstream downstream,
                int poolSize) {
            super(E2eConfig.PERSIST, identity, pollingInterval, executionLog);
            this.downstream = downstream;
            this.connections = new Semaphore(poolSize, true);
        }

        @Override
        TaskResult doExecute(Task task) {
            try {
                if (!connections.tryAcquire(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for a connection");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            try {
                downstream.call("persist");
            } finally {
                connections.release();
            }
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("persisted", true);
            return result;
        }
    }

    /** Long-running pattern: reports IN_PROGRESS with a callback until the partner acknowledges, then completes. */
    static class NotifyPartnerWorker extends InstrumentedWorker {

        private final Downstream downstream;
        private final int callbackAfterSeconds;

        NotifyPartnerWorker(String identity, int pollingInterval, ExecutionLog executionLog, Downstream downstream,
                int callbackAfterSeconds) {
            super(E2eConfig.NOTIFY_PARTNER, identity, pollingInterval, executionLog);
            this.downstream = downstream;
            this.callbackAfterSeconds = callbackAfterSeconds;
        }

        @Override
        TaskResult doExecute(Task task) {
            downstream.call("profile");
            TaskResult result = new TaskResult(task);
            Map<String, Object> output = task.getOutputData();
            if (output == null || !output.containsKey("sentAt")) {
                result.setStatus(TaskResult.Status.IN_PROGRESS);
                result.setCallbackAfterSeconds(callbackAfterSeconds);
                result.addOutputData("sentAt", System.currentTimeMillis());
            } else {
                result.setStatus(TaskResult.Status.COMPLETED);
                result.addOutputData("sentAt", output.get("sentAt"));
                result.addOutputData("ackedAt", System.currentTimeMillis());
            }
            return result;
        }
    }

    /** HTTP client for the {@link DependencySimulator}. */
    static class Downstream {

        private final HttpClient client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        private final String baseUrl;

        Downstream(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        long call(String endpoint) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("Downstream " + endpoint + " returned " + response.statusCode());
                }
                return MAPPER.readTree(response.body()).get("latencyMicros").asLong();
            } catch (IOException e) {
                throw new IllegalStateException("Downstream " + endpoint + " failed", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }
}
