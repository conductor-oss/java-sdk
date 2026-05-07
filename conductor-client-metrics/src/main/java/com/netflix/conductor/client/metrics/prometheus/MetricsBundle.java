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

/**
 * One-stop convenience that creates a Prometheus metrics collector (legacy or
 * canonical based on {@code WORKER_CANONICAL_METRICS}) and starts the scrape
 * HTTP endpoint. The collector is ready to be passed to
 * {@link com.netflix.conductor.client.http.ConductorClient.Builder#withMetricsCollector}
 * so downstream clients auto-register themselves.
 *
 * <p>Typical usage:
 * <pre>
 * MetricsBundle bundle = MetricsBundle.create();
 * ConductorClient client = ConductorClient.builder()
 *         .basePath("...")
 *         .withMetricsCollector(bundle.getCollector())
 *         .build();
 * </pre>
 */
public final class MetricsBundle {

    private final AbstractPrometheusMetricsCollector collector;
    private final int port;

    private MetricsBundle(AbstractPrometheusMetricsCollector collector, int port) {
        this.collector = collector;
        this.port = port;
    }

    /**
     * Create the bundle with defaults: factory-selected collector on port 9991.
     */
    public static MetricsBundle create() throws IOException {
        return create(9991);
    }

    /**
     * Create the bundle on the given port. The scrape endpoint is started
     * immediately at {@code /metrics}.
     */
    public static MetricsBundle create(int port) throws IOException {
        return create(port, "/metrics");
    }

    /**
     * Create the bundle on the given port and endpoint path. The scrape
     * endpoint is started immediately.
     */
    public static MetricsBundle create(int port, String endpoint) throws IOException {
        AbstractPrometheusMetricsCollector collector = MetricsCollectorFactory.create();
        collector.startServer(port, endpoint);
        return new MetricsBundle(collector, port);
    }

    public AbstractPrometheusMetricsCollector getCollector() {
        return collector;
    }

    public int getPort() {
        return port;
    }
}
