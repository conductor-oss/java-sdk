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
package com.netflix.conductor.client.metrics.prometheus;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.netflix.conductor.client.metrics.MetricsCollector;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Shared base for Prometheus-backed {@link MetricsCollector} implementations.
 * Owns the reference to a {@link PrometheusMeterRegistry} and the built-in
 * HTTP scrape server so both legacy and canonical implementations share the
 * same plumbing.
 *
 * <p>Each concrete subclass holds its own {@code static final} registry so
 * that multiple instances of the same subclass share a single registry (as
 * the original 4.0.x {@code PrometheusMetricsCollector} did), while legacy
 * and canonical registries remain isolated from each other.
 */
public abstract class AbstractPrometheusMetricsCollector implements MetricsCollector {

    protected final PrometheusMeterRegistry registry;

    private boolean autoWiringEnabled = false;
    private boolean activeWorkersTrackingEnabled = false;
    private boolean diagnosticEventsEnabled = false;

    protected AbstractPrometheusMetricsCollector(PrometheusMeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean isAutoWiringEnabled() {
        return autoWiringEnabled;
    }

    @Override
    public void setAutoWiringEnabled(boolean enabled) {
        this.autoWiringEnabled = enabled;
    }

    @Override
    public boolean isActiveWorkersTrackingEnabled() {
        return activeWorkersTrackingEnabled;
    }

    @Override
    public void setActiveWorkersTrackingEnabled(boolean enabled) {
        this.activeWorkersTrackingEnabled = enabled;
    }

    @Override
    public boolean isDiagnosticEventsEnabled() {
        return diagnosticEventsEnabled;
    }

    @Override
    public void setDiagnosticEventsEnabled(boolean enabled) {
        this.diagnosticEventsEnabled = enabled;
    }

    private static final int DEFAULT_PORT = 9991;
    private static final String DEFAULT_ENDPOINT = "/metrics";

    public void startServer() throws IOException {
        startServer(DEFAULT_PORT, DEFAULT_ENDPOINT);
    }

    public void startServer(int port, String endpoint) throws IOException {
        var server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(endpoint, exchange -> {
            var body = registry.scrape();
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, body.getBytes().length);
            try (var os = exchange.getResponseBody()) {
                os.write(body.getBytes());
            }
        });
        server.start();
    }

    public abstract String collectorName();

    public PrometheusMeterRegistry getRegistry() {
        return registry;
    }

    protected static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * Produce a bounded-cardinality label value for an exception. Uses the
     * simple class name so that the label space stays small.
     */
    protected static String exceptionLabel(Throwable t) {
        if (t == null) {
            return "";
        }
        Throwable cause = t;
        if (cause.getCause() != null && (
                cause instanceof java.util.concurrent.ExecutionException
                || cause instanceof java.util.concurrent.CompletionException
                || cause instanceof java.lang.reflect.InvocationTargetException)) {
            cause = cause.getCause();
        }
        String simple = cause.getClass().getSimpleName();
        if (simple == null || simple.isEmpty()) {
            String fqn = cause.getClass().getName();
            int dot = fqn.lastIndexOf('.');
            return dot < 0 ? fqn : fqn.substring(dot + 1);
        }
        return simple;
    }
}
