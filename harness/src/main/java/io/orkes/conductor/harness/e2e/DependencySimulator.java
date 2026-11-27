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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A downstream service shared by every worker process. Each endpoint serves at most {@code capacity} requests at once;
 * requests beyond that wait for a free server, so latency rises with load the way a real database or API does.
 */
public class DependencySimulator {

    private static final Logger log = LoggerFactory.getLogger(DependencySimulator.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, Endpoint> endpoints = new LinkedHashMap<>();

    public DependencySimulator(Map<String, Endpoint> endpoints) {
        this.endpoints.putAll(endpoints);
    }

    public static void main(String[] args) throws IOException {
        int port = E2eConfig.envInt("SIM_PORT", 8090);
        Map<String, Endpoint> endpoints = new LinkedHashMap<>();
        endpoints.put("profile", new Endpoint(E2eConfig.envInt("SIM_PROFILE_CAPACITY", 10), 100, 0.8));
        endpoints.put("persist", new Endpoint(E2eConfig.envInt("SIM_PERSIST_CAPACITY", 30), 20, 0.5));
        new DependencySimulator(endpoints).start(port);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 1024);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        endpoints.forEach((name, endpoint) -> server.createContext("/" + name, exchange -> serve(exchange, endpoint)));
        server.createContext("/stats", this::stats);
        server.createContext("/control", this::control);
        server.start();
        log.info("Dependency simulator listening on {} with endpoints {}", port, endpoints.keySet());
    }

    private void serve(HttpExchange exchange, Endpoint endpoint) throws IOException {
        long startNanos = System.nanoTime();
        try {
            endpoint.handle();
            long latencyMicros = (System.nanoTime() - startNanos) / 1_000;
            respond(exchange, 200, "{\"latencyMicros\":" + latencyMicros + "}");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            respond(exchange, 503, "{}");
        }
    }

    private void stats(HttpExchange exchange) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        endpoints.forEach((name, endpoint) -> body.put(name, endpoint.snapshot()));
        respond(exchange, 200, MAPPER.writeValueAsString(body));
    }

    private void control(HttpExchange exchange) throws IOException {
        Map<String, String> query = query(exchange.getRequestURI());
        Endpoint endpoint = endpoints.get(query.get("endpoint"));
        if (endpoint == null || !query.containsKey("latencyMultiplier")) {
            respond(exchange, 400, "{\"error\":\"endpoint and latencyMultiplier are required\"}");
            return;
        }
        endpoint.latencyMultiplier = Double.parseDouble(query.get("latencyMultiplier"));
        log.info("Endpoint {} latency multiplier set to {}", query.get("endpoint"), endpoint.latencyMultiplier);
        respond(exchange, 200, "{}");
    }

    private static Map<String, String> query(URI uri) {
        Map<String, String> values = new HashMap<>();
        if (uri.getQuery() == null) {
            return values;
        }
        for (String pair : uri.getQuery().split("&")) {
            int separator = pair.indexOf('=');
            if (separator > 0) {
                values.put(pair.substring(0, separator), pair.substring(separator + 1));
            }
        }
        return values;
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /** A service with {@code capacity} servers and log-normal service time. */
    static class Endpoint {

        private final int capacity;
        private final Semaphore servers;
        private final double medianMillis;
        private final double sigma;
        private final AtomicInteger inFlight = new AtomicInteger();
        private final AtomicInteger maxInFlight = new AtomicInteger();
        private final AtomicLong requests = new AtomicLong();
        private final List<Long> windowLatencyMicros = Collections.synchronizedList(new ArrayList<>());
        private volatile double latencyMultiplier = 1.0;

        Endpoint(int capacity, double medianMillis, double sigma) {
            this.capacity = capacity;
            this.servers = new Semaphore(capacity, true);
            this.medianMillis = medianMillis;
            this.sigma = sigma;
        }

        void handle() throws InterruptedException {
            long startNanos = System.nanoTime();
            int current = inFlight.incrementAndGet();
            maxInFlight.accumulateAndGet(current, Math::max);
            try {
                servers.acquire();
                try {
                    double serviceMillis = medianMillis * latencyMultiplier
                            * Math.exp(sigma * ThreadLocalRandom.current().nextGaussian());
                    Thread.sleep((long) serviceMillis, (int) ((serviceMillis % 1) * 1_000_000));
                } finally {
                    servers.release();
                }
            } finally {
                inFlight.decrementAndGet();
                requests.incrementAndGet();
                windowLatencyMicros.add((System.nanoTime() - startNanos) / 1_000);
            }
        }

        /** Returns counters and the latency distribution since the previous snapshot. */
        Map<String, Object> snapshot() {
            List<Long> latencies;
            synchronized (windowLatencyMicros) {
                latencies = new ArrayList<>(windowLatencyMicros);
                windowLatencyMicros.clear();
            }
            Collections.sort(latencies);
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("capacity", capacity);
            values.put("requests", requests.get());
            values.put("inFlight", inFlight.get());
            values.put("maxInFlight", maxInFlight.getAndSet(inFlight.get()));
            values.put("latencyMultiplier", latencyMultiplier);
            values.put("p50Millis", percentile(latencies, 0.50) / 1000.0);
            values.put("p99Millis", percentile(latencies, 0.99) / 1000.0);
            return values;
        }

        private static long percentile(List<Long> sorted, double quantile) {
            if (sorted.isEmpty()) {
                return 0;
            }
            return sorted.get(Math.min(sorted.size() - 1, (int) Math.ceil(quantile * sorted.size()) - 1));
        }
    }
}
