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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.http.TaskClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Samples the system once per second from three independent sources: server queue sizes, the dependency simulator,
 * and each worker's Prometheus endpoint. Rows are kept in memory for the analysis and written to a CSV.
 */
class E2eSampler {

    private static final Logger log = LoggerFactory.getLogger(E2eSampler.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern METRIC_LINE = Pattern.compile("^([a-zA-Z_:][a-zA-Z0-9_:]*)(\\{(.*)})?\\s+(\\S+)$");
    private static final Pattern LABEL = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)=\"([^\"]*)\"");

    private final TaskClient taskClient;
    private final List<String> queueNames;
    private final String simulatorUrl;
    private final List<String> workerUrls;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "e2e-sampler");
        t.setDaemon(true);
        return t;
    });
    private final List<Sample> samples = Collections.synchronizedList(new ArrayList<>());
    private final long startMillis = System.currentTimeMillis();
    private volatile String phase = "setup";
    private volatile Map<String, Double> counters = Map.of();

    E2eSampler(TaskClient taskClient, String simulatorUrl, List<String> workerUrls) {
        this(taskClient, E2eConfig.QUEUE_NAMES, simulatorUrl, workerUrls);
    }

    /**
     * @param simulatorUrl the dependency simulator, or null when the run has none
     */
    E2eSampler(TaskClient taskClient, List<String> queueNames, String simulatorUrl, List<String> workerUrls) {
        this.taskClient = taskClient;
        this.queueNames = queueNames;
        this.simulatorUrl = simulatorUrl;
        this.workerUrls = workerUrls;
    }

    /** One row: wall-clock time, phase, and named values. */
    record Sample(long epochMillis, double seconds, String phase, Map<String, Double> values) {

        double get(String name) {
            return values.getOrDefault(name, Double.NaN);
        }
    }

    void start() {
        scheduler.scheduleAtFixedRate(this::sample, 0, 1, TimeUnit.SECONDS);
    }

    void stop() {
        scheduler.shutdownNow();
    }

    void setPhase(String phase) {
        this.phase = phase;
        log.info("Phase {}", phase);
    }

    /** Driver-side counters (workflows started, start errors) recorded with each sample. */
    void setCounters(Map<String, Double> counters) {
        this.counters = counters;
    }

    List<Sample> samples() {
        synchronized (samples) {
            return new ArrayList<>(samples);
        }
    }

    Sample latest() {
        synchronized (samples) {
            return samples.isEmpty() ? null : samples.get(samples.size() - 1);
        }
    }

    int workerCount() {
        return workerUrls.size();
    }

    private void sample() {
        long now = System.currentTimeMillis();
        Map<String, Double> values = new LinkedHashMap<>(counters);
        try {
            Map<String, Integer> sizes = taskClient.getQueueSizesForTasks(queueNames);
            for (String queue : queueNames) {
                values.put("depth." + queue, sizes.getOrDefault(queue, 0).doubleValue());
            }
        } catch (Exception e) {
            log.warn("Queue size sample failed: {}", e.getMessage());
        }
        if (simulatorUrl != null) {
            try {
                JsonNode stats = MAPPER.readTree(get(simulatorUrl + "/stats"));
                stats.fields().forEachRemaining(endpoint -> endpoint.getValue().fields().forEachRemaining(
                        field -> values.put("sim." + endpoint.getKey() + "." + field.getKey(),
                                field.getValue().asDouble())));
            } catch (Exception e) {
                log.warn("Simulator sample failed: {}", e.getMessage());
            }
        }
        for (int i = 0; i < workerUrls.size(); i++) {
            try {
                sampleWorker("w" + i + ".", get(workerUrls.get(i) + "/metrics"), values);
                values.put("w" + i + ".up", 1.0);
            } catch (Exception e) {
                values.put("w" + i + ".up", 0.0);
                log.warn("Worker {} sample failed: {}", i, e.getMessage());
            }
        }
        samples.add(new Sample(now, (now - startMillis) / 1000.0, phase, values));
    }

    private static void sampleWorker(String prefix, String body, Map<String, Double> values) {
        for (String line : body.split("\n")) {
            if (line.startsWith("#")) {
                continue;
            }
            Matcher matcher = METRIC_LINE.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            String name = matcher.group(1);
            Map<String, String> labels = labels(matcher.group(3));
            double value;
            try {
                value = Double.parseDouble(matcher.group(4));
            } catch (NumberFormatException e) {
                continue;
            }
            String taskType = labels.getOrDefault("taskType", labels.get("task_type"));
            switch (name) {
                case "adaptive_process_concurrency_target" -> values.put(prefix + "processTarget", value);
                case "adaptive_process_held_slots" -> values.put(prefix + "processHeld", value);
                case "adaptive_admission_paused" -> values.put(prefix + "paused", value);
                case "process_cpu_usage" -> values.put(prefix + "cpu", value);
                case "jvm_threads_live_threads" -> values.put(prefix + "threads", value);
                case "e2e_process_inflight" -> values.put(prefix + "inflight", value);
                case "e2e_process_inflight_max" -> values.put(prefix + "inflightMax", value);
                case "adaptive_worker_concurrency_target" -> values.put(prefix + taskType + ".target", value);
                case "adaptive_worker_held_slots" -> values.put(prefix + taskType + ".held", value);
                case "adaptive_queue_depth" -> values.put(prefix + taskType + ".depth", value);
                case "e2e_inflight" -> values.put(prefix + taskType + ".inflight", value);
                case "e2e_inflight_max" -> values.put(prefix + taskType + ".inflightMax", value);
                case "e2e_pool_threads" -> values.put(prefix + taskType + ".threads", value);
                case "jvm_gc_live_data_size_bytes" -> values.put(prefix + "liveDataBytes", value);
                case "jvm_gc_max_data_size_bytes" -> values.put(prefix + "maxDataBytes", value);
                case "task_poll_total" -> values.merge(prefix + taskType + ".polls", value, Double::sum);
                case "task_execution_started_total" -> values.merge(prefix + taskType + ".started", value,
                        Double::sum);
                case "adaptive_concurrency_changes_total" -> values.merge(
                        prefix + "reason." + labels.get("reason"), value, Double::sum);
                case "jvm_memory_used_bytes" -> {
                    if ("heap".equals(labels.get("area"))) {
                        values.merge(prefix + "heapUsedMb", value / 1_048_576, Double::sum);
                    }
                }
                case "jvm_memory_max_bytes" -> {
                    if ("heap".equals(labels.get("area")) && value > 0) {
                        values.merge(prefix + "heapMaxMb", value / 1_048_576, Double::sum);
                    }
                }
                case "jvm_gc_pause_seconds_sum" -> values.merge(prefix + "gcSeconds", value, Double::sum);
                case "http_api_client_request_seconds_count" -> {
                    String uri = labels.getOrDefault("uri", "");
                    String key = uri.contains("queue/sizes") ? "req.queueSizes"
                            : uri.contains("/poll") ? "req.poll"
                            : uri.startsWith("/tasks") ? "req.update" : "req.other";
                    values.merge(prefix + key, value, Double::sum);
                }
                default -> {
                }
            }
        }
    }

    private static Map<String, String> labels(String text) {
        Map<String, String> labels = new HashMap<>();
        if (text != null) {
            Matcher matcher = LABEL.matcher(text);
            while (matcher.find()) {
                labels.put(matcher.group(1), matcher.group(2));
            }
        }
        return labels;
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(url + " returned " + response.statusCode());
        }
        return response.body();
    }

    void writeCsv(Path file) throws IOException {
        List<Sample> rows = samples();
        List<String> columns = new ArrayList<>();
        for (Sample row : rows) {
            for (String key : row.values().keySet()) {
                if (!columns.contains(key)) {
                    columns.add(key);
                }
            }
        }
        StringBuilder out = new StringBuilder("epochMillis,seconds,phase");
        columns.forEach(column -> out.append(',').append(column));
        out.append('\n');
        for (Sample row : rows) {
            out.append(row.epochMillis()).append(',').append(String.format("%.1f", row.seconds())).append(',')
                    .append(row.phase());
            for (String column : columns) {
                Double value = row.values().get(column);
                out.append(',').append(value == null ? "" : String.format("%.3f", value));
            }
            out.append('\n');
        }
        Files.writeString(file, out);
    }
}
