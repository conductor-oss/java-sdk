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
package org.conductoross.conductor.ai.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conductor agent tuning knobs for the Spring Boot auto-configuration.
 *
 * <p>Server connectivity (URL, auth key/secret) is handled by the Conductor
 * Java SDK's own Spring starter via {@code conductor.*} properties — see
 * {@link io.orkes.conductor.client.spring.OrkesConductorClientAutoConfiguration}.
 * Only the Conductor agent worker-runner settings live here.
 *
 * <pre>{@code
 * # application.properties
 *
 * # Conductor client (from conductor-client-spring):
 * conductor.root-uri=http://localhost:6767/api
 * conductor.security.client.key-id=my-key       # optional
 * conductor.security.client.secret=my-secret    # optional
 *
 * # Conductor agent worker tuning (this class):
 * conductor.agent.worker-poll-interval-ms=100
 * conductor.agent.worker-thread-count=1
 * }</pre>
 */
@ConfigurationProperties(prefix = "conductor.agent")
public class AgentProperties {

    private int workerPollIntervalMs = 100;
    private int workerThreadCount = 1;

    public int getWorkerPollIntervalMs() {
        return workerPollIntervalMs;
    }

    public void setWorkerPollIntervalMs(int workerPollIntervalMs) {
        this.workerPollIntervalMs = workerPollIntervalMs;
    }

    public int getWorkerThreadCount() {
        return workerThreadCount;
    }

    public void setWorkerThreadCount(int workerThreadCount) {
        this.workerThreadCount = workerThreadCount;
    }
}
