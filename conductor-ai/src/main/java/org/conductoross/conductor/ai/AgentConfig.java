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

import java.util.function.Function;

/**
 * Worker-runner tuning for the Agentspan SDK.
 *
 * <p>Connection details (server URL, auth key/secret) are NOT here — those are
 * transport concerns owned by the Conductor client
 * ({@code io.orkes.conductor.client.ApiClient}). Build one with
 * {@code ApiClient.builder()} (or {@code new ApiClient()} for environment-based
 * resolution) and pass it to {@link AgentRuntime}. This class carries only how
 * the local worker runner and runtime-owned background threads behave.
 *
 * <p>Environment variables (invalid or empty values fall back to the default):
 * <ul>
 *   <li>{@code AGENTSPAN_WORKER_POLL_INTERVAL} — worker poll interval in ms (default: 100)</li>
 *   <li>{@code AGENTSPAN_WORKER_THREADS} — worker thread count (default: 1)</li>
 *   <li>{@code AGENTSPAN_AUTO_START_WORKERS} — register + start workers on run/start/stream (default: true)</li>
 *   <li>{@code AGENTSPAN_DAEMON_WORKERS} — SDK-owned threads are daemons (default: true)</li>
 *   <li>{@code AGENTSPAN_STREAMING_ENABLED} — use SSE for stream(); false = status polling (default: true)</li>
 *   <li>{@code AGENTSPAN_LIVENESS_ENABLED} — monitor stateful runs for worker stalls (default: true)</li>
 *   <li>{@code AGENTSPAN_LIVENESS_STALL_SECONDS} — seconds a task may sit unpolled before it counts
 *       as a stall (default: 30.0)</li>
 *   <li>{@code AGENTSPAN_LIVENESS_CHECK_INTERVAL_SECONDS} — seconds between liveness checks
 *       (default: 10.0)</li>
 * </ul>
 */
public class AgentConfig {
    private final int workerPollIntervalMs;
    private final int workerThreadCount;

    private boolean autoStartWorkers = true;
    private boolean daemonWorkers = true;
    private boolean streamingEnabled = true;
    private boolean livenessEnabled = true;
    private double livenessStallSeconds = 30.0;
    private double livenessCheckIntervalSeconds = 10.0;

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
        return fromEnv(System::getenv);
    }

    /** Env-seam variant so tests can exercise parsing without mutating process env. */
    static AgentConfig fromEnv(Function<String, String> env) {
        AgentConfig config = new AgentConfig(
                intVar(env, "AGENTSPAN_WORKER_POLL_INTERVAL", 100),
                intVar(env, "AGENTSPAN_WORKER_THREADS", 1));
        config.autoStartWorkers = boolVar(env, "AGENTSPAN_AUTO_START_WORKERS", true);
        config.daemonWorkers = boolVar(env, "AGENTSPAN_DAEMON_WORKERS", true);
        config.streamingEnabled = boolVar(env, "AGENTSPAN_STREAMING_ENABLED", true);
        config.livenessEnabled = boolVar(env, "AGENTSPAN_LIVENESS_ENABLED", true);
        config.livenessStallSeconds = doubleVar(env, "AGENTSPAN_LIVENESS_STALL_SECONDS", 30.0);
        config.livenessCheckIntervalSeconds = doubleVar(env, "AGENTSPAN_LIVENESS_CHECK_INTERVAL_SECONDS", 10.0);
        return config;
    }

    // ── lenient env parsing: invalid or empty values → default ────────────

    private static int intVar(Function<String, String> env, String key, int defaultValue) {
        String val = env.apply(key);
        if (val == null || val.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double doubleVar(Function<String, String> env, String key, double defaultValue) {
        String val = env.apply(key);
        if (val == null || val.trim().isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean boolVar(Function<String, String> env, String key, boolean defaultValue) {
        String val = env.apply(key);
        if (val == null || val.trim().isEmpty()) return defaultValue;
        String normalized = val.trim().toLowerCase();
        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)) return true;
        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) return false;
        return defaultValue;
    }

    public int getWorkerPollIntervalMs() {
        return workerPollIntervalMs;
    }

    public int getWorkerThreadCount() {
        return workerThreadCount;
    }

    /** When false, run/start/stream skip worker registration + polling ({@code serve} always starts). */
    public boolean isAutoStartWorkers() {
        return autoStartWorkers;
    }

    public AgentConfig autoStartWorkers(boolean autoStartWorkers) {
        this.autoStartWorkers = autoStartWorkers;
        return this;
    }

    /**
     * Whether SDK-owned background threads (SSE reader, liveness monitor) run as
     * daemons and so never keep the JVM alive. The task-runner threads are owned
     * by {@code TaskRunnerConfigurer}, which does not expose a daemon flag —
     * this knob does not apply to them.
     */
    public boolean isDaemonWorkers() {
        return daemonWorkers;
    }

    public AgentConfig daemonWorkers(boolean daemonWorkers) {
        this.daemonWorkers = daemonWorkers;
        return this;
    }

    /** When false, {@code stream()} skips SSE entirely and degrades to status polling. */
    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }

    public AgentConfig streamingEnabled(boolean streamingEnabled) {
        this.streamingEnabled = streamingEnabled;
        return this;
    }

    /** When true, stateful runs are monitored for worker stalls (spec R11). */
    public boolean isLivenessEnabled() {
        return livenessEnabled;
    }

    public AgentConfig livenessEnabled(boolean livenessEnabled) {
        this.livenessEnabled = livenessEnabled;
        return this;
    }

    /** Seconds a {@code SCHEDULED} task may sit with zero polls before it counts as a stall. */
    public double getLivenessStallSeconds() {
        return livenessStallSeconds;
    }

    public AgentConfig livenessStallSeconds(double livenessStallSeconds) {
        this.livenessStallSeconds = livenessStallSeconds;
        return this;
    }

    /** Seconds between liveness checks. */
    public double getLivenessCheckIntervalSeconds() {
        return livenessCheckIntervalSeconds;
    }

    public AgentConfig livenessCheckIntervalSeconds(double livenessCheckIntervalSeconds) {
        this.livenessCheckIntervalSeconds = livenessCheckIntervalSeconds;
        return this;
    }

    @Override
    public String toString() {
        return "AgentConfig{workerPollIntervalMs=" + workerPollIntervalMs
                + ", workerThreadCount=" + workerThreadCount
                + ", autoStartWorkers=" + autoStartWorkers
                + ", daemonWorkers=" + daemonWorkers
                + ", streamingEnabled=" + streamingEnabled
                + ", livenessEnabled=" + livenessEnabled
                + ", livenessStallSeconds=" + livenessStallSeconds
                + ", livenessCheckIntervalSeconds=" + livenessCheckIntervalSeconds
                + "}";
    }
}
