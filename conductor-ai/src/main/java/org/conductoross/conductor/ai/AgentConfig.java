/*
 * Copyright 2025 Conductor Authors.
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
package org.conductoross.conductor.ai;

/**
 * Worker-runner tuning for the Agentspan SDK.
 *
 * <p>Connection details (server URL, auth key/secret) are NOT here — those are
 * transport concerns owned by the Conductor client
 * ({@code io.orkes.conductor.client.ApiClient}). Build one with
 * {@link AgentRuntime#clientFromEnv()} / {@link AgentRuntime#client(String)} and
 * pass it to {@link AgentRuntime}. This class carries only how the local worker
 * runner behaves.
 *
 * <p>Environment variables:
 * <ul>
 *   <li>{@code AGENTSPAN_WORKER_POLL_INTERVAL} — worker poll interval in ms (default: 100)</li>
 *   <li>{@code AGENTSPAN_WORKER_THREADS} — worker thread count (default: 1)</li>
 * </ul>
 */
public class AgentConfig {
    private final int workerPollIntervalMs;
    private final int workerThreadCount;

    /**
     * Create worker tuning with explicit values.
     *
     * @param workerPollIntervalMs worker poll interval in milliseconds (≤0 → 100)
     * @param workerThreadCount    number of worker threads (≤0 → 1)
     */
    public AgentConfig(int workerPollIntervalMs, int workerThreadCount) {
        this.workerPollIntervalMs = workerPollIntervalMs > 0 ? workerPollIntervalMs : 100;
        this.workerThreadCount = workerThreadCount > 0 ? workerThreadCount : 1;
    }

    /** Default worker tuning (poll 100ms, 1 thread). */
    public AgentConfig() {
        this(100, 1);
    }

    /** Load worker tuning from environment variables with sensible defaults. */
    public static AgentConfig fromEnv() {
        return new AgentConfig(
                Integer.parseInt(env("AGENTSPAN_WORKER_POLL_INTERVAL", "100")),
                Integer.parseInt(env("AGENTSPAN_WORKER_THREADS", "1")));
    }

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return val != null ? val : defaultValue;
    }

    public int getWorkerPollIntervalMs() {
        return workerPollIntervalMs;
    }

    public int getWorkerThreadCount() {
        return workerThreadCount;
    }

    @Override
    public String toString() {
        return "AgentConfig{workerPollIntervalMs=" + workerPollIntervalMs + ", workerThreadCount=" + workerThreadCount
                + "}";
    }
}
